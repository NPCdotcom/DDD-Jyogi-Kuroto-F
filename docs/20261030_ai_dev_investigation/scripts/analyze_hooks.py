"""Read-only analysis of Claude Code session transcripts (hooks / tools / tokens / timings)."""
import json, sys, statistics, re, collections, datetime, glob, os

def pct(vals, p):
    if not vals:
        return None
    s = sorted(vals)
    k = max(0, min(len(s) - 1, int(round(p / 100 * (len(s) - 1)))))
    return s[k]

def summ(vals):
    if not vals:
        return "n=0"
    return f"n={len(vals)} median={statistics.median(vals):.1f} p95={pct(vals,95):.1f} min={min(vals):.1f} max={max(vals):.1f} sum={sum(vals):.1f}"

def ts(s):
    return datetime.datetime.fromisoformat(s.replace("Z", "+00:00"))

def load(path):
    rows = []
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                rows.append(json.loads(line))
            except Exception:
                pass
    return rows

def analyze(path, label):
    rows = load(path)
    print(f"\n######## {label}: {os.path.basename(path)} rows={len(rows)}")
    # hook attachments
    hook_by = collections.Counter()
    hook_dur = collections.defaultdict(list)
    for r in rows:
        a = r.get("attachment") if isinstance(r.get("attachment"), dict) else None
        if a and str(a.get("type", "")).startswith("hook_"):
            cmd = a.get("command", "")
            key = (a.get("type"), a.get("hookName"), cmd[:70])
            hook_by[key] += 1
            if "durationMs" in a:
                hook_dur[key].append(a["durationMs"])
    print("-- hook attachments (type, hookName, command) count / duration ms")
    for k, v in hook_by.most_common(25):
        print(f"  {v:5d} {k} {summ(hook_dur[k])}")
    # stop hook summaries
    stop_d, stop_err, stop_ctx = [], 0, 0
    for r in rows:
        if r.get("subtype") == "stop_hook_summary":
            for h in r.get("hookInfos", []):
                if "final-check" in h.get("command", "") and "durationMs" in h:
                    stop_d.append(h["durationMs"] / 1000)
            if r.get("hookErrors"):
                stop_err += 1
            if r.get("hookAdditionalContext"):
                stop_ctx += 1
    print(f"-- Stop hook final-check.sh seconds: {summ(stop_d)} ; summaries with errors={stop_err} with additionalContext={stop_ctx}")
    # tool uses and their latency
    tool_use = {}
    tool_cnt = collections.Counter()
    model_tokens = collections.defaultdict(lambda: collections.Counter())
    first_ts = last_ts = None
    for r in rows:
        t = r.get("timestamp")
        if t:
            tt = ts(t)
            first_ts = tt if first_ts is None or tt < first_ts else first_ts
            last_ts = tt if last_ts is None or tt > last_ts else last_ts
        if r.get("type") == "assistant":
            m = r.get("message", {})
            model = m.get("model", "?")
            u = m.get("usage") or {}
            mt = model_tokens[model]
            mt["msgs"] += 1
            for k in ("input_tokens", "output_tokens", "cache_read_input_tokens", "cache_creation_input_tokens"):
                mt[k] += u.get(k, 0) or 0
            for c in m.get("content", []) or []:
                if isinstance(c, dict) and c.get("type") == "tool_use":
                    name = c.get("name")
                    tool_cnt[name] += 1
                    inp = c.get("input", {})
                    tool_use[c.get("id")] = (name, t, inp)
    print(f"-- span {first_ts} .. {last_ts}")
    print("-- tool use counts:", dict(tool_cnt.most_common(20)))
    print("-- tokens by model (note: streamed assistant rows may duplicate usage per content block):")
    for model, c in model_tokens.items():
        print(f"  {model}: {dict(c)}")
    # tool latency from tool_result timestamps
    lat = collections.defaultdict(list)
    gradle = []
    for r in rows:
        if r.get("type") == "user":
            m = r.get("message", {})
            cont = m.get("content")
            if isinstance(cont, list):
                for c in cont:
                    if isinstance(c, dict) and c.get("type") == "tool_result":
                        tu = tool_use.get(c.get("tool_use_id"))
                        if tu and tu[1] and r.get("timestamp"):
                            d = (ts(r["timestamp"]) - ts(tu[1])).total_seconds()
                            lat[tu[0]].append(d)
                            cmd = (tu[2] or {}).get("command", "") if isinstance(tu[2], dict) else ""
                            if tu[0] in ("Bash", "PowerShell") and re.search(r"gradlew", cmd):
                                body = c.get("content")
                                if isinstance(body, list):
                                    body = " ".join(x.get("text", "") for x in body if isinstance(x, dict))
                                body = str(body)
                                res = "FAILED" if re.search(r"BUILD FAILED|FAILED", body) else ("OK" if "BUILD SUCCESSFUL" in body else "?")
                                tasks = re.findall(r"gradlew(?:\.bat)?\s+([^|;&\n]*)", cmd)
                                gradle.append((d, res, (tasks[0] if tasks else cmd)[:90]))
    print("-- tool latency seconds by tool:")
    for k, v in sorted(lat.items(), key=lambda x: -sum(x[1])):
        print(f"  {k}: {summ(v)}")
    print(f"-- gradle invocations via Bash/PowerShell: {len(gradle)}")
    cat = collections.defaultdict(list)
    for d, res, task in gradle:
        key = re.sub(r"\s+", " ", re.sub(r"--tests\s+\S+", "--tests X", task)).strip()
        key = re.sub(r"(2>&1|\|.*|>.*)$", "", key).strip()
        cat[key[:60]].append((d, res))
    for k, v in sorted(cat.items(), key=lambda x: -len(x[1]))[:15]:
        ds = [x[0] for x in v]
        rs = collections.Counter(x[1] for x in v)
        print(f"  [{k}] {summ(ds)} results={dict(rs)}")
    # subagent / Task usage
    for r in rows:
        pass
    return rows

if __name__ == "__main__":
    for p in sys.argv[1:]:
        analyze(p, "session")
