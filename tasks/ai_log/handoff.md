# ai_log/handoff.md

> **Claude セッションが圧縮 / 再開した時に最初に読む 1 枚スナップショット**。
> ここを読めば「今ここから何をすべきか」が即座に分かる構造にする。
> 状況が動くたびに 1 行ずつ更新する (古い行は decisions.md / lessons.md に流す)。

---

## 最終更新

- **日時**: 2026-05-14 (火) 深夜 (**E-3 層構造の最小実装まで完了**。Stage 1 メタレビュー + Stage 2 異質 3 並列で「審査員視点最優先」と確定して着手、ローグライト名乗れる状態に進化)
- **更新者**: Claude (本セッション)

## アクティブブランチ

`chore/post-ui-handoff-update` (handoff.md 更新用、低リスク docs PR)

ローカルメインリポジトリ: `C:\.program\DDD-Jyogi-Kuroto-F`

## 進行中の作業 (圧縮後はここから再開)

**現在地**: **M1.5 コア機能完成 + 毎ターンドロー実装、`gradlew run` で「カードで敵を殴れる + ターン頭に手札補充」がエンドツーエンドで動作可能**。本セッションで以下 8 PR を self-merge:

| # | PR | 内容 | Commit |
|---|---|---|---|
| 1 | **#13** | E-1 カードシステムドメイン層 (`core.domain.card/` 11 ファイル) | `48033de` |
| 2 | **#16** | 依存 B: Stats 6 ステ + `CardEffect.Damage.resolve` | `bbaa630` |
| 3 | **#17** | 依存 A: ActionPoints 使い切り化 (`refilledTo(int)`) | `d77432e` |
| 4 | **#19** | 依存 D: BattleAction.UseCard + TurnEngine 統合 | `42c10b0` |
| 5 | **#20** | handoff.md 依存 D 完了状態更新 | `adbebea` |
| 6 | **#21** | **最小 UI 結合 + 初期手札注入 + 6 ステ初期値設定** | `b2c86c3` |
| 7 | **#22** | handoff M1.5 コア完成状態更新 | `63946a5` |
| 8 | **#23** | **毎ターン 1 枚ドロー + Random 引数注入 (ADR-19)** | `4075815` |
| 9 | **#24** | handoff 毎ターンドロー後の状態更新 | `d4d14c6` |
| 10 | **#25** | docs: PowerShell 対応 gradlew コマンド表記修正 | `0e87ee8` |
| 11 | **#26** | **解像度 1920×1080 化 + FitViewport (ADR-04 / ADR-20)** | `201b454` |
| 12 | **#27** | handoff 解像度完了状態更新 | `e930aa6` |
| 13 | **#28** | **カード設計テンプレ (docs/templates/) + 仮アイコン + fireball 追加** | `7ebc2fe` |
| 14 | **#29** | handoff カードテンプレ反映 | `b01b56d` |
| 15 | **#30** | **Move カード実装 + 移動 α 案 PlayerInputs 3 ステート (ADR-21)** | `dc35e2e` |
| 16 | **#31** | handoff Move カード反映 | `17e2304` |
| 17 | **#32** | **E-4 ラン内通貨 Gold record 新設 (§15-2)** | `dcb03ec` |
| 18 | **#33** | **Trap カード実装 + PlacedTrap + 踏み判定 + ライフタイム管理 (ADR-22)** | `e386f93` |
| 19 | **#34** | handoff Gold + Trap 反映 | `4882ad8` |
| 20 | **#35** | **E-3 層構造の最小実装 (Layer + advanceLayer、ADR-23、195 件 PASS)** | `d9d4e06` |

`gradlew test` 全 **157 件 PASS** (+2 for 毎ターンドロー検証)、final-architect レビュー全 PR で **A 判定** (#21 は B → 修正 2 件で A 相当)。

実機確認: 本セッション中に `gradlew run` を 11 分間動作確認、エラーなく終了。

### 圧縮後の Claude が最初にやること

1. **移動カード化 + E-5 装備をセットで実装** (ADR-19 で明日着手と予告済、X/Y/Z 案の最終確定が必要):
   - 案 X: `BattleAction.Move` 完全廃止、移動はすべて UseCard 経由 (§15-5 L534 「移動カードを切らないと動けない」純粋路線)
   - 案 Y: WASD = 「移動カード自動プレイ」のショートカット、UI 学習コスト最小
   - 案 Z: 共存、しかし「驚き最小」「DRY」違反で却下推奨
   - E-5 装備 (`Equipment(EquipmentId, displayName, Stats statsBonus)`)、初期装備「ぼろ靴」固有カードで初期デッキ構築
   - 推奨: 3 並列レビュー → 新 ADR-20 で確定 → 実装 → PR → self-merge (推定 2〜3h)
2. 並列で **E-3 層構造 + ノード分岐** (`core.domain.layer/`) - P0'、§15-6 (推定 1.5h)
3. 並列で **E-4 通貨 Gold** (`core.domain.meta/Gold.java`) - P0'、§15-2 (推定 1h)
4. **E-6 ポップアップ UI 基盤** (`core.presentation.window/`、Scene2D Window) - P0'、ADR-03 で全 UI 改修の土台 (推定 2〜3h)
5. **E-2 ソウルツリー** (`core.domain.tree/` + `SoulTreeScreen`) - P0'、§15-7 (推定 3h)

### 本セッションの実装サマリ

```
┌─────────────────────────────────────────────────────┐
│   gradlew run (DungeonScreen)                       │
│   ├── PlayerInputs (2 ステート: 数字→方向)            │
│   ├── HudRenderer (手札テキスト + 選択中ハイライト)    │
│   └── BattleAction.UseCard(handIndex, direction)    │
│         ↓                                            │
│   TurnEngine.applyPlayerUseCard                     │
│   ├── Hand 範囲チェック / AP コストチェック             │
│   ├── CardEffect で switch                          │
│   │   ├── Damage  → resolve(attacker, defender, element) → Stats.damaged  │
│   │   ├── Move    → reject (未実装)                  │
│   │   ├── Buff    → reject (未実装)                  │
│   │   └── Trap    → reject (未実装)                  │
│   ├── Hand → Discard 移動 (CardPileState.playFromHand)│
│   ├── AP 消費 (ActionPoints.spend)                  │
│   └── ターン頭で AP 全リセット (ActionPoints.refilledTo)│
└─────────────────────────────────────────────────────┘
```

### 残スコープ (5/15 以降)

#### M1.5 P0' 機能 (5/15-18 想定)

- **E-3** 層構造 + ノード分岐 (`core.domain.layer/`) — §15-6
- **E-4** 通貨 Gold (`core.domain.meta/Gold.java`) — §15-2
- **E-6** ポップアップ UI 基盤 (`core.presentation.window/`、Scene2D Window) — §15-1, §15-8、ADR-03
- **E-2** ソウルツリー (`core.domain.tree/` + `SoulTreeScreen`) — §15-7
- **E-5** 装備システム + 依存 E (装備固有カードで Deck 動的化) — §15-9 + ADR-18 で予告

#### P1 (5/19-21 想定)

- **E-8** シームレス戦闘演出 — §15-5
- **E-9** セーブ最小 — §15-11
- **E-10** チュートリアル — §15-10
- **依存 C** Direction8 (罠 8 方向用) — 罠カード実装時のみ必要

#### スキップ済 (時間切れ判定)

- **E-7** Bestiary — P2 (ADR-15 で降格)
- **Move/Buff/Trap カード実装** — Damage で十分デモ可能、別 Issue

## 直近のマージ済成果

| Commit | 内容 | 日付 |
|---|---|---|
| `c1fc9d6` | MVP コア実装一式 (#7) | 2026-05-12 |
| `4c97f0c` | **PR #9 merged**: §15 ブラッシュアップ | 2026-05-14 朝 |
| `f62b482` | **PR #11 merged**: AI 体制整備 | 2026-05-14 朝 |
| `48033de` | **PR #13 merged**: E-1 カードシステム (#12) | 2026-05-14 昼 |
| `bbaa630` | **PR #16 merged**: 依存 B Stats 6 ステ (#15) | 2026-05-14 午後 |
| `d77432e` | **PR #17 merged**: 依存 A ActionPoints 使い切り (#14) | 2026-05-14 午後 |
| `42c10b0` | **PR #19 merged**: 依存 D UseCard 統合 (#18) | 2026-05-14 夜 |
| `b2c86c3` | **PR #21 merged**: 最小 UI 結合 + 初期手札注入 + 6 ステ初期値 | 2026-05-14 夜 |

## Open Issues / PRs

| 番号 | 種類 | 状態 | 内容 |
|---|---|---|---|
| #1 | Issue | OPEN | リポジトリの土台整備 (古い、Close 候補) |
| #4 | Issue | OPEN | CI 軽量化とドキュメント微調整 (古い、Close 候補) |
| #8 | Issue | OPEN | §15 バランス調整 (PR #9 マージで実質完了、Close 推奨) |
| #12 | Issue | OPEN | E-1 カードシステム実装 (ドメイン + UI 結合完了、Close 候補) |

依存 #14 (A) / #15 (B) / #18 (D) はすべて MERGED PR で実質完了 (手動 close 待ち)。

## 据置き判断 (議論済、再議論不要)

詳細は [decisions.md](./decisions.md) を参照。**ADR-17 / ADR-18 が本セッションで追加**:

1. **AP モデル**: §15 使い切り型 — ADR-01、PR #17 で実装
2. **敵 AP**: 層番号 N に等しい
3. **Stats**: 6 種 — ADR-02、PR #16 で実装
4. **ステ計算**: `max(1, base + 物攻 - 物防)` — **ADR-17** で `CardEffect.Damage.resolve` 配置確定、PR #16 で実装
5. **Stop hook**: 全テスト走行 ON
6. **フォント**: DotGothic16 採用
7. **mvp 統合**: `-X ours` 戦略
8. **E-7 Bestiary**: P2 降格
9. **クリア条件**: 階段踏破のみ
10. **Agent Teams**: 当面 OFF
11. **E-1 ドメイン設計**: ADR-16、PR #13 で実装
12. **依存事項の起票戦略**: A+B 先行、C/D/E 段階起票
13. **ダメージ計算配置**: `CardEffect.Damage.resolve` (card 層内)、DamageFormula 廃案 — ADR-17
14. **Skill 経路不変保持**: SkillEffect = 固定ダメ継続 — ADR-17
15. **CardPileState の所有**: **Player record に内蔵 (案 X)** — **ADR-18**
16. **Move/Buff/Trap カード**: 本 PR では reject、別 Issue で順次 — ADR-18
17. **BattleEvent**: `CardUsed` 新設せず `SkillUsed` 流用 — ADR-18
18. **ダメージヘルパ**: Skill/Card 両経路で `int finalDamage` 受け取り統一 — ADR-18
19. **UI 形式**: 本日は MVP テキスト UI、E-6 で Scene2D Window 化 — ADR-03 中間実装 (PR #21)
20. **初期手札**: ハードコード 3 枚 (`InitialStateFactory`)、E-5 Equipment で動的化予定 — PR #21
21. **NUM_1〜9**: UseCard に完全置換、UseSkill 一時無効化 (Shift+1〜4 等で将来復活可) — PR #21
22. **毎ターンドロー**: プレイヤーターン頭で 1 枚ドロー、Random は引数注入で TurnDirector → DddGame `new Random()` で連鎖、テストは `Random(42)` 固定シード — **ADR-19** (PR #23)
23. **移動カード化**: §15-5 「移動カードを切らないと動けない」は明日 5/15 以降に E-5 装備とセットで実装、X/Y/Z 案は新 ADR-20 で確定予定 — ADR-19 で予告
24. **移動仕様 = α 案**: 「移動カード 1 枚を切る → distance ぶん AWSD で連続移動権を得る」を採用。PlayerInputs に `pendingMoveCount` ステートを追加して実装予定 — **ADR-20**
25. **装備 = B 案 (折衷)**: `Equipment(EquipmentId, displayName, EquipmentSlot, StatsBonus, List<CardId> grantedCards)` の 5 引数 record。`grantedCards` は空リスト可で柔軟性最大 — **ADR-20**
26. **画面解像度 = 1920×1080 (16:9)**: `FitViewport` + `setResizable(true)` で実装済 (PR #26)。素材は 1920×1080 基準で作る — **ADR-20**
27. **音タグ = M2 以降に予約**: 主タグ × 副タグで SE 出し分け方針、本セッションでは実装しない — ADR-20
28. **Android 方針 = Desktop 基本 + デモ動画代替**: §15-12 クロスプラットフォーム実演は録画動画で補足、本気でビルド通す優先度は低い — ADR-20
29. **Move カード = 案 Z 実装 (Player.pendingMoveCount)**: ドメイン側に状態を持ちセーブ整合 + AP 切れ自動ターン終了との競合を回避。`startPlayerTurn` で 0 リセット (ターン跨ぎ持越なし)。途中ブロックは reject 統一 (pendingMoveCount 据置) — **ADR-21**、PR #30
30. **PlayerInputs 3 ステート (通常/カード選択中/移動権保持中)**: `poll(DungeonState)` でドメインから `pendingMoveCount` を毎フレーム読取。状態 2 は数字キー無視、ESC 破棄は YAGNI で見送り — ADR-21
31. **Buff/Trap カードは引き続き reject 維持**: ADR-21 で Move のみ実装と確定、Buff (`activeBuffs`)・Trap (`placedTraps`) は明日 5/15 別 PR で実装予定 → **Trap は本セッションで PR #33 (ADR-22) で実装完了、Buff のみ明日に持ち越し**
32. **Gold record 新設**: §15-2 ラン内通貨 (`core.domain.meta.Gold`)、Soul と同型で値オブジェクト単独実装、Player/GameContext 統合は ADR-22 (PlayerStatuses 集約案) と一緒に明日 — PR #32
33. **Trap カード = 設置者ステ依存なし + 同座標上書き + UntilStepped 踏み除去 / Turns 時間消滅**: TurnEngine.checkAndTriggerTrap で player/enemy 両経路共通、`Turns(0)` 中間値許容で `decrementedLifetime()` を単純化 — **ADR-22**、PR #33
34. **明日 5/15 の作業順** (Stage 2 異質 3 並列の審査員視点で確定):
    1. Discord 共有 (3 投稿、本日準備済)
    2. **E-3 UI 連動** (CLEARED → ENTER で `InitialStateFactory.advanceLayer` 呼出、`BattleAction.AdvanceFloor` or 既存 EndTurn 流用、E-6 ポップアップと一緒)
    3. **E-6 ポップアップ UI 基盤** (Scene2D Window、層末 4 種ノード提示)
    4. **シームレス戦闘演出** (SE + シェイク + ダメージポップ、審査員視点で 30 秒試遊の決定打)
    5. **E-2 ソウルツリー画面** (簡素でも円樹形 UI、PoE 訴求の絵)
    6. **E-5 装備 (Equipment B 案)** (Player 9 引数化問題は ADR-24 で PlayerStatuses 集約案を確定してから)
    7. **Buff カード実装** (上の ADR-24 と一緒、プレイヤー視点で緊急 5 だが審査員視点で印象薄)
    8. **(訴求軸変更検討)**: 「Doko-demo」→ 「AI 駆動開発の実演」 (.claude/agents/ + ADR-23 までの蓄積を見せる)、Android backend は完成確率 10% 未満で動画代替も審査員に通用しない
35. **E-3 層構造**: `Layer record (number, displayName)`、DungeonState 6 引数化 + 互換 5/4 引数、`InitialStateFactory.advanceLayer` + `newSlimeForLayer`、敵 AP = 層番号 (ADR-06 が初実装)、UI 連動は別 PR — **ADR-23**、PR #35
36. **メタレビュー教訓 (本セッション)**: 「multifaceted ≠ 並列数」「同質コホート (技術者 3 名) は擬似的多角性」「真の多角性 = 観点の独立性 + ステークホルダー網羅 + 時間軸 + リスクカテゴリ」。次回以降 Agent 構成を異質化必須 (teammate-pov / playtester-pov / judge-pov を含める)
37. **本日の機会費用**: Trap (PR #33) は審査員視点で「30 秒試遊に刺さらない」と判定済、もっと先に E-3 をやるべきだった (deck 構築は OK だが進行感ゼロは敗北筋)。本日 5 PR (#22/#24/#27/#29/#31/#34) は handoff 更新のみで PR 数を盛った状態、明日以降は 1 機能 = 1 PR を徹底

## 採用済ツール / バージョン

| ツール | バージョン |
|---|---|
| JDK | Oracle JDK 25.0.3 LTS |
| Gradle | 9.5.0 (wrapper 同梱) |
| LibGDX | 1.14.0 |
| LWJGL | 3.4.1 (force) |
| JUnit Jupiter | 5.12.2 |
| Jackson Databind | 2.21.3 |
| Spotless | 8.4.0 |
| google-java-format | 1.35.0 |
| gdx-freetype | 1.14.0 |

JAVA_HOME: `C:\Program Files\Java\jdk-25.0.3`

## チーム体制 (リーダー判断)

- **リーダー (ユーザー)**: AI 駆動でコード実装
- **チームメイト**: 素材収集 + カード設計 (JSON / Excel)
- **AI 駆動**: Subagent + Skills + Hooks (.claude/)
- **Architect** = 本セッションの Claude (`final-architect` Agent)

## AI 駆動運用の起動方法

### 通常の機能実装 (E-X / 依存事項 着手時)

1. `/m1-5-start E-X 機能名` Skill で Issue 起票 + ブランチ作成
2. **`domain-architect` Agent で設計レビュー → ユーザー確認**。大きな設計判断は **3 並列レビュー** 推奨 (本セッションで ADR-17 / ADR-18 を救済した運用パターン)
3. ADR 記録 (`decisions.md` 追記) — 不可逆判断は実装前に記録
4. 実装 (Edit/Write) → hook で `spotlessApply` + 関連テスト自動実行
5. `test-writer` Agent でテスト補強
6. (LibGDX 依存時のみ) `libgdx-implementer` Agent でプレゼン層
7. **`final-architect` Agent を Agent ツールから直接呼ぶ** — `/architect-review` Skill は使わない (worktree 問題)
8. **`gh pr create` を main session から実行** — `/japanese-pr-create` Skill はテンプレ参照のみ
9. self-merge (ADR-14) または チームレビュー → マージ

### Skill 利用時の注意 (lessons.md エントリ参照)

- `context: fork` 付き Skill は worktree を生成し、後続 Skill の cwd 汚染が連鎖する
- **Skill 内の git 出力は信用しない**、必ず `git -C <main-repo-path>` 明示パスで再確認
- `gh pr create` や `git commit` の不可逆操作は必ず `-C C:/.program/DDD-Jyogi-Kuroto-F` 付き

### 不可逆判断が必要なとき

- `decisions.md` に ADR を追記 (現在 **ADR-18** まで蓄積済)

### 失敗 / 判断ミスをしたとき

- `lessons.md` にエントリ追加

## スケジュール

- **2026-05-12**: MVP 完成
- **2026-05-13**: §15 仕様策定 + バランス調整
- **2026-05-14 (今日)** ✅: **M1.5 コア + 毎ターンドロー + 解像度 + テンプレ配布準備 + Move + Gold + Trap + E-3 層構造** (PR #13〜#35、ADR-23 まで蓄積、**195 件全 PASS**)
- **2026-05-15 (金)** **朝**: Discord にカード設計テンプレ共有 (3 投稿構成) + Google Drive / Sheets URL 共有
- **2026-05-15 (金)** 日中:
  - **ADR-23 起票**: Player 引数増加問題の解決 (PlayerStatuses 集約 record 案 vs 8 引数維持)。Buff + 装備の同時実装に必要
  - **Buff カード TurnEngine 実装** (Player.activeBuffs フィールド or PlayerStatuses 集約、`ActiveBuff(BuffKind, amount, remainingTurns)` record、`Player.effectiveStats()` で合算、`startPlayerTurn` で durationTurns--) — テンプレ最後の 1 種 iron_skin が動く
  - **E-5 装備 (Equipment B 案)** (`Equipment(EquipmentId, displayName, slot, StatsBonus, List<CardId> grantedCardIds)`、ぼろ靴・ぼろい短剣を初期装備、Player.finalStats() で素ステ + statsBonus 合算)
  - **Gold の Player/GameContext 統合** (PR #32 で record だけ作った、雑魚 5 / 強化 15 / ボス 50 のレート実装 + Player.gold or GameContext.gold)
- **2026-05-16〜18**: 並列で E-3 (層構造) / E-2 (ソウルツリー) / E-6 (Scene2D Window 化 + HudRenderer 画像連携)
- **2026-05-16〜18**: E-2 ソウルツリー、E-5 装備 + 依存 E、E-6 完成
- **2026-05-19〜21**: E-8 演出、E-9 セーブ、E-10 チュートリアル、依存 C
- **2026-05-22**: 統合テスト + デモ録画
- **2026-05-23〜24**: ハッカソン本番

## 関連ドキュメントへの動線

- 全体仕様: [docs/GAME_DESIGN.md §15](../../docs/GAME_DESIGN.md)
- 実装俯瞰: [docs/SystemSummary.md](../../docs/SystemSummary.md)
- M1.5 並列プラン: [tasks/todo.md](../todo.md) の Phase 9
- 設計判断ログ: [decisions.md](./decisions.md) (ADR-18 まで)
- AI 失敗ログ: [lessons.md](./lessons.md)
- 過去レビュー: [phase_6_5_review.md](./phase_6_5_review.md)
- プロジェクト記憶: [../../CLAUDE.md](../../CLAUDE.md)
