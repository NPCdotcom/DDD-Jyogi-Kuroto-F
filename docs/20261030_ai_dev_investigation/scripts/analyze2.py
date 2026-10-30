"""Second pass: dedup tokens, precise gradle classification, subagents, turn timing."""
import json, sys, statistics, re, collections, datetime, glob, os

def pct(vals, p):
    s = sorted(vals)
    return s[max(0, min(len(s) - 1, int(round(p / 100 * (len(s) - 1)))))]

def summ(vals, unit=""):
    if not vals:
        return "n=0"
    return f"n={len(vals)} median={statistics.median(vals):.1f}{unit} p95={pct(vals,95):.1f}{unit} max={max(vals):.1f}{unit} sum={sum(vals):.1f}{unit}"

def ts(s):
    return datetime.datetime.fromisoformat(s.replace("Z", "+00:00"))

def load(path):
    out = []
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            try:
                out.append(json.loads(line))
            except Exception:
                pass
    return out

def tokens(rows):
    seen = {}
    for r in rows:
        if r.get("type") != "assistant":
            continue
        m = r.get("message", {})
        mid = m.get("id") or r.get("requestId") or r.get("uuid")
        u = m.get("usage") or {}
        seen[(m.get("model"), mid)] = u  # last write wins (final usage of the streamed message)
    agg = collections.defaultdict(collections.Counter)
    for (model, _), u in seen.items():
        a = agg[model]
        a["api_calls"] += 1
        for k in ("input_tokens", "output_tokens", "cache_read_input_tokens", "cache_creation_input_tokens"):
            a[k] += u.get(k, 0) or 0
    return agg

GRADLE_RE = re.compile(r"(?:^|[;&|(]\s*|\s)(?:&\s*)?['\"]?(?:\.[/\\])?gradlew(?:\.bat)?['\"]?\s+([^|;&\n\"]*)")

def gradle_calls(rows):
    tool_use = {}
    for r in rows:
        if r.get("type") == "assistant":
            for c in r.get("message", {}).get("content", []) or []:
                if isinstance(c, dict) and c.get("type") == "tool_use" and c.get("name") in ("Bash", "PowerShell"):
                    tool_use[c["id"]] = (r.get("timestamp"), (c.get("input") or {}).get("command", ""))
    out = []
    for r in rows:
        if r.get("type") != "user":
            continue
        cont = r.get("message", {}).get("content")
        if not isinstance(cont, list):
            continue
        for c in cont:
            if not (isinstance(c, dict) and c.get("type") == "tool_result"):
                continue
            tu = tool_use.get(c.get("tool_use_id"))
            if not tu or not tu[0]:
                continue
            cmd = tu[1]
            first_line = cmd.strip().splitlines()[0] if cmd.strip() else ""
            if re.match(r"\s*git\s", first_line):
                continue  # commit messages mentioning gradlew
            m = GRADLE_RE.search(cmd)
            if not m:
                continue
            body = c.get("content")
            if isinstance(body, list):
                body = " ".join(x.get("text", "") for x in body if isinstance(x, dict))
            body = str(body)
            if "BUILD FAILED" in body or re.search(r"tests completed, \d+ failed", body):
                res = "FAILED"
            elif "BUILD SUCCESSFUL" in body:
                res = "OK"
            else:
                res = "?"
            args = m.group(1)
            tasks = [t for t in args.split() if not t.startswith("-") and not t.startswith("*") and t not in ("2>&1",)]
            flags = [t for t in args.split() if t.startswith("--")]
            key = " ".join(sorted(set(tasks))[:3]) or "?"
            if "--tests" in args:
                key += " --tests"
            if "--rerun-tasks" in args:
                key += " --rerun-tasks"
            d = (ts(r["timestamp"]) - ts(tu[0])).total_seconds()
            out.append((key, d, res, "--no-daemon" in flags, body[:0]))
    return out

def turn_timing(rows):
    # human prompts = user rows whose content is a str or has text (not tool_result)
    events = []
    for r in rows:
        t = r.get("timestamp")
        if not t:
            continue
        if r.get("type") == "user":
            cont = r.get("message", {}).get("content")
            is_prompt = isinstance(cont, str) or (isinstance(cont, list) and any(isinstance(c, dict) and c.get("type") == "text" for c in cont) and not any(isinstance(c, dict) and c.get("type") == "tool_result" for c in cont))
            if is_prompt and not r.get("isMeta") and not r.get("isCompactSummary"):
                events.append(("prompt", ts(t)))
        elif r.get("type") == "assistant":
            events.append(("asst", ts(t)))
    turns = []
    cur = None
    last_asst = None
    for kind, t in events:
        if kind == "prompt":
            if cur and last_asst:
                turns.append((last_asst - cur).total_seconds())
            cur, last_asst = t, None
        else:
            last_asst = t
    if cur and last_asst:
        turns.append((last_asst - cur).total_seconds())
    return turns

def main(main_path, sub_dir=None):
    rows = load(main_path)
    print("== main session token usage (dedup by message id)")
    for model, a in tokens(rows).items():
        print(f"  {model}: {dict(a)}")
    tt = [x / 60 for x in turn_timing(rows)]
    print("== turn wall time (prompt -> last assistant row), minutes:", summ(tt, "m"))
    g = gradle_calls(rows)
    print(f"== gradle calls (Bash/PowerShell, git commands excluded): {len(g)}")
    cat = collections.defaultdict(list)
    for key, d, res, nd, _ in g:
        cat[key].append((d, res, nd))
    for k, v in sorted(cat.items(), key=lambda x: -sum(y[0] for y in x[1])):
        ds = [y[0] for y in v]
        print(f"  [{k}] {summ(ds,'s')} results={dict(collections.Counter(y[1] for y in v))} no-daemon={sum(1 for y in v if y[2])}")
    allr = collections.Counter(x[2] for x in g)
    print("  overall results:", dict(allr), "total seconds:", round(sum(x[1] for x in g), 1))
    if sub_dir:
        print("== subagents")
        tot = collections.defaultdict(collections.Counter)
        durs = []
        for mp in sorted(glob.glob(os.path.join(sub_dir, "*.meta.json"))):
            meta = json.load(open(mp, encoding="utf-8"))
            jp = mp.replace(".meta.json", ".jsonl")
            if not os.path.exists(jp):
                continue
            sr = load(jp)
            tss = [ts(r["timestamp"]) for r in sr if r.get("timestamp")]
            dur = (max(tss) - min(tss)).total_seconds() / 60 if tss else 0
            durs.append(dur)
            tk = tokens(sr)
            tools = collections.Counter()
            for r in sr:
                if r.get("type") == "assistant":
                    for c in r.get("message", {}).get("content", []) or []:
                        if isinstance(c, dict) and c.get("type") == "tool_use":
                            tools[c.get("name")] += 1
            models = ",".join(f"{m}:{a['api_calls']}calls/out{a['output_tokens']}" for m, a in tk.items())
            for m, a in tk.items():
                tot[m].update(a)
            print(f"  {meta.get('agentType')} | {meta.get('description')[:48]} | {dur:.1f}min | tools={sum(tools.values())} | {models}{' | stoppedByUser' if meta.get('stoppedByUser') else ''}")
        print("  subagent duration minutes:", summ(durs, "m"))
        for m, a in tot.items():
            print(f"  subagent total {m}: {dict(a)}")

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else None)
