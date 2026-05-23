# システム・ゲーム性まとめ

DDD-Jyogi-Kuroto-F の **MVP 時点の実装俯瞰** + 今後の拡張計画を 1 枚に集約したドキュメント。
仕様の確定事項は [GAME_DESIGN.md](./GAME_DESIGN.md) に、実装タスクは [tasks/todo.md](../tasks/todo.md) に分かれているので、本書では「全体像と関係性」を一目で掴めることを目的とする。

---

## 1. このゲームを 30 秒で説明する

| 観点 | 内容 |
|---|---|
| ジャンル | ローグライト (ローグライク + メタ進行) / 2D ピクセル |
| コア体験 | 「死を繰り返して強くなる」 — 1 プレイ 3〜5 分 |
| 戦闘の核 | **AP 制 + 変動ターン** スキル戦闘。速度が高いほど 1 ターン内に多く動ける |
| メタ進行 | 死亡時にソウルが残る (= 永続強化通貨)。金貨と装備は喪失 |
| クリア条件 | ダンジョン階段 (`>`) 到達 (敵全滅では遷移しない) |
| 「D」の意味 | **Doko-demo** — Java + LibGDX で Desktop / Android を 1 ソースで動かす |

---

## 2. プレイループ (現状の MVP)

```
 [ Title ]                                 [ GameOver / Cleared ]
    │  ENTER                                       │  ENTER
    ▼                                              │
 [ Dungeon ] ── プレイヤー入力 ──┐                 │
    │   ▲                       ▼                 │
    │   │              TurnEngine (純関数)          │
    │   │                       │                 │
    │   │                       ▼                 │
    │   │       新 DungeonState + BattleEvent[]    │
    │   │                       │                 │
    │   │      AP 切れ → 自動 EndTurn → ENEMY_TURN  │
    │   │                       │                 │
    │   │              EnemyAi → TurnEngine        │
    │   │                       │                 │
    │   │      全敵終了 → startPlayerTurn (AP回復) ─┘
    │   └─────────────── PLAYER_TURN へ戻る
    │
    ▼ (HP 0 → GAME_OVER / 階段踏破 → CLEARED)
```

---

## 3. アーキテクチャ俯瞰

[GAME_DESIGN.md §9](./GAME_DESIGN.md) のレイヤー分離を、現状の実装ファイルにマッピング。

```
core/
├── domain/                                  ← LibGDX 非依存・純 Java
│   ├── common/   Position, Direction
│   ├── entity/   ActorId, Stats, Player, Enemy, EnemyKind
│   ├── skill/    SkillId, Skill, SkillEffect (sealed), SkillSlot
│   ├── battle/   ActionPoints, TurnPhase, BattleAction (sealed),
│   │             BattleEvent (sealed), TurnEngine, EnemyAi
│   ├── dungeon/  Tile, DungeonMap, DungeonState
│   └── meta/     Soul
├── application/                             ← LibGDX 非依存・状態保持
│   ├── GameContext      (ラン 1 回ぶんの可変状態)
│   └── TurnDirector     (TurnEngine / EnemyAi のオーケストレータ)
├── infrastructure/                          ← I/O・起動
│   ├── desktop/         DesktopLauncher (main + Lwjgl3 設定)
│   └── bootstrap/       InitialStateFactory (定数値で MVP 初期状態を作る)
└── presentation/                            ← LibGDX 依存・描画/入力
    ├── screen/          DddGame, TitleScreen, DungeonScreen, GameOverScreen
    ├── render/          Fonts, Strings, RenderLayout, DungeonRenderer, HudRenderer
    └── input/           PlayerInputs
```

### 依存方向の規則

- `domain` は他の core 配下に **依存しない**
- `application` は `domain` のみ依存
- `infrastructure` は `domain` と `application` を使う
- `presentation` は `application` (→ `domain`) と `infrastructure` (起動だけ) を使う
- LibGDX への import は `presentation` / `infrastructure` 配下のみで OK

---

## 4. ターン進行の具体

### AP 制 (GAME_DESIGN §5-2)

- AP は **使い切り型** (Slay the Spire 風)。毎ターン開始時に **速度ステ分まで全リセット** し、前ターンの残 AP は **蓄積しない**
- プレイヤー AP = 速度ステ。敵 AP = **層番号** (1 層 = AP 1、2 層 = AP 2、…、強化個体・ボスは +α)

### コスト表 (MVP)

| 行動 | AP | 備考 |
|---|---|---|
| 1 マス移動 | 1 | `BattleAction.Move(Direction)` |
| 軽攻撃 | 1 | `Skill(light_slash, Damage 5)` |
| 強攻撃 | 3 | `Skill(heavy_slash, Damage 15)` |
| スライムの噛みつき | 1 | `Skill(slime_bite, Damage 4)` |
| 待機 | 1 | `BattleAction.Wait` |
| ターン終了 | 0 | `BattleAction.EndTurn` (残 AP は破棄して敵ターンへ) |

### フェーズ遷移

`TurnPhase` 列挙の遷移は TurnEngine が一手担う:

```
PLAYER_TURN ──(プレイヤーが EndTurn or AP 0)──→ ENEMY_TURN
ENEMY_TURN  ──(全敵 AP 切れ)──────────────→ PLAYER_TURN
PLAYER_TURN ──(階段に乗る Move)──────────→ CLEARED
任意フェーズ ──(プレイヤー HP 0)───────────→ GAME_OVER
```

### 失敗した行動の扱い

- AP 不足・壁衝突・対象不在 などは **状態を変えず** `BattleEvent.ActionRejected(reason)` だけを返す
- これで「再描画は走るがゲームは進まない」ようになり、入力ループのバグが状態を壊さない

---

## 5. メタ進行 (ソウル経済)

| 操作 | 実装 |
|---|---|
| ソウル獲得 | 敵撃破時に `EnemyKind.SLIME.soulReward() = 1` を `Player.addSoul` で加算 |
| 死亡時の保持 | 死亡しても `Player.soul` は不変 (TurnEngine がフェーズだけ GAME_OVER にする) |
| 階段踏破時 | ソウル・装備すべて持ち帰り (現状装備は未実装) |
| ソウル消費 | **MVP 後** — スキル習得・枠拡張・ステ強化の UI を編成画面で実装する |

---

## 6. ドメイン用語 ↔ 実装名 マッピング

| ゲーム用語 (GAME_DESIGN §13) | 実装の型 |
|---|---|
| AP (行動ポイント) | `core.domain.battle.ActionPoints` (record, immutable) |
| スキル枠 | `core.domain.skill.SkillSlot` (defensive copy 付き record) |
| スキル | `core.domain.skill.Skill` + `SkillEffect` (sealed) |
| ソウル | `core.domain.meta.Soul` (record + add/subtract) |
| 変動ターン制 | `TurnPhase` + `TurnEngine` の AP 回復ロジックで表現 |
| ダンジョン階層 | `DungeonMap` (現状 1 階層、`InitialStateFactory.firstFloor()`) |
| プレイヤー | `core.domain.entity.Player` |
| 敵 | `core.domain.entity.Enemy` + `EnemyKind` (種別 enum) |
| 戦闘イベント | `BattleEvent` (sealed: Moved / SkillUsed / DamageDealt / ActorDied / SoulGained / TurnPhaseChanged / ActionRejected) |

---

## 7. 設計原則の自己評価 (MVP 時点)

| 原則 | 守れている例 | リスク / 検証ポイント |
|---|---|---|
| **SOLID** | 単一責務 (TurnEngine = ルール / EnemyAi = 意思決定 / TurnDirector = 進行) | sealed が増えてきたら switch の分岐が肥大化する。Visitor 化が必要になったら検討 |
| **YAGNI** | `Actor` / `RunResult` / `Stats.power` は MVP で未使用と判明し削除済み | 「将来使うかも」で生やしたコードは早めに殺す |
| **KISS** | record + 静的ファクトリ中心。リポジトリ層は MVP では `InitialStateFactory` 定数で代用 | 動的化が始まるとリポジトリ抽象が必要に |
| **DRY** | `EnemyKind.soulReward()` をテストでも参照、文言は `Strings.Ja/En` に集約 | 同じ値を 2 か所書いていないか、PR 時に grep |
| **驚き最小** | パッケージ構成と命名が GAME_DESIGN §9 / CommonSense.md と完全一致 | 名前を変えるときは GAME_DESIGN とセットで PR |
| **不変性** | record + `List.copyOf` + Tile[][] 内包化 + record 内コンパクトコンストラクタで防御 | 可変共有を 1 箇所でも作ると崩れる |
| **副作用の分離** | `domain/` は LibGDX/Random/時計に依存しない (grep で確認済み)。状態保持は `GameContext` のみ | 新しい features を `domain` に書く前に「副作用が無いか」を確認 |
| **明示的な依存関係** | `DddGame` で `GameContext` / `TurnDirector` / `Fonts` を集約し、Screen は `game.xxx()` で都度取得 | static singleton は使わない |

---

## 8. 拡張ポイント (MVP 後の段階導入)

[GAME_DESIGN §11-3](./GAME_DESIGN.md) のスコープアウト項目を、実装難度の低い順に並べる。

### Phase A: コンテンツ拡張 (低リスク)

1. **敵種別の追加** — `EnemyKind` に enum 値を増やすだけ。InitialStateFactory で配置
2. **スキルの追加** — `SkillEffect` を sealed のまま `Heal` などを追加 → switch が網羅性チェック
3. **マップを 2 階層に** — `DungeonMap.of(rows)` で 2 個目の floor を作り、階段で進める仕組み
4. **HP 警告演出** — `presentation` で HP 比率が低いとき赤フィルタ ([GAME_DESIGN §7](./GAME_DESIGN.md))

### Phase B: 動的化 (中リスク)

5. **動的マップ生成** — `domain.dungeon.MapGenerator` (純関数 + 乱数 seed) を新設、`DungeonMap.of(generated)` で接続
6. **動的敵配置** — マップ生成と難易度パラメータから敵リストを生成
7. **アイテム生成** — 品質パラメータ込みの `Item` レコードを `Loot` パッケージで

### Phase C: メタ進行 UI (高インパクト)

8. **編成画面 (CompositionScreen)** — 死亡/生還後に表示。ソウル消費でスキル習得・枠拡張・ステ強化
9. **永続セーブ** — `infrastructure.save` で JSON (Jackson) 経由の保存・ロード
10. **装備システム** — `Equipment` レコード、耐久値、テーマ変動の演出

### Phase D: マルチプラットフォーム (技術的アピール)

11. **Android backend** — LibGDX Android extension + Gradle android プロジェクト追加
12. **タッチ入力** — `PlayerInputs` をジェスチャ対応 (`GestureDetector`)

---

## 9. テスト戦略

- **ドメインのみテスト 61 件** (`src/test/java/core/domain/`)
- 副作用が無い純関数なので JUnit 5 の通常ケースで網羅可能
- AP 制 / 死亡継承 / 階段踏破 / EnemyAi の代表シナリオを全部カバー
- アプリ・プレゼン・インフラ層のテストは MVP では書いていない (LibGDX のテスト容易性は低く、E2E で代用)

将来:
- `application.TurnDirector` のシナリオテスト (LibGDX 不要なので追加可能)
- `presentation.input.PlayerInputs` の mock 入力テスト

---

## 10. ライセンス・素材方針

- コード: 提出直前に決定 ([ContributingGuide §6](./ContributingGuide.md))
- 素材: [AssetGuidelines.md](./AssetGuidelines.md) のチェックリスト準拠
- 使用素材一覧: [LICENSES/INDEX.md](../LICENSES/INDEX.md)

---

## 11. デモ提示用のチェックリスト (M2 = ハッカソン本番)

- [x] 日本語フォント (`assets/fonts/DotGothic16-Regular.ttf`) を配置済み
- [ ] `gradlew run` で起動し、HUD が日本語表示
- [ ] `gradlew fatJar` で配布 JAR を作成
- [ ] mac / Linux 環境でも起動確認
- [ ] プレイ動画 3〜5 分の試走
- [ ] スクリーンショットを README に追加
- [ ] LICENSE ファイル決定 + リポジトリに配置
- [ ] 使用素材のクレジット表記 (CreditsScreen または README 末尾)
