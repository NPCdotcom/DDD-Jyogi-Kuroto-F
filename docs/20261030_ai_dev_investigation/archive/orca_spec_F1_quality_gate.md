# Orca 実装指示文 F-1: 品質ゲート (Claude Code Hook) の修理と軽量化 (Critic M02/M03 反映版)

> 使い方: ユーザーが F-1 の着手を承認した後、Lead が新しい Run を作り、本文を spec として Implementer へ渡す。
> 例: `orca orchestration worker-start --spec "<本文>" --task-title "F-1 quality gate" --worktree new-child --agent codex --json`
> (`--worktree new-child` などの配置は、実行時の CLI ヘルプと placement ガイドで確認してから決める)
> 実装は main から切った専用ブランチ (例 `chore/quality-gate-hooks`) でのみ行う。push と PR の作成は人間の承認後。

## Target
- `.claude/settings.json` の hooks
- `.claude/hooks/final-check.sh` / `block-destructive.sh` (修正)、`format-java.sh` / `run-related-test.sh` (設定から外す)
- 新規: `.claude/hooks/tests/` (Hook の fixture テスト。bash で書き、Gradle は不要)
- `CLAUDE.md` の「Hooks の挙動」表

## Change
1. **PostToolUse の format / related-test を外す**。PR 本文に理由を書く: `command` と `args` の併用で毎回 ENOENT になる。Windows ではパスの正規表現も一致しない。編集のたびに Gradle を起動する構造は、起動回数が編集回数に比例する (1 セッションで Java 編集 149 回)。整形はタスク末尾の `spotlessApply` 1 回に、検証は Stop Hook と `check` に寄せる。
2. **Stop Hook (`final-check.sh`)**:
   - stdin の JSON の `stop_hook_active` が true なら、Gradle を起動せずに exit 0。
   - **内容ベースの指紋**: `git rev-parse HEAD` + `git diff HEAD -- <対象>` の内容 + 未追跡ファイルの内容ハッシュ (`git ls-files --others --exclude-standard -- <対象>`) を連結してハッシュにする。対象は `src assets build.gradle settings.gradle gradle.properties gradle/ .claude/hooks`。削除と未追跡のファイルも反映される。前回 green の指紋 (`build/.claude-gate/last-green`) と一致すれば、Gradle を起動せずに exit 0。
   - 実行: `--no-daemon` を外す。JAVA_HOME は既存の環境変数を優先し、未設定で既定パスが存在する場合だけ補う。**Gradle の終了コードを成否の主な根拠にする** (`|| true` で捨てない)。タイムアウト・起動失敗・出力が解析できない場合は「失敗/不明」として通知し、green を更新しない。
   - 失敗時: 今回の実行で更新された `build/test-results/test/*.xml` (実行開始時刻より新しいものだけ) から、失敗したテスト名を最大 10 件抜き出す。改行・引用符・バックスラッシュを正しくエスケープした JSON (`hookSpecificOutput.additionalContext`) で返す。成功時は何も出力せず、指紋を保存する。
3. **PreToolUse ガード (`block-destructive.sh`)**:
   - matcher を `Bash|PowerShell` に、timeout を 15 秒にする。
   - `tool_input.command` の抽出で、`[^"]*` の正規表現を使わない。JSON のエスケープ (`\"` と `\\`) を考慮した抽出にするか、stdin の生の JSON 全体に対してパターンを照合する (説明文などへの誤検出は安全側として許容)。python や jq には依存しない。
   - 既存のパターンに加え、PowerShell の `Remove-Item … -Recurse` を `rm -rf` と同等に扱う。それ以外のパターン追加 (`git reset --hard` 全般、`git clean` など) は、PR 本文で人間の判断を仰ぐ。
   - 文書には「既知のパターンの防止」と書き、「すべての破壊操作の防止」とは書かない。
4. **fixture テスト** (`bash .claude/hooks/tests/run.sh`、スタブの gradlew は環境変数 `GRADLEW` で差し替える):
   - Stop: (a) `stop_hook_active: true` なら Gradle を呼ばない (b) 指紋が一致すれば呼ばない (c) 未追跡ファイルの追加・削除・assets の変更で指紋が変わる (d) 非ゼロ終了で `FAILED` の文字列がなくても失敗として通知する (e) タイムアウトや起動失敗で green を更新しない (f) 古い XML を拾わない (g) 前回失敗した後の再実行 (h) 成功時は出力しない (i) 出力が妥当な JSON である。
   - ガード: (j) `cd "C:/x" && git reset --hard origin/main` を deny する (k) PowerShell の `Write-Output "ok"; Remove-Item -Recurse C:/x` を deny する (l) 通常のコマンドを許可する (m) 空入力や解析できない入力で異常終了しない。

## Constraints
- `src/` のプロダクションコード・テスト・資産・セーブ形式には触れない。
- macOS / Linux / Windows Git Bash で動くこと。python と jq に依存しない。
- 品質保証を減らさない。PR 前の `gradlew check` は従来どおり必須。Stop Hook は「検証対象の入力が変われば必ず走る」こと。
- 重い計測は追加しない。

## Ownership
`.claude/settings.json`、`.claude/hooks/**`、`CLAUDE.md` の Hook 表だけを変更する。

## Observable acceptance
- `bash .claude/hooks/tests/run.sh` が全件 PASS する (Gradle 不要で数秒)。
- 実際の Gradle での確認を 2 回だけ行う (承認済みの場合): 変更がある状態で Stop Hook を 1 回 (`test` が走る)、変更がない状態で 1 回 (Gradle が起動しない)。
- `gradlew check` が 1 回成功する。
- 導入後、3 セッション以上の記録で次を満たす: PostToolUse の hook_non_blocking_error が 0 件。Java/assets/ビルド設定に変更がないターンの Stop Hook (`stop_hook_summary.hookInfos[].durationMs`) が中央値 1,000 ms 未満。変更があるターンの所要時間を、導入前 (n=31、中央値 18.5 秒、no-daemon) と CLI のバージョン・warm/cold・変更の有無を揃えて比べ、報告する。
- worker_done の要約に、変更ファイル一覧・fixture の結果・`check` の結果を含める。
