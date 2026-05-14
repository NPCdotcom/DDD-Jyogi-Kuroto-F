---
name: gradle-runner
description: DDD-Jyogi-Kuroto-F の Gradle コマンド (test / build / run / spotlessApply / fatJar) を JAVA_HOME 自動設定付きで実行。Windows / macOS / Linux のシェル差分を吸収
argument-hint: [test | build | run | spotlessApply | fatJar | clean | check]
allowed-tools: Bash, PowerShell
---

## あなたのタスク

Gradle コマンドを実行する。`$ARGUMENTS` で実行するタスクを指定:

- `test` — ドメイン層テスト全件実行
- `build` — テスト + Spotless チェック + コンパイル
- `run` — ゲーム起動 (Desktop) 、バックグラウンド推奨
- `spotlessApply` — フォーマット適用
- `fatJar` — 配布用 JAR 生成 (`build/libs/*-all.jar`)
- `clean` — ビルド成果物クリア
- `check` — テスト + Spotless チェックのみ (build より軽量)

引数が空または不明な場合は `test` をデフォルトとする。

## 環境設定 (重要)

実行前に **JAVA_HOME と PATH を必ず設定**する (Claude Code セッションは PATH をプロジェクト由来で持っていないため):

### Windows (PowerShell)

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.3"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
& "C:\.program\DDD-Jyogi-Kuroto-F\gradlew.bat" -p "C:\.program\DDD-Jyogi-Kuroto-F" --no-daemon $ARGUMENTS
```

### macOS / Linux (Bash)

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 25 2>/dev/null || echo /usr/lib/jvm/java-25-openjdk)"
export PATH="$JAVA_HOME/bin:$PATH"
cd /path/to/DDD-Jyogi-Kuroto-F
./gradlew --no-daemon "$@"
```

## 実行モード

### 短時間タスク (test / build / spotlessApply / check / clean)
- フォアグラウンドで実行、出力末尾を取得
- `2>&1 | Select-Object -Last 20` (PowerShell) / `2>&1 | tail -20` (Bash) で要点表示

### 長時間タスク (run / fatJar)
- **run** は GUI 起動でセッションをブロックする → **`run_in_background: true` でバックグラウンド実行**
- 起動完了後 (10〜15 秒) に `Get-Process java | Where-Object {$_.MainWindowTitle -ne ""}` でウィンドウ確認
- ユーザーに「ウィンドウ出ましたか?」を確認、終了は OS のウィンドウ閉じる

### fatJar
- 出力: `build/libs/*-all.jar`
- 生成後に `ls -la build/libs/` で確認

## 想定エラーと対処

| エラー | 原因 | 対処 |
|---|---|---|
| `java: command not found` | JAVA_HOME / PATH 未設定 | 上記の環境設定ブロックを実行 |
| `Unsupported class file major version 69` | Gradle が Java 25 非対応バージョン | `gradle/wrapper/gradle-wrapper.properties` で 9.5.0+ を確認 |
| `Entry .gitkeep is a duplicate` | sourceSets srcDirs に複数の .gitkeep | `build.gradle` に `tasks.withType(Copy).configureEach { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }` 追加 |
| `BUILD FAILED` (テスト失敗) | コード変更で既存テストが壊れた | 失敗テストを特定 (`--info` で詳細)、原因究明 → bug-hunter 呼出 |

## 出力

- 実行したコマンド
- 終了コード (0 = 成功)
- 重要なログ (テスト件数 / エラー詳細)
- 次のアクション (失敗時の修正方針)
