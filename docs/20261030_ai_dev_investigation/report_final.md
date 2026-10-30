# DDD-Jyogi-Kuroto-F 総合調査レポート (最終版)

作成: 2026-10-30 / Orca Run `run_e05f7624b620` (Task 3 件すべて completed、Worker 端末はすべて解放済み)
方式: Read-only。repo の変更・commit・push・Gradle 実行・GUI 起動・有料 API の呼び出しは 0 件。

ラベル: **[確認済み]** コード・ログ・公式文書で裏付けあり / **[有力な仮説]** 機構は確認済みだが発生条件や影響量は未実測 / **[未検証]** 根拠不足
`main:` は local main (60ac172) を `git archive` で書き出した読み取り専用コピー
(`{S}\main`) からの相対パスを表す。

成果物 (同じ scratchpad 配下):
- `report_final.md` (本書) / `report_draft.md` (Critic 前の版)
- `workers\uiux_audit.md` (Worker B: UI/UX 静的監査、所見 25 件)
- `workers\card_image_audit.md` (Worker A: カード画像 60 枚の対照表)
- `workers\critic_review.md` (独立 Critic: Blocker 1 / Major 7 / Minor 4)
- `analyze_hooks.py` / `analyze2.py` / `analyze3.py` (セッション記録の集計。analyze3 は Critic の指摘を受けた修正版)
- `orca_spec_F1_quality_gate.md` (次フェーズ用の Orca 実装指示文)

---

## 0. 前提: 調査対象の確定

| 項目 | 事実 | 根拠 |
|---|---|---|
| 作業ツリー | `docs/consistency-sprint-plan` @ cec61b4 (2026-08-12)。**main より 36 コミット遅れ** | `git rev-list --left-right --count main...HEAD` = 36/0 |
| 未コミット差分 | 9 文書 (追跡 2 + 未追跡 7)。**全て main の同名ファイルとバイト一致** (固有の作業はなし) | `git show main:<f>` と比較 |
| 調査対象 | **local main @ 60ac172 (2026-08-15)** | 最新の統合コード |
| origin/main | e8f19bd。**local main に未 push のコミットが 6 件ある** (保存・Soul 精算・装備消失の修正)。ネットワーク同期はしていないため、リモートの現況はローカルの追跡参照に基づく | `git rev-list --left-right --count main...origin/main` = 6/0 |
| 過去 Run `run_ac27d9fcc2b8` | `run_not_found`。ローカルの Claude/Codex 記録にも痕跡がないため**再利用できなかった**。過去報告は仮説として照合 | `orca orchestration run-show/run-list`、各記録の grep |
| CI | `.github/workflows` が存在しない。品質ゲートはローカル Hook と手動実行だけ | main: `.github/` |

---

## A. 現状分析

### A-1. 規模・構成 [確認済み]

- Java 25 + LibGDX 1.14.0 + LWJGL 3.4.1、Gradle 9.5.0、Spotless 8.4.0 (google-java-format 1.35.0)、単一モジュール。
- main: 本体 136 クラス / 18,531 行、テスト 95 クラス / 13,631 行、`@Test` 936 + `@ParameterizedTest` 2 (静的な本数。実行結果は未検証)。
- テストの層別本数: domain 531 / application 30 / infrastructure 215 / presentation 160。
- レイヤー規則は守られている。`core.domain` の import は `java.*`・自層・Jackson だけ、`core.application` は `java.*`・domain・自層だけ (grep で違反 0)。軽微な例外として `domain/save/SoulSettlement.java` が `java.util.logging` を使う。

### A-2. 機能の接続状況

| 機能 | 状態 | 根拠 |
|---|---|---|
| ゲームループ Title→Dungeon→GameOver→SoulTree→Title | 接続済み [確認済み] | `TitleScreen.java`、`DungeonScreen.java:653-659`、`GameOverScreen.java:114` |
| 戦闘 (TurnEngine/TurnDirector/敵 AI/可変テンポ) | 接続済み [確認済み] | `DungeonScreen.java:246-300` |
| セーブ (Profile/Checkpoint 分離・移行 journal・層境界保存・再開・放棄精算) | 接続済み [確認済み]。**層境界セーブが失敗してもログに出るだけで、プレイヤーには通知されない** | `DddGame.java:630-658` |
| 結果画面の異常系 | SAVE_FAILED は説明なしで再試行、INVALID_STATE は入力が no-op で退路なし [確認済み。異常系に限られ、通常プレイでの発生は未実証] | `GameOverScreen.java:99-124` |
| 装備 | 所有品だけを表示・装着する [確認済み]。保護枠の UI は未実装 | `EquipmentScreen.java:71,81`、`DddGame.java:186` |
| ソウルツリー | 座標の SSoT 化で旧不具合は対策済み [確認済み]。実機での視覚 QA は未実施 | `SoulTreeLayout.java:41`、a3ac8e7 |
| **新カード描画 `drawCardComposite`** | **本番コードからの呼び出しは 0 件 = 未接続** [確認済み]。手札は文字が焼き込まれた PNG を 120×168 に引き伸ばす `drawCard` (`HudRenderer.java:235`)、図鑑は 32×42 のサムネ (`CardCollectionScreen.java:122`)、ツリーはノードアイコン (`SoulTreeScreen.java:201-209`) | grep。定義は `CardRenderer.java:69`、呼び出しは CardRendererTest だけ |
| カード画像の自動検証 | `CardImageValidatorTest` の 3 テストは合成画像だけを検査し、**実際の PNG を一度も読まない** [確認済み] | 同テスト `:197-245` |
| カード報酬の抽選 | presentation 層にあり (LibGDX と Random に依存、レアリティは未反映) [確認済み] | `EliteRewardOrchestrator.java:3-13,32` |
| レアリティ | 59 枚中 55 枚が未定義 (COMMON 扱い) [確認済み] | `cards.json` |

### A-3. 過去報告 (2026-10-29) との照合

| 過去の指摘 | 判定 | 根拠 |
|---|---|---|
| 924 テスト中 1 件失敗 | **[未検証]**。main は静的に 936+2 本あり、本数が合わない。過去報告の対象コミットと環境は Run 消失のため不明。8/14 の文書は「924 全件 PASS」と記録 | `docs/20260814_adversarial_verification_review.md:13` |
| カードのコスト表示の不整合 | **[確認済み]** 手札は PNG に焼き込まれたコストを表示する。画像と AP が食い違うのは 36/59 枚 (Worker A の目視) | `HudRenderer.java:225-235` |
| 装備画面 | 全開放は**対策済み**。残るのは行クリック判定に x の制限がないこと (F19) と、最後の装備を外そうとした時に理由が出ないこと | `EquipmentScreen.java:133-155` |
| ソウルツリー | 座標の二重管理は**対策済み** | a3ac8e7 |
| 泉イベント後の進行不能候補 | **origin/main (e8f19bd) には実在する経路**。泉で Soul が減り `finalRunSoul < initialRunSoul` になると精算が拒否され、終了記録と Checkpoint 削除がスキップされる。その結果、再起動などの条件次第で成果の消失や古い Checkpoint の再開が起こりうる (画面はツリーへ進むので、即座に進行不能になるとは限らない)。**local main 60ac172 で修正済みだが未 push** | `git show e8f19bd:…/SoulSettlement.java`、main `SoulSettlement.java:41-51`、`RunCheckpoint.java:71-72` |

### A-4. 仕様書・文書と実装の不一致 [確認済み]

1. `CLAUDE.md:165-167` の Hook 表には「Java 編集後に spotlessApply」「関連テストを即実行」とあるが、実際には**毎回 ENOENT で失敗**している (B-2)。
2. `CLAUDE.md:82` に「save 未実装」、`:88` に「effect は DamagePopup のみ」とあるが、どちらも実装済み (save 系 16 クラス、`LowHpWarning`)。冒頭の M1.5 とハッカソン日程も古い。
3. `SoulSettlement.java:11-12` の Javadoc は「final<initial なら empty」と書くが、実装 (`:41-51`) は算出結果が負になる場合と int 超過の場合だけを拒否する。
4. 休憩の表示は「HP 全回復 / Full HP heal」(`Strings.java:139,365`) だが、実装は `max(1, maxHp/3)` (`LayerEndNode.java:89-99`)。コメントの「30% 切り上げ」とも違う。回復量は 47dc4da で意図的に変えられたので、**表示の方を直す**。
5. `tasks/todo.md:240-278` のバックログは全行が「未着手/待機」のまま。SAVE 系などは実装済み。
6. G1 文書の昇格手順 (240×336 のアートを `drawCardComposite` へ接続) は、Renderer のアート枠 (基準 108×72 の横長) と矛盾する。そのまま接続すると横方向に約 2.1 倍伸びる (`CardRenderer.java:121-129` はテクスチャ全体を枠に描き、UV の切り出しをしない)。

### A-5. テストが保証するもの / しないもの

- 保証する: ドメイン規則 (AP・ダメージ・山札・精算の純関数・Checkpoint の不変条件・移行写像)、`RunLifecycle` の契約、レイアウト定数の制約、テンポの計算。
- **保証しない** [確認済み]: `DddGame` が `RunLifecycle` を呼ぶ側の統合 (引数の組み立て、メモリへの反映、失敗時や二度目の呼び出し。`SaveLifecycleContractTest.java:21-22` 自身が LibGDX なしでは DddGame を生成できないと明記)、画面の入力排他 (F15)、クリック座標の変換 (F10)、実 PNG と cards.json の整合、描画の見え方。
- `tasks/todo.md:498` の記録によると、実機の通しプレイで「自動テスト 875 件と敵対的検証 8 回をすり抜けた」所見が 2 件あった。

### A-6. 設計負債

1. `DddGame` (792 行) に LibGDX ライフサイクル・ラン・保存呼び出し・装備・ツリー操作が集中している。全履歴で最も多く変更されたファイル (46 コミット) で、2026-08-12 以降に触れた 14 コミットのうち 10 件が `fix:` [確認済み]。変更回数はそのまま欠陥率を意味しないが、修正の集中は検証不足の兆候と考える [有力な仮説]。
2. カード表示の経路が 2 系統 (使われている `drawCard` と、使われていない `drawCardComposite`) あり、画像仕様も 2 系統 (現行の完成カード PNG と、未承認の 240×336 アートのみ画像) ある。
3. 文書が多く、内容が古くなっている: docs と tasks で計 8,272 行。過去セッションでは `.md` の Read が 31 回 (6.6 万文字) あった。

### A-7. 強みと、既存資産で効く改善

- ドメイン層の純度とテスト密度は高い。保存層は fail-closed で設計され、移行 journal も備えている。
- 既存資産で効くもの: (a) 書かれたまま使われていない `drawCardComposite` と、既存 PNG からのアート切り出し (条件付き。C-7 参照)、(b) 追加の計装なしで集計できるセッション記録、(c) Orca の Run/Task/Dispatch。

---

## B. ボトルネック分析

### B-1. 計測済みの値

データ源: Claude Code の過去セッション記録 1 本 (`ffc4d2c5…jsonl`、2026-08-12〜14、worktree、claude-opus-5、CLI 2.1.227→2.1.229) と subagents 12 本、JUnit XML 2 回分、本調査の Orca Run。**標本は実質 1 セッション**なので、一般化には注意が必要。p95 は観測値の順位で求めた値 (`round(0.95·(n-1))`)。再現: `python -I analyze3.py <jsonl>` (Gradle・応答時間・人間待ち)、`analyze_hooks.py` (Hook・ツール待ち)、`analyze2.py` (サブエージェント)。

| 指標 | n | 中央値 | p95 | 合計・備考 |
|---|---:|---:|---:|---|
| 人間のプロンプト 1 回あたりの応答経過時間 (承認待ちを含む) | 41 | 4.4 分 | 26.5 分 | 316 分 |
| 応答終了から次のプロンプトまで (0〜120 分の範囲、除外 6) | 34 | 4.7 分 | 72.9 分 | 586 分 |
| 計画承認待ち (ExitPlanMode) | 6 | 5.3 分 | 20.7 分 (max) | 42 分 |
| **Gradle を含むツール呼び出し** (Bash 30 + PowerShell 37) | 67 | 46.0 秒 | 82.1 秒 | **3,232 秒 = 53.9 分** (1 呼び出しに複数タスクが混ざる時間。Gradle 起動は計 108 回)。**67 件すべて `--no-daemon`**。結果は OK 37 / FAILED 18 / 不明 12 (FAILED には TDD の意図的な RED も含まれ、区別できない) |
| └ spotlessApply を含む呼び出し | 41 | 64.1 秒 | 84.0 秒 | **spotlessApply を単独で実行した呼び出しは 0 件**。Spotless 単体の時間は未計測 |
| Stop Hook `final-check.sh` (毎応答の終了時に全テスト) | 31 | 18.5 秒 | 22.4 秒 | 597 秒。変更の有無は区別できない。**フィードバックの出力は 0/31** |
| PostToolUse Hook (format / related-test) | 記録 8 件 | 8〜10 ms | — | **すべて ENOENT**。現行 CLI 2.1.314 の本セッションでも Write のたびに再現 |
| PreToolUse `block-destructive.sh` のタイムアウト | 2 | 5.9 秒 | — | 上限 5 秒を超えて打ち切られ、**ガードを通らずに実行された** |
| テスト実行本体 (JUnit の suite 時間合計、761 テスト時点) | 2 回 | 4.7〜5.6 秒 | — | 現行 main での性能は未検証 |
| サブエージェント (devils-advocate 11 本 + compliance 1 本) | 12 | 9.0 分 | 12.5 分 | 稼働時間の合計 101 分 (並列化による節約時間ではない) |
| Java 編集 (Edit/Write) | 149 | — | — | main 102 / test 47 |

**費用** (公式価格による API 換算。サブスクリプション利用時の実請求ではなく、相対比較のための値):

| 区分 | 呼び出し | API 換算 | 内訳 |
|---|---:|---:|---|
| Lead セッション (Opus 5) | 668 | **$256.24** | キャッシュ読込 63% / キャッシュ書込 31% / 出力 7% |
| サブエージェント (Opus 5 × 11、Sonnet 5 × 1) | 207 | $23.99 | |
| **合計** | | **$280.23** | 1 時間キャッシュの書込 6.88M、5 分キャッシュの書込 1.61M を実際の内訳で計算 |

- 適用単価 (2026-10-30 公式): Opus 5 は入力 $5 / 5 分書込 $6.25 / 1 時間書込 $10 / 読込 $0.50 / 出力 $25、Sonnet 5 は $2 / $2.50 / $4 / $0.20 / $10 (いずれも per MTok)。
- **費用の約 94% はコンテキストの書込と再読込**。出力は 7%。1 呼び出しあたりのキャッシュ読込は、Lead で平均約 48 万トークン、サブエージェントで約 7.3 万トークン。
- 外部変更の通知 (`edited_text_file`) は Java 関連が 66 件。うち 52 件 (snippet 本文で約 26 万文字) は、spotlessApply を含むコマンドの直後に出ている。後からまとめて整形すると、その差分が文脈に入り、以降の呼び出しのたびに再読込される [機構は確認済み。トークン量への換算は未定義]。
- 本調査 (参考): Lead は 123 呼び出し・約 57 分・API 換算 $12.43 (Opus 5.5)。Codex Worker 3 本は各 13〜19 分、入力は計約 1,090 万トークン (約 95% がキャッシュ)、出力は計約 11 万トークン。
- Orca の運用実測: `worker-start` 3 回のうち 2 回で「30 秒以内にターン開始を観測できず」と返った。1 本は実際には稼働していたが、もう 1 本 (Critic) は指示文が入力欄に残ったままで、手動で Enter を送るまで約 3 分止まっていた。

### B-2. Hook の実態

1. **[確認済み] PostToolUse の整形・関連テストは機能していない**: `.claude/settings.json:16-33` が `command: "bash ${CLAUDE_PROJECT_DIR}/…sh"` と `args` を併記している。公式仕様では、`args` があると `command` を実行ファイル名として**シェルを介さず直接起動**するため ENOENT になる。また `${tool_input.*}` の展開は mcp_tool の `input` 専用で、command Hook は stdin の JSON で値を受け取る。8 月 (2.1.227) 当時は記録が不完全だが、手動整形の直後に整形差分の通知が大量に出ていることから、自動整形は働いていなかったと強く推定される [有力な仮説]。
2. **設計どおりに直しても、Java 編集のたびに Gradle を起動する構造が残る**: 起動回数が編集回数 (149 回/セッション) に比例する。1 回あたりの単体コストは未計測 (B-3)。参考までに、Gradle を含む呼び出しは 67 件すべて `--no-daemon` で、混合時間の中央値は 46 秒。
3. **[確認済み]** Windows では `tool_input.file_path` がバックスラッシュ区切りで届く (公式)。`run-related-test.sh:40` の `src/main/java/…` という正規表現は一致しないため、テストの対応付けは何も言わずに skip される。
4. **Stop Hook**: Java の変更がない応答でも全テストを `--no-daemon` で実行する [確認済み]。`final-check.sh:35` は終了コードを `|| true` で捨て、`:38` の文字列 `FAILED` の有無で成否を推測している。そのため、起動失敗 (JAVA_HOME の誤りなど) で `FAILED` が出なければ、**失敗しても何も通知されない** [確認済み: コード読解。Critic M02]。`--quiet` 指定なので、失敗時の通知にテスト名はほぼ含まれない [有力な仮説]。`stop_hook_active` を見ていないため、失敗が続くと「停止→継続」が上限の連続 8 回まで連鎖しうる [有力な仮説: 機構は公式仕様で確認、発生は未観測]。
5. **破壊的コマンドのガード** [確認済み]: (a) matcher が `Bash` だけで、PowerShell ツールは通過する。(b) `block-destructive.sh:19` は grep/sed で JSON を読むため、`"` を含むコマンドでは抽出が途中で切れる。例えば `cd "C:/x" && git reset --hard origin/main` は `cd \` までしか抽出されず、**素通りする** (Critic M03 が実装と同じ正規表現で照合)。(c) timeout 5 秒で打ち切られると、そのまま実行される (fail-open)。(d) `git reset --hard` (origin 以外)、`git clean`、`Remove-Item -Recurse` は検査の対象外。
6. Gradle の設定: `caching=true` と `parallel=true` は有効だが、Hook と AI は常に `--no-daemon` で実行している。configuration cache は未設定。`build.gradle:96-99` が `passed` も出力するため、`--quiet` なしで実行するとテスト 1 件ごとに 1 行が文脈に流れ込む。

### B-3. 未計測の項目と計測方法

| 仮説 | 計測方法 (低負荷) | コスト |
|---|---|---|
| Gradle 1 回の大半は起動と設定で、daemon を使えば短縮できる | repo の外のコピーで `help` / `test --tests X` / `spotlessApply` / `test` を no-daemon と warm daemon で各 1 回、`--profile` 付き (ローカル HTML のみ、外部送信なし) | 約 5〜8 分、**要承認** |
| Spotless の単体時間と主因 | 上と同じ回で、`-PspotlessIdeHook=<file>` による単一ファイル整形とも比較 | 上に含む |
| main のテストは全件 PASS か (「924 中 1 失敗」の照合) | 上の `test` 1 回で JUnit XML を回収 | 上に含む |
| Orca Worker の起動と待機 | `worker-start` の受領結果と `worker-list` の時刻 | 0 (既存ログ) |
| Codex のトークン | `~/.codex/sessions/**/rollout-*.jsonl` の token_count | 0 |
| 人間による PR レビュー待ち | `gh pr list --state merged --json createdAt,mergedAt` | 読み取りのみ (ネットワーク) |
| テストの flaky 率 | 実行ごとに JUnit XML を保存して比較 | 数 ms |

### B-4. 優先して測る項目

1. Gradle の daemon 有無による差と Spotless の単体時間 (C-1 の効果予測に直結)。
2. main のテストの現状 (以降のすべての改善の基準線)。
3. C-1 導入前後の Stop Hook 時間と ENOENT 件数 (セッション記録から自動で集計できる)。

---

## C. 改善候補一覧 (優先順。Critic の指摘を反映済み)

| # | 改善 | 問題と根拠 | 対象 | 影響 | 規模 / リスク / 依存 | 自動で検証できる完了条件 |
|---|---|---|---|---|---|---|
| **C-1** | **品質ゲートの修理** | B-2 (壊れた PostToolUse、毎応答の全テスト、失敗を見逃す判定、ガードの迂回経路) | `.claude/settings.json`, `.claude/hooks/*` | 応答ごとの固定費を減らし、通知されない失敗をなくし、破壊操作の迂回を塞ぐ | S〜M / 低 / なし | 下記 F-1 の受入条件 |
| C-2 | **未 push の 6 コミットの扱いを決める** (push か PR) | GitHub 側は泉の精算不具合などを含む古い版のまま | git (**人間**) | クローンした人が古い不具合を踏まなくなる | XS / 低 / **人間の承認** | 内容の統合を確認 (squash する場合は rev-list ではなく diff で確認) |
| **C-3** | **Elite 報酬ポップアップ中の入力排他** (F15, High) | `DungeonScreen.java:200-221` は `eliteReward.isActive()` を見ずに `updateState` (敵の進行と `playerInputs.poll`) を回す。数字キーが手札選択と報酬選択の両方に使われる [確認済み] | `DungeonScreen`, `EliteRewardOrchestrator` | 報酬を読む間の被弾と、二重入力をなくす | S〜M / 中 (戦闘ループ) | 入力ゲートを純関数に抽出してテストする。報酬表示中のフレームで敵のステップ数 0・手札選択が不変・開閉時に選択がリセットされること |
| **C-4a** | **結果画面の異常系の説明** (F21 の前半) + 層境界セーブ失敗の通知 | `GameOverScreen.java:99-124` | `GameOverScreen`, `Strings`, HUD | 異常時に「固まった」と誤解されなくなる | S / 低 (表示だけ) | `RunEndResult` から表示文への写像を 4 状態すべてでテスト |
| C-4b | 異常系の退路 (F21 の後半) | 退路を作ると、タイトルに戻した後の新規開始判定がディスク上の Checkpoint しか見ないため (`RunLifecycle.java:61-63`)、1 層目で失敗した場合などに未精算の状態が破棄されうる (Critic M04) | 同上 + `DddGame` | 安全な退路 | M / **中〜高 (保存)** / 「データ保持」の意味を先に決める | 失敗→再試行で成功、二重の show、INVALID_STATE、Checkpoint の有無のそれぞれで Profile/Checkpoint/runCount/Soul/RunSession の変化を呼び出しの境界で検証 |
| **C-5** | **表示と実際の効果を一致させる**: 休憩 (F03)、放棄確認の L 表記 (F04)、「基礎威力」表記 (F06)、イベント結果の差分表示 (F17)、結果画面の今回の増減 (F18) | 表示が実装と食い違う [確認済み] | `Strings` ほか | 初見の誤解を減らす | S×5 / 低 / `Strings.java` を触るタスクとは直列 | 文言のテストと、LayerEndNode 適用前後の差分テスト |
| C-5b | 泉/ショップを支払えない時の扱い (F16) | **3 択すべてが支払い不可になる組合せが実在する** (`DungeonScreen.java:395-411` の 8 候補から Shop・泉・装備購入が同時に出て、Gold 0・Soul 10 未満の場合)。「不可なら選び直し」にすると**進行不能になる** (Critic B01) | 仕様 → `NodeChoicePopup`, `DungeonScreen` | 「選んだのに何も起きない」をなくす | S〜M / **仕様の決定が必要** (スキップ選択肢を設けるか、無料の選択肢を必ず残すか。どちらも報酬分布が変わる) | 全部支払い不可・全額支払える・HP 満タンでの泉、の 3 ケースで消費・層遷移・重複適用を検証 |
| C-6 | 手札の番号・AP・使用可否を動的に表示 (F05) | 焼き込み AP の不一致 36/59、AP 不足を事前に見分けられない | `HudRenderer` | 最も害の大きいコスト誤表示を緩和 | S / 中: 焼き込みの数字と二重に見える恐れ → カードの外側に表示 | 手札描画モデルの純関数テスト |
| C-7 | カード描画を `drawCardComposite` へ移す | 名称・AP・レアリティのいずれかが食い違うのは 44/59 枚、効果文がないのは 48 枚。Composite 側は**レアリティと効果を描画せず**、`isJapanese` も未使用 (Critic M05) | `CardRenderer`, 手札、図鑑、ツリー、アート素材 | 不一致を構造的に解消 | M / 中 / **人間の判断が必要**: 既存 PNG の出所と利用許諾 (`LICENSES/INDEX.md` に記載なし)、5 枚で他カードの絵を共用してよいか、アート枠の縦横比 | まず 1 枚で試作し、日英・手札/図鑑/ツリーで視覚 QA。本番の呼び出し経路が cards.json を描くことをテスト。既存の詳細表示 (`CardPresentationText`) は維持 |
| C-8 | `DddGame` から `RunLifecycle` への呼び出しとメモリ反映を検証可能にする | A-5/A-6。ただし `RunLifecycle` が既に順序を担っており、application 層から infrastructure 層への依存は禁止 (Critic M06) | `DddGame` | 修正が集中している箇所に回帰の網を張る | M / 中〜高 / C-1 の後。**特性テストを先に書き**、境界は最小限 (LibGDX に依存しない素のクラス。DI やイベントバスは入れない)。抽出と挙動変更は別 PR | 持ち越し Soul・泉による負の差分・所有/保護装備・保存失敗・再開・放棄・二重終了を LibGDX なしで通す統合テスト |
| C-9 | `CLAUDE.md` と `tasks/todo.md` の事実を更新 | A-4 | 文書 | AI が誤った前提で動くのを減らす | XS / 低 / C-1 の後 | Hook の記述がテスト済みのスクリプトと一致 |
| C-10 | UI の次点群: クリック座標を FitViewport に統一 (F10)、タイトル項目のクリック対応 (F13)、ライトテーマの背景 (F07)、操作説明の再表示 (F01)、設定変更でウィンドウサイズが戻る問題 (F23)、装備の行クリック (F19)、選択カードの当たり判定 (F20)、報酬ポップアップの resize (F11) | Worker B | presentation | 操作性の向上 | 各 S〜M / ルールは変えない | 入力処理の純関数化とテスト |

**優先順位の理由**: C-1 は確認済みの故障で、毎ターンの固定費になっており、以降のすべての改善の計測と検証の土台になる。C-3 と C-4a は High の被害で、変更は小さい。C-5 は小さな変更で誤解を減らせる。C-5b・C-7・C-4b・C-8 は価値が大きい一方で、仕様判断・権利判断・保存の回帰リスクを伴うため、計測基盤と特性テストを整えてから着手する。
**ユーザー提示の課題設定からの変更**: カード画像の作り直しより先に、品質ゲート (C-1) と High の UX 2 件 (C-3/C-4a) を優先した。画像は「再生成」ではなく、既存素材の切り出しを第一候補にした (Worker A: 新規生成が必須の画像は 0。ただし 5 枚は他カードの絵の共用が条件)。

---

## D. AI 自律開発アーキテクチャ案 (Orca)

### D-1. 1 サイクルの流れ (上限あり)

1. **発見** (Lead、読み取りのみ): 計測 (analyze3 系 + JUnit XML + git) とバックログから候補を最大 5 件挙げる。
2. **人間ゲート H1**: 今サイクルで扱う 1〜3 件と予算を承認する。
3. **設計** (Lead): Task-spec 契約 (Target / Change / Constraints / Ownership / Observable acceptance) を書く。保存やドメインの不変条件に触れる場合は、先に特性テストの範囲を決める。
4. **実装** (Implementer、タスクごとにブランチと worktree を分ける): TDD で進める。途中は対象テストだけを daemon で実行し、最後に `spotlessApply` と `check` を 1 回ずつ実行する。
5. **敵対的検証** (Critic、Implementer とは別系統のモデル): diff と受入条件を照合し、Blocker/Major/Minor と file:line・再現手順を JSON で返す。
6. **修正**: 最大 2 回。
7. **再計測**: 同じパーサで前後を比較する。
8. **人間ゲート H2**: PR のレビューとマージ (自動マージはしない)。

### D-2. 並列化の基準 (暫定の運用値)

- 並列にする: ファイルも共有契約も重ならない独立タスク。調査の fan-out は、1 本あたり 10〜15 分以上の作業がある場合に限る (起動、文脈の再読込、統合と再検証の手間がかかる。閾値は測定値ではなく暫定値)。最初は 2 本程度にとどめ、単独で進めた場合と所要時間を比べる。
- 直列にする: `DddGame`・`DungeonScreen`・`Strings`・`HudRenderer` などの変更集中ファイル、保存契約、マスタデータ。同時に走る Gradle の資源競合 (`-Xmx2g` × 本数) も考慮する。

### D-3. モデルの割り当て

- 費用の主因は、コンテキストの書込と再読込 (94%)。**長い Lead セッションを続けないこと**が最大の節約策になる。1 サイクル = 1 Run = 新しい Lead セッションとし、ファイル量の多い作業は文脈の新しい Worker に任せる。
- Lead: 高性能モデル。Implementer: S タスクは中位モデル (Sonnet 5.5 は入力 $2 / 出力 $10 / 読込 $0.10) か Codex。保存やドメインの不変条件に触れる場合、または 2 回失敗した場合は上位モデル (Opus 5.5 は $4 / $20 / $0.20) へ切り替える。
- 低価格モデル (Haiku 5.5 は $0.10 / $0.50、10 万トークン以下。OpenRouter の低価格モデルで構造化出力に対応するもの) は、**品質ゲートにしない作業**に限る: ログの一次分類、要約、変更履歴の下書き。件数・集計・金額は Python で確定値を出す。安価なモデルの推定だけで FAIL/UNKNOWN を PASS に格上げしない。スキーマが保証するのは構造だけで、意味の正しさは保証しない。
- すべてを高性能モデルにする案: 費用の大半は思考の質ではなく文脈の処理なので、機械的な工程にまで高単価を払うことになる。
- すべてを低価格モデルにする案: Haiku と Opus の出力単価は 40 倍違うため、反復が 2 倍になってもトークン代の差は消えない。本当のリスクは、①誤って PASS と判定した欠陥が実機まで漏れること、②人間の確認と修正の時間、③上位モデルへ引き継ぐ際の文脈の再読込である。比較は「受理された成果 1 件あたりの総費用・所要時間・後から見つかった欠陥」で行う。S タスク 5〜10 件での A/B は探索用であり、安全性の証明ではない。
- **「Clef 系」モデル**: OpenRouter の公開モデル一覧 (2026-10-30 取得、456 件) に該当は 0 件だった。正確な名称を確認するまでは前提にしない。

### D-4. 停止条件

- タスク単位: 実装→テスト→レビューは最大 3 周、壁時計で 60 分まで。同じテストが同じエラーで 2 回失敗する、または Critic の Blocker 数が減らない場合は「進展なし」として停止する。
- 予算: **Dispatch の境界 (worker_done ごと、修正ラウンドごと) で累積使用量を確認する** (Critic M07)。予算の対象には Lead の按分・Critic・再試行・文脈の再読込を含める。単価からの推定値なので「目安での停止」とし、厳密な上限とは呼ばない。使用量が取れない場合は、壁時計と反復回数の上限で止める。
- サイクル単位: 最大 3 タスク。Critic は各タスク 1 回と再確認 1 回まで。
- 停止時は Orca の Task を failed にして理由を残し、次の人間ゲートへ回す。自動の巻き戻しはしない。cron などの常時稼働は有効化しない。

### D-5. 状態管理と再開

Orca の Run (サイクル) / Task (項目) / Dispatch (試行) を正とする。再開は `run-use --id` → `task-list --ready` → `worker-list --run`。稼働中の試行を勝手に作り直さない。Worker の起動後は、受領結果が `ready` でなければ `terminal read` で入力欄に残っていないかを確認する (今回 1 件発生)。

### D-6. PR 単位の安全策

1 タスク = 1 PR (目安 300 行以下)。テストの追加・`check` の成功・Critic 報告の添付・人間の承認を必須にする。force push は禁止。セーブ形式を変える場合は、移行テストと人間の承認を必須にする。文書だけの変更に無意味なテストは求めない。

### D-7. 計測方式

| 対象 | ソース | 指標 |
|---|---|---|
| Claude | `~/.claude/projects/<proj>/*.jsonl` (+ subagents) | 応答経過時間、ツール待ち、Gradle 呼び出し、Hook の時間とエラー、トークン (5 分/1 時間キャッシュの内訳込み)、承認待ち |
| Codex | `~/.codex/sessions/**/rollout-*.jsonl` | token_count、所要時間 |
| Orca | `worker-start` の受領結果、`worker-list` / `worker-show` | 起動、ターン開始の観測、settle までの時間 |
| Gradle | JUnit XML、サンプリング時だけ `--profile` | テストの件数・失敗・所要時間、タスク内訳 |
| Git/GitHub | `git log`、`gh pr list` | fix の比率、変更集中、PR 待ち |

パーサは小さく保つ (analyze3 程度)。必要なのは数件の fixture と、CLI のバージョン・欠測・除外件数を返すことだけ。常駐の収集基盤は作らない。

---

## E. 敵対的検証の結果 (独立 Critic: Codex gpt-6.1-sol、1 回)

| ID | 区分 | 指摘 | Lead の採否と理由 |
|---|---|---|---|
| B01 | Blocker | C-5 の「支払えない選択を止めて同じ 3 択を維持」は、3 つとも支払えない組合せで**進行不能**を生む | **採用**。`DungeonScreen.java:395-411` で候補 8 件から 3 件を抽選することを確認。F16 を C-5b として分離し、仕様の決定事項にした |
| M01 | Major | Gradle 集計が PowerShell 経由の起動を拾っておらず、git commit 文の中の文字列を誤検出し、spotlessApply の時間に他のタスクが混ざっていた | **全面採用**。`analyze3.py` を作って再集計し、Critic の値 (67 件・3,232 秒) を再現した。「Spotless 単体 75 秒」と「149 回 × 75 秒」の外挿は削除した |
| M02 | Major | C-1 で代わりに使う green の判定と検査範囲が足りない。`final-check.sh` は終了コードを捨てている | **採用**。指紋の対象を src・assets・ビルド設定・Hook・未追跡/削除ファイルに広げ、終了コードを主な判定根拠にし、失敗やタイムアウトでは green を更新しないようにした |
| M03 | Major | ガードの JSON 抽出は `"` を含むコマンドで切れ、素通りする | **採用**。Lead の見落としだった。C-1 で抽出方式を直し、fixture を追加する。「すべての破壊操作を防ぐ」とは書かない |
| M04 | Major | 結果画面の退路は、保存の保護を損なう回帰経路になりうる | **採用**。C-4a (説明だけ、先に実施) と C-4b (退路。データ保持の意味を決めてから) に分けた |
| M05 | Major | Composite はレアリティと効果を描画せず、移行で既存の詳細表示を失う恐れがある | **採用**。C-7 の範囲に描画の拡張・既存の詳細表示の維持・1 枚での試作と視覚 QA を加えた |
| M06 | Major | 新サービスを足すと RunLifecycle と二重になり、層規則にも違反する | **採用**。C-8 を「呼び出し側の検証可能化」に絞り、特性テストを先行させ、境界は最小限にした。なお「修正の集中」はバグ率そのものの計測ではないので、有力な仮説として扱う (部分採用) |
| M07 | Major | 終了時にまとめて集計するだけでは、予算超過で止められない | **採用**。Dispatch の境界ごとに累積を確認し、推定値に基づく目安での停止と位置付けた |
| m01 | Minor | origin の泉の不具合は、即座の Soul 消失や進行不能とは限らない | **採用**。条件付きの表現に修正した |
| m02 | Minor | 並列化の閾値は測定値ではない | **採用**。暫定値と明記した |
| m03 | Minor | 「反復が 2 倍なら単価差は消える」は Haiku と Opus では誤り (40 倍の差) | **採用**。D-3 の費用論を、受理された成果あたりの総費用で比べる形に直した |
| m04 | Minor | 60/60 枚の切り出し流用は、5 枚で絵の共用を許すことが条件 | **採用**。人間の判断事項に加え、切り出しの工数も計上した |

**却下した指摘はない** (根拠はすべて実ファイルで確認できた)。Critic も「判定不能」と明記した未解決事項:
現行 main のテストが全件成功するか、daemon と Spotless 単体の性能、実画面の視認性と入力の回帰、切り出し後の素材の品質。いずれも実行か目視が必要なので、F-0 で扱う。

---

## F. 次の実装フェーズ

- **F-0 (人間の判断が必要)**:
  1. 未 push の 6 コミットの扱い (C-2)。
  2. 基準線の計測を 1 回だけ実施する: repo の外のコピーで、no-daemon と warm daemon について `test` / `spotlessApply` / `test --tests X` を `--profile` 付きで実行 (約 5〜8 分、CPU のみ、外部送信なし。依存が `~/.gradle` にキャッシュ済みならネットワークも不要)。
  3. カード画像の方針: 既存 PNG の出所と許諾、5 枚の絵の共用の可否、アート枠の縦横比 (G1 相当)。
  4. C-5b の進行保証のルール (スキップか、無料の選択肢を残すか)。
- **F-1 = C-1 (品質ゲートの修理)**: Orca 実装指示文は `orca_spec_F1_quality_gate.md`。導入前後の比較は analyze3 系のパーサで行う: ENOENT 件数 (→0)、Java の変更がないターンの Stop Hook 時間、変更があるターンの Gradle 時間 (CLI のバージョン、warm/cold、変更の有無を揃えて比べる)、セッションあたりの `edited_text_file` の量。
- **F-2 = C-3**、**F-3 = C-4a**、**F-4 = C-5 (C-5b を除く)**、**F-5 = C-9**。いずれも別 PR で、直列に進める。
- その後: C-6 → C-7 (1 枚の試作) → C-8 (特性テストを先行) → C-10。
