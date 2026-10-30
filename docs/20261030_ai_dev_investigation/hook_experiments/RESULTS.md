# 現行 Hook スクリプトの実測 (2026-10-30, Claude Code 2.1.314 環境, Git Bash)

対象: `<repo>\.claude\hooks\*.sh` を `hooks/` へコピーして実行 (sha256 先頭16桁: block-destructive 85b470d0bc13ac5b / final-check 6245c1ebde9af0ec / format-java 0f8908c0edae50fb / run-related-test a2949ffd63c2d12a)。Gradle 本体は使わず、`fakeproj/gradlew.bat` スタブ (CRLF のバッチ) で出力と終了コードを再現した。

## 1. block-destructive.sh の判定 (stdin に PreToolUse 形式 JSON)

| 入力コマンド | 結果 |
|---|---|
| `git reset --hard origin/main` | DENY |
| `cd "C:/x" && git reset --hard origin/main` | **ALLOW (素通り)**: `"[^"]*"` 抽出が `\"` で切れる |
| `echo "x"; git push --force origin main` | **ALLOW (素通り)** |
| `rm -rf build` | DENY |
| PowerShell `Remove-Item -Recurse -Force C:/x` | **ALLOW** (パターン外。設定の matcher も Bash のみ) |
| `git reset --hard HEAD~3` | **ALLOW** (origin 以外は対象外) |
| `git clean -fdx` | **ALLOW** (対象外) |

- 追加の不具合: deny 時のメッセージを出すヒアドキュメントが引用符なし (`<< EOF`) で、本文中の `` `.claude/hooks/block-destructive.sh` `` がコマンド置換として実行される (実験では cwd が異なるため "No such file or directory" を stderr に出力)。プロジェクト直下ではスクリプト自身が再起動され、理由文の該当部分は空になる。
- レイテンシ (無害コマンド `ls`、n=10): 764 / 787 / 791 / 805 / 817 / 825 / 839 / 842 / 872 / 885 ms (中央値 約 821 ms)。

## 2. final-check.sh (Stop Hook) の失敗検出 (gradlew.bat スタブ)

| スタブの挙動 | Hook の結果 |
|---|---|
| exit 1、stderr に `ERROR: JAVA_HOME is set to an invalid directory` だけ | **exit 0・出力なし = 成功扱い** (終了コードを `\|\| true` で捨て、`FAILED` 文字列が無いため) |
| exit 1、`BUILD FAILED in 3s` を含む 1 行の要約 | 有効な JSON (additionalContext) を出力 |
| exit 1、`ATest > x() FAILED` / `3 tests completed, 1 failed` / `BUILD FAILED in 5s` (2 行の要約) | **不正な JSON** (`Invalid control character`: 文字列中に生の改行)。公式契約では exit 0 の不正 JSON は非ブロッキングエラーとなり、フィードバックは Claude に届かない |
| exit 0、出力なし | 出力なし (成功) |

注: 実際の `--quiet` 実行で `N tests completed` 行が出るかは未実測。出ない場合は要約 1 行で JSON は有効だが、`FAILED$` で終わる行が無く失敗テスト名が空になる (いずれの場合も実用上の失敗通知にならない)。

## 3. run-related-test.sh のパス処理 (第 1 引数)

| 引数 | 結果 |
|---|---|
| `C:\.program\x\src\main\java\core\A.java` (Windows 形式。公式仕様で stdin の file_path はこの形式) | **Gradle を呼ばず無言で skip** |
| `C:/.program/x/src/main/java/core/A.java` | スタブ Gradle を呼び、JSON を出力 |

## 4. 実環境の Stop Hook 1 回分 (本セッション 1 ターン目終了時)

- セッション記録 `stop_hook_summary`: final-check.sh `durationMs=19009`、additionalContext なし (2026-10-30T11:57:16Z)。
- Gradle 単発デーモンのログ `~/.gradle/daemon/9.5.0/daemon-33548.out.log`: 20:57:02.797 JVM 起動 → 20:57:03.553 ビルド開始 → 20:57:15.999 ビルド終了 (12.4 秒、結果 Success) → "Daemon will be stopped at the end of the build" (単発デーモン)。作業ツリーの JUnit XML は 2026-08-12 のまま更新なし (= test タスクは UP-TO-DATE で実行されず)。
- 解釈: 変更のない応答で、JVM 2 個の起動とビルド設定・UP-TO-DATE 判定に約 19 秒を使った (n=1)。
