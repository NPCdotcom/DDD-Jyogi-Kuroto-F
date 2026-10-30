# C-1 品質ゲート復旧 実装仕様 v2 (承認待ち・未実装)

作成: 2026-10-30 / 対象: `main` @ 60ac172 (= origin/main) / Claude Code 2.1.314 / 前版: `orca_spec_F1_quality_gate.md` (v1)
ラベル: **[実測]** 今回スクリプト・実験で取得 / **[確認済み]** コード・公式文書で確認 / **[前回引用]** report_final.md から / **[設計]** 本仕様の提案 / **[未計測]**

---

## 1. 前提にする公式 Hook・権限の契約 (2.1.314 で使う範囲) [確認済み]

出典: https://code.claude.com/docs/en/hooks , /permissions , /permission-modes (2026-10-30 取得)。インストール版は `claude --version` = 2.1.314。

| 項目 | 契約 | 本仕様での使い方 |
|---|---|---|
| command Hook の `args` | `args` があると `command` を実行ファイルとして直接起動 (シェルなし)。`${tool_input.*}` は展開されない (mcp_tool の input 専用) | `args` は使わない。入力は stdin の JSON から読む |
| 入力 | stdin に JSON (common: session_id, transcript_path, cwd, permission_mode, hook_event_name)。Stop は `stop_hook_active` 等。Windows の file_path はバックスラッシュ | Stop は `stop_hook_active` のみ参照。変更検出は git で行い file_path に依存しない |
| 終了コード | 0 = 成功 (JSON を適用)、2 = ブロック (PreToolUse は実行阻止、Stop は停止を阻止し stderr を理由として会話継続)、その他 = 非ブロッキングエラー | 修正可能な失敗は exit 2 + stderr (JSON エスケープ不要) |
| exit 0 の不正 JSON | 非ブロッキングエラー扱いで action は続行 (フィードバックは捨てられる) | JSON を出す経路は専用エスケープ関数 + fixture で妥当性を検査 |
| `systemMessage` | ユーザーに表示される警告 (Claude を継続させない) | 環境起因の失敗 (Claude が直せない) の通知に使う |
| Stop の継続保護 | `stop_hook_active` と連続 8 回上限 | 指紋が変わらない限り再実行しない |
| `onFailure` | v2.1.295 以上。`"block"` で失敗 (起動不可・exit 0/2 以外・timeout・不正出力) 時に action を阻止。Stop には無効 | PreToolUse ガードに `"block"` |
| `timeout` | command の既定 600 秒 | Stop は 330 秒 (スクリプト内上限 300 秒)、ガードは 15 秒 |
| matcher | 英数字と `\|` だけなら完全一致のリスト | `Bash\|PowerShell` |
| 権限ルール | deny → ask → allow の順で最初の一致が決定。**deny は bypassPermissions を含む全モードで有効、ask も bypass で確認を求める**。Bash/PowerShell とも複合コマンドの各部分に適用。`*` は空白を含む任意文字列 | 破壊的操作の一次防御を権限ルールに置く |
| 権限ルールの限界 | 「コマンド文字列に対する照合でありセキュリティ境界ではない」(`/bin/rm`、`bash -c '…'`、`git -C . push` などは一致しない) | Hook の生入力スキャンで補完。完全防御とは書かない |

## 2. 現状の欠陥 (C-1 の根拠)

| # | 欠陥 | 区分 | 根拠 |
|---|---|---|---|
| D1 | PostToolUse 2 本が毎回 ENOENT (`command`+`args` の併用) | [確認済み・実測] | `.claude/settings.json:16-33`、セッション記録の hook_non_blocking_error (8 月 8 件、現行 2.1.314 でも Write ごとに発生) |
| D2 | 仮に起動しても Windows 形式パスでテスト対応付けが無言 skip | [実測] | `hook_experiments/RESULTS.md` §3 |
| D3 | Stop Hook が終了コードを捨て、起動失敗を成功扱い | [実測] | RESULTS.md §2 (exit 1 → exit 0・無出力) |
| D4 | 失敗要約が 2 行以上だと不正 JSON → フィードバック消失 | [実測] | RESULTS.md §2 (`Invalid control character`) |
| D5 | 変更のない応答でも毎回 Gradle 起動 (`--no-daemon` でも単発デーモンで JVM 2 個) | [実測] | 本セッション Stop 19.0 秒 (ビルド 12.4 秒・UP-TO-DATE)、8 月 n=31 中央値 18.5 秒 [前回引用] |
| D6 | ガードの引用符入り抽出切れ・PowerShell 対象外・timeout で素通り | [実測] | RESULTS.md §1、8 月の hook_cancelled 2 件 [前回引用] |
| D7 | ガードの deny メッセージ内バッククォートがコマンド置換として実行 | [実測] | RESULTS.md §1 |
| D8 | プロジェクト標準の Gradle 実行 Skill と計画文書が `--no-daemon` を指示 | [確認済み] | `.claude/skills/gradle-runner/SKILL.md:31,40`、`docs/20260812_development_task_plan.md` に 37 箇所 |
| D9 | 整形を後から何度も一括適用し、差分が会話へ注入 | [実測] | 8 月: edited_text_file 71 件・snippet 359,680 文字、うち Java 52 件が spotlessApply を含むコマンド直後 (`metrics_v2_old_session.txt`) |
| D10 | Codex Worker は Claude の Hook・権限ルールの対象外 | [確認済み] | Orca 起動の Codex 画面表示 `permissions: YOLO mode` (本 Run の 3 Worker)。Claude の設定は Claude Code にしか効かない |

## 3. 要求事項 → 設計 → 検証の対応

| ユーザー要求 | 設計 (§4) | 検証 (§5) |
|---|---|---|
| JSON 標準入力を正しく処理 | stdin 全量読み込み。`[^"]*` 抽出を廃止。Stop は真偽値キーの正規表現のみ、ガードは最小限の JSON アンエスケープ後に全文スキャン | F-G1〜G6、F-S1 |
| Java 変更を正しく検出 | git による内容ベースの指紋 (HEAD + `git diff --binary HEAD` + 未追跡ファイル内容、`-z` で空白・引用符を含むパスも扱う) | F-S2〜S6、R5、R9、R10 |
| Windows パス・空白・引用符・エスケープ | パスは git の相対パス (`-z`) で扱い Windows 絶対パスに依存しない。ガードの fixture に Windows パスと引用符 | F-G2〜G5、F-S6 |
| コンパイル失敗・テスト失敗・Gradle 起動失敗の区別 | 終了コード + ログのタスク失敗表示 + 今回生成された JUnit XML で COMPILE / TEST / GRADLE / TIMEOUT に分類 | F-S7〜S10、R3〜R6 |
| 失敗を黙って成功にしない | 終了コード非 0 は必ず非 PASS。PASS 以外で green を更新しない。分類不能は UNKNOWN として通知 | F-S7〜S11、R5、監視 M3 |
| 変更のない応答で全テストを走らせない | 指紋が前回 green と一致すれば Gradle を起動しない | F-S2、R1、R2、監視 M2 |
| Gradle Daemon の有効性を計測 | §5.3 のプロトコル (n=3) と判定規則。採否が決まるまで Skill の `--no-daemon` 撤去は保留 | D-1〜D-4 |
| 整形差分を会話へ大量投入しない | ゲートは整形しない (spotlessApply を Hook から外す)。整形はタスク末尾に 1 回、PR 前の `check` で spotlessCheck | 監視 M4 |
| 破壊防止を Hook だけに頼らない | `permissions.deny/ask` を一次防御、Hook は補助。Codex Worker は専用 worktree で隔離 (D10) | F-G、R11、承認 A2 |

## 4. 設計 [設計]

### 4.1 `.claude/settings.json` (共有設定)

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
      "Bash(git reset --hard*)", "Bash(git clean *)", "Bash(git stash drop*)", "Bash(git stash clear*)",
      "Bash(git checkout -- *)", "Bash(git restore *)", "Bash(rm -rf *)", "Bash(rm -fr *)",
      "PowerShell(Remove-Item *)", "PowerShell(git reset --hard*)", "PowerShell(git clean *)",
      "PowerShell(git stash drop*)", "PowerShell(git stash clear*)"
    ]
  },
  "hooks": {
    "PreToolUse": [
      { "matcher": "Bash|PowerShell",
        "hooks": [ { "type": "command", "command": "bash \"${CLAUDE_PROJECT_DIR}/.claude/hooks/block-destructive.sh\"", "timeout": 15, "onFailure": "block" } ] }
    ],
    "Stop": [
      { "hooks": [ { "type": "command", "command": "bash \"${CLAUDE_PROJECT_DIR}/.claude/hooks/final-check.sh\"", "timeout": 330 } ] }
    ]
  }
}
```

- PostToolUse の format / related-test は削除する (編集回数に比例して Gradle を起動する構造ごと廃止)。
- deny と ask の区分はポリシー判断なので**承認事項** (A2)。`ask` は bypass モードでも確認を求める (公式)。stash の破棄 (`git stash drop/clear`) を ask にするのは、ユーザーの stash@{0} を守るため。
- 引用符付きの `"${CLAUDE_PROJECT_DIR}/…"` は空白を含むパス対策 (シェル形式なので有効)。

### 4.2 `final-check.sh` (Stop) — 状態機械

```
入力: stdin JSON → stop_hook_active (正規表現 "stop_hook_active"\s*:\s*true)
状態: build/.claude-gate/ (build/ は .gitignore 済み) に last-green / last-result / metrics.jsonl / last-run.log
1. 指紋 FP = sha256( git rev-parse HEAD
                   + git diff --binary HEAD -- src assets build.gradle settings.gradle gradle.properties gradle
                   + 未追跡ファイル (git ls-files -z --others --exclude-standard -- 同上) の パス+内容ハッシュ )
2. FP == last-green           → exit 0 (Gradle を起動しない)            [目標: 1 秒前後]
3. FP == last-result.FP (前回失敗のまま変更なし)
      → Gradle を起動せず、systemMessage「前回の検証失敗が未解決 (変更なし)」を JSON で出して exit 0 (継続させない)
4. それ以外 → Gradle を起動:
      run.start を touch → "$GRADLEW" test --console=plain > last-run.log 2>&1 (daemon 方針は §4.6)
      移植可能なウォッチドッグ (sleep+kill。macOS に timeout コマンドが無いため) で 300 秒上限
5. 分類 (終了コードを主根拠にする):
      rc == 0                                       → PASS     : last-green=FP、出力なし、exit 0
      ウォッチドッグ発火                             → TIMEOUT  : systemMessage、green 更新なし、exit 0
      rc != 0 かつ ログに compileJava/compileTestJava の失敗表示
                                                    → COMPILE  : `*.java:<行>: error:` を最大 10 件、stderr、exit 2
      rc != 0 かつ run.start より新しい XML に <failure|<error>
                                                    → TEST     : 失敗テスト (class#method + 1 行目) 最大 10 件、stderr、exit 2
      rc != 0 かつ上記以外 (JAVA_HOME 不正・依存解決・デーモン起動失敗等)
                                                    → GRADLE   : 先頭のエラー行最大 5 行を systemMessage、exit 0 (Claude が直せない)
      その他・解析不能                              → UNKNOWN  : systemMessage、green 更新なし
6. 会話へ送る文は 1,500 文字以内。詳細はログのパス (build/.claude-gate/last-run.log) を示すだけ。
7. 毎回 metrics.jsonl に {start,end,duration_ms,ran_gradle,category,rc,fingerprint_changed,daemon} を 1 行追記。
```

- stop_hook_active が true の時: 手順 3 と同様に、指紋が変わっていなければ Gradle を起動しない (ループ防止)。
- JSON を出す経路 (systemMessage) は bash の `json_escape` 関数 (`\`→`\\`、`"`→`\"`、改行/CR/タブ→`\n`/`\r`/`\t`、その他の制御文字は除去) を通す。exit 2 の経路は stderr の平文なのでエスケープ不要。
- JAVA_HOME は既存の環境変数を優先し、未設定で既定パスが存在する時だけ補う。

### 4.3 `block-destructive.sh` (PreToolUse・補助層)

- stdin 全量を読み、`\\`→`\`、`\"`→`"`、`\n`/`\t`→空白 の最小アンエスケープ後、**入力全体**に対して照合する (フィールド抽出をしない。説明文などへの誤検出は安全側として許容)。
- パターン (大文字小文字無視): 既存 (rm -rf 系、git push --force/-f、git reset --hard origin、gh repo delete、gh repo edit --visibility、sudo) + PowerShell `Remove-Item … -Recurse`。それ以外の追加は ask ルール側 (§4.1) で扱い、Hook のパターンは増やさない。
- 一致時は **exit 2 + stderr に理由** (JSON 不要。PreToolUse の exit 2 は実行阻止)。ヒアドキュメントは使わない (D7 の再発防止)。
- 外部プロセスを減らす (bash の `[[ =~ ]]` で照合し、パターンごとの echo|grep を廃止)。目標は現行の中央値 821 ms 以下。
- `onFailure: "block"` で、タイムアウト・異常終了時は実行を止める (fail-closed)。

### 4.4 PostToolUse の撤去

`format-java.sh` と `run-related-test.sh` を削除する (残すと誤解を招く)。編集ごとの整形・テストは行わない。

### 4.5 文書と Skill

- `CLAUDE.md` の「Hooks の挙動」表を新仕様に合わせる (権限ルールとの二層、Stop の状態遷移、整形はタスク末尾に 1 回)。
- `.claude/skills/gradle-runner/SKILL.md` の `--no-daemon` は §5.3 の判定で daemon 採用となった場合に限り外す。
- 計画文書 (`docs/20260812_development_task_plan.md` の 37 箇所) は履歴文書として変更しない (CLAUDE.md の現行規則が優先と明記)。

### 4.6 Gradle Daemon の方針

§5.3 の計測で採否を決める。採用する場合は Stop Hook と Skill から `--no-daemon` を外す (デーモンは `-Xmx2g` で常駐し、既定のアイドル 3 時間で終了する。Windows のファイルロックとの干渉を R7 で確認)。不採用なら `--no-daemon` を維持し、指紋による省略だけで効果を得る。

## 5. 検証計画

### 5.1 fixture テスト (Gradle 不要・数秒) — `bash .claude/hooks/tests/run.sh`

スタブは環境変数 `CLAUDE_GATE_GRADLEW` で差し替える (Windows の `gradlew.bat` 経路もバッチスタブで検証する。今回の実験で bash スタブだけでは Windows 経路を通らないことを確認済み)。

| ID | 入力・状況 | 期待 |
|---|---|---|
| F-S1 | `stop_hook_active: true`、指紋変化なし | Gradle 呼び出し 0 回 |
| F-S2 | 指紋 == last-green | Gradle 呼び出し 0 回、出力なし |
| F-S3 | 未追跡の .java を追加 | 指紋が変化し Gradle 起動 |
| F-S4 | 追跡ファイルを削除 | 指紋が変化 |
| F-S5 | assets / build.gradle / gradle.properties の変更 | 指紋が変化 |
| F-S6 | 空白・引用符・日本語を含むファイル名の追加 | 指紋が変化、スクリプトが異常終了しない |
| F-S7 | スタブ: exit 1・stderr のみ (JAVA_HOME 不正) | GRADLE、systemMessage の JSON が妥当、green 更新なし |
| F-S8 | スタブ: compileJava 失敗 + `A.java:3: error:` | COMPILE、exit 2、stderr に `A.java:3` |
| F-S9 | スタブ: test 失敗 + 新しい XML (failure 2 件) | TEST、exit 2、2 件のテスト名 |
| F-S10 | スタブ: 古い XML だけ失敗を含む (今回の XML は成功) | 古い XML を拾わない |
| F-S11 | スタブ: 300 秒超 (テスト用に上限 3 秒) | TIMEOUT、green 更新なし |
| F-S12 | 失敗後に変更なしで再 Stop | Gradle 0 回、systemMessage のみ、exit 0 |
| F-S13 | 失敗後に修正して PASS | last-green 更新、次回は Gradle 0 回 |
| F-S14 | 出力する全 JSON | `json.loads` 相当で妥当 (改行・`"`・`\`・Windows パスを含むメッセージ) |
| F-G1〜G7 | ガード: RESULTS.md §1 の 7 入力 + `git stash drop` + 空入力 + 不正 JSON | 破壊系は exit 2、通常コマンドは exit 0、空/不正入力で異常終了しない |
| F-G8 | ガードのレイテンシ n=10 | 中央値 ≤ 821 ms (現行実測) |

### 5.2 実 Gradle による意図的失敗テスト (使い捨てクローン) — 要承認 (A3)

`git clone --local` で scratchpad に main を複製して実施する (ユーザーの作業ツリーと stash に触れない)。

| ID | 操作 | 期待カテゴリ・経路 |
|---|---|---|
| R1 | 変更なしで Stop | Gradle 起動なし、metrics に ran_gradle=false |
| R2 | `.md` だけ変更 | Gradle 起動なし |
| R3 | main の Java に構文エラー | COMPILE、exit 2、ファイル:行 |
| R4 | テストに失敗する assert | TEST、exit 2、テスト名 |
| R5 | `JAVA_HOME` を存在しないパスに | GRADLE、systemMessage、green 更新なし (現行は成功扱い) |
| R6 | ウォッチドッグ上限を一時的に 5 秒 | TIMEOUT、Gradle クライアントが残らない |
| R7 | 修正後に Stop を 2 回 | 1 回目 PASS、2 回目 Gradle 起動なし。デーモン利用時はファイルロックで clone 削除が妨げられないこと |
| R8 | `stop_hook_active: true` を stdin で与えて失敗状態のまま | Gradle 起動なし、継続させない |
| R9 | 未追跡の Java ファイルを追加 | 指紋変化 → Gradle 起動 |
| R10 | Java ファイルを削除 | 指紋変化 → Gradle 起動 (コンパイル失敗等を正しく分類) |
| R11 | (権限ルール) 承認後に Claude Code で `cd "x" && git push --force` 等を依頼 | deny で実行されない (ルールは静的に JSON 構文検査、動作は R11 の 1 回だけ実機確認) |

### 5.3 Gradle Daemon の計測 (使い捨てクローン) — 要承認 (A3)

| ID | シナリオ | 構成 | n |
|---|---|---|---|
| D-1 | `help` (起動と設定のみ) | `--no-daemon` / daemon (1 回目 cold は別記録、以降 warm) | 各 3 |
| D-2 | `test` 変更なし (UP-TO-DATE) | 同上 | 各 3 |
| D-3 | `test` main の Java 1 ファイルのコメント変更後 | 同上 | 各 3 |
| D-4 | `spotlessCheck` / `spotlessApply -PspotlessIdeHook=<file>` | 同上 | 各 1 |

- 記録: 壁時計 (bash の `date +%s%N`)、終了コード、各構成 1 回は `--profile` (ローカル HTML、外部送信なし)。
- 判定 (事前登録): D-2 と D-3 の中央値が daemon で短く、9 回中の失敗・ロック異常が 0 なら daemon を採用。どちらかを満たさなければ `--no-daemon` を維持。
- 見込み時間: 約 22 回の Gradle 実行 (推定 10〜15 分、未計測)。

### 5.4 Claude Code 実機での契約確認 (任意・要承認 A4)

クローン内で `claude -p` を 1〜2 回実行し、Stop Hook の exit 2 経路と systemMessage 経路が 2.1.314 で文書どおりに働くかを確認する (API 使用が発生するため任意)。行わない場合は、導入後の通常セッションの記録で確認する (§5.5)。

### 5.5 導入後の監視 (追加費用なし)

`metrics_v2.py` とゲートの `metrics.jsonl` で、導入後 3 セッション以上 (または Stop 30 回以上) を集計する。

| ID | 指標 | 合格条件 |
|---|---|---|
| M1 | プロジェクト Hook の hook_non_blocking_error / hook_cancelled | 0 件 (ENOENT 解消) |
| M2 | 変更のない Stop の ran_gradle と所要時間 | ran_gradle=false が 100%、所要中央値 1 秒前後 (目標値。導入前 19.0 秒 n=1 / 18.5 秒 n=31) |
| M3 | rc≠0 の実行のうち PASS 扱い | 0 件 |
| M4 | セッションあたり edited_text_file の件数・snippet 文字数、spotlessApply を含むコマンド数 | 導入前 (71 件・359,680 文字・spotlessApply 含む 41 回) から減少していること (具体的な削減率は目標にしない) |

## 6. 完了条件 (すべて満たした時のみ完了。Hook の高速化だけでは完了にしない)

1. §5.1 の全 fixture が PASS。
2. §5.2 の R1〜R10 で期待カテゴリと経路を確認 (R5・R6 など意図的失敗を必ず含む)。
3. §5.3 の計測を実施し、daemon の採否を記録。
4. 承認済みの権限ルールを設定し、R11 を 1 回確認。
5. `gradlew check` が 1 回成功。
6. `CLAUDE.md` の Hook 記述が実装と一致。
7. (導入後) §5.5 の M1〜M4 を報告。

## 7. 変更範囲

| 変更する | 変更しない |
|---|---|
| `.claude/settings.json` / `.claude/hooks/final-check.sh` (書き換え) / `.claude/hooks/block-destructive.sh` (書き換え) / `.claude/hooks/format-java.sh`・`run-related-test.sh` (削除) / `.claude/hooks/tests/**` (新規) / `CLAUDE.md` の Hook 表 / (daemon 採用時のみ) `.claude/skills/gradle-runner/SKILL.md` | `src/**`、`build.gradle`、`gradle.properties`、`assets/**`、セーブ形式、docs の計画文書、`.claude/settings.local.json`、ユーザーの stash@{0} |

## 8. リスクとロールバック

- 共有設定の変更はチーム全員の Claude Code に効く (ask は確認が増える)。→ ルール一覧を承認事項 A2 に分離。
- デーモン常駐によるメモリ (2 GB 上限) とファイルロック → R7 と §5.3 で確認。不採用時は `--no-daemon` 維持。
- Codex Worker には Claude の権限・Hook が効かない (D10) → 実装 Worker は専用 worktree に限定し、ユーザーの作業ツリーと stash を作業範囲から外す。Codex 側の承認設定の見直しは別途ユーザー判断。
- ロールバック: 当該 PR の revert (ゲーム本体・セーブには影響しない)。

## 9. 承認が必要な事項

| ID | 内容 | 費用・影響 |
|---|---|---|
| A1 | C-1 の実装着手 (§7 の変更範囲、専用ブランチ、PR 作成まで。push・マージは別途) | 実装は Codex Worker 1 本 + 独立 Critic 1 回を想定 |
| A2 | 権限ルール (§4.1 の deny/ask の各行) の採否 | チーム全員の操作に影響 |
| A3 | 使い捨てクローンでの実 Gradle 実行 (§5.2 R1〜R10、§5.3 の約 22 回) | ローカル CPU のみ、推定 10〜15 分、外部送信なし |
| A4 | (任意) `claude -p` による実機契約確認 1〜2 回 | 少量の API 使用 |
