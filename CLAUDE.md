# CLAUDE.md (プロジェクト固有)

> 本ファイルは Claude Code がこのプロジェクトを開いた時に必ず読まれる "プロジェクト記憶"。
> グローバル CLAUDE.md (`~/.claude/CLAUDE.md`) のルールに加えて、本プロジェクト固有のコンテキストを定義する。
> 矛盾するルールは本ファイルが優先する (Local overrides Global)。

---

## プロジェクト概要

**DDD-Jyogi-Kuroto-F** は Java 25 + LibGDX で書く **2D ピクセルローグライト**。Slay the Spire 風 AP デッキ構築 + ToME 系シームレス戦闘 + Path of Exile 風ソウルツリーを組み合わせる。
仕様の単一ソース: [docs/GAME_DESIGN.md](docs/GAME_DESIGN.md) (特に §11 MVP / §15 MVP 後)。

- **MVP 状態**: 完成 (`mvp` ブランチで動作確認済、`gradlew run` で起動)
- **次の目標 M1.5**: §15 の E-1〜E-10 を 10 日で並列実装 → ハッカソン本番 (5/23-24)
- **役割分担**: コードはリーダー (Claude 駆動)、チームメイトは素材 + カード設計
- **AI 体制**: `.claude/agents/` の 3 種 (domain-architect / libgdx-implementer / test-writer) + ユーザー固有の `final-architect` / `bug-hunter`

---

## グローバル CLAUDE.md からの上書き

ユーザーグローバル `~/.claude/CLAUDE.md` の以下の項目を本プロジェクトでは置き換える:

| グローバル §5 | 本プロジェクト |
|---|---|
| `npm install` | (依存は `gradlew` が自動取得) |
| `npm run build` | `./gradlew build` |
| `npm run test` | `./gradlew test` |
| `npm run lint` | `./gradlew spotlessCheck` (適用は `spotlessApply`) |

起動コマンドはシェルによって違う:

- **Windows (PowerShell)**: `.\gradlew.bat run` (PowerShell はカレントディレクトリの実行ファイルに `.\` 必須)
- **Windows (cmd.exe)**: `gradlew.bat run`
- **macOS/Linux**: `./gradlew run`

以下この文書および他 docs の `./gradlew <task>` 表記は PowerShell では `.\gradlew <task>` (または `.\gradlew.bat <task>`) に読み替えること。JAVA_HOME / PATH の自動設定は `/gradle-runner` Skill が担当。

---

## 設計原則 8 項目 (PR レビュー時の必須チェックリスト)

| 原則 | 本プロジェクトでの解釈 |
|---|---|
| **SOLID** | ドメイン層は単一責務 / 拡張に開き修正に閉じる / LSP / 小さなインターフェース / 依存は抽象 |
| **YAGNI** | §15 に無い機能は実装しない。未使用クラスは削除 |
| **KISS** | シングルモジュール、過度な抽象を避ける、`new` で十分なら DI コンテナを入れない |
| **DRY** | ステ計算・AP 計算は値オブジェクト / static メソッドに集約 |
| **驚き最小** | [docs/GAME_DESIGN.md §9](docs/GAME_DESIGN.md) のアーキテクチャと [docs/CommonSense.md](docs/CommonSense.md) の命名規約に従う |
| **不変性** | `record` + `List.copyOf` + compact constructor で外部からの変更を防ぐ |
| **副作用の分離** | `core.domain.*` は `java.*` / `java.util.*` / Jackson のみ依存。LibGDX / Random / 時刻 / I/O は `core.infrastructure.*` と `core.presentation.*` に閉じる |
| **明示的な依存関係** | コンストラクタ注入。静的 state / シングルトンを作らない |

各 PR では「どの原則の選択をしたか」を 1 行で記録する習慣を付ける。

---

## アーキテクチャ (レイヤー分離)

```
core/
├── domain/         # LibGDX 非依存、純 Java、record + sealed + 純関数
│   ├── common/     # Position, Direction
│   ├── entity/     # Stats, Player, Enemy, EnemyKind
│   ├── skill/      # SkillId, Skill, SkillEffect (sealed), SkillSlot
│   ├── battle/     # ActionPoints, TurnPhase, BattleAction (sealed), TurnEngine, EnemyAi
│   ├── dungeon/    # Tile, DungeonMap, DungeonState
│   ├── meta/       # Soul, Gold (§15-2)
│   ├── card/       # Card, Deck, Hand (§15-3, 未実装)
│   ├── tree/       # SoulTree, TreeNode (§15-7, 未実装)
│   ├── equipment/  # Equipment (§15-9, 未実装)
│   └── layer/      # Layer, Node (§15-6, 未実装)
├── application/    # 状態保持と orchestration (LibGDX 非依存)
│   ├── GameContext     # ラン 1 回ぶんの可変状態
│   └── TurnDirector    # TurnEngine / EnemyAi のオーケストレータ
├── infrastructure/ # I/O・起動 (LibGDX 依存可)
│   ├── desktop/    # DesktopLauncher (Lwjgl3 設定)
│   ├── bootstrap/  # InitialStateFactory
│   └── save/       # 層単位セーブ (§15-11, 未実装)
└── presentation/   # 描画・入力 (LibGDX 必須)
    ├── screen/     # DddGame, TitleScreen, DungeonScreen, GameOverScreen
    ├── render/     # Fonts, Strings, RenderLayout, DungeonRenderer, HudRenderer
    ├── input/      # PlayerInputs
    ├── window/     # ポップアップ式 UI (§15-1, §15-8, 未実装)
    └── effect/     # HP 警告演出、装備テーマ変動 (§7-2, §15-9, 未実装)
```

**依存方向ルール**:
- `domain` は他層に依存しない (import 文を grep でチェック)
- `application` は `domain` のみ依存
- `infrastructure` は `domain` + `application` を使う、LibGDX 依存可
- `presentation` は `application` (→ `domain`) と `infrastructure` を使う、LibGDX 必須

---

## §15 核仕様 (実装時の即時参照)

詳細は [docs/GAME_DESIGN.md §15](docs/GAME_DESIGN.md) を読むが、忘れがちな核ルールを抜粋:

| トピック | ルール |
|---|---|
| AP 回復 | **使い切り型** (Slay the Spire 風)。毎ターン頭で速度ステ分まで全リセット (蓄積不可、MVP の蓄積型からの breaking change) |
| ダメージ計算 | `max(1, カード基礎値 + 物攻 - 物防)` (魔法は魔攻/魔防)、加減算のみ、最低 1 ダメ保証 |
| 山札切れ | 捨て札をシャッフルして山札に戻す |
| 敵 AP | 層番号 N と等しい (1 層 = AP 1、2 層 = AP 2、…)、強化個体・ボスは +α |
| 敵情報 | Bestiary 登録済みなら次行動を点線予告、未登録は予告なし |
| ステ | 6 種 (HP / 速度 / 物攻 / 魔攻 / 物防 / 魔防)。速度 = 1 ターン AP 量 |
| 初期デッキ | 装備固有カードのみ (ぼろ靴 = 移動、ぼろい短剣 = 斬撃 を中央スタートで無料解放) |
| カード追加 UI | **強化個体撃破時のみ表示** (雑魚撃破は SE + シェイク + ダメージポップで触感補完) |
| 層末ノード | 4 種から 3 提示 (ステ強化 / 休憩 / イベント / ショップ)、カード削除はショップ統合 |
| クリア | 階段踏破のみ (敵全滅では CLEARED にしない) |
| 解像度 | 1920×1080 ベース、初回起動で UI プリセット 3 種 (ミニマル / 標準 / 情報マシマシ) |
| 装備 | 1 部位スタート、耐久なし、特殊能力なし、装備固有カードがデッキに自動追加 |
| セーブ | 層単位 (層と層の間のみ、戦闘途中はセーブ不可)、スロット 1 つ |

---

## AI 駆動レビュー必須ルール

**`final-architect` を通さずに PR を出してはいけない**。

- 実装が終わったら `/architect-review` Skill を実行
- 総合判定 A or B でないと PR を出さない (C 以下は修正してから)
- バランス・整合性が気になったら `game-design-reviewer` (将来追加予定) を呼ぶ
- バグ調査は `bug-hunter` (ユーザー固有) を使う

参照: [docs/ContributingGuide.md §3](docs/ContributingGuide.md) (人間レビュー 1 名以上必須は別途維持)

---

## 命名規約 ([docs/CommonSense.md](docs/CommonSense.md) を抜粋)

- パッケージ: 小文字 (`core.domain.battle`)
- クラス: PascalCase (`TurnEngine`)
- テストクラス: 末尾 `Test` (`TurnEngineTest`)
- メソッド・変数: lowerCamelCase
- 定数: SCREAMING_SNAKE_CASE (`MAX_HAND_SIZE`)
- 値オブジェクト ID: `record FooId(String value)` 形式
- 静的ファクトリ: `Foo.of(...)`, `Foo.empty(...)`, `Foo.full(...)`
- メタドキュメント (README.md, GAME_DESIGN.md, INDEX.md, LICENSE) のみ SCREAMING_SNAKE_CASE 許可

---

## LibGDX のお作法 (`presentation/` `infrastructure/` で書く時)

1. **`dispose()` を必ず実装**: `BitmapFont`, `Texture`, `SpriteBatch`, `ShapeRenderer`, `Stage`, `FreeTypeFontGenerator` を持つクラス
2. **Screen ライフサイクル準拠**: `show()` でリソース確保、`render(delta)` 内で `new Texture(...)` しない
3. **入力**: `Gdx.input.isKeyJustPressed` (押した瞬間) と `isKeyPressed` (継続押し) を使い分け
4. **`Stage` のサイズと `Viewport`**: ポップアップ UI は `ScreenViewport` か `FitViewport` で 1920×1080 ベース、`resize` で `viewport.update()`
5. **DotGothic16 (ピクセルフォント)**: `Nearest` フィルタ、サイズは 16 の倍数 (16 / 32 / 48) で生成 (pixel-perfect)
6. **FreeType**: `incremental: true` で必要文字のみビットマップ化

---

## Hooks の挙動 (自動実行されるもの)

`.claude/settings.json` の hooks 設定により、以下が自動で走る:

| 種類 | 発火 | 何をするか |
|---|---|---|
| `PreToolUse(Bash)` | Bash 実行前 | `rm -rf` / `git push --force` 等の破壊的コマンドを拒否 |
| `PostToolUse(Edit\|Write Java)` | Java ファイル編集後 | `spotlessApply` で自動フォーマット |
| `PostToolUse(Edit\|Write Java)` | Java ファイル編集後 | 該当 `FooTest` を即実行、失敗時は追加コンテキストで Claude に通知 |
| `Stop` | 応答終了時 | 全テスト走行 (品質ゲート)、失敗時は次ターンで修正対応 |

---

## サブエージェント / Skills クイックリファレンス

### プロジェクト固有 Agents (`.claude/agents/`)
- `domain-architect`: ドメイン層設計、Opus
- `libgdx-implementer`: LibGDX 実装、Sonnet
- `test-writer`: JUnit 5 テスト生成、Sonnet

### プロジェクト固有 Skills (`.claude/skills/`)
- `/add-card`: カード新規追加 (JSON + record + テスト + §15-3 更新)
- `/m1-5-start`: E-X 着手 (Issue 起票 → ブランチ → domain-architect 設計レビュー)
- `/gradle-runner`: gradlew コマンドを JAVA_HOME 自動設定で実行

### ユーザー固有 Agents (`~/.claude/agents/`)
- `final-architect`: 最終 Architect レビュー、PR 前必須
- `bug-hunter`: バグ調査 (競合仮説でデバッグ)

### ユーザー固有 Skills (`~/.claude/skills/`)
- `/architect-review`: final-architect を 1 コマンドで起動
- `/japanese-pr-create`: PR を日本語規約準拠で作成
- `/lessons-add`: tasks/ai_log/lessons.md にエントリ追加

---

## タスク管理

- **これからやること**: [tasks/todo.md](tasks/todo.md) (Phase 6 以降)
- **AI 運用ログ**: [tasks/ai_log/](tasks/ai_log/) (リーダー個人ログ、チーム共有資産ではない)
  - [tasks/ai_log/lessons.md](tasks/ai_log/lessons.md): 失敗パターンと一般化したルール
  - [tasks/ai_log/phase_6_5_review.md](tasks/ai_log/phase_6_5_review.md): サブエージェントレビュー記録

---

## 関連ドキュメント

- [docs/GAME_DESIGN.md](docs/GAME_DESIGN.md) — **Single Source of Truth** (§11 MVP / §15 MVP 後)
- [docs/SystemSummary.md](docs/SystemSummary.md) — 実装俯瞰 + 拡張ロードマップ (Phase A〜D)
- [docs/AssetGuidelines.md](docs/AssetGuidelines.md) — 素材取り込み + ライセンス管理
- [docs/Schedule.md](docs/Schedule.md) — M1 / M1.5 / M2 マイルストン
- [docs/RolesDivision.md](docs/RolesDivision.md) — 機能カテゴリ A〜E (E-1〜E-10 が M1.5 対象)
- [docs/ContributingGuide.md](docs/ContributingGuide.md) — Issue / PR / ラベル運用
- [docs/BranchingStrategy.md](docs/BranchingStrategy.md) — ブランチ命名と保護
- [docs/CommonSense.md](docs/CommonSense.md) — 命名規約 + 用語集
