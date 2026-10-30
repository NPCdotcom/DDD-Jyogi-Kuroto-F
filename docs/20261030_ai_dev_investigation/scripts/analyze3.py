"""Corrected session-transcript metrics (addresses Critic M01).

- Gradle detection per command *segment* (split on && ; | newline), PowerShell `& "...gradlew.bat"` forms included.
- Text inside git commit messages / heredocs is ignored (segments whose first word is git are skipped,
  and quoted strings are blanked before splitting).
- One tool call may contain several gradle invocations; latency is the whole tool call (mixed time).
- Turn elapsed time: chronologically sorted, negative intervals excluded and counted.
Usage: python -I analyze3.py <session.jsonl>
"""
import json, re, sys, statistics, collections, datetime

def ts(s):
    return datetime.datetime.fromisoformat(s.replace("Z", "+00:00"))

def pct(v, p):
    s = sorted(v)
    return s[max(0, min(len(s) - 1, int(round(p / 100 * (len(s) - 1)))))]

def summ(v, unit=""):
    if not v:
        return "n=0"
    return f"n={len(v)} median={statistics.median(v):.1f}{unit} p95(rank)={pct(v,95):.1f}{unit} max={max(v):.1f}{unit} sum={sum(v):.1f}{unit}"

HEREDOC = re.compile(r"<<-?\s*'?(\w+)'?.*?^\s*\1\s*$", re.S | re.M)
QUOTED = re.compile(r"\"(?:\\.|[^\"\\])*\"|'[^']*'")
GRADLE_TOKEN = re.compile(r"""^(?:&\s*)?["']?[^\s"']*gradlew(?:\.bat)?["']?$""", re.I)

def gradle_invocations(cmd):
    """Return list of task-lists, one per gradle invocation found in executable segments."""
    text = HEREDOC.sub(" ", cmd)
    # keep quoted gradlew paths (PowerShell `& "$wt\\gradlew.bat"`), blank other quoted strings
    def keep(m):
        s = m.group(0)
        return s if re.search(r"gradlew(\.bat)?[\"']$", s, re.I) else '""'
    text = QUOTED.sub(keep, text)
    out = []
    for seg in re.split(r"&&|\|\||;|\||\n", text):
        toks = seg.strip().split()
        if not toks:
            continue
        if toks[0].lower() in ("git", "echo", "write-output", "write-host", "#"):
            continue
        # allow leading `&` call operator and env assignments
        i = 0
        while i < len(toks) and (toks[i] == "&" or re.match(r"^\$?env:|^[A-Z_]+=", toks[i])):
            i += 1
        if i < len(toks) and GRADLE_TOKEN.match(toks[i].lstrip("&")):
            args = toks[i + 1:]
            tasks = [a for a in args if not a.startswith("-") and not a.startswith("*") and not a.startswith("$") and a not in ("2>&1",) and not re.match(r"^\d?>", a)]
            out.append((tasks, "--no-daemon" in args))
    return out

def main(path):
    rows = [json.loads(l) for l in open(path, encoding="utf-8", errors="replace") if l.strip()]
    uses = {}
    for r in rows:
        if r.get("type") == "assistant":
            for c in r.get("message", {}).get("content", []) or []:
                if isinstance(c, dict) and c.get("type") == "tool_use" and c.get("name") in ("Bash", "PowerShell"):
                    uses[c["id"]] = (r.get("timestamp"), (c.get("input") or {}).get("command", ""), c["name"])
    calls = {}
    for r in rows:
        if r.get("type") != "user" or not r.get("timestamp"):
            continue
        cont = r.get("message", {}).get("content")
        if not isinstance(cont, list):
            continue
        for c in cont:
            if isinstance(c, dict) and c.get("type") == "tool_result" and c.get("tool_use_id") in uses and c["tool_use_id"] not in calls:
                t0, cmd, tool = uses[c["tool_use_id"]]
                inv = gradle_invocations(cmd)
                if not inv:
                    continue
                d = (ts(r["timestamp"]) - ts(t0)).total_seconds()
                body = c.get("content")
                if isinstance(body, list):
                    body = " ".join(x.get("text", "") for x in body if isinstance(x, dict))
                body = str(body)
                res = "FAILED" if ("BUILD FAILED" in body or re.search(r"tests completed, \d+ failed", body)) else ("OK" if "BUILD SUCCESSFUL" in body else "?")
                calls[c["tool_use_id"]] = (tool, d, inv, res)
    print(f"== tool calls containing >=1 gradle invocation: {len(calls)}; invocations: {sum(len(v[2]) for v in calls.values())}")
    for tool in ("Bash", "PowerShell"):
        v = [x[1] for x in calls.values() if x[0] == tool]
        print(f"  {tool}: {summ(v, 's')}")
    allv = [x[1] for x in calls.values()]
    print(f"  ALL (mixed tool time): {summ(allv, 's')}  results={dict(collections.Counter(x[3] for x in calls.values()))}")
    print(f"  calls using --no-daemon in every invocation: {sum(1 for x in calls.values() if all(nd for _, nd in x[2]))}/{len(calls)}")
    cat = collections.defaultdict(list)
    for tool, d, inv, res in calls.values():
        tasks = set(t for ts_, _ in inv for t in ts_)
        if tasks == {"spotlessApply"}:
            k = "spotlessApply ONLY"
        elif "spotlessApply" in tasks:
            k = "spotlessApply + other tasks"
        elif tasks <= {"test"}:
            k = "test only"
        elif "check" in tasks:
            k = "check (+others)"
        else:
            k = "other: " + ",".join(sorted(tasks))[:40]
        cat[k].append(d)
    for k, v in sorted(cat.items(), key=lambda x: -sum(x[1])):
        print(f"  [{k}] {summ(v, 's')}")
    # turn timing
    ev = []
    for r in rows:
        t = r.get("timestamp")
        if not t or r.get("isSidechain"):
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
                turns.append((last - cur).total_seconds() / 60)
                gaps.append((t - last).total_seconds() / 60)
            cur, last = t, None
        else:
            last = t
    if cur and last:
        turns.append((last - cur).total_seconds() / 60)
    neg = sum(1 for x in turns if x < 0)
    print(f"== response elapsed per human prompt (includes approval waits), negatives excluded={neg}: {summ([x for x in turns if x >= 0], 'm')}")
    g = [x for x in gaps if 0 <= x <= 120]
    print(f"== gap assistant-end -> next prompt (0..120 min only, excluded {len(gaps)-len(g)}): {summ(g, 'm')}")

if __name__ == "__main__":
    main(sys.argv[1])
