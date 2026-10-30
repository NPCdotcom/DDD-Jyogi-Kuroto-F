# DDD-Jyogi-Kuroto-F 総合調査レポート (Lead ドラフト v1 / Critic レビュー前)

作成: 2026-10-30 / Orca Run `run_e05f7624b620` / Read-only 調査 (repo 変更・commit・push・Gradle 実行・GUI 起動・有料 API 0 件)

ラベル: **[確認済み]** = コード・ログ・公式文書で裏付け / **[有力な仮説]** = 機構は確認済みだが発生条件や影響量が未実測 / **[未検証]** = 根拠不足

パス表記: `main:` は local main (60ac172) を `git archive` した読み取り専用コピー
`{S}\main` からの相対パス。

---

## 0. 前提と調査対象の確定

| 項目 | 事実 | 根拠 |
|---|---|---|
| 作業ツリー | `docs/consistency-sprint-plan` @ cec61b4 (2026-08-12)。**main より 36 コミット遅れ** | `git rev-list --left-right --count main...HEAD` = 36/0 |
| 作業ツリーの未コミット差分 | 9 文書。**全て main の同名ファイルと同一内容** (固有の作業なし) | `git show main:<f>` とバイト比較 |
| 調査対象 | **local main @ 60ac172 (2026-08-15)** | 最新の統合コード |
| origin/main | e8f19bd (2026-08-14)。**local main の 6 コミットが未 push** (セーブ・Soul 精算・装備消失の修正を含む) | `git rev-list --left-right --count main...origin/main` = 6/0 |
| 過去 Run `run_ac27d9fcc2b8` | 現行 Orca ランタイムで `run_not_found`。ローカルの Claude/Codex セッション記録にも痕跡なし (今回セッション以外) → **再利用不可**。過去報告の内容は仮説として照合 | `orca orchestration run-show/run-list`、`~/.claude/projects`、`~/.codex/sessions` の grep |
| CI | `.github/workflows` 不在。品質ゲートはローカル Hook と手動実行のみ | main: `.github/` |

---

## A. 現状分析

### A-1. 規模・構成 [確認済み]

- Java 25 + LibGDX 1.14.0 + LWJGL 3.4.1、Gradle 9.5.0、Spotless 8.4.0 (google-java-format 1.35.0)、単一モジュール。
- main: 本体 136 クラス / 18,531 行、テスト 95 クラス / 13,631 行、`@Test` 936 + `@ParameterizedTest` 2。
- テスト分布: domain 531 / application 30 / infrastructure 215 / presentation 160。
- レイヤー規則は概ね遵守: `core.domain` の import は `java.*`・`core.domain`・Jackson のみ、`core.application` は `java.*`・`core.domain`・自層のみ (grep で違反 0)。例外的に `domain/save/SoulSettlement.java` が `java.util.logging` を使用 (副作用の分離原則からは軽微な逸脱)。
- テスト実行自体は高速: 既存 JUnit XML (2026-08-12、761 テスト) の suite 時間合計は 4.7〜5.6 秒。

### A-2. 機能の接続状況

| 機能 | 状態 | 根拠 |
|---|---|---|
| ゲームループ Title→Dungeon→GameOver→SoulTree→Title | 接続済み [確認済み] | `main:src/main/java/core/presentation/screen/TitleScreen.java` 新規開始、`DungeonScreen.java` → `GameOverScreen`、`GameOverScreen.java:114` → SoulTree |
| 戦闘 (TurnEngine/TurnDirector/敵 AI/敵ターン可変テンポ) | 接続済み [確認済み] | `DungeonScreen.java:246-300`、`EnemyTurnIntervalTest` |
| セーブ (Profile / RunCheckpoint 分離・移行 journal・層境界保存・再開・放棄精算) | 接続済み [確認済み]。ただし **層境界セーブ失敗はログのみでプレイヤー通知なし** | `DddGame.java:630-658` (`LOG.severe` のみ) |
| 結果画面の異常系 | **SAVE_FAILED は無言で再試行、INVALID_STATE は入力が no-op で退路なし** (アプリ終了しかない) [確認済み・発生は異常系限定] | `GameOverScreen.java:108-124`、表示文は状態によらず同じ (`:99-104`) |
| 装備 (所有モデル・所有品のみ表示/装着) | 接続済み [確認済み]。保護枠トグル UI は未実装 | `EquipmentScreen.java:71,81`、`DddGame.java:186` |
| ソウルツリー (22 ノード、座標 SSoT) | 接続済み [確認済み]。旧「一個飛ばしに見える」は座標統一で対策済み | `SoulTreeLayout.java`、commit a3ac8e7 |
| **新カード描画 `CardRenderer.drawCardComposite` (枠・名称・コスト・アートの分離描画)** | **本番コードからの呼び出し 0 件 = 未接続** [確認済み] | grep: 呼び出しは `CardRendererTest` のみ。手札は `HudRenderer.java:235` の `CardRenderer.drawCard` (完成カード PNG を 120×168 へ引き伸ばし)、図鑑は `CardCollectionScreen.java:122` (32×42 サムネ)、ツリーは `SoulTreeScreen.java:201-209` |
| カード画像の自動検証 | `CardImageValidatorTest` は**合成画像のみ検査し、実 PNG を一度も読まない** [確認済み] | 同テストの 3 テスト (`:197,:223,:237`) は TempDir/メモリ画像のみ |
| カード報酬抽選 | presentation 層に残存 (LibGDX + Random 依存、レアリティ未反映) [確認済み] | `EliteRewardOrchestrator.java:3-13,32` |
| レアリティデータ | 59 枚中 55 枚が `rarity` 未定義 (COMMON 扱い) [確認済み] | `main:src/main/resources/cards.json` |

### A-3. 過去報告 (2026-10-29) との照合

| 過去の指摘 | 現行 main での判定 | 根拠 |
|---|---|---|
| 924 テスト中 1 件失敗 | **[未検証]**。main は 936+2 テストで本数が異なる。過去報告の対象コミット・環境が不明 (Run 消失)。8/14 時点の文書は「924 全件 PASS」(e8f19bd 相当)。静的には特定不能 → F-0 の計測で 1 回だけ確認を提案 | `docs/20260814_adversarial_verification_review.md:13` |
| カードのコスト表示不整合 | **[確認済み]** 手札は PNG の焼き込みコストを表示。AP 不一致 36/59 | Worker A 監査、`HudRenderer.java:225-235` |
| 装備画面 | 全開放は**対策済み**。残課題は行クリック判定 (x 制限なし)・最後の装備解除時の無言拒否 [確認済み] | `EquipmentScreen.java:133-155` (Worker B F19) |
| ソウルツリー | 座標二重管理は**対策済み** (main)。実機の全ラベル視覚 QA は未実施 | a3ac8e7、`SoulTreeLayout.java:41` |
| 泉イベント後の進行不能候補 | **origin/main (e8f19bd) では実在する経路**: 泉で Soul を消費し `finalRunSoul < initialRunSoul` になると `SoulSettlement.settle` が拒否 → 精算・Checkpoint 削除がスキップされ、獲得 Soul 消失とラン残骸が残る。**local main (60ac172) で修正済み・未 push**。main では代わりに「異常時の結果画面に退路がない」問題が残る | `git show e8f19bd:.../SoulSettlement.java` の `finalRunSoul < initialRunSoul` 拒否、e8f19bd の `DddGame.onRunEnded`、main `SoulSettlement.java:41-51` |

### A-4. 仕様書・文書と実装の不一致 [確認済み]

1. `CLAUDE.md:165-167` (Hook 表): 「Java 編集後に spotlessApply」「該当 FooTest 即実行」は **実際には ENOENT で毎回失敗** (B-2)。
2. `CLAUDE.md:82` 「save/ 層単位セーブ (未実装)」→ 実装済み (16 クラス)。`CLAUDE.md:88` 「effect/ DamagePopup のみ実装」→ `LowHpWarning` も実装済み。冒頭の M1.5/ハッカソン (5/23-24) 日程も陳腐化。
3. `SoulSettlement.java:11-12` の Javadoc は「`finalRunSoul < initialRunSoul` は empty を返す」と記すが、60ac172 の実装は `settled < 0` と int 超過のみ拒否。
4. `Strings.java:139/365` 「HP 全回復 / Full HP heal」 vs `LayerEndNode.Rest` の実装 `max(1, maxHp/3)` (`LayerEndNode.java:89-99`、コメントの「30% 切り上げ」とも不一致)。
5. `tasks/todo.md:240-278` の実装バックログは全行「未着手/待機」のまま (SAVE-01〜03B 等は main で完了済み)。
6. G1 文書の Phase 7 手順 (240×336 アートを `drawCardComposite` へ接続) は、Renderer のアート枠 (基準 108×72 = 横長 1.5) と画像仕様 (`CardImageValidatorTest`: 240×336 縦長・上部 240px がアート) が矛盾し、そのまま接続すると約 2 倍の横伸びになる (`CardRenderer.java:121-129` はテクスチャ全体を枠へ描画、UV 切り出しなし)。

### A-5. テストが保証するもの / しないもの

- 保証: ドメイン規則 (AP・ダメージ・カード山札・精算純関数・Checkpoint 不変条件・移行写像)、レイアウト定数の制約、テンポ計算。
- **保証しない** [確認済み]:
  - `DddGame` のラン・ライフサイクル統合 (`startNewRun`/`onRunEnded`/`abandonActiveRun`/`loadFromSave`/`saveAtLayerBoundary`/`resolveLayerEndChoice`)。`DddGame` は LibGDX `Game` 継承のためテストで生成できないと `SaveLifecycleContractTest` 自身が明記。
  - 画面の入力排他 (Elite 報酬中の入力・敵進行、F15)、クリック座標変換 (F10)、実 PNG と cards.json の整合。
  - `tasks/todo.md:498` の記録: 実機通しで「自動テスト 875 件と敵対的検証 8 回をすり抜けた」所見が 2 件。

### A-6. 設計負債 (変更コストを上げている箇所)

1. **`DddGame` (792 行) への責務集中** [確認済み]: LibGDX ライフサイクル + ラン/セーブ/精算/装備/ツリー操作。全履歴で最多変更ファイル (46 コミット)。2026-08-12 以降の 14 コミット中 10 件が `fix:` (自作退行の修正を含む)。テスト不能な場所にバグ源が集中している。
2. カード表示経路が 2 系統 (使われる `drawCard` / 使われない `drawCardComposite`) + 画像仕様も 2 系統 (現行完成カード PNG / 未承認の 240×336 art-only)。
3. 文書の量と陳腐化: docs/tasks 計 8,272 行。過去セッションで `.md` の Read は 31 回 (66K 文字)、状態表の陳腐化が AI の誤認を誘う。

### A-7. 強みと低コストで効く改善余地

- ドメイン層の純度とテスト密度は高い。保存層は fail-closed 設計で、移行 journal まである。
- 既存資産で効くもの: (a) 既に書かれた `drawCardComposite` + 既存 PNG のアート切り出し (Worker A: 60/60 枚がクロップ流用候補、新規生成必須 0)、(b) 既に計測可能なセッション記録 (追加計装なしでボトルネック集計可能)、(c) Orca の Run/Task/Dispatch。

---

## B. ボトルネック分析

### B-1. 計測済みの値

データ源: (1) Claude Code セッション記録 `ffc4d2c5…jsonl` (2026-08-12〜14、worktree、claude-opus-5、Claude Code 2.1.227→2.1.229、**サンプルは 1 セッション**)、(2) 同 subagents 12 本、(3) JUnit XML 2 回分、(4) 本調査セッション。再現: `scratchpad/analyze_hooks.py`、`scratchpad/analyze2.py`。

| 指標 | n | 中央値 | p95 | 合計 / 備考 |
|---|---:|---:|---:|---|
| 人間プロンプト 1 回あたりの AI 稼働時間 | 41 | 4.4 分 | 26.5 分 | 316 分 |
| AI 応答終了→次のプロンプトまで (≤120 分のみ) | 34 | 4.7 分 | 72.9 分 | 586 分 (AI 稼働より長い) |
| 計画承認待ち (ExitPlanMode) | 6 | 5.3 分 | 20.7 分 (max) | 42 分 |
| AskUserQuestion 待ち | 3 | 16.5 秒 | 112 秒 (max) | |
| AI が手動実行した Gradle (git 系除く) | 33 | — | — | **1,457 秒 (24 分)**、FAILED 12/33 (TDD の意図的 RED を含み分離不能) |
| └ `spotlessApply --no-daemon` | 14 | **74.9 秒** | 92.0 秒 | 1,008 秒 = Gradle 時間の 69% |
| └ `test --tests X --no-daemon` | 10 | 23.0 秒 | 40.2 秒 | 234 秒 |
| └ `check --no-daemon` | 5 | 33.0 秒 | 57.0 秒 | 152 秒 |
| Stop Hook `final-check.sh` (全テスト、毎応答終了時) | 31 | **18.5 秒** | 22.4 秒 | 597 秒、**フィードバック出力 0/31** |
| PostToolUse Hook (format / related-test) | 記録 8 件 + 本セッション 2 件 | 8〜15 ms | — | **全件 ENOENT** |
| PreToolUse `block-destructive.sh` タイムアウト | 2 | 5.9 秒 | — | timeout 5 秒超過で**素通り** |
| テスト実行本体 (JUnit suite 時間合計) | 2 回 | 4.7〜5.6 秒 | — | 761 テスト |
| サブエージェント (devils-advocate 11 + compliance 1) | 12 | 9.0 分 | 12.5 分 | 101 分 |
| Java 編集 (Edit/Write) | 149 | — | — | main 102 / test 47 |

トークン・費用 (公式価格で API 換算。サブスクリプション利用なら実請求ではなく相対比較用):

| 区分 | API 呼出 | 出力 | キャッシュ読込 | キャッシュ書込 | API 換算 |
|---|---:|---:|---:|---:|---:|
| Lead セッション (Opus 5) | 668 | 0.67M | **321M** | 8.5M | $230〜262 |
| サブエージェント (Opus 5 ×11) | 198 | 0.32M | 14.5M | 1.3M | $23〜28 |
| サブエージェント (Sonnet 5 ×1) | 9 | 0.01M | 0.6M | 0.1M | ≈$0.5 |
| **合計** | | | | | **≈$254〜291** |

- 価格 (2026-10-30 公式ページ): Opus 5 入力 $5 / 5 分キャッシュ書込 $6.25 / 1 時間書込 $10 / キャッシュ読込 $0.50 / 出力 $25 per MTok。範囲はキャッシュ TTL (5 分/1 時間) の違い。
- **費用の約 6 割はキャッシュ読込 = 巨大コンテキストの再読込**。出力は 1 割弱。Lead の 1 呼出あたり平均コンテキスト ≈ 481K トークン、サブエージェントは ≈ 73K (約 6.6 倍の差)。
- コンテキスト構成 (Lead、文字数): 外部変更ファイル通知 `edited_text_file` が最大 22.6%。Java 66 件のうち **52 件 (約 287K 文字 ≈ 80K トークン) は手動 `spotlessApply` の直後**。整形を後追いで一括適用すると、整形差分がコンテキストに注入され、以降の全呼出で再読込される。
- 本調査セッション自体 (参考): 87 呼出時点で約 $6.6〜7.4 (Opus 5.5 価格)。

### B-2. Hook の実態 [確認済み]

1. **PostToolUse の format / related-test は機能していない**: `.claude/settings.json:16-33` が `command: "bash ${CLAUDE_PROJECT_DIR}/…sh"` と `args: ["${tool_input.file_path}"]` を併記。公式仕様では `args` があると `command` を**実行ファイル名として直接起動** (シェルなし) するため、`'bash C:\…\format-java.sh'` という名前の実行ファイルを探して ENOENT になる。また `${tool_input.*}` の展開は mcp_tool の `input` 専用で、command Hook には stdin JSON で渡る。現行 2.1.314 で Write のたびに ENOENT を確認。8 月 (2.1.227) は記録が不完全だが、手動 `spotlessApply` 直後に整形差分通知が 52 件出ている事実から、編集ごとの自動整形は働いていなかったと強く推定 [有力な仮説]。
2. **仮に直しても設計どおりでは遅すぎる**: Java 編集 149 回 × spotlessApply 中央値 74.9 秒 ≈ **3.1 時間**、関連テスト 23 秒 × 149 ≈ 57 分 (同セッションの AI 稼働 316 分に対して)。しかも `format-java.sh` の timeout 60 秒は spotlessApply の中央値 74.9 秒より短い。
3. Windows では `tool_input.file_path` がバックスラッシュ区切りで届く (公式)。`run-related-test.sh:40` の `src/main/java/…` 正規表現は一致しない → stdin 対応だけ直してもテスト対応付けは無言で skip される [確認済み: 文書と正規表現の突合]。
4. **Stop Hook** (`final-check.sh`): 毎応答終了時に `--no-daemon` で全テスト (変更の有無を問わない。Q&A でも走る)。`--quiet` のため成功時は無出力、失敗時の JSON に失敗テスト名はほぼ入らない (quiet では `FAILED` 行が出ない) [有力な仮説]。`stop_hook_active` を見ていないため、失敗が続くと Stop→継続 が上限 8 回まで連鎖しうる (公式: additionalContext は会話を継続させ、ループ保護は stop_hook_active と 8 回上限) [有力な仮説: 機構は確認済み、発生は未観測]。
5. **破壊的コマンドガード**: matcher が `Bash` のみで **PowerShell ツールは素通り** (この環境では両方が有効、過去セッションで PowerShell 37 回)。timeout 5 秒は Windows Git Bash 起動で超過しうる (2 回観測)。公式仕様で PreToolUse の timeout は呼び出しを止めない (fail-open)。`git reset --hard` (origin 以外)、`git clean -fdx`、`Remove-Item -Recurse` は対象外。
6. Gradle 設定: `org.gradle.caching=true`・`parallel=true` は有効だが、Hook と AI の実行は常に `--no-daemon` で daemon を捨てている。configuration cache 未設定。`build.gradle:96-99` の testLogging が `passed` も出力するため、非 quiet 実行ではテスト 1 件ごとに 1 行がコンテキストへ流れる。

### B-3. 未計測の項目 (仮説と計測方法)

| 仮説 | 計測方法 (低オーバーヘッド) | コスト |
|---|---|---|
| Gradle 1 回の大半は JVM/Gradle 起動・設定で、daemon 化で 10〜20 秒短縮 | `help` / `test --tests X` / `spotlessApply` / `test` を `--no-daemon` と warm daemon で各 1 回、`--profile` (ローカル HTML、外部送信なし) | 約 5 分、要承認 |
| spotlessApply 75 秒の主因 (起動・gjf 初期化・全ファイル走査のどれか) | `--profile` + 単一ファイル整形 (`-PspotlessIdeHook=<file>`) との比較 | 上記に含む |
| main のテストは全件 PASS か (過去の「924 中 1 失敗」) | 上記の `test` 1 回で JUnit XML を回収 | 上記に含む |
| Orca Worker の起動・待機時間 | `worker-start` の receipt と `worker-list` の時刻 (本 Run では 1 本が 30 秒以内に turn 開始を観測できず `start_unknown`) | 0 (既存ログ) |
| Codex のトークン・費用 | `~/.codex/sessions/**/rollout-*.jsonl` の token_count イベント | 0 |
| 人間の PR レビュー待ち | `gh pr list --state merged --json createdAt,mergedAt` | 読み取りのみ、ネットワーク |
| テストの flaky 率 | 実行ごとの JUnit XML を保存して比較 | 実行時に数 ms |

### B-4. 優先して測るもの

1. Gradle の daemon / no-daemon 差と spotlessApply の内訳 (改善 C-1 の効果予測に直結)。
2. main のテストの現状 (以降の全改善の基準線)。
3. Hook 修正後の Stop Hook 所要時間と ENOENT 件数 (既存のセッション記録から自動集計)。

---

## C. 改善候補一覧 (優先順)

| # | 改善 | 根拠 | 対象 | 影響 | 規模・リスク・依存 | 自動で検証できる完了条件 |
|---|---|---|---|---|---|---|
| C-1 | **品質ゲート (Hook) の修理と軽量化**: 壊れた PostToolUse 2 本は撤去 (編集ごとの整形・テストはしない)。Stop Hook は「前回グリーン以降に Java/resources 変更がある時だけ」daemon で実行、`stop_hook_active` を尊重、失敗テスト名を JUnit XML から返す、JSON は正しくエスケープ。破壊的コマンドガードは `Bash\|PowerShell` に拡張し timeout を延長 | B-1, B-2 | `.claude/settings.json`, `.claude/hooks/*.sh` | 毎応答 18.5 秒の固定費を変更時のみに、誤った安心感の解消、PowerShell 経由の破壊操作の防止 | S / 低 / 依存なし | Hook スクリプトを stdin JSON の fixture とスタブ gradlew で検証する bash テストが通る。導入後のセッション記録で ENOENT 0 件、無変更ターンの Stop Hook < 1 秒 |
| C-2 | **未 push の 6 コミットの扱いを決める** (push / PR) と作業ツリーを main 系へ | A-0 | git (人間) | GitHub 上は泉精算バグ等が残ったまま。クローンした人が古い不具合を踏む | XS / 低 / **人間の承認が必要** | `git rev-list main...origin/main` = 0/0 |
| C-3 | **Elite 報酬中の入力排他** (F15, High): 報酬ポップアップ表示中は `updateState` (敵進行・`playerInputs.poll`) を止め、開閉時にカード選択をリセット | Worker B F15 + Lead 再確認 (`DungeonScreen.java:200-221` で `eliteReward.isActive()` を見ずに `updateState`) | `DungeonScreen.java`, `EliteRewardOrchestrator.java` | 報酬を読んでいる間の被弾・数字キー二重処理の解消 | S〜M / 中 (戦闘ループ) | 入力ゲートを純関数に抽出し単体テスト。報酬中フレームで敵ステップ 0・手札選択不変 |
| C-4 | **結果画面の異常系の説明と退路** (F21, High) + 層境界セーブ失敗の通知 | `GameOverScreen.java:108-124`, `DddGame.java:656` | `GameOverScreen.java`, `Strings.java`, `DungeonScreen` (通知) | 異常時のフリーズ様状態の解消。精算規則は不変 | S〜M / 中 (保存の不変条件を壊さない) | `RunEndResult`→表示文・可能操作の写像を純関数化して全 4 状態をテスト |
| C-5 | **表示と実際の効果の一致 (文言バッチ)**: 休憩 (F03)、泉/ショップの支払い可否を確定前に判定 (F16)、イベント結果の差分表示 (F17)、放棄確認の L 表記 (F04)、「基礎威力」表記 (F06)、結果画面の今回増減 (F18) | Worker B + Lead 再確認 (F03) | `Strings.java`, `LayerEndNode`, `NodeChoicePopup`, `DungeonScreen` | 初見の誤解と「選んだのに何も起きない」体験の解消 | S×6 / 低 / `Strings.java` を触る他タスクと直列 | 文言テスト (`InputHelpTextTest` 型) と LayerEndNode 適用前後の差分テスト |
| C-6 | **手札の AP・番号・使用可否の動的表示** (F05) | 焼き込み AP 不一致 36/59、AP 不足の事前表示なし | `HudRenderer.java` | 最も実害の大きい「誤コスト表示」を緩和 | S / **中: 焼き込み数字と二重表示になる恐れ** → カード外側への表示を推奨 | 手札描画モデルの純関数テスト (AP 不足時の状態) |
| C-7 | **カード描画を `drawCardComposite` へ移行 (既存 PNG のアート切り出し)** | 44/59 で名称・AP・レアリティ差、効果文欠落 48。Worker A: 60/60 がクロップ流用候補 | `CardRenderer`, `HudRenderer`, `CardCollectionScreen`, `SoulTreeScreen`, アート切り出し資産 | データとの不一致を構造的に解消 | M / 中 / **人間の判断: 既存 PNG の出所・利用許諾 (LICENSES/INDEX.md に記載なし)、見た目の方針、アート枠の縦横比** | 全 59 枚の描画モデルが cards.json 由来であることのテスト、実 PNG を読む資産テスト |
| C-8 | **ラン・ライフサイクルの `DddGame` からの抽出** (LibGDX 非依存のアプリケーションサービス) と E2E テスト | A-6 の変更・修正集中 | `DddGame.java` → `core.application` | 最頻出のバグ源をテスト可能に | M / 中〜高 (保存回帰) / C-1 後、特性テストを先に | 開始→層境界保存→再開→死亡/クリア→再起動 を LibGDX なしで通すテスト |
| C-9 | `CLAUDE.md` と `tasks/todo.md` の事実更新 (Hook 表、save/effect、日程、バックログ状態) | A-4 | 文書 | AI の誤前提を減らす | XS / 低 / C-1 の後 | 文書中の Hook 記述がテスト済みスクリプトと一致 |
| C-10 | UI の次点群: クリック座標の FitViewport 統一 (F10, M)、タイトルのクリックメニュー (F13)、ライトテーマ背景 (F07)、操作説明の再表示 (F01)、設定変更でウィンドウサイズが戻る (F23)、装備行クリック (F19)、選択カード当たり判定 (F20)、Elite ポップアップ resize (F11) | Worker B | presentation 各所 | 操作性 | S〜M | 各入力の純関数化とテスト |

優先順位の理由: C-1 は確認済みの故障であり、毎ターンの固定費になっている。しかも以降の全改善の計測と検証の土台になる。C-3/C-4 は High のプレイヤー被害。C-5/C-6 は小さい割に体験の誤解を減らす。C-7/C-8 は価値が大きいが人間の判断と回帰リスクを伴うため、計測基盤とテストを先に整える。

---

## D. AI 自律開発アーキテクチャ案 (Orca)

### D-1. 1 サイクルの流れ (有界)

1. **発見** (Lead、読み取りのみ): 計測レポート (セッション記録パーサ + JUnit XML + git) とバックログから候補を最大 5 件。
2. **人間ゲート H1** (5 分): 今サイクルの 1〜3 件と予算を承認。
3. **設計** (Lead): Task-spec 契約 (Target/Change/Constraints/Ownership/Observable acceptance) を書く。domain/save に触れる場合は domain-architect 相当のレビューを先に。
4. **実装** (Implementer、タスクごとに専用ブランチ/worktree): TDD。途中は対象テストのみ daemon で実行。最後に `spotlessApply` 1 回 + `check` 1 回。
5. **敵対的検証** (Critic、Implementer と別系統のモデル): diff と受入条件を照合し、Blocker/Major/Minor を JSON で返す (file:line と再現手順が必須)。
6. **修正**: 最大 2 回。
7. **再計測**: 同じパーサで前後比較。
8. **人間ゲート H2**: PR のレビューとマージ (自動マージなし)。

### D-2. 並列化の基準

- 並列: ファイル集合が重ならない独立タスク (例: Hook 修理と GameOver UX)。調査の fan-out は 1 本あたり 10〜15 分以上の作業がある場合のみ (Worker の起動に 30〜60 秒、文脈の再読込が必要)。
- 直列: `DddGame.java`・`DungeonScreen.java`・`Strings.java`・`HudRenderer.java` (変更集中ファイル) と保存層。`tasks/todo.md:291` も Strings.java の並列化を禁じている。

### D-3. モデルの割り当てとコスト制御

- 費用の主因はコンテキストの再読込 (B-1)。**長い Lead セッションを続けないこと**が最大の節約策。1 サイクル = 1 Run = 新しい Lead セッションとし、ファイル量の多い作業は新しい文脈の Worker へ渡す。
- Lead: 高性能モデル (判断・統合)。Implementer: S タスクは中位モデル (Sonnet 5.5 $2/$10、キャッシュ読込 $0.10) または Codex。保存・ドメイン不変条件・2 回失敗後は上位モデル (Opus 5.5 $4/$20、読込 $0.20) へ切り替え。
- 低価格モデル (Haiku 5.5 $0.10/$0.50 (≤100K)、OpenRouter の構造化出力対応の低価格モデル) は、出力がスキーマ検証でき決定的な検査がある作業に限る: 計測集計、ログ分類、変更履歴の下書き、テスト失敗の一次分類。コードレビューの合否判定やドメイン変更には使わない。
- 全部を高性能にする案: 品質は高いが、費用の大半は思考の質ではなく文脈の再読込なので、機械的な工程まで高単価で払うことになる。
- 全部を低価格にする案: 誤った PASS がいちばん高くつく (手動通しで初めて見つかる欠陥は自動テスト 875 件をすり抜けた実績がある)。修正の往復ごとに Gradle 20〜75 秒とレビューが必要なため、反復回数が 2 倍になれば単価差は消える。採用前に S タスク 5〜10 件で A/B を取り、「受理された PR 1 件あたりの費用・所要時間・後から見つかった欠陥」で比較する。
- **「Clef 系」モデルは OpenRouter の公開モデル一覧 (2026-10-30 取得、456 件) に該当 0 件**。正確な名称の確認が必要で、存在を前提にしない。

### D-4. 停止条件

- タスク単位: 実装→テスト→レビューは最大 3 周。壁時計 60 分。API 換算予算 (目安 S: $5 / M: $15) をセッション記録から集計して超過で停止。同じテストが同じエラーで 2 回失敗、または Critic の Blocker 数が減らなければ「進展なし」で停止。
- サイクル単位: 最大 3 タスク、Critic は各タスク 1 回 + 再確認 1 回まで。
- 停止時: Orca の Task を failed にして理由を記録し、次の人間ゲートへ。自動の巻き戻しはしない。
- 常時稼働のループ (cron 等) は今回は有効化しない。

### D-5. 状態管理と再開

- Orca の Run (サイクル) / Task (項目) / Dispatch (試行) を正とする。再開は `run-use --id` → `task-list --ready` → `worker-list`。
- タスクごとにブランチと worktree。計測結果は repo 外 (または .gitignore 下) に JSONL で保存。

### D-6. PR 単位の安全策

1 タスク = 1 PR (目安 300 行以下)。テスト追加・`check` 成功・Critic 報告の添付・人間の承認を必須とする。force push 禁止。セーブ形式の変更は移行テストと人間の承認を必須とする。

### D-7. 計測方式 (追加計装をほぼ不要にする)

| 対象 | ソース | 指標 |
|---|---|---|
| Claude | `~/.claude/projects/<proj>/*.jsonl` (+ subagents/) | ターン時間、ツール待ち、Gradle 呼出と所要、Hook 時間とエラー、モデル別トークンと API 換算、承認待ち |
| Codex | `~/.codex/sessions/**/rollout-*.jsonl` | token_count、所要 |
| Orca | `worker-list` / `worker-show` / Run・Task 時刻 | 起動・待機・settle 時間 |
| Gradle | JUnit XML、サンプリング時のみ `--profile` | テスト数・失敗・所要、タスク内訳 |
| Git/GitHub | `git log`、`gh pr list` | fix 比率、変更集中、PR 待ち時間 |

集計はサイクル終了時に 1 回だけ実行する。中央値・p95・n を出す。

---

## E. 敵対的検証の結果

(Critic レビュー後に追記)

---

## F. 次の実装フェーズ

- **F-0 (人間の承認が必要)**: (1) 未 push 6 コミットの扱い。(2) 基準線計測 1 回 (約 5 分、外部送信なし): repo 外のコピーで `test`/`spotlessApply`/`test --tests` を no-daemon と warm daemon で各 1 回、`--profile` 付き。(3) カード画像の方針 (C-7) と既存 PNG の出所確認。
- **F-1 = C-1 (品質ゲート修理)**: 前後比較は B-1 と同じパーサ。対象は無変更ターンの Stop Hook 時間 (18.5 秒→目標 1 秒未満)、ENOENT 件数 (→0)、変更ありターンの Gradle 時間 (daemon 化の効果)、セッションあたりの `edited_text_file` 文字数。
- **F-2 = C-3**、**F-3 = C-4** (それぞれ別 PR、直列)。
- **F-4 = C-9** (C-1 完了後)。

(Orca 用の実装指示文は最終版に添付)
