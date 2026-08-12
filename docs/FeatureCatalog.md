# DDD-Jyogi-Kuroto-F 機能カタログ

> 本作の実装済全機能を網羅するドキュメント (Wave 1〜17 + MVP 累積、合計 58 機能)。チームメイトへの引き継ぎ・将来開発者向けの **実装機能の俯瞰インデックス** として機能する。
>
> 仕様の意図・設計判断は [GAME_DESIGN.md](GAME_DESIGN.md) を参照 (こちらが仕様の Single Source of Truth)。本ドキュメントは「何が実装されているか」を網羅する索引で、「なぜそう作ったか」は GAME_DESIGN.md に集約される。

---

## 概要

- **ジャンル**: 2D ピクセルローグライト (Slay the Spire 風 AP デッキ構築 + ToME 系シームレス戦闘 + Path of Exile 風ソウルツリー)
- **技術スタック**: Java 25 LTS + LibGDX 1.14.0 + Gradle 9.5 + JUnit 5 + Jackson
- **アーキテクチャ**: DDD 4 層分離 (`core.domain.*` / `core.application.*` / `core.infrastructure.*` / `core.presentation.*`)
- **解像度**: 1920×1080 ベース (FitViewport)、初回起動で UI プリセット 3 種選択
- **テスト**: 755 件 PASS (Wave 17 時点、`@Test` 実カウント)、JUnit 5 + DomainFixtures

## 関連ドキュメント

- [GAME_DESIGN.md](GAME_DESIGN.md) — 仕様の Single Source of Truth (§11 MVP / §15 MVP 後)
- [SystemSummary.md](SystemSummary.md) — 実装俯瞰 + 拡張ロードマップ (Phase A〜D)
- [CommonSense.md](CommonSense.md) — 命名規約 + 用語集
- [SpecificationIssues.md](SpecificationIssues.md) — 仕様整合性監査リスト (Wave 15-17 で 10/11 件対応済)
- [ContributingGuide.md](ContributingGuide.md) — Issue / PR / ラベル運用
- [BranchingStrategy.md](BranchingStrategy.md) — ブランチ命名と保護
- [AssetGuidelines.md](AssetGuidelines.md) — 素材取り込み + ライセンス管理

---

## カテゴリ A: 戦闘・カード・スキル (10 機能)

### A-1. ターン制戦闘システム

プレイヤー / 敵の交互ターン進行、5 フェーズ状態機械 (`PLAYER_TURN` / `ENEMY_TURN` / `CLEARED` / `GAME_OVER` / `RUN_CLEARED`)。純関数で副作用ゼロ、失敗アクションは `ActionRejected` イベント発火で `state` は不変保持。

- 関連: `core/domain/battle/TurnEngine.java` / `TurnPhase.java`
- 導入: MVP、Wave 5 W5-α で 3 クラス分離 (`TurnEngineMovement` / `TurnEngineCardResolver` / `TurnEngineSkillResolver`)、Wave 7 W7-α で `TurnEngineHelpers` 切り出し

### A-2. AP (使い切り型) 制

毎ターン頭で速度ステ分まで全リフィル。蓄積不可 (MVP の蓄積型からの breaking change、§15-3)。`ActionPoints.refilledTo(speed)` で `effectiveStats().speed()` を参照、装備 + Buff 込みで動的。

- 関連: `core/domain/battle/ActionPoints.java`
- 導入: MVP、ADR-01 で蓄積型→使い切り型に改訂

### A-3. BattleAction sealed (5 種)

`Move(Direction)` / `UseCard(handIndex, Direction)` / `UseSkill(slotIndex)` / `Wait` / `EndTurn` の sealed permits。TurnEngine の switch 網羅性で「新アクション追加時にコンパイラが全箇所警告」を担保。

- 関連: `core/domain/battle/BattleAction.java`
- 導入: MVP、Wave 3 で UseCard に CardId 参照を統一

### A-4. CardEffect sealed (4 種)

`Damage(baseValue, range, areaRadius)` / `Move(distance)` / `Buff(kind, amount, durationTurns)` / `Trap(baseValue, lifetime)` の sealed。後方互換コンストラクタで Wave 11 (rarity) / Wave 12 (range/areaRadius) 拡張時もテスト fixture 無修正。

- 関連: `core/domain/card/CardEffect.java`
- 導入: MVP、Wave 12 W12-α で `range` + `areaRadius` 追加

### A-5. カード使用フロー (line scan + AOE)

カード選択 → 方向選択 → AP 消費 → 効果適用。Damage は `range` マス先まで直線スキャン (WALL / BREAKABLE_WALL で line of sight 遮断)、`areaRadius > 0` で中心ターゲット周囲をチェビシェフ距離で巻き込み。step=1 開始で自爆クリック防止。

- 関連: `core/domain/battle/TurnEngineCardResolver.java`
- 導入: MVP、Wave 12 W12-β で line scan + AOE

### A-6. カードレアリティ (CARD_DRAW SE 連動)

`CardRarity { COMMON / UNCOMMON / RARE }` を Card record に `Optional<CardRarity>` で保持 (後方互換、未指定は COMMON フォールバック)。手札の最高 rarity に応じて毎ターン頭の CARD_DRAW SE が C/U/R に分岐。

- 関連: `core/domain/card/CardRarity.java` / `Card.java` / `DungeonScreen.drawSeFor()`
- 導入: Wave 11 W11-β

### A-7. ダメージ計算 (DamageFormula)

`max(1, baseValue + 攻撃ステ - 防御ステ)`、PHYSICAL なら物攻/物防、MAGICAL なら魔攻/魔防参照。最低 1 ダメ保証。スキル/罠は固定値から防御のみ引く片側計算。

- 関連: `core/domain/battle/DamageFormula.java` / `CardEffect.Damage#resolve()`
- 導入: MVP、ADR-17 改訂で DRY 集約

### A-8. スキル機構 (敵側のみ)

`Skill` / `SkillSlot` / `SkillEffect` (record + sealed)。Wave 15 W15-β で **プレイヤー側は空 4 枠廃止** (`SkillSlot.empty(4)`)、F1-F4 入力削除、HUD スキル枠描画削除 → デッキ・カード一本化 (§15-3)。**敵側全種** (SLIME / SWIFT_SLIME / TOUGH_SLIME / ELITE_SLIME = `slimeBite`、BOSS = `bossSlam`) は `Enemy.skillSlot` 経由で継続使用、`TurnEngineSkillResolver.applyEnemySkill` から発動。

- 関連: `core/domain/skill/*` / `core/domain/battle/TurnEngineSkillResolver.java`
- 導入: MVP、Wave 15 W15-β でプレイヤー側廃止

### A-9. ActiveBuff (持続バフ)

`PHYSICAL_ATTACK_UP` / `PHYSICAL_DEFENSE_UP` / `MAGICAL_ATTACK_UP` / `MAGICAL_DEFENSE_UP` / `SPEED_UP` の 5 種 BuffKind。毎ターン頭で残ターン -1、0 で除去。`effectiveStats()` 経由でステ計算に反映。同 kind は上書き。Wave 15 W15-α で「AP リフィル → Buff decrement」順序 swap で持続 1 SPEED_UP の論理消滅バグ修正。

- 関連: `core/domain/card/ActiveBuff.java` / `core/domain/battle/TurnEngine.startPlayerTurn`
- 導入: ADR-27 (持続バフ)、Wave 15 W15-α で順序修正

### A-10. PlacedTrap (罠システム)

`TrapLifetime` sealed: `UntilStepped` (踏まれるまで永続) / `Turns(remaining)` (N ターン経過消滅)。同座標は新規設置で上書き、敵 / プレイヤー進入時に `TrapTriggered` 発火 + ダメージ + 即座除去 (UntilStepped) or 残ターン -1 (Turns)。

- 関連: `core/domain/dungeon/PlacedTrap.java` / `core/domain/card/TrapLifetime.java`
- 導入: ADR-22

---

## カテゴリ B: ダンジョン・マップ・敵 AI (10 機能)

### B-1. DungeonMap / Tile

`Tile { FLOOR / WALL / STAIRS_DOWN / BREAKABLE_WALL }` の 4 enum。`DungeonMap` は final class で `withTileAt(pos, tile)` で不変更新、外枠 (周囲 1 マス) は境界不変条件として IAE で弾く (Wave 11 W11-α CTO #1)。

- 関連: `core/domain/dungeon/DungeonMap.java` / `Tile.java`
- 導入: MVP、Wave 11 W11-α で BREAKABLE_WALL + withTileAt + 境界防御

### B-2. DungeonState 集約

`DungeonMap + Player + List<Enemy> + List<PlacedTrap> + TurnPhase + Layer` を 1 record に集約、全更新は `with*` で新インスタンス返却。`withEnemyReplaced` / `withEnemyRemoved` / `withMap` 等の便利メソッド。

- 関連: `core/domain/dungeon/DungeonState.java`
- 導入: MVP、Wave 11 W11-α で `withMap` 追加

### B-3. ダンジョン生成 (BSP)

`DungeonGenerator` は BSP (Binary Space Partitioning) で部屋分割 → L 字廊下連結 → 全部屋 1 本パス保証。層番号に応じてグリッド拡大 (層 1=14×12 / 層 2=19×15 / 層 3=26×20)、敵数も増加 (3/6/8)。Random 引数注入で決定性。

- 関連: `core/infrastructure/bootstrap/DungeonGenerator.java` / `InitialStateFactory.generateLayerState`
- 導入: MVP、Wave 16 W16-β で `placeBreakableWalls` に対向 FLOOR 縛りで真のショートカット優先化

### B-4. マップヘルパ (BFS + Bresenham)

`DungeonMap` 上の到達性 / 経路探索 / 視界判定:
- `reachable(from, to)`: 4 近傍 BFS でソフトロック検証
- `firstStepToward(from, to)`: BFS 経路の 1 歩目を Direction で返す (敵 AI 壁迂回用)
- `hasLineOfSight(from, to)`: Bresenham line で中間マスの WALL/BREAKABLE_WALL を判定 (from/to マス自体は壁判定対象外)
- `canSeeWithin(from, to, maxRange)`: チェビシェフ距離 + LOS の組合せ

- 関連: `core/domain/dungeon/DungeonMap.java`
- 導入: MVP (BFS)、Wave 13 W13-α で LOS 追加

### B-5. 層構成 (Layer / LayerEndNode)

各層は `Layer(number, displayName)`、最終層 (DEFAULT_MAX_LAYER=3、SoulTree.LayerExtendEffect で拡張) にボス出現。階段踏破で CLEARED → 層末ノード 3 択提示 (`NodeChoicePopup`)。

- 関連: `core/domain/layer/Layer.java` / `LayerEndNode.java`
- 導入: MVP、Wave 2 W2-B で層数拡張、Wave 3 W3-A で ShopEquipment / NodeResolveContext

### B-6. LayerEndNode sealed (6 種)

`HpMaxUp(amount)` / `SpeedUp(amount)` / `Rest()` / `Shop(goldCost, cardId)` / `Event(soulDelta, hpDelta, goldDelta, EventKind)` / `ShopEquipment(goldCost, equipmentId)`。Rest は Wave 15 W15-γ で「HP 全回復→maxHp/3 回復」に制限。ShopEquipment は Wave 16 W16-γ で「ラン中即時反映 + maxHp 増加分の currentHp 同期」。

- 関連: `core/domain/layer/LayerEndNode.java` / `EventKind.java`
- 導入: Wave 3 (ShopEquipment / Event 多様化)、Wave 8 W8-α (EventKind 化)、Wave 15-16 (バランス是正)

### B-7. 敵バリエーション (EnemyKind)

5 種: `SLIME / SWIFT_SLIME / TOUGH_SLIME / ELITE_SLIME / BOSS`。各 EnemyKind が `soulReward` + `goldReward` + `sightRange` + `aiProfile` を保持。Wave 10 W10-β でスプライト個別割当 (skeleton.png / goblin.png 等)。

- 関連: `core/domain/entity/EnemyKind.java`
- 導入: MVP (SLIME / BOSS)、Wave 4 W4-δ (SWIFT/TOUGH)、Wave 13 W13-α (sightRange + aiProfile)

### B-8. 敵 AI (視界 + 3 状態 + kind 別行動)

`EnemyAi.computeNewState` が純関数で状態遷移を計算 (`IDLE / ALERT / SEARCHING` の 3 状態 + `lastKnownPlayerPos: Optional<Position>`)。視界内 → ALERT + 位置記憶、視界外 + ALERT → SEARCHING、視界外 + SEARCHING + 到達 → IDLE 戻り。AGGRESSIVE (BFS 追跡 + 隣接攻撃) / CAUTIOUS (kite 型、Wave 15 で遠距離スキル不在時 AGGRESSIVE フォールバック) の 2 プロファイル分岐。

- 関連: `core/domain/battle/EnemyAi.java` / `EnemyAiState.java` / `EnemyAiProfile.java`
- 導入: MVP (BFS 追跡のみ)、Wave 13 W13-β (3 状態 + 2 プロファイル)、Wave 15 W15-α (CAUTIOUS フォールバック)

### B-9. BattleEvent sealed (16 種)

戦闘中の事実通知: `Moved / SkillUsed / DamageDealt / ActorDied / SoulGained / GoldGained / TurnPhaseChanged / ActionRejected / MovementGranted / TrapPlaced / TrapTriggered / FloorAdvanced / BuffApplied / EliteDefeated / AutoTurnEnded / WallBroken`。HudRenderer のログ表示 + DungeonScreen の SE/エフェクト発火が消費。

- 関連: `core/domain/battle/BattleEvent.java`
- 導入: MVP、Wave 11 W11-α で `WallBroken` 追加 (16 種目)

### B-10. 壊れる壁ギミック (BREAKABLE_WALL)

各層 2-3 個ランダム配置 (Wave 16 で対向 FLOOR 縛り)。移動カード (`pendingMoveCount > 0`) の経路でのみ破壊可能、AP 移動 / 攻撃カードでは遮断扱い (一貫性)。破壊時に Map → FLOOR 化 + `BattleEvent.WallBroken` 発火 + `block_brake.ogg` SE 再生。

- 関連: `core/domain/battle/TurnEngineMovement.applyPlayerMove` / `InitialStateFactory.placeBreakableWalls`
- 導入: Wave 11 W11-α、Wave 16 W16-β で配置改善

---

## カテゴリ C: メタ進捗・ソウルツリー・装備・図鑑 (10 機能)

### C-1. PlayerProgress 集約

ラン外 7 要素 (playerSoul / runCount / tutorialSeen / soulTree / bestiary / loadout / obtainedCards) を 1 つの不変 record に統合。7 種の `with*` チェインメソッドで状態遷移を純関数化、Escape Analysis で GC 負荷なし。

- 関連: `core/domain/meta/PlayerProgress.java`
- 導入: Wave 5 W5-β (定義)、Wave 6 W6-γ (DddGame 統合)

### C-2. Soul / Gold 通貨区分

`Soul` は **ラン外通貨**、死亡時持ち越し、ソウルツリー解放の対価。`Gold` は **ラン中通貨**、死亡時消失、ショップで装備 / カード購入。敵撃破時に `EnemyKind.soulReward` / `goldReward` で付与。Wave 15 W15-α で SaveData v3 にラン中通貨を永続化 (層境界セーブ時の Gold/Soul 消失バグ修正)。

- 関連: `core/domain/meta/Soul.java` / `Gold.java`
- 導入: MVP (Soul)、Wave 3 W3-B (Gold)、Wave 15 W15-α (永続化バグ修正)

### C-3. SoulTree システム (25 ノード)

ステ強化 (HP/速度/物攻/魔攻/物防/魔防 各 Lv1+Lv2) + カード獲得 (5 種) + 枠拡張 (5 種) + 層数拡張 (layer_extend_4/5) の 25 ノード木構造。`SoulTree.unlock(nodeId)` で prerequisites 検証 + Soul 消費、`reset()` でリセット + 返金。tree.json から起動時ロード。

- 関連: `core/domain/tree/SoulTree.java` / `TreeNode.java` / `NodeEffect.java` / `assets/tree.json`
- 導入: §15-7、Wave 2 W2-A で JSON 化、Wave 5 W5-γ で Supplier 注入

### C-4. NodeEffect sealed (5 種)

`None / StatsBonusEffect / CardGrantEffect / SlotExpandEffect / LayerExtendEffect`。ツリー解放済み効果を順序保持で Player に `applyTo()`。

- 関連: `core/domain/tree/NodeEffect.java`
- 導入: Wave 2 W2-A、Wave 2 W2-B で LayerExtendEffect 追加

### C-5. 装備システム (Equipment)

6 部位スロット (`HEAD / BODY / HAND / MAIN / FEET / ACCESSORY`)、Equipment record (`id / displayName / slot / statsBonus / grantedCards / themeName / iconPath`)。装備の `grantedCards` が初期デッキに自動追加 (Wave 16 W16-γ でラン中購入も即時反映)。装備変更時に山札再構築。

- 関連: `core/domain/equipment/Equipment.java` / `EquipmentSlot.java` / `StatsBonus.java`
- 導入: §15-9 / ADR-25 / ADR-26、Wave 10 W10-γ で iconPath 追加

### C-6. EquipmentCatalog (equipment.json)

起動時に `equipment.json` から全装備をロード、`EquipmentId → Equipment` マップを保持。tattered_boots (FEET) / tattered_dagger (HAND) を初期装備に。Wave 10 W10-γ で 2 装備にアイコン (boots1.png/boots2.png) 追加。

- 関連: `core/infrastructure/bootstrap/EquipmentCatalog.java` / `assets/equipment.json`
- 導入: §15-9、Wave 10 W10-γ で iconPath 拡張

### C-7. CardCatalog (cards.json)

59 カードを `cards.json` から起動時ロード (ATTACK / MOVEMENT / BUFF / TRAP タグ分類)。Wave 11 で rarity 拡張、Wave 12 で range / areaRadius 拡張、すべて Optional / デフォルト値で graceful 読込。

- 関連: `core/infrastructure/bootstrap/CardCatalog.java` / `assets/cards.json`
- 導入: §15-3、Wave 3 W3-A で CardId 化、Wave 11-12 で rarity/range 拡張

### C-8. Bestiary (撃破済敵種図鑑)

`Bestiary` は `EnumSet<EnemyKind>` で撃破済 EnemyKind を保持、`withDefeated(kind)` で不変更新。BestiaryScreen で撃破済敵を一覧表示 (タイトル B キー)。SaveData v2 で永続化。

- 関連: `core/domain/meta/Bestiary.java` / `core/presentation/screen/BestiaryScreen.java`
- 導入: §15-5 / E-7、Wave 4 W4-δ (BestiaryScreen)、Wave 6 W6-β (永続化)

### C-9. カード図鑑 (CardCollectionScreen)

入手済 CardId 集合 (`PlayerProgress.obtainedCards`) を表示、CardImageRegistry のテクスチャを使ったタイル表示。SaveData で永続化。

- 関連: `core/presentation/screen/CardCollectionScreen.java`
- 導入: §15-3、Wave 10 W10-γ で CardImageRegistry 連携

### C-10. ショップ (Shop / ShopEquipment)

層末ノードで Gold 消費して購入。Shop = カード追加、ShopEquipment = 装備購入。Wave 16 W16-γ で ShopEquipment は「ラン中の装備マップ即時反映 + grantedCards を drawPile にシャッフル追加 + maxHp 増加分の currentHp 同期 (UX 違和感解消)」。

- 関連: `core/domain/layer/LayerEndNode.Shop` / `LayerEndNode.ShopEquipment` / `DddGame.resolveLayerEndChoice`
- 導入: §15-9 / E-5、Wave 3 W3-B、Wave 16 W16-γ で即時反映

---

## カテゴリ D: UI・画面遷移・入出力 (11 機能)

### D-1. Screen 群 (10 画面)

`TitleScreen` (開始) / `DungeonScreen` (戦闘・探索) / `SettingsScreen` (設定) / `FirstRunPresetScreen` (初回 UI プリセット選択) / `SoulTreeScreen` (ツリー表示・解放) / `EquipmentScreen` (装備変更) / `BestiaryScreen` (敵図鑑) / `CardCollectionScreen` (カード図鑑) / `CreditsScreen` (クレジット) / `GameOverScreen` (ゲームオーバー)。

- 関連: `core/presentation/screen/*`
- 導入: MVP (Title/Dungeon/GameOver)、Wave 1 (Credits/Settings)、Wave 4 (Bestiary)

### D-2. Window/Popup 群 (5 種)

`TutorialOverlay` (初回チュートリアル) / `NodeChoicePopup` (層末 3 ノード選択) / `StatusPopup` (ステータス確認 Tab キー) / `ConfirmationDialog` (確認ダイアログ R リセット等) / `SoulNodeUnlockDialog` (ツリー解放確認)。Scene2D Stage ベース。

- 関連: `core/presentation/window/*`
- 導入: §15-10 (Tutorial)、§15-8 (NodeChoice)、Wave 1 W1-3 (ConfirmationDialog)

### D-3. HUD 描画 (HudRenderer)

HP / AP / Soul / Gold / Phase / 手札 / メッセージログ / ターン終了ボタン / カード詳細欄 / 操作ヒント / 移動権残量 を描画。UI プリセット別に表示量制御、UiTheme でカラーパレット適用。Wave 15 で SkillSlot 描画削除、Wave 14 でターン終了ボタン追加。

- 関連: `core/presentation/render/HudRenderer.java`
- 導入: MVP、Wave 10 W10-γ でカード枠合成、Wave 14 W14-β でターン終了ボタン

### D-4. マップ描画 (DungeonRenderer)

3 フェーズ描画 (1: マップテクスチャ batch / 2: 境界線 + 階段マーカー + 罠 shapes / 3: アクタースプライト batch)。Wave 10 W10-β で ShapeRenderer 矩形→Texture 描画化、ELITE_SLIME は SLIME を `batch.setColor` で赤ティント。BREAKABLE_WALL は wall.png + 茶色ティント。

- 関連: `core/presentation/render/DungeonRenderer.java`
- 導入: MVP、Wave 10 W10-β (Texture 化)、Wave 11 W11-α (壊れる壁ティント)

### D-5. カード描画 (CardRenderer)

card_frame.png + イラスト + AP コスト + カード名 + element 色枠を Z-Index 順序で合成描画。`GlyphLayout` で名前幅計測 + 切り詰め、`SpriteBatch.setColor` のリセット遵守。

- 関連: `core/presentation/render/CardRenderer.java`
- 導入: Wave 10 W10-γ

### D-6. 入力系 (PlayerInputs)

3 ステート (通常 / カード選択中 / 移動権保持中) のキーボード入力 → BattleAction マッピング。WASD/矢印 移動、数字キー 1-9 カード選択、SPACE 待機、ENTER ターン終了、ESC キャンセル、Tab ステータス、R リセット。Wave 15 W15-β で F1-F4 スキル経路廃止。

- 関連: `core/presentation/input/PlayerInputs.java`
- 導入: MVP、Wave 14 W14-α でマウス方向選択統合、Wave 15 W15-β で F1-F4 削除

### D-7. マウス操作 (Wave 14 主要フロー対応)

マップタイルクリックで方向選択 → Move/UseCard 発火 (Viewport.unproject 経由でレターボックス耐性 / 自分マス short-circuit ガード)、カード手札クリック選択、ターン終了ボタンクリック。**マウス対応済 Screen は 5 種**: DungeonScreen / TitleScreen (起動クリック) / FirstRunPresetScreen / GameOverScreen / EquipmentScreen + NodeChoicePopup / TutorialOverlay 系 Popup。**未対応** (Wave 18+ 候補): SettingsScreen の値変更 < > / CardCollectionScreen / BestiaryScreen / SoulTreeScreen / TitleScreen のサブメニュー (L/T/C/E/S/K/B キー対応分)。キーボードと両立。

- 関連: `core/presentation/render/RenderLayout.screenToTile()` / `directionToward()` / `ButtonBounds.java`
- 導入: Wave 14 W14-α / W14-β

### D-8. UI プリセット (3 種)

`UiPreset { MINIMAL / STANDARD / INFO_RICH }`、初回起動の `FirstRunPresetScreen` で選択 + 永続化。HUD 要素表示量を制御 (MINIMAL = HP/AP のみ / STANDARD = + Soul/Gold/Phase / INFO_RICH = + 操作ヒント常時表示)。

- 関連: `core/infrastructure/save/UiPreset.java` / `core/presentation/render/RenderLayout.showExtendedHud / showPersistentHint`
- 導入: §15-1 / §15-8

### D-9. UI テーマ (Wave 17 ライト/ダーク)

`ThemeMode { LIGHT / DARK }` で SettingsScreen から切替可能。`UiThemeResolver.resolve(themeMode)` で `UiTheme.light()` / `UiTheme.dark()` を返す。`DddGame.activeUiTheme()` が毎フレーム評価するため、切替で即座に UI 色がリアルタイム反転。装備依存テーマは Wave 17 で廃止 (Equipment.themeName は record 残置で後方互換)。

- 関連: `core/infrastructure/save/ThemeMode.java` / `core/presentation/render/UiTheme.java` / `UiThemeResolver.java`
- 導入: Wave 4 W4-ε (装備依存)、Wave 17 (ライト/ダーク 2 トグル化)

### D-10. 画面エフェクト (ScreenEffects)

シェイク / フラッシュ / DamagePopup (ダメージ数値ポップ) / LowHpWarning (HP 警告点滅) を統合。BattleEvent.DamageDealt を購読してポップアップ生成 + シェイクトリガ。

- 関連: `core/presentation/screen/ScreenEffects.java` / `core/presentation/effect/DamagePopup.java` / `LowHpWarning.java`
- 導入: §15-5 / E-8、Wave 4 W4-β で集約

### D-11. i18n (日英二系統)

`Fonts.isJapaneseAvailable()` で日本語フォント (DotGothic16) 有無判定、`Strings.Ja` / `Strings.En` の二系統定数で文言切替。HUD / メニュー / ヒント / バフラベル / 層末ノード / イベント / チュートリアル すべて双言語対応。

- 関連: `core/presentation/render/Strings.java` / `Fonts.java`
- 導入: MVP、Wave 1 W1-4 で例外メッセージ i18n、Wave 6 W6-α で Screen 内ハードコード集約

---

## カテゴリ E: 永続化・セーブ・設定 (9 機能)

### E-1. SaveData v3 (層単位セーブ)

`schemaVersion=3`、スロット 1 つ、層境界 (戦闘外) でのみセーブ。stats 7 値 / deck / soulTotal / runCount / unlockedNodeIds / obtainedCardIds / loadout / defeatedEnemyKinds / tutorialSeen / **currentRunGold / currentRunSoul** (Wave 15 W15-α 追加で永続化バグ修正)。

- 関連: `core/infrastructure/save/SaveData.java`
- 導入: §15-11、Wave 6 W6-β (v2 Bestiary+tutorialSeen)、Wave 15 W15-α (v3 通貨永続化)

### E-2. graceful migration (v1 → v2 → v3)

旧バージョンの settings.json / save.json が現行スキーマで読み込めない場合、欠落 field を compact constructor で安全弁にフォールバック (v1 で defeatedEnemyKinds=null → 空リスト、v2 で currentRunGold/Soul 欠落 → 0)。起動時クラッシュなし。

- 関連: `core/infrastructure/save/SaveData.java` compact constructor / `Settings.java` compact constructor
- 導入: Wave 6 W6-β、Wave 15 W15-α、Wave 17 W17-α

### E-3. SaveDataConverter (双方向変換)

live (Player + GameContext) ↔ SaveData の純関数変換。未知 ID (旧 NodeId / 削除済 EnemyKind 等) は WARN ログ + graceful skip (Fonts フォールバック同思想)。

- 関連: `core/infrastructure/save/SaveDataConverter.java`
- 導入: §15-11、Wave 6 W6-β で Bestiary / tutorialSeen 対応

### E-4. SaveManager

ファイル I/O (`<user.home>/.ddd-jyogi-kuroto-f/save.json`)、Jackson + INDENT_OUTPUT で可読 JSON 生成。破損 / I/O エラーは SEVERE ログ + graceful (ゲームクラッシュなし)。ディレクトリ自動生成。

- 関連: `core/infrastructure/save/SaveManager.java`
- 導入: §15-11

### E-5. SettingsManager

`settings.json` を SaveManager と同ディレクトリで管理、破損 JSON は WARN + `Settings.DEFAULT` フォールバック。Jackson 処理は SaveManager と同型。

- 関連: `core/infrastructure/save/SettingsManager.java`
- 導入: §15-1

### E-6. Settings record (5 field)

`bgmVolume / seVolume / fullscreen / uiPreset / themeMode`。with* メソッドで immutable コピー、compact constructor で null/負値ガード + graceful migration。

- 関連: `core/infrastructure/save/Settings.java`
- 導入: §15-1、Wave 17 W17-α (themeMode 追加)

### E-7. PersistenceServices (集約)

`SaveManager + SettingsManager + Settings` を 1 final class に統合。apply はデータ純粋、副作用 (フルスクリーン / 音量 / テーマ反映) は DddGame 仲介で infrastructure 層の純粋性維持。

- 関連: `core/infrastructure/save/PersistenceServices.java`
- 導入: Wave 9 W9-β

### E-8. RunSession (集約)

`GameContext + TurnDirector + Random` を不変 record で統合、`Optional<RunSession>` でラン未開始を型表現。`requireRunSession()` で IllegalStateException + 明示メッセージのデバッグ性向上。

- 関連: `core/application/RunSession.java` / `DddGame.requireRunSession()`
- 導入: Wave 9 W9-α

### E-9. GameResources (集約)

`Fonts + SoundManager + CardImageRegistry` を 1 final class に集約 + 完全な防衛的 dispose (try-catch + null チェック内包)。Wave 8 W8-β で 3 個別フィールドから統合。

- 関連: `core/presentation/render/GameResources.java`
- 導入: Wave 8 W8-β

---

## カテゴリ F: 音響・素材・i18n (8 機能)

### F-1. SoundManager

BGM (Music) / SE (Sound) 一元管理、graceful fallback (ファイル欠損時 no-op + WARN)。Settings.bgmVolume / seVolume を参照、playBgm は重複再生回避・stopBgm で切替。EnumMap キャッシュで loaded only。

- 関連: `core/infrastructure/audio/SoundManager.java`
- 導入: §15-5、Wave 10 W10-α で 19 件投入

### F-2. BgmKind (2 種)

`TITLE / DUNGEON` の 2 値 enum。`assets/audio/bgm/*.ogg` 対応。

- 関連: `core/infrastructure/audio/BgmKind.java`
- 導入: Wave 10 W10-α

### F-3. SeKind (18 種)

`ENEMY_DEFEATED / PLAYER_DAMAGED / DEAL_DAMAGE / BUTTON_DECISION / BUTTON_SELECTION / FLOOR_ADVANCE / HP_LOW / LEVEL_UP / BLOCK_BREAK / STATUS_UP / CARD_DRAW_C/U/R (レアリティ別 3 分割) / CARD_PHYSICAL / CARD_MAGIC / CARD_MOVE / CARD_BUFF / CARD_TRAP (種別別 5 分割)`。

- 関連: `core/infrastructure/audio/SeKind.java`
- 導入: Wave 10 W10-α (18 種拡張)、Wave 11 W11-β (CARD_DRAW 分岐)

### F-4. キャラクタースプライト

`assets/sprites/` に `player.png / slime.png / goblin.png / skeleton.png / dragon_boss.png` (5 種、EnemyKind マッピング)。Nearest filter (ピクセルアート)。ELITE_SLIME は SLIME を `batch.setColor` で赤ティント。

- 関連: `core/presentation/screen/DungeonScreen.show` (Texture ロード)
- 導入: Wave 10 W10-β (スプライト投入)、Wave 10 W10-β-2 (skeleton/goblin 入替)

### F-5. カードイラスト + CardImageRegistry

`assets/cards/card_01.png 〜 card_50.png` (49 ファイル、card_27 のみ欠番) を `card_image_map.json` で CardId → filename マッピング、test.png fallback。Wave 10 W10-γ でカード枠テクスチャ (icons/cards/card_frame.png) も同時ロード。Nearest filter + 防衛的 dispose。

- 関連: `core/infrastructure/bootstrap/CardImageRegistry.java`
- 導入: §15-3、Wave 10 W10-γ で frame 追加

### F-6. マップタイル素材

`assets/tiles/wall.png` + `floor.png`、80×80 px、Nearest filter (ピクセルパーフェクト)。階段は floor.png + ShapeRenderer 黄色マーカーで識別 (専用テクスチャは Wave 18+ 候補)。

- 関連: `core/presentation/screen/DungeonScreen.show` / `DungeonRenderer.drawMapTextures`
- 導入: Wave 10 W10-β

### F-7. Fonts (DotGothic16 + Latin)

`assets/fonts/DotGothic16-Regular.ttf` を FreeType で 16/32/48 px サイズ生成。事前グリフ生成 (`GAME_GLYPH_CHARS` = ひらがな + カタカナ + 主要漢字 + 記号) + Reflection で Strings.Ja を動的収集して豆腐文字予防。2048×2048 PixmapPacker で 1 texture page。フォント欠損時 BitmapFont フォールバック (英数のみ)。

- 関連: `core/presentation/render/Fonts.java`
- 導入: §15-5 / E-9

### F-8. Strings (i18n 二系統)

`Strings.Ja / Strings.En` で文言定数分離 (国際化ライブラリ未使用、YAGNI)。HUD ヒント / ステータス / バフ名 / 層末ノード / イベント / チュートリアル / 設定 を双言語定義。`Fonts.isJapaneseAvailable()` で判定。

- 関連: `core/presentation/render/Strings.java`
- 導入: MVP、Wave 1 W1-4 / Wave 6 W6-α で集約

---

## 機能一覧サマリー表

| カテゴリ | 機能数 | 代表機能 |
|---|---|---|
| A 戦闘・カード・スキル | 10 | ターン制 / AP / BattleAction / CardEffect / line scan + AOE / レアリティ / ダメージ計算 / Skill (敵) / ActiveBuff / 罠 |
| B ダンジョン・マップ・敵 AI | 10 | Tile / DungeonState / BSP / BFS+LOS / 層 / LayerEndNode / 敵 5 種 / AI 3 状態 / BattleEvent 16 / 壊れる壁 |
| C メタ進捗・ソウルツリー・装備・図鑑 | 10 | PlayerProgress / Soul/Gold / ツリー 25 / NodeEffect / 装備 / Catalog / Bestiary / カード図鑑 / Shop |
| D UI・画面遷移・入出力 | 11 | 10 Screen / 5 Popup / HUD / マップ描画 / カード描画 / 入力 / マウス / プリセット / テーマ / エフェクト / i18n |
| E 永続化・セーブ・設定 | 9 | SaveData v3 / migration / Converter / Manager / SettingsManager / Settings / PersistenceServices / RunSession / GameResources |
| F 音響・素材・i18n | 8 | SoundManager / BGM 2 / SE 18 / スプライト / カードイラスト / タイル / Fonts / Strings |
| **合計** | **58** | Wave 1〜17 + MVP 累積 |

---

## Wave 別の実装履歴

| Wave | 主要実装 |
|---|---|
| MVP | 基本戦闘ループ + スキル + 階段 + Title/Dungeon/GameOver + i18n 骨格 |
| Wave 1 | InitialStateFactory 経由化 / CreditsScreen / ConfirmationDialog / i18n 4 ペア |
| Wave 2 | SoulTree JSON 化 (tree.json) / 層数拡張 (LayerExtendEffect) / スキル枠 4 常時表示 |
| Wave 3 | LayerEndNode.ShopEquipment / NodeResolveContext / イベントノード多様化 |
| Wave 4 | DungeonScreen 責務分割 (EnemyKindMemory/ScreenEffects/EliteRewardOrchestrator) / BestiaryScreen / UiTheme 装備依存 |
| Wave 5 | TurnEngine 3 クラス分離 / PlayerProgress 新設 |
| Wave 6 | Screen 内 i18n 集約 / SaveData v2 (bestiary + tutorialSeen) / DddGame 統合 |
| Wave 7 | TurnEngineHelpers 切出 / progress() 公開 / presentation テスト補強 |
| Wave 8 | LayerEndNode 日本語汚染除去 (EventKind 化) / GameResources 集約 |
| Wave 9 | RunSession / PersistenceServices |
| Wave 10 | 素材投入 (音 19 + スプライト 5 + 装備 2 + カード枠) + DungeonRenderer Texture 化 + CardRenderer + SE 連結 + 装備アイコン |
| Wave 11 | 壊れる壁ギミック / CARD_DRAW レアリティ別音分け |
| Wave 12 | 攻撃範囲の実装 (射程 + AOE) |
| Wave 13 | 敵 AI 強化 (視界 LOS + 3 状態 + AGGRESSIVE/CAUTIOUS) |
| Wave 14 | マウス操作対応 (マップクリック / ターン終了ボタン / メニュー画面) |
| Wave 15 | P0 致命バグ 4 件 (Gold/Soul 永続化 + CAUTIOUS AI + Buff 順序 + ドロー仕様) + SkillSlot 廃止 + Rest 制限 |
| Wave 16 | チュートリアル文言充実 + HUD スキル枠削除 + 壊れる壁配置改善 + ShopEquipment 即時反映 |
| Wave 17 | UiTheme ライト/ダーク 2 トグル化 |

---

## SpecificationIssues.md 最終ステータス

[SpecificationIssues.md](SpecificationIssues.md) で挙げられた 11 件のうち **10 件修正完了**、#4 ソウルツリー描画は既知 UI 不具合として未対応 (ユーザー判断で本セッションスコープ外、Wave 18+ で対応余地):

| # | 項目 | ステータス | 完了 Wave |
|---|---|---|---|
| 1 | 壊れる壁の生成位置改善 | ✅ 完了 | Wave 16 W16-β |
| 2 | 休憩ノード無償全回復 | ✅ 完了 | Wave 15 W15-γ |
| 3 | SkillSlot 廃止 | ✅ 完了 | Wave 15 W15-β |
| 4 | ソウルツリー描画 (既知 UI 不具合) | ⏸️ 未対応 | 既知の UI 描画不整合 (枝線/座標)、優先度低でリリース後対応、ユーザー判断で本セッションスコープ外 |
| 5 | チュートリアル文言充実 | ✅ 完了 | Wave 16 W16-α |
| 6 | HUD 重なり整理 | ✅ 完了 | Wave 15-16 副次改善 |
| 7 | CAUTIOUS AI 崩壊 | ✅ 完了 | Wave 15 W15-α |
| 8 | UiTheme 装備依存 → ライト/ダーク | ✅ 完了 | Wave 17 |
| 9 | ShopEquipment ラン中反映 | ✅ 完了 | Wave 16 W16-γ |
| 10 | SPEED_UP 持続 1 消滅 | ✅ 完了 | Wave 15 W15-α |
| 11 | セーブロード Gold/Soul 消滅 | ✅ 完了 | Wave 15 W15-α |

---

## Wave 18+ 候補 (将来の拡張)

新機能は本ドキュメントに「実装済」として書かれていないが、`tasks/m2_backlog.md` に候補がリストアップされている:

- **AI / 戦闘拡張**: 敵側 SkillEffect への range 追加 / 新敵種 (弓兵 / 魔法使い) / IDLE ランダムウォーク / PATROL/DEFENDER AI プロファイル
- **UI / UX**: マウス操作完全網羅 (SettingsScreen 値変更 < > / CardCollectionScreen / BestiaryScreen / SoulTreeScreen / TitleScreen サブメニュー) / ドラッグ&ドロップカード / マウスホバーツールチップ / 文字出るときにアクション
- **音響**: CARD_USED 種別音分け (BattleEvent.SkillUsed に CardEffect 情報追加)
- **エネミー**: EnemyKind enum リネーム (SWIFT_SLIME → SKELETON 等)
- **演出**: AOE LOS 精密化 / DEAL_DAMAGE SE 連打回避 / 装備色 tint バリエーション
- **品質**: Wave 9 監査 Must 残 4 件 (DEFAULT_MAX_LAYER DRY / i18n 残 / BattleEvent.CardUsed / 行数表記)
- **データ整理**: 壁床バリエーション機構 / 階段専用テクスチャ
- **プラットフォーム**: Android は**現行版の対象外** (2026-08-12 PLATFORM-01)。再採択する場合は backend 追加ではなく、共有 core の言語水準・モジュール分離・タッチ E2E・セーブ同期方式を含む独立計画とする
- **#4 ソウルツリー描画 (既知 UI 不具合)**: 枝線/座標の不整合、優先度低でリリース後対応 (本セッションスコープ外、Wave 18+ で実機再現 → CTO チェックポイント「データと UI 分離原則」に則り修正)

---

## 運用方針

- 本ドキュメントは **実装機能の俯瞰インデックス** として継続更新する (仕様の SoT は GAME_DESIGN.md、本ドキュメントは索引)
- Wave 18+ で新機能追加時は、該当カテゴリの末尾に追記 + 「Wave 別の実装履歴」表に行追加
- 機能の意図 / 設計判断は `GAME_DESIGN.md` に集約 (本ドキュメントは「何」のみ、「なぜ」は別)
- バグ修正のみで機能カタログに追加項目がない場合は本ドキュメント無修正で OK
- 数値 (テスト件数 / カード数 / 装備数 / イラスト数 等) は実コード/JSON/Grep 実測値を反映、推測や下限表記 (「20+」) は避ける
