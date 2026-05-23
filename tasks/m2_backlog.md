# M2 / 本番後バックログ

> 本セッション (2026-05-23) でハッカソン直前まで実装した範囲のうち、時間的・設計責任分担の
> 都合で意図的に**未着手・部分実装で停止**した項目を集約する。
>
> 主たる関連: [tasks/todo.md](todo.md) の Phase 6/7/8、`docs/SystemSummary.md §8` 拡張ロードマップ、
> `docs/RolesDivision.md` の機能カテゴリ A〜E。

---

## 2026-05-24 セッション最終アップデート + Wave 1〜11 完了

本セッションで P2 (致命 3) / P3 (テスト 9 / 26 件追加) / P4 (設計負債 7) + M2 Wave 1〜9 + M2 Wave 10 (5 段階 α/β/β-2/γ/δ) + M2 Wave 11 (3 段階 α/β/γ) を完了。テスト 545 → 706 件 (+161)。DddGame は 9 Wave 通算で 700+ → 549 行 = ~22% 削減、責務 4 集約 (RunSession / GameResources / PlayerProgress / PersistenceServices) に分離。Wave 10 でチームメイト素材 (音声 19 / スプライト 5 / 装備 2 / カード枠 1) を一括投入し、ShapeRenderer 矩形描画 → Texture 描画 + カード枠合成 + SE 9 箇所追加で UI 質感を大幅向上。Wave 11 で壊れる壁ギミック (Tile.BREAKABLE_WALL + 移動カード破壊 + block_brake SE) + CARD_DRAW レアリティ別音分け (Card.rarity Optional + drawSeFor 純関数) を実装。残った M2 着手項目:

- **完了 (P2-P4)**: JSON 欠損 graceful、セーブ後ドロー仕様確認、Texture リーク、ダメージ計算 DRY 集約、
  BuffKindLabels 集約、HAND_DETAIL_TEXT_Y 文字数上限、EnemyKind isElite/isBoss、Optional 可読性、
  CardCollectionScreen の game.cardCatalog 経由化、DamageFormula 新設
- **完了 (M2 Wave 1)**: Fonts/EquipmentScreen の InitialStateFactory 経由化 (P4-8 残り)、
  CreditsScreen 拡充 (素材クレジット表示)、汎用 ConfirmationDialog 新設 + R キー確認ダイアログ、
  ソウルツリー以外の例外/通知メッセージ i18n 移管 (4 ペア)
- **完了 (M2 Wave 2)**: SoulTree マスタの JSON 化 (tree.json + SoulTreeCatalog、23 → 25 ノード)、
  層数拡張 (NodeEffect.LayerExtendEffect 新設 + GameContext.maxLayer + tree.json に layer_extend_4/5)、
  スキル枠 2→4 常時表示 (HudRenderer.drawSkillSlots + F1-F4 キーバインド)
- **完了 (M2 Wave 3)**: LayerEndNode.Shop + NodeResolveContext 統一 (cards + equipments resolver
  をラップした record で将来拡張耐性、CardId 化で domain 純度向上)、ShopEquipment 新設 (装備購入
  ノード、本物の装備名表示)、イベントノード 3 種多様化 (治療の泉 / 黄金の宝箱 + 負値 delta 許容)
- **完了 (M2 Wave 4)**: DungeonScreen 697 → 463 行責務分割 (EnemyKindMemory / ScreenEffects /
  EliteRewardOrchestrator の 3 クラス切り出し、~34% 削減)、BestiaryScreen 新規 (タイトル B キー
  で撃破済敵一覧)、装備テーマ変動 UI (UiTheme + UiThemeResolver、equipment.json themeName 5 件設定)
- **完了 (M2 Wave 5)**: SoulTree.allNodes() Supplier 注入で依存方向違反解消 (W5-γ)、
  TurnEngine 663 → 330 行を 3 クラス分離 (TurnEngineMovement / TurnEngineCardResolver
  / TurnEngineSkillResolver、計約 50% 削減、checkAndTriggerTrap は共通ヘルパとして TurnEngine 残置、
  W5-α-1/2/3)、PlayerProgress record 新規追加 (ラン外進捗 7 要素を集約、内部リファクタは Wave 6 で実施、W5-β)
- **完了 (M2 Wave 6)**: Screen 内ハードコード日本語 9 箇所を Strings i18n に集約 (W6-α)、
  SaveData schemaVersion 1→2 + bestiary / tutorialSeen 永続化 + v1 graceful migration
  (compact constructor で null → empty 正規化、EnemyKind.valueOf の IllegalArgumentException catch +
  WARN + skip パターン、W6-β)、DddGame の 7 ラン外フィールドを PlayerProgress 1 record に内部統合
  (タイガーリリー戦略で getter / setter 中継化、Screen 公開 API 互換維持、W6-γ)
- **完了 (M2 Wave 7)**: TurnEngineHelpers 切り出し (TurnEngine 330 → 197 行、共通ヘルパ 3 件を独立クラス化、W7-α)、
  DddGame の中継 getter 7 件を progress() 1 メソッドに集約 (6 Screen の 16 箇所を機械的置換、W7-β)、
  presentation 層テスト補強 (BuffKindLabelsTest + NodeIconPathResolverTest を新規追加、+22 件、W7-γ)
- **完了 (M2 Wave 8)**: LayerEndNode の displayName 撤去 + EventKind enum 化 + LayerEndNodeLabels 新設で
  ドメイン日本語汚染を完全排除 + Strings 8 キー追加 (W8-α)、DddGame の LibGDX リソース 3 件を GameResources に集約
  + 完全なる防衛的 dispose (try-catch + null チェック内包、W8-β)
- **完了 (M2 Wave 9)**: DddGame の application 層を RunSession record に集約 + Optional<RunSession> でラン未開始
  を型表現 + onRunEnded() で Optional.empty() ライフサイクル同期 + requireRunSession() で IllegalStateException
  デバッグ性向上 (W9-α)、DddGame の永続化層を PersistenceServices final class に集約 + apply はデータ層純粋
  + LibGDX 副作用 (フルスクリーン / 音量) は DddGame.updateHardwareConfigurations に分離 (W9-β)
- **完了 (M2 Wave 10)**: 音声素材 19 件投入 + SeKind enum 18 種に拡張 (旧 6 → 新 18、CARD_USED 廃止) + 発火点 3 箇所連結 (W10-α、commit ab55ad5)、
  キャラスプライト 5 種投入 + DungeonRenderer の 3 フェーズ Texture 描画化 + ELITE_SLIME 赤ティント + 色リセット遵守 (W10-β、commit aa2bf09)、
  SE 7 発火点追加 + スケルトン/ゴブリン入れ替え + チュートリアル閉じ BUTTON_DECISION + ノード習得 STATUS_UP + HP_LOW 境界線管理 + Elite 撃破 LEVEL_UP 流用 + カードドロー CARD_DRAW_C + EquipmentScreen クリック SE (W10-β-2、commit ef48bc3)、
  装備アイコン 2 種 + Equipment.iconPath Optional 追加 + EquipmentScreen で 40×40 行頭描画 + カード枠合成描画 CardRenderer 新規 + HudRenderer.drawHand 連携 + CardImageRegistry.frame() 追加 + Z-Index 厳守 + GlyphLayout 切り詰め (W10-γ、commit ed2ac24)
- **SettingsScreen バグ修正**: ESC 押下時の "No buffer allocated" クラッシュを handleInput() boolean 化 + return true で解消 (commit 2d63a8e)
- **完了 (M2 Wave 11)**: 壊れる壁ギミック実装 (Tile.BREAKABLE_WALL + DungeonMap.withTileAt + 外枠境界防御 IAE + BattleEvent.WallBroken + TurnEngineMovement 3 段順序遵守 + InitialStateFactory 各層 2-3 個配置 + DungeonRenderer 茶色ティント (既存 wall.png 流用、新素材ゼロ) + DungeonScreen BLOCK_BREAK SE 発火、W11-α、commit 04995d2、テスト 8 件追加)、CARD_DRAW レアリティ別音分け (CardRarity enum 新規 + Card.rarity Optional + 後方互換コンストラクタ + rarityOrDefault 型安全窓口 + CardCatalog graceful 読込 + cards.json 4 枚仮設定 + DungeonScreen.drawSeFor 純関数で手札最高 rarity → SE 分岐、W11-β、commit 03ae133、テスト 9 件追加)
- **M2 送り (Wave 12+ 候補)**:
  - **新機能 3 件** (ユーザー要望、要仕様詰め): 遠距離攻撃 AI 強化 / マウス操作 / 文字出るときにアクション
  - **CARD_USED カード種別別音分け**: BattleEvent.SkillUsed に CardEffect 情報追加 (sealed switch 全箇所影響 = 破壊的変更)
  - **cards.json への rarity 全カード割当** (チームメイト領域、カードバランス確定とセット)
  - **EnemyKind enum リネーム**: SWIFT_SLIME → SKELETON / TOUGH_SLIME → GOBLIN / BOSS → DRAGON (cards.json / equipment.json / 各 fixture 影響あり、破壊的変更)
  - **装備色 tint バリエーション**: boots1/boots2 → 他装備派生 (SpriteBatch.setColor or シェーダー)
  - **Wave 9 監査の Must 4 件**: DEFAULT_MAX_LAYER DRY 解消 / i18n 残 16 / BattleEvent.CardUsed 追加 / 行数表記補正
  - **HudRenderer の WallBroken UI 演出** (W11-α は no-op スタブのみ、破壊アニメ/シェイクは未実装)
  - **Bestiary 次行動の点線予告 UI** (AI 設計絡み、チームメイト領域)
  - **装備テーマのセット装備複合 / 漸進的アニメーション** (equipment.json 仕様拡張)
  - **壁床バリエーション機構** (チームメイト素材投入待ち、層ごとローテーション)
  - **階段専用テクスチャ** (チームメイト素材待ち、現状黄色マーカー代替)
  - **Android 対応** (Phase D、M2 後半、「Doko-demo」スローガンの核)

---

## A. CTO 範囲・本番後着手可能 (技術負債)

| 項目 | 出所 | 規模 | 備考 |
|---|---|---|---|
| ~~**DungeonScreen 697 行の責務分割**~~ → **Wave 4 W4-α/β/γ で完了** (commit 40a4cff / 9a24c01 / 53c4cb3、697 → 463 行 ~34% 削減) | final-architect 2026-05-23 | L | God Object 化 |
| ~~**DddGame の PlayerProgress 内部統合**~~ → **Wave 6 W6-γ で完了** (commit 0bdb387、7 フィールド → 1 record に集約、Screen 公開 API 互換維持) | final-architect 2026-05-23 | M | God Object 化 |
| ~~**LayerEndNode.Shop vs NodeEffect.CardGrantEffect の Card/CardId 表現統一**~~ → **Wave 3 Task A で完了** (commit 20aff7a、NodeResolveContext 導入で将来拡張耐性も確保) | final-architect 2026-05-23 | M | 驚き最小 |
| ~~**EquipmentScreen / Fonts.java の InitialStateFactory 直接参照を game.cardCatalog 経由化**~~ → **Wave 1 Task 1 で完了** (commit cacb7ec) | A7 multi-perspective Must | S | static rowText / glyphs 階層が深い |
| ~~**SoulTree ノード定義の JSON 化**~~ → **Wave 2 Task A で完了** (commit fe0febf、tree.json + SoulTreeCatalog) | final-architect | M | 保守性向上 |
| ~~**TurnEngine 671 行の分割**~~ → **Wave 5 + Wave 7 W7-α で完了** (commit 7fa67a0 / 406232b / 5b352fe / addd6b0、663 → 197 行、Movement / CardResolver / SkillResolver / Helpers の 4 クラス分離 ~70% 削減) | final-architect | M〜L | 商品化前リファクタ |
| ~~**LayerEndNode 日本語 displayName のドメイン汚染解消**~~ → **Wave 8 W8-α で完了** (commit 5970c76、EventKind enum 化 + LayerEndNodeLabels 新設、ドメイン日本語完全排除) | domain-architect | M | i18n 全面対応とセットで |
| **presentation / infrastructure テスト補強** (Wave 7 W7-γ で部分対応: BuffKindLabels / NodeIconPathResolver / UiThemeResolver / RenderLayout 系。Screen 群はヘッドレス対応コスト overkill のため M2 後半送り) | devils-advocate | M | カバレッジ偏在 |
| ~~**SaveData v2 migration**~~ → **Wave 6 W6-β で完了** (commit e97a76e、schemaVersion 1→2、bestiary + tutorialSeen 永続化、v1 graceful migration) | multi-perspective-reviewer | M | 後方互換 |
| ~~**スキル枠 2 → 4 の常時表示**~~ → **Wave 2 Task C で完了** (commit 38a10a3、HudRenderer.drawSkillSlots + F1-F4) | tasks/todo.md Phase 7-3 | M | 4 枠あるが MVP は 2 個のみ使用 |

---

## B. M2 拡張機能 (Phase A〜D)

| 項目 | 出所 | 規模 |
|---|---|---|
| ~~**Bestiary フル UI**~~ → **Wave 4 W4-δ で完了** (commit 58f9a46、BestiaryScreen 新規 + TitleScreen B キー動線、次行動予告は M2 送り) | E-7 | M |
| **Android 対応** (「Doko-demo」スローガンの核、Phase D) | D-4 | L |
| ~~**層数拡張**~~ → **Wave 2 Task B で完了** (commit ee78190、NodeEffect.LayerExtendEffect + GameContext.maxLayer + layer_extend_4/5) | Phase A | M |
| ~~**ショップノードの装備購入機能**~~ → **Wave 3 Task B で完了** (commit 9c34457、ShopEquipment record) | §15-9 / E-5 | M |
| ~~**イベントノード多様化**~~ → **Wave 3 Task C で完了** (commit 4a8eda9、3 種 + 負値 delta 許容) | §15-6 | M |
| ~~**装備テーマ変動 UI**~~ → **Wave 4 W4-ε で完了** (commit 06c62ba、UiTheme + UiThemeResolver、equipment.json themeName 5 件設定) | Phase A | M |
| ~~**CreditsScreen 拡充**~~ → **Wave 1 Task 2 で完了** (commit b56cdb1) | tasks/todo.md Phase 6 | S |
| ~~**R キー無確認リセット → 確認ダイアログ化**~~ → **Wave 1 Task 3 で完了** (commit 6552b1b、汎用 ConfirmationDialog 新設) | 前々セッション申し送り | S |
| ~~**ソウルツリー以外の例外メッセージのローカライズ**~~ → **Wave 1 Task 4 で完了** (commit 10f0208、4 ペア i18n 移管) | 前セッション申し送り | S |
| **階段専用テクスチャ投入** (現状: 床テクスチャ + 黄色マーカーで代替、commit 8502888) | 2026-05-23 セッション追加 | S |
| **壁・床素材のバリエーション** (複数素材を層ごとにローテーション) | 2026-05-23 セッション追加 | M |

---

## C. チームメイト / 設計領域 (CTO スコープ外)

| 項目 | 担当 |
|---|---|
| **スプライト/ピクセルアート素材投入** (敵・プレイヤー)。マップタイル (壁/床) は本日 commit 8502888 で投入完了。制作仕様: [docs/AssetProductionSpec.md §A-1](../docs/AssetProductionSpec.md) | チームメイト |
| **音声素材実ファイル投入** (BGM / SE、`assets/audio/README.md` 配置規約あり)。制作仕様: [docs/AssetProductionSpec.md §A-2](../docs/AssetProductionSpec.md) | チームメイト |
| **スキルカード未画像 11 枚** (zangeki, strong_strike, magic_bolt, fireball, ember_shot, blaze_nova, blink_step, flame_circle, arcane_veil, stone_wall, haste — 現在 test.png fallback)。制作仕様: [docs/AssetProductionSpec.md §A-3](../docs/AssetProductionSpec.md) | チームメイト (アーティスト) |
| **equipment.json grantedCards コンセプト不一致 5 件** (dwarven_boots→stone_throw、phoenix_feather_cloak→fireball、venom_fang→piercing_arrow、earth_bracers→armor_break、crimson_bow→strong_strike) | カード/装備設計 |
| **カード完全一致グループの抜本差別化** (副次効果の付与) | カード設計 |
| **新 EnemyKind (SWIFT_SLIME / TOUGH_SLIME) のフレーバー・バランス確定** | 敵設計 |
| **クリア条件再検討** (階段直行最適解の回避、敵配置) | レベル設計 |

---

## 確定済み判断 (本仕様書から除外)

ハッカソン本番 (2026-05-23) で次の判断が確定し、本バックログから除外:

| 項目 | 決定内容 | 確定日 |
|---|---|---|
| **コードライセンス選定** | ライセンス指定なし (オーナー保有)。README.md に明記 | 2026-05-23 |
| **キーマッピング議論** | WASD / 矢印キー固定で確定 (hjkl / 8 方向対角線は不採用) | 2026-05-23 |

---

## D. 申し送りの運用

- 本ファイルは `tasks/todo.md` の補助。todo.md は MVP/M1.5/M2 の包括計画、本ファイルは「本セッション
  時点で意図的に残した」具体項目。
- 当日のハッカソン中に追加発覚した問題は `tasks/ai_log/lessons.md` に記録する。
- 各項目を着手する際は GitHub Issue を立て、ブランチ命名 (`feat/#<issue番号>`) は
  `docs/BranchingStrategy.md §2` 準拠。
