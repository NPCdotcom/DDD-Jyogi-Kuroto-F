# M2 / 本番後バックログ

> 本セッション (2026-05-23) でハッカソン直前まで実装した範囲のうち、時間的・設計責任分担の
> 都合で意図的に**未着手・部分実装で停止**した項目を集約する。
>
> 主たる関連: [tasks/todo.md](todo.md) の Phase 6/7/8、`docs/SystemSummary.md §8` 拡張ロードマップ、
> `docs/RolesDivision.md` の機能カテゴリ A〜E。

---

## A. CTO 範囲・本番後着手可能 (技術負債)

| 項目 | 出所 | 規模 | 備考 |
|---|---|---|---|
| **UI プリセットの HUD 描画反映** (`HudRenderer` が `Settings.uiPreset` を消費し、MINIMAL/STANDARD/INFO_RICH で `showExtendedHud` / `showPersistentHint` を実利用する) | final-architect 2026-05-22 | S〜M | プリセット選択 UX は実装済、視覚差分が未配線。signature 変更が多重 |
| **手札 (HudRenderer.drawHand) への card_image サムネイル追加** | 本セッション Phase A 申し送り | S | CardCollectionScreen は対応済。drawHand はテキストのまま |
| **スキル枠 2 → 4 の常時表示** (`HudRenderer` / `PlayerInputs`、ソウルツリーの `slot_expand_*` で増えた枠も 4 まで描画する) | tasks/todo.md Phase 7-3 | M | 4 枠あるが MVP は 2 個のみ使用 |
| **SoulTree ノード定義の JSON 化** (現在 Java ハードコード、カード/装備は JSON 化済で非対称) | final-architect | M | 保守性向上 |
| **TurnEngine 671 行の分割** (カード解決 / 敵解決へ) | final-architect | M〜L | 商品化前リファクタ |
| **LayerEndNode 日本語 displayName のドメイン汚染解消** (i18n 移行時) | domain-architect | M | i18n 全面対応とセットで |
| **presentation / infrastructure テスト補強** (Screen 群のヘッドレス可能部分、LibGDX 非依存の純粋ロジック) | devils-advocate | M | カバレッジ偏在 |
| **SaveData v2 migration** (現状は破損時 graceful「セーブなし扱い」のみ) | multi-perspective-reviewer | M | 後方互換 |
| **`fatJar` 配布形態の検証** (gradle task が本セッションで追加されていれば再ビルドテスト) | tasks/todo.md M2 提出 | S | |
| **mac/Linux 動作確認** (CI または別マシン) | tasks/todo.md Phase 6 | S | |
| **デモシナリオ動画録画** (§15-12) | M2 提出 | S | 提出物 |
| **`.\gradlew run` 実機通しプレイで残バグ発掘** (タイトル→3 層→Elite→層末→セーブ→つづき→ゲームオーバー) | devils-advocate 最重要 | S | ユーザー手動、当日マシン |

---

## B. M2 拡張機能 (Phase A〜D)

| 項目 | 出所 | 規模 |
|---|---|---|
| **Bestiary フル UI** (画面 + 次行動の点線予告、§15-5) — Phase C で record スタブのみ着手 | E-7 | M |
| **Android 対応** (「Doko-demo」スローガンの核、Phase D) | D-4 | L |
| **層数拡張** (ソウルツリーノード経由で MAX_LAYER を増やす、現在 3 固定) | Phase A | M |
| **ショップノードの装備購入機能** (現状カード追加報酬のみ) | §15-9 / E-5 | M |
| **イベントノード多様化** (現状「ソウルの祠」固定 1 種) | §15-6 | M |
| **HP 低下警告演出** (§7-2 / C-2、画面周囲赤フィルタ等) | Phase A | S |
| **装備テーマ変動 UI** (§7-2 / §15-9、装備で UI 色テーマが変わる) | Phase A | M |
| **CreditsScreen** (使用素材クレジット表示画面) — Phase C で簡易版を実装する場合 | tasks/todo.md Phase 6 | S |

---

## C. チームメイト / 設計領域 (CTO スコープ外)

| 項目 | 担当 |
|---|---|
| **スプライト/ピクセルアート素材投入** (敵・プレイヤー・マップ。現在 ShapeRenderer 矩形)。制作仕様: [docs/AssetProductionSpec.md §A-1](../docs/AssetProductionSpec.md) | チームメイト |
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
