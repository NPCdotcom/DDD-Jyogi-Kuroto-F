# M2 / 本番後バックログ

> 本セッション (2026-05-23) でハッカソン直前まで実装した範囲のうち、時間的・設計責任分担の
> 都合で意図的に**未着手・部分実装で停止**した項目を集約する。
>
> 主たる関連: [tasks/todo.md](todo.md) の Phase 6/7/8、`docs/SystemSummary.md §8` 拡張ロードマップ、
> `docs/RolesDivision.md` の機能カテゴリ A〜E。

---

## 2026-05-24 セッション最終アップデート + Wave 1〜7 完了

本セッションで P2 (致命 3) / P3 (テスト 9 / 26 件追加) / P4 (設計負債 7) + M2 Wave 1 (4 タスク) + M2 Wave 2 (3 タスク) + M2 Wave 3 (3 タスク) + M2 Wave 4 (5 段階 α/β/γ/δ/ε) + M2 Wave 5 (5 段階 γ/α-1/α-2/α-3/β) + M2 Wave 6 (4 段階 α/β/γ/δ) + M2 Wave 7 (4 段階 α/β/γ/δ) を完了。テスト 545 → 682 件 (+137)。残った M2 着手項目:

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
- **M2 送り (Wave 7 残置)**: Bestiary 次行動の点線予告 UI (AI 設計絡み、チームメイト領域)、
  装備テーマのセット装備複合 / 漸進的アニメーション (equipment.json 仕様拡張がチームメイト領域)、
  DddGame の component holder 分離 (GameContext / TurnDirector / Fonts / SaveManager を別クラスへ、Wave 8+)
- **M2 送り**: 階段専用テクスチャ (チームメイト素材待ち)、
  Bestiary 次行動の点線予告 (AI 戦術絡み、M2 送り)、
  装備テーマのセット装備複合 / 漸進的アニメーション (M2 送り)

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
| **LayerEndNode 日本語 displayName のドメイン汚染解消** (i18n 移行時) | domain-architect | M | i18n 全面対応とセットで |
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
