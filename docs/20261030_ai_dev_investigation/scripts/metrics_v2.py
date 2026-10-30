"""Reproducible AI-development metrics from local transcripts (v2).

Usage:
  python -I metrics_v2.py claude <session.jsonl> [<subagents_dir>] [--since ISO] [--until ISO]
  python -I metrics_v2.py codex <rollout.jsonl> [<rollout.jsonl> ...]

Everything printed is computed from the files. Definitions:
- tokens: Anthropic usage fields per unique assistant message id (last streamed row wins).
- api_equiv_usd: tokens x list price (prices_anthropic_20261030.json). NOT a bill.
- model_s: per message, last row ts - latest preceding user/tool_result ts.
- tool_s: tool_result ts - tool_use ts (includes Pre/PostToolUse hook time). Human-wait tools
  (AskUserQuestion, ExitPlanMode) are reported separately.
- stop_hook_s: stop_hook_summary hookInfos[].durationMs for project hooks (bash ...).
- human_gap_s: last assistant row of a turn -> next human prompt (approval/away/other work not separable).
- gradle: tool calls whose command has an executable gradlew segment (see analyze3.gradle_invocations).
- retry: a gradle call that follows a FAILED gradle call with no human prompt in between.
- failure_loop_usd: api_equiv of messages that start after a FAILED gradle result and before the
  next OK gradle call in the same turn (definition-based estimate, not causal proof).
- reinjection_char_calls: sum over edited_text_file attachments of snippet chars x number of later
  API calls before the next compact boundary (an index in chars, no token conversion).
"""
import json, sys, os, glob, re, statistics, collections, datetime

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from analyze3 import gradle_invocations  # noqa: E402

PRICES = json.load(open(os.path.join(HERE, "prices_anthropic_20261030.json"), encoding="utf-8"))
HUMAN_TOOLS = {"AskUserQuestion", "ExitPlanMode"}


def ts(s):
    return datetime.datetime.fromisoformat(s.replace("Z", "+00:00"))


def pct(v, p):
    s = sorted(v)
    return s[max(0, min(len(s) - 1, int(round(p / 100 * (len(s) - 1)))))]


def summ(v, unit=""):
    if not v:
        return "n=0"
    return f"n={len(v)} median={statistics.median(v):.1f}{unit} p95(rank)={pct(v,95):.1f}{unit} sum={sum(v):.1f}{unit}"


def price_of(model, u):
    p = PRICES.get(model)
    if not isinstance(p, list):
        return None
    cc = u.get("cache_creation") or {}
    w5 = cc.get("ephemeral_5m_input_tokens")
    w1 = cc.get("ephemeral_1h_input_tokens")
    total_w = u.get("cache_creation_input_tokens", 0) or 0
    if w5 is None and w1 is None:
        w5, w1 = total_w, 0  # unknown split: assume 5m (lower bound), flagged by caller
    prompt = (u.get("input_tokens", 0) or 0) + (u.get("cache_read_input_tokens", 0) or 0) + total_w
    if model == "claude-haiku-5-5" and prompt > 100_000:
        p = [0.50, 0.625, 1.0, 0.05, 2.50]
    return ((u.get("input_tokens", 0) or 0) * p[0] + (w5 or 0) * p[1] + (w1 or 0) * p[2]
            + (u.get("cache_read_input_tokens", 0) or 0) * p[3] + (u.get("output_tokens", 0) or 0) * p[4]) / 1e6


def load(path):
    out = []
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            try:
                out.append(json.loads(line))
            except Exception:
                pass
    return out


def claude(path, sub_dir=None, since=None, until=None):
    rows = [r for r in load(path) if not r.get("isSidechain")]
    if since or until:
        rows = [r for r in rows if not r.get("timestamp") or ((not since or r["timestamp"] >= since) and (not until or r["timestamp"] <= until))]
    stamps = [r["timestamp"] for r in rows if r.get("timestamp")]
    print(f"### claude main: {os.path.basename(path)}  period={min(stamps)}..{max(stamps)}")
    versions = collections.Counter(r.get("version") for r in rows if r.get("version"))
    print("cli versions:", dict(versions))
    # --- messages, tokens, model time
    msgs = {}
    last_input = None
    for r in rows:
        t = r.get("timestamp")
        if r.get("type") == "user" and t:
            last_input = t
        if r.get("type") == "assistant" and t:
            m = r.get("message", {})
            mid = m.get("id")
            e = msgs.setdefault(mid, {"model": m.get("model"), "start": last_input, "end": t, "usage": {}})
            e["end"] = t
            e["usage"] = m.get("usage") or e["usage"]
    tok = collections.defaultdict(collections.Counter)
    cost = collections.Counter()
    unknown_split = 0
    for e in msgs.values():
        u = e["usage"]
        cc = u.get("cache_creation") or {}
        t = tok[e["model"]]
        t["calls"] += 1
        t["input"] += u.get("input_tokens", 0) or 0
        t["output"] += u.get("output_tokens", 0) or 0
        t["cache_read"] += u.get("cache_read_input_tokens", 0) or 0
        t["cache_write_5m"] += cc.get("ephemeral_5m_input_tokens", 0) or 0
        t["cache_write_1h"] += cc.get("ephemeral_1h_input_tokens", 0) or 0
        if not cc and (u.get("cache_creation_input_tokens") or 0):
            unknown_split += 1
        c = price_of(e["model"], u)
        e["usd"] = c or 0.0
        if c is not None:
            cost[e["model"]] += c
    for model, t in tok.items():
        print(f"tokens[{model}]: {dict(t)}  api_equiv_usd={cost.get(model, 0):.2f}")
    if unknown_split:
        print(f"note: {unknown_split} messages lacked the 5m/1h cache split (priced as 5m = lower bound)")
    # Critic v2 N04: this is a log-timestamp response-wait interval (not server-side model time); exclude <synthetic>.
    model_s = [(ts(e["end"]) - ts(e["start"])).total_seconds() for e in msgs.values()
               if e["start"] and e["end"] >= e["start"] and e["model"] != "<synthetic>"]
    print("response_wait_s per API call (log-timestamp estimate, synthetic excluded):", summ(model_s, "s"))
    # --- tools
    uses = {}
    for r in rows:
        if r.get("type") == "assistant":
            for c in r.get("message", {}).get("content", []) or []:
                if isinstance(c, dict) and c.get("type") == "tool_use":
                    uses[c["id"]] = (r.get("timestamp"), c.get("name"), c.get("input") or {})
    tool_s = collections.defaultdict(list)
    results = {}
    big = []
    for r in rows:
        if r.get("type") != "user" or not r.get("timestamp"):
            continue
        cont = r.get("message", {}).get("content")
        if not isinstance(cont, list):
            continue
        for c in cont:
            if isinstance(c, dict) and c.get("type") == "tool_result" and c.get("tool_use_id") in uses and c["tool_use_id"] not in results:
                t0, name, inp = uses[c["tool_use_id"]]
                d = (ts(r["timestamp"]) - ts(t0)).total_seconds()
                body = c.get("content")
                if isinstance(body, list):
                    body = " ".join(x.get("text", "") for x in body if isinstance(x, dict))
                body = str(body)
                results[c["tool_use_id"]] = (t0, r["timestamp"], name, inp, d, body)
                tool_s[name].append(d)
                big.append((len(body), name, (inp.get("command") or inp.get("file_path") or inp.get("url") or "")[:70]))
    work = [d for n, v in tool_s.items() if n not in HUMAN_TOOLS for d in v]
    wait = [d for n, v in tool_s.items() if n in HUMAN_TOOLS for d in v]
    print("tool_s (non-human tools):", summ(work, "s"))
    print("human_wait_in_tools_s (AskUserQuestion/ExitPlanMode):", summ(wait, "s"))
    total_chars = sum(b[0] for b in big)
    print(f"tool_result chars total={total_chars:,}; top5:", [(n, name, cmd) for n, name, cmd in sorted(big, reverse=True)[:5]])
    # --- hooks
    stop = []
    for r in rows:
        if r.get("subtype") == "stop_hook_summary":
            for h in r.get("hookInfos", []):
                if str(h.get("command", "")).startswith("bash") and "durationMs" in h:
                    stop.append(h["durationMs"] / 1000)
    print("stop_hook_s (project):", summ(stop, "s"))
    herr = collections.Counter()
    for r in rows:
        a = r.get("attachment") if isinstance(r.get("attachment"), dict) else {}
        if str(a.get("type", "")).startswith("hook_") and str(a.get("command", "")).startswith("bash"):
            herr[(a.get("type"), a.get("hookName"))] += 1
    print("project hook records:", dict(herr))
    # --- turns / human gaps
    ev = []
    for r in rows:
        t = r.get("timestamp")
        if not t:
            continue
        if r.get("type") == "user":
            c = r.get("message", {}).get("content")
            isp = isinstance(c, str) or (isinstance(c, list) and any(isinstance(x, dict) and x.get("type") == "text" for x in c) and not any(isinstance(x, dict) and x.get("type") == "tool_result" for x in c))
            if isp and not r.get("isMeta") and not r.get("isCompactSummary"):
                ev.append(("p", ts(t)))
        elif r.get("type") == "assistant":
            ev.append(("a", ts(t)))
    ev.sort(key=lambda x: x[1])
    turns, gaps, cur, last = [], [], None, None
    for k, t in ev:
        if k == "p":
            if cur and last:
                turns.append((last - cur).total_seconds())
                gaps.append((t - last).total_seconds())
            cur, last = t, None
        else:
            last = t
    if cur and last:
        turns.append((last - cur).total_seconds())
    print("turn_elapsed_s (prompt->last assistant row):", summ([x for x in turns if x >= 0], "s"))
    print("human_gap_s (all, no cap):", summ([g for g in gaps if g >= 0], "s"))
    # --- gradle, retries, failure loops
    prompts = sorted(t for k, t in ev if k == "p")
    gcalls = []
    for tid, (t0, t1, name, inp, d, body) in results.items():
        if name not in ("Bash", "PowerShell"):
            continue
        inv = gradle_invocations(inp.get("command", ""))
        if not inv:
            continue
        res = "FAILED" if ("BUILD FAILED" in body or re.search(r"tests completed, \d+ failed", body)) else ("OK" if "BUILD SUCCESSFUL" in body else "?")
        gcalls.append((ts(t0), ts(t1), d, res, len(body)))
    gcalls.sort()
    print("gradle tool calls (mixed time):", summ([g[2] for g in gcalls], "s"), "results:", dict(collections.Counter(g[3] for g in gcalls)),
          f"output chars total={sum(g[4] for g in gcalls):,}")

    def same_turn(a, b):
        return not any(a < p <= b for p in prompts)
    retries = [g for i, g in enumerate(gcalls) if i > 0 and gcalls[i - 1][3] == "FAILED" and same_turn(gcalls[i - 1][1], g[0])]
    print("gradle retries after FAILED (same turn):", summ([g[2] for g in retries], "s"))
    # Critic v2 M08: windows overlap when FAILED calls repeat; count each message once (union of windows).
    in_loop = set()
    memberships = 0
    for i, g in enumerate(gcalls):
        if g[3] != "FAILED":
            continue
        nxt_ok = next((h for h in gcalls[i + 1:] if h[3] == "OK" and same_turn(g[1], h[0])), None)
        if not nxt_ok:
            continue
        for mid, e in msgs.items():
            if e["start"] and g[1] <= ts(e["start"]) < nxt_ok[0]:
                in_loop.add(mid)
                memberships += 1
    loop_usd = sum(msgs[m]["usd"] for m in in_loop)
    print(f"failure_loop (FAILED -> next OK, same turn, deduplicated): api_calls={len(in_loop)} api_equiv_usd={loop_usd:.2f} (window memberships incl. overlaps={memberships})")
    # --- re-injection index
    compact = sorted(ts(r["timestamp"]) for r in rows if r.get("subtype") == "compact_boundary" and r.get("timestamp"))
    starts = sorted(ts(e["start"]) for e in msgs.values() if e["start"])
    n_att, chars, idx = 0, 0, 0
    for r in rows:
        a = r.get("attachment") if isinstance(r.get("attachment"), dict) else {}
        if r.get("type") == "attachment" and a.get("type") == "edited_text_file" and r.get("timestamp"):
            t = ts(r["timestamp"])
            c = len(str(a.get("snippet", "")))
            limit = next((b for b in compact if b > t), None)
            k = sum(1 for s in starts if s > t and (limit is None or s < limit))
            n_att += 1
            chars += c
            idx += c * k
    print(f"edited_text_file: n={n_att} snippet_chars={chars:,} reinjection_char_calls={idx:,}")
    # --- subagents
    if sub_dir:
        durs, sub_cost = [], 0.0
        for jp in glob.glob(os.path.join(sub_dir, "*.jsonl")):
            sr = load(jp)
            st = [ts(r["timestamp"]) for r in sr if r.get("timestamp")]
            if st:
                durs.append((max(st) - min(st)).total_seconds() / 60)
            seen = {}
            for r in sr:
                if r.get("type") == "assistant":
                    m = r.get("message", {})
                    seen[m.get("id")] = (m.get("model"), m.get("usage") or {})
            sub_cost += sum((price_of(mo, u) or 0) for mo, u in seen.values())
        print("subagents minutes:", summ(durs, "m"), f"api_equiv_usd={sub_cost:.2f}")


def codex(paths):
    for p in paths:
        cwd, first, last, usage = None, None, None, None
        for r in load(p):
            t = r.get("timestamp")
            if t:
                first = first or t
                last = t
            pl = r.get("payload") if isinstance(r.get("payload"), dict) else {}
            cwd = cwd or pl.get("cwd")
            if pl.get("type") == "token_count" and pl.get("info"):
                usage = pl["info"].get("total_token_usage")
        mins = (ts(last) - ts(first)).total_seconds() / 60 if first and last else 0
        print(f"### codex {os.path.basename(p)[:44]} cwd={cwd} period={first}..{last} ({mins:.1f} min) usage={usage} (price: not published for this model in the checked sources)")


if __name__ == "__main__":
    a = sys.argv[1:]
    since = until = None
    if "--since" in a:
        since = a[a.index("--since") + 1]
        del a[a.index("--since"):a.index("--since") + 2]
    if "--until" in a:
        until = a[a.index("--until") + 1]
        del a[a.index("--until"):a.index("--until") + 2]
    if a[0] == "claude":
        claude(a[1], a[2] if len(a) > 2 else None, since, until)
    else:
        codex(a[1:])
