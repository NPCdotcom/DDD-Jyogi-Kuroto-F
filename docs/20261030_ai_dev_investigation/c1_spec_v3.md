# C-1 品質ゲート復旧 実装仕様 v3 (承認用の最終版・未実装)

作成: 2026-10-30 / 対象 `main` @ 60ac172 (= origin/main) / Claude Code 2.1.314 / Orca 1.4.244
前版: `c1_spec_v2.md` (独立 Critic v2 の Major 10・Minor 5 をすべて反映。対応は §0)
ラベル: **[実測]** 今回のスクリプト・実験 / **[確認済み]** コード・公式文書 / **[前回引用]** report_final.md / **[設計]** 提案 / **[未計測]**

---

## 0. v2 からの変更 (Critic v2 の採用)

| Critic | 指摘 | v3 の対応 |
|---|---|---|
| M01 | 指紋が、テストの読む文書・ラッパー・無視済み資源を漏らす (`PlatformClaimTest` は全 .md/.java/.gradle/.yml を読む) | 指紋をリポジトリ全体 (追跡 + 未追跡、ビルド出力を除く) + 明示した無視済み入力 (`assets/fonts/*`) に拡張。検証の前後で指紋が一致した時だけ green とする。Gradle の test が .md を入力として宣言していない問題 [確認済み] には `test --rerun` で対処する (C-1b で入力宣言も提案) |
| M02 | 環境だけ直しても、同じ指紋の失敗が再検証されない | green のキーに環境識別を追加。失敗時の再実行抑止は同じ Stop 継続中 (`stop_hook_active`) に限定。強制再検証の手段を用意 |
| M03 | 残余の失敗を「Claude が直せない」と決めつけている | ENV / BUILD / UNKNOWN に分け、BUILD と UNKNOWN は Claude へ返す |
| M04 | 8 回上限はツール呼び出しのたびにリセットされる (公式) ので、編集→失敗のループを止めない | セッション単位の修正予算 (連続失敗 3 回) をゲート側で管理。意図的な RED の終了手段を用意 |
| M05 | worktree は stash・refs・リモートを共有するので、YOLO の Codex を隔離できない | 実装 Worker は独立クローン + Codex のサンドボックス設定 (承認 A5)。stash のバックアップ ref は任意 (A6)。守れない範囲は明記 |
| M06 | fixture と権限エンジンの期待値が混ざり、onFailure の故障経路が未試験 | 試験を「Hook 単体 / 権限エンジン / 故障注入 / 状態初期化」の 4 層に分ける |
| M07 | 評価 E2 の重大誤りが未定義 | 評価計画 (`model_roles_eval_plan.md` §4) で修正 |
| M08 | 失敗ループ費用の二重計上 | `metrics_v2.py` を修正 (81 呼出・$21.01) |
| M09 | 報酬ポップアップが開いたフレームの入力 | C-3 の仕様 (`review_reflection_v2.md` §3) で修正 |
| M10 | 整形 1 回でも全体適用なら大量注入しうる | 整形は変更ファイルに限定。限定できない全体整形は別タスクにする |
| N01〜N05 | stash 比較の引用、Daemon 計測の条件、ask の広さ、時間の呼称、単価比の条件 | 各所を修正 |

## 1. 前提にする契約 [確認済み] (出典: code.claude.com/docs/en/hooks, /permissions, /permission-modes。2026-10-30 取得)

- command Hook: `args` を付けると `command` を実行ファイルとして直接起動する (シェルなし)。`${tool_input.*}` は展開されない。入力は stdin の JSON。
- 終了コード: 0 = 成功 (JSON を適用)。2 = ブロック (PreToolUse は実行を止める。Stop は停止を阻止し、stderr を理由として会話を続ける)。その他 = 非ブロッキングエラー。exit 0 で不正な JSON を出すと、非ブロッキングエラーになり出力は捨てられる。
- `systemMessage` はユーザーに表示される警告。Claude を継続させない。
- Stop の連続継続上限は 8 回で、**Claude がツールを呼ぶたびにリセットされる** (環境変数 `CLAUDE_CODE_STOP_HOOK_BLOCK_CAP` で変更可)。
- `onFailure: "block"` は v2.1.295 以上で使え、Stop には効かない。
- 権限ルール: deny → ask → allow の順で評価する。**deny は bypassPermissions を含む全モードで有効**、ask も bypass モードで確認を求める。複合コマンドの各部分に適用され、PowerShell は別名を正規化して大文字小文字を区別しない。ただし「セキュリティ境界ではない」(`/bin/rm`、`bash -c`、`git -C . push` などは一致しない)。
- 生テキストを保存していない部分 (Hooks の前半、permission-modes) は Lead の WebFetch による抜粋に依存している。実機での最終確認は A4 で行う。

## 2. 現状の欠陥 (C-1 の根拠)

| # | 欠陥 | 区分 | 根拠 |
|---|---|---|---|
| D1 | PostToolUse 2 本が毎回 ENOENT | [確認済み・実測] | `.claude/settings.json:16-33`、セッション記録 |
| D2 | Windows 形式パスでテストの対応付けが無言 skip | [実測] | `hook_experiments/RESULTS.md` §3 |
| D3 | Stop Hook が終了コードを捨て、起動失敗を成功扱いにする | [実測] | RESULTS.md §2 |
| D4 | 失敗要約が 2 行以上だと不正な JSON になり、フィードバックが消える | [実測] | RESULTS.md §2 |
| D5 | 変更のない応答でも Gradle を起動する (単発デーモンで JVM 2 個) | [実測] | 19.0 秒 (n=1、ビルド 12.4 秒・UP-TO-DATE)、[前回引用] 8 月 n=31 中央値 18.5 秒 |
| D6 | ガードが引用符で抽出を打ち切る、PowerShell を対象外にする、timeout で素通りする | [実測] | RESULTS.md §1 (7 入力中 5 件素通り) |
| D7 | ガードの deny 文のバッククォートがコマンド置換として実行される | [実測] | RESULTS.md §1 |
| D8 | 標準の Gradle 実行 Skill と計画文書が `--no-daemon` を指示している | [確認済み] | `gradle-runner/SKILL.md:31,40`、計画文書に 37 箇所 |
| D9 | 後からの一括整形で差分が会話へ注入される | [実測] | 8 月: edited_text_file 71 件・359,680 文字 |
| D10 | Codex Worker は Claude の Hook・権限の対象外 (YOLO モード)。worktree は stash を共有する | [確認済み] | Codex 画面表示、`git rev-parse --git-common-dir` (Critic v2 M05) |
| D11 | Gradle の test タスクが、テストの読む .md 等を入力として宣言していない。文書だけを変えると test が UP-TO-DATE になり `PlatformClaimTest` が再実行されない | [確認済み] | `build.gradle:94-100`、`PlatformClaimTest.java:25-29,55-58` |

## 3. ユーザー要求 → 設計 → 検証

| 要求 | 設計 | 検証 (§5) |
|---|---|---|
| JSON 標準入力を正しく処理 | stdin 全量を読む。Stop は真偽値と UUID だけを正規表現で取る (エスケープを含まない値に限る)。ガードは最小限のアンエスケープ後に全文スキャンする。任意の JSON を解釈するパーサではないことを明記し、空・不正な入力では安全側 (ガードは拒否、Stop は検証を実行) に倒す | U-S1, U-G1〜G9 |
| Java 変更を正しく検出 | リポジトリ全体の内容指紋 (Java に限らない) + 前後一致の確認 | U-S2〜S8, R9, R10 |
| Windows パス・空白・引用符・エスケープ | git の `-z` 出力でパスを扱う。設定のコマンドはシェル形式で引用符を付ける。fixture に空白・引用符・日本語・バックスラッシュを入れる | U-S6, U-G2〜G5 |
| コンパイル失敗・テスト失敗・起動失敗の区別 | 終了コード + ログのタスク失敗表示 + 今回生成された XML で COMPILE / TEST / BUILD / ENV / TIMEOUT / UNKNOWN に分類 | U-S9〜S15, R3〜R6, R12, R13 |
| 失敗を黙って成功にしない | 終了コード非 0 は必ず非 PASS。PASS 以外では green を更新しない。分類不能は UNKNOWN として Claude とユーザーの両方に通知 | U-S9〜S15, 監視 M3 |
| 変更のない応答で全テストを走らせない | 指紋と環境キーが有効な green と一致すれば Gradle を起動しない (初回・clean 後・状態破損時は検証する) | U-S2, U-S16, R1, 監視 M2 |
| Gradle Daemon の有効性を計測 | §5.5 の計測 (事前登録した判定規則) | P-1〜P-4 |
| 整形差分を会話へ大量投入しない | ゲートは整形しない。整形はタスク内で変更した Java ファイルに限定する (Spotless の単一ファイル整形、または C-1b の `ratchetFrom`)。全体整形は独立したタスクにする | P-4, 監視 M4 |
| 破壊防止を Hook だけに頼らない | 権限ルールを一次防御、Hook を補助にする。Codex はサンドボックスと独立クローン (A5)。守れない範囲を明記 (§8) | E-1〜E-6 |

## 4. 設計 [設計]

### 4.1 `.claude/settings.json`

```json
{
  "$schema": "https://json.schemastore.org/claude-code-settings.json",
  "permissions": {
    "deny": [
      "Bash(git push *--force*)", "Bash(git push -f*)", "Bash(git push * -f*)",
      "Bash(gh repo delete*)", "Bash(gh repo edit *--visibility*)",
      "PowerShell(git push *--force*)", "PowerShell(git push -f*)", "PowerShell(git push * -f*)"
    ],
    "ask": [
      "Bash(git reset --hard*)", "Bash(git clean *-f*)", "Bash(git clean -f*)",
      "Bash(git stash drop*)", "Bash(git stash clear*)", "Bash(git checkout -- *)", "Bash(git restore *)",
      "Bash(rm -rf *)", "Bash(rm -fr *)",
      "PowerShell(Remove-Item *)", "PowerShell(git reset --hard*)", "PowerShell(git clean *-f*)",
      "PowerShell(git stash drop*)", "PowerShell(git stash clear*)"
    ]
  },
  "hooks": {
    "PreToolUse": [ { "matcher": "Bash|PowerShell", "hooks": [
      { "type": "command", "command": "bash \"${CLAUDE_PROJECT_DIR}/.claude/hooks/block-destructive.sh\"", "timeout": 15, "onFailure": "block" } ] } ],
    "Stop": [ { "hooks": [
      { "type": "command", "command": "bash \"${CLAUDE_PROJECT_DIR}/.claude/hooks/final-check.sh\"", "timeout": 330 } ] } ]
  }
}
```

- PostToolUse の整形・関連テストは削除する。
- deny と ask の各行はポリシー判断 (A2)。確認が増える範囲を明示する: `git restore --staged` などの非破壊操作も ask になる。PowerShell の `Remove-Item` はすべて ask になる (別名を含む)。`git clean -n` (dry-run) は v3 で対象外にした。導入後の確認回数を監視 M5 で数える。

### 4.2 `final-check.sh` (Stop)

**状態ファイル**: `build/.claude-gate/` (build/ は .gitignore 済み) に `green` (指紋 + 環境キー)、`last-result`、`streak` (セッション ID + 連続失敗数)、`metrics.jsonl`、`last-run.log` を置く。どれかが壊れていたり欠けていたりしたら「未検証」として扱う。

**指紋 FP** = sha256 の対象は次のとおり。
- `git rev-parse HEAD`
- `git diff --binary HEAD` (追跡ファイルすべて。pathspec で絞らない)
- 未追跡かつ非無視のファイル (`git ls-files -z --others --exclude-standard`) のパスと内容のハッシュ
- 明示した無視済み入力 (`assets/fonts/*`) の内容ハッシュ

**環境キー EK** = `JAVA_HOME` の値 + `java` 実体のパス + `gradle/wrapper/gradle-wrapper.properties` のハッシュ + OS 名。

```
1. stdin を読み、session_id と stop_hook_active を取る。
2. CLAUDE_GATE_FORCE=1 なら手順 3・4 を飛ばして検証する (強制再検証)。
3. (FP, EK) == green → exit 0 (Gradle を起動しない)。
4. stop_hook_active == true かつ (FP, EK) == last-result の失敗 (同じ Stop 継続中の重複)
     → Gradle を起動せず exit 0 (systemMessage で未解決を表示)。※ 新しいターンでは抑止しない
5. Gradle 実行: run.start を記録 → "$GRADLEW" test --rerun --console=plain > last-run.log 2>&1
   (--rerun で test の UP-TO-DATE を無効にし、D11 を回避する。採用後は daemon。ウォッチドッグの上限は 300 秒)
6. 実行後に FP を再計算する。実行前と違えば green を確定せず「検証中に変更あり」として扱う。
7. 分類 (終了コードが主根拠):
   rc == 0                                → PASS   : green = (FP, EK)、streak = 0、出力なし
   ウォッチドッグが発火                   → TIMEOUT: systemMessage、green は更新しない
   compileJava / compileTestJava の失敗表示 → COMPILE: `*.java:<行>: error:` を最大 10 件 → exit 2 (Claude へ)
   今回生成の XML に <failure|<error>       → TEST   : 失敗テスト最大 10 件 → exit 2
   ENV の既知の印 (JAVA_HOME が不正、java が見つからない、デーモンを起動できない)
                                          → ENV    : systemMessage (ユーザーへ)、exit 0
   ビルドスクリプトの評価エラー、依存解決、プラグイン、テスト JVM の起動失敗
                                          → BUILD  : exit 2 (Claude へ) + systemMessage
   上記以外                               → UNKNOWN: exit 2 (Claude へ、ログのパス付き) + systemMessage
8. 修正予算: 同じ session_id で COMPILE / TEST / BUILD / UNKNOWN が連続 3 回に達したら、
   exit 2 をやめて systemMessage「連続 3 回失敗。人間の判断が必要」+ exit 0 (継続させない)。green は更新しない。
9. 意図的な RED: `build/.claude-gate/intentional-red` (理由を 1 行) があり、分類が TEST の場合は
   失敗を記録して systemMessage で表示し、継続させない (exit 0)。マーカーは 1 回で削除する。green は更新しない。
10. 会話へ送る文は 1,500 文字以内。詳細はログのパスで示す。metrics.jsonl に 1 行追記する。
```

- JSON を出力する経路は `json_escape` 関数を通す。exit 2 の経路は stderr の平文。
- JAVA_HOME は既存の値を優先し、未設定で既定パスが存在する時だけ補う。

### 4.3 `block-destructive.sh` (PreToolUse・補助層)

- stdin 全量を読み、`\\` `\"` `\n` `\t` だけをアンエスケープしてから、入力全体に照合する (フィールドは抽出しない。誤検出は安全側として許容する)。
- 照合するのは既存のパターンと PowerShell の `Remove-Item … -Recurse`。その他の操作は権限ルール (§4.1) で扱う。
- 一致したら **exit 2 + stderr**。ヒアドキュメントは使わない。bash の `[[ =~ ]]` で照合し、外部プロセスを減らす。
- 空入力・不正な入力では exit 2 (fail-closed。`onFailure: "block"` とも一致させる)。

### 4.4 整形の方針 (M10)

- ゲートは整形しない。
- Implementer はタスク内で変更した Java ファイルだけを整形する: `gradlew spotlessApply -PspotlessIdeHook=<絶対パス>` (ファイルごと、daemon 前提)。動作は P-4 で確認する。
- 代替案 **C-1b** (要承認): `build.gradle` の spotless に `ratchetFrom 'origin/main'` を設定し、変更ファイルだけを対象にする。あわせて test タスクに文書入力 (`**/*.md` など、`PlatformClaimTest` が読む範囲) を宣言して D11 を解消する。**ビルド設定の変更なので C-1 本体とは分ける。**
- 既存ファイルの一括整形が必要になった場合は、通常の編集タスクから切り離した独立タスクにする。

### 4.5 文書と Skill

- `CLAUDE.md` の Hook 表を、二層防御・状態遷移・強制再検証 (`CLAUDE_GATE_FORCE=1`)・意図的 RED・整形方針に合わせて書き換える。
- `gradle-runner/SKILL.md` の `--no-daemon` は、P の判定で daemon を採用した場合に限り外す。
- 計画文書の `--no-daemon` (37 箇所) は履歴として残し、CLAUDE.md の現行規則が優先すると明記する。

## 5. 検証計画 (4 層 + 計測)

### 5.1 Hook 単体 (Gradle 不要・数秒、`bash .claude/hooks/tests/run.sh`)

Stop: スタブは `CLAUDE_GATE_GRADLEW` で差し替える。Windows 経路は CRLF のバッチスタブで検証する。

| ID | 状況 | 期待 |
|---|---|---|
| U-S1 | stdin が空・不正 | 検証を実行する (省略しない)。異常終了しない |
| U-S2 | (FP, EK) == green | Gradle 0 回 |
| U-S3 | README.md だけ変更 | FP が変化 → Gradle 実行 (M01) |
| U-S4 | 未追跡ファイルを追加 / 追跡ファイルを削除 | FP が変化 |
| U-S5 | `assets/fonts/*.ttf` (無視済み) を変更 | FP が変化 |
| U-S6 | 空白・引用符・日本語を含むファイル名 | FP が変化。異常終了しない |
| U-S7 | 実行中にファイルが変わる (スタブ内で書き換え) | green を確定しない |
| U-S8 | JAVA_HOME だけを変更 (green あり) | EK が変化 → 再検証 (M02) |
| U-S9 | exit 1・stderr に JAVA_HOME 不正 | ENV、systemMessage の JSON が妥当、green 更新なし |
| U-S10 | `:compileJava` 失敗 + `A.java:3: error:` | COMPILE、exit 2、stderr に `A.java:3` |
| U-S11 | 新しい XML に failure 2 件 | TEST、exit 2、2 件のテスト名 |
| U-S12 | 古い XML にだけ failure | 古い XML を拾わない |
| U-S13 | `build.gradle` の評価エラー / 依存解決失敗 | BUILD、exit 2 + systemMessage (M03) |
| U-S14 | 印のない非 0 終了 | UNKNOWN、exit 2 + systemMessage |
| U-S15 | ウォッチドッグ (テスト用の上限 3 秒) | TIMEOUT、green 更新なし |
| U-S16 | 状態ファイルなし / 破損 / clean 後 | 未検証として Gradle 実行 |
| U-S17 | stop_hook_active=true・同一 (FP, EK) の失敗 | Gradle 0 回、exit 0 |
| U-S18 | 新しいターン (stop_hook_active=false)・同一 FP の失敗 | 再検証する (M02) |
| U-S19 | 指紋を変えながら 3 回連続で失敗 (同じセッション) | 3 回目は exit 0 + systemMessage (修正予算、M04) |
| U-S20 | `intentional-red` あり + TEST 失敗 | exit 0 + systemMessage、マーカー削除、green 更新なし |
| U-S21 | `CLAUDE_GATE_FORCE=1` | 必ず Gradle 実行 |
| U-S22 | 出力するすべての JSON | 妥当 (改行・`"`・`\`・Windows パスを含む) |
| U-G1〜G9 | ガード単体: `cd "C:/x" && git reset --hard origin/main`、`echo "x"; git push --force origin main`、`rm -rf build`、`gh repo delete x`、PowerShell `Remove-Item -Recurse -Force C:/x`、通常の `ls`、空入力、不正な JSON、説明文に `rm -rf` を含む無害なコマンド | 最初の 5 件と空入力・不正 JSON は exit 2、`ls` は exit 0、最後の件は exit 2 (誤検出は許容。件数を記録) |
| U-G10 | ガードのレイテンシ n=10 | 中央値が現行実測の 821 ms 以下 |

### 5.2 権限エンジン (使い捨てクローン、A2・A3 承認後)

| ID | 内容 | 期待 |
|---|---|---|
| E-1 | deny の各行を、PATH 先頭の無害な偽 `git` / `gh` (呼び出しを記録するだけで、リモートに届かない) を使って Claude Code から依頼 | 実行されない (偽コマンドに記録なし) |
| E-2 | ask の各行 | 確認が出る (bypass モードでも) |
| E-3 | 複合: `cd "x" && git push --force`、`echo a; git reset --hard HEAD~1` | deny または ask |
| E-4 | PowerShell の別名: `rm -r -fo x`、`ri x -Recurse` | ask (別名の正規化) |
| E-5 | ルールが一致しない形 (`bash -c 'git push --force'`) | ガードの全文スキャンで exit 2 (二層目が効くこと) |
| E-6 | 非破壊操作の誤確認: `git restore --staged x`、`git clean -n` | 前者は ask (仕様どおり、M5 で計数)、後者は確認なし |

### 5.3 故障注入 (`onFailure: block` の検証。A4 承認後に `claude -p` で実施)

| ID | 注入 | 期待 |
|---|---|---|
| F-1 | ガードのスクリプトを一時的に存在しないパスへ | Bash の実行がブロックされる |
| F-2 | ガードが exit 1 | ブロック |
| F-3 | ガードが 20 秒 sleep (timeout 15) | ブロック |
| F-4 | ガードが不正な JSON を出して exit 0 | ブロック (onFailure の "invalid output") |
| F-5 | Stop が exit 2 + stderr | Claude が継続し、理由を受け取る (契約の実機確認) |
| F-6 | Stop が systemMessage だけを出す | ユーザーに表示され、継続しない |

### 5.4 実 Gradle による意図的失敗 (使い捨てクローン、A3 承認後)

| ID | 操作 | 期待 |
|---|---|---|
| R1 | 新しいクローン (green なし) で Stop | 検証を実行 → PASS → green |
| R2 | 変更なしで Stop | Gradle 0 回 |
| R3 | main の Java に構文エラー | COMPILE、ファイル:行 |
| R4 | 失敗する assert | TEST、テスト名 |
| R5 | `JAVA_HOME` を存在しないパスに (green ありの状態から) | EK が変化 → 検証 → ENV、green 更新なし |
| R6 | ウォッチドッグ上限 5 秒 | TIMEOUT。Java クライアントとビルドが残らない (実 Gradle での kill の確認。Critic は単純なバッチの子プロセスでのみ確認済み) |
| R7 | 修正 → PASS → 変更なしで Stop | 2 回目は Gradle 0 回。daemon 利用時もクローン削除がロックで失敗しない |
| R8 | 失敗のまま stop_hook_active=true | Gradle 0 回、継続しない |
| R9 | 未追跡の Java を追加 / R10 Java を削除 | 指紋が変化 → 検証 |
| R11 | README に `PlatformClaimTest` の禁止表明を追加 | `test --rerun` で TEST 失敗を検出 (D11 回避の確認) |
| R12 | `build.gradle` に構文エラー | BUILD (Claude へ返す) |
| R13 | 存在しない依存座標を追加 (`--offline`) | BUILD |
| R14 | `gradlew clean` 後の Stop | 状態なし → 検証を実行 |

### 5.5 Gradle Daemon の計測 (使い捨てクローン、A3 承認後。探索的な判定)

| ID | シナリオ | 試行 |
|---|---|---|
| P-1 | `help` | no-daemon 3 回、daemon は cold 1 回 (別記録) + warm 3 回。交互に実行 |
| P-2 | `test --rerun` 変更なし | 同上 |
| P-3 | main の Java 1 ファイルに同じ差分 (メソッド本体の変更) を与えて `test --rerun` | 同上。各回で test が実行されたことをログで確認 |
| P-4 | `spotlessApply -PspotlessIdeHook=<file>` / `spotlessCheck` | 各構成 1 回。単一ファイル整形が働くかも確認 |

- 時計は Python の `time.monotonic` (macOS でも使える)。終了コード・タスクの実行/UP-TO-DATE・JDK と引数を記録する。各構成 1 回は `--profile` (ローカル HTML)。
- 判定 (事前登録): P-2 と P-3 の warm 中央値がともに no-daemon より短く、全試行で失敗・ロック異常が 0 なら daemon を採用する。差が小さい (例: 各中央値の差が 1 秒未満) 場合は追加で 3 回ずつ測る。`-Xmx2g` はヒープの上限であり、常駐プロセス全体のメモリは別途記録する。
- 試行の計: P-1〜P-3 で 21 回、P-4 で 4 回 (推定 10〜15 分、未計測)。

### 5.6 導入後の監視 (追加費用なし)

`metrics_v2.py` とゲートの `metrics.jsonl` で、導入後 3 セッション以上 (または Stop 30 回以上) を集計する。

| ID | 指標 | 合格条件 |
|---|---|---|
| M1 | プロジェクト Hook の hook_non_blocking_error / hook_cancelled | 0 件 |
| M2 | (FP, EK) が green と一致した Stop の ran_gradle と所要時間 | ran_gradle=false が 100%。所要時間の中央値は報告するだけ (導入前は 19.0 秒 n=1、18.5 秒 n=31) |
| M3 | rc≠0 のうち PASS と記録されたもの | 0 件 |
| M4 | 整形 1 回あたりの edited_text_file 件数と文字数、タスクあたりの整形コマンド数 | 整形の対象が変更ファイルだけであること (件数 ≤ そのタスクで変更した Java ファイル数) |
| M5 | ask による確認の回数と、その内訳 (破壊的 / 非破壊的) | 報告し、ルールの狭め方を判断する材料にする |

## 6. 完了条件 (すべて満たした場合のみ完了。Hook の高速化だけでは完了にしない)

1. §5.1 の全項目が PASS。
2. §5.4 の R1〜R14 で期待どおりの分類と経路を確認 (意図的な失敗を含む)。
3. §5.5 の計測を行い、daemon の採否を記録。
4. 承認された権限ルールを設定し、§5.2 の E-1〜E-6 を確認。
5. (A4 承認時) §5.3 の F-1〜F-6 を確認。承認されない場合は「未検証」として報告に残す。
6. `gradlew check` が 1 回成功。
7. `CLAUDE.md` の記述が実装と一致。
8. (導入後) §5.6 の M1〜M5 を報告。

## 7. 変更範囲

| 変更する (C-1) | 変更する (C-1b、別承認) | 変更しない |
|---|---|---|
| `.claude/settings.json`、`.claude/hooks/final-check.sh` (書き換え)、`.claude/hooks/block-destructive.sh` (書き換え)、`format-java.sh` と `run-related-test.sh` (削除)、`.claude/hooks/tests/**` (新規)、`CLAUDE.md` の Hook 表、(daemon 採用時) `.claude/skills/gradle-runner/SKILL.md` | `build.gradle`: spotless の `ratchetFrom`、test タスクの文書入力宣言 | `src/**`、`assets/**`、`gradle.properties`、セーブ形式、docs の計画文書、`.claude/settings.local.json`、ユーザーの stash@{0} |

## 8. リスク・守れない範囲・ロールバック

- **守れない範囲 (明記)**: 権限ルールと Hook はどちらも文字列への照合であり、意図的な迂回 (別プログラム経由、エンコード) は防げない。Codex Worker には効かない。Codex はサンドボックス設定 (A5) と独立クローンでしか制限できない。ユーザーの stash は同じリポジトリの `refs/stash` にあり、リポジトリへの書き込み権限を持つプロセスなら破棄できる (A6 のバックアップ ref で、少なくとも到達不能化は防げる)。
- 共有設定の変更はチーム全員に効く (ask の確認が増える) → A2。
- デーモンの常駐 (メモリ・ファイルロック) → R7・P で確認。採用しなければ no-daemon を維持する。
- `test --rerun` によって、変更のある応答ではテストが毎回フル実行される。所要時間は P-3 で計測する。
- ロールバック: PR を revert する (ゲーム本体とセーブには影響しない)。

## 9. 承認が必要な事項

| ID | 内容 | 影響・費用 |
|---|---|---|
| A1 | C-1 の実装着手 (§7 の C-1 列。専用ブランチ、PR の下書きまで。push とマージは別途) | Implementer 1 本 (Codex、独立クローン) + 独立 Critic 1 回。修正は最大 2 回 |
| A2 | 権限ルールの各行 (deny/ask) の採否 | チーム全員の操作に影響 |
| A3 | 使い捨てクローンでの実 Gradle (§5.4 の約 16 回、§5.5 の約 25 回) | ローカル CPU のみ。推定 20〜30 分 (未計測)、外部送信なし |
| A4 | (任意) `claude -p` による故障注入と実機での契約確認 (§5.2 E-1〜E-6 の一部、§5.3) | 少量の API 使用 |
| A5 | 実装 Worker の Codex を YOLO ではなくサンドボックス付き (書き込みを作業ディレクトリに限定) で起動する設定の確認・変更 | Orca/Codex の設定。ユーザー環境に依存 |
| A6 | (任意) stash@{0} のバックアップ ref の作成 (例: `git branch backup/stash-20261030 stash@{0}`) | リポジトリに ref が 1 つ増える |
| A7 | C-1b (`build.gradle` の変更) | ビルド設定の変更。チーム全員に影響 |
