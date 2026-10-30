# Critic v2 への入力一覧 (根拠の所在)

すべて読み取り専用で参照すること。S = {S}

## レビュー対象 (Lead の成果物)
- S\c1_spec_v2.md (最重要)
- S\review_reflection_v2.md
- S\model_roles_eval_plan.md

## 根拠
- コード: S\main (local main 60ac172 を git archive したコピー。リモート main も 60ac172)
- 現行 Hook の実験: S\hook_experiments\RESULTS.md、コピーしたスクリプト S\hook_experiments\hooks\*.sh、スタブ環境 S\hook_experiments\fakeproj\ (再実行可。Windows では gradlew.bat の CRLF バッチをスタブにする必要がある。bash スタブだけでは Windows 経路を通らない)
- 計測: S\metrics_v2.py、S\analyze3.py、S\prices_anthropic_20261030.json、出力 S\metrics_v2_old_session.txt、S\metrics_v2_turn1_and_codex.txt
- セッション記録 (読み取りのみ): ~\.claude\projects\C---program-DDD-Jyogi-Kuroto-F--claude-worktrees-comprehensive-adversarial-review-cc2991\ffc4d2c5-aae0-4717-820f-b08e04e9f388.jsonl (+ 同名ディレクトリ\subagents)、本セッション ~\.claude\projects\C---program-DDD-Jyogi-Kuroto-F\85194b4f-1d3f-4247-a63f-a22c61012ccc.jsonl
- Gradle 単発デーモンのログ: ~\.gradle\daemon\9.5.0\daemon-33548.out.log
- OpenRouter モデル一覧 (2026-10-30T13:02Z 取得): S\dl_openrouter\models_v2.json。抽出結果 S\model_candidates_20261030.txt
- 公式文書の取得結果 (生テキスト):
  - 権限: ~\.claude\projects\C---program-DDD-Jyogi-Kuroto-F\85194b4f-1d3f-4247-a63f-a22c61012ccc\tool-results\toolu_01DhFWDYy2fmRAyyCqiqHcR8.txt (deny→ask→allow は 56 行、PowerShell ルールは 315-335 行、ワイルドカードは 144-190 行、「セキュリティ境界ではない」は 246-262 行)
  - Hooks (後半): 同ディレクトリ\toolu_01HmyvruWx6bgPZPjj5Nr4yu.txt (Stop の入力と判定制御は 58-160 行)
  - Hooks (前半) と permission-modes は生テキストを保存していない。Lead が WebFetch で得た要点 (引用) は次のとおり:
    - `onFailure`: "Whether the action that triggered the hook goes ahead when the hook fails: "continue" or "block" … Requires Claude Code v2.1.295 or later"。Stop, SubagentStop, TaskCompleted, TeammateIdle と非同期 Hook には効かない。failure には起動不可・exit 0/2 以外・timeout・不正出力を含む。
    - `if`: permission rule syntax。tool イベントでのみ評価。
    - 終了コード: 0 = success (JSON 適用)、2 = blocking (PreToolUse は実行阻止、Stop は停止を阻止し会話を継続)、その他 = non-blocking error。"Exit code 1 without valid JSON output is a non-blocking error"。
    - exit 0 で JSON が不正またはスキーマ違反: "the action goes ahead … the run is a non-blocking error"。
    - `systemMessage`: "Warning message shown to the user"。`continue: false` は処理を停止。
    - command の既定 timeout は 600 秒。matcher は英数字と `|` だけなら完全一致のリスト。
    - permission-modes: "Deny rules block in every mode, including bypassPermissions." / "Explicit ask rules … still prompt" (bypass モードでも確認を求める)。
- インストール版: `claude --version` → 2.1.314。Orca 1.4.244。
- 本 Run で Orca が起動した Codex Worker の画面表示には `permissions: YOLO mode` とあった (前回 Run の 3 Worker)。
