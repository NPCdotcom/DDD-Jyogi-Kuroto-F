# ai_log/handoff.md

> **Claude セッションが圧縮 / 再開した時に最初に読む 1 枚スナップショット**。
> ここを読めば「今ここから何をすべきか」が即座に分かる構造にする。
> 状況が動くたびに 1 行ずつ更新する (古い行は decisions.md / lessons.md に流す)。

---

## 最終更新

- **日時**: 2026-05-14 (火) (E-1 設計案取得後)
- **更新者**: Claude (本セッション)

## アクティブブランチ

`feat/#12/E-1-card-skeleton` (Issue #12 E-1 カードシステムの実装ブランチ)

ローカルメインリポジトリ: `C:\.program\DDD-Jyogi-Kuroto-F`

## 進行中の作業 (圧縮後はここから再開)

**現在地**: E-1 (カードシステム) の domain-architect 設計案を取得済、**ユーザー承認待ち** の状態。

### 設計案サマリ (domain-architect 出力、要点)

#### `core.domain.card/` 新規パッケージ
- `CardId` (record + `of`)
- `CardTag` enum: ATTACK / MOVEMENT / BUFF / TRAP
- `CardElement` enum: PHYSICAL / MAGICAL (ハイブリッドは YAGNI)
- `Card` (record): id + 表示名 + apCost + tag + element + effect
- `CardEffect` sealed interface: Damage / Move / Buff / Trap
- `TrapLifetime` sealed: UntilStepped (物理) / Turns(int) (魔法)
- `Deck` (record, 静的マスター)
- `DrawPile` / `DiscardPile` / `Hand` (record, 戦闘中状態)
- `CardPileState` (record, 山札+手札+捨て札の束ねと純関数操作)

#### 設計判断のポイント
- Card は単一 record、多態性は CardEffect (sealed) に集約 — 8 サブカテゴリのクラスを作らない (KISS)
- 戦闘状態は CardPileState で局所閉包 (山札切れ→捨て札再シャッフルもここ)
- Random を引数注入、ドメイン副作用ゼロ
- TrapLifetime sealed で物理/魔法の差を型表現 (boolean フラグにしない)

#### 依存している未実装事項 (別 Issue 化提案、優先順)
- **A**: `ActionPoints` を §15-3 「使い切り型」に書き換え (現 蓄積型)
- **B**: `Stats` を 6 ステに拡張
- **C**: `Direction8` 新設 (罠の周囲 8 方向用、`Direction` は移動カード用に残す)
- **D**: `BattleAction.UseCard(int handIndex, Direction dir)` 追加 + TurnEngine 修正
- **E**: `core.domain.equipment.Equipment` 新設 (装備固有カードを `List<CardId>` で持たせる)

### 次のアクション (圧縮後の Claude も実行可能)

1. **ユーザー承認を取る**: 「設計案で進めて良いか」「依存 Issue A〜E をどう処理するか (同時並行 / 順次 / 統合)」
2. 承認後、ブランチ `feat/#12/E-1-card-skeleton` で実装着手
   - `core.domain.card/` の record / sealed を順次 Write
   - test-writer Agent でテスト書く
   - `/architect-review` で最終レビュー
   - `/japanese-pr-create draft` で PR
3. 依存 A〜E は本 Issue とは別 PR にする (本 PR は `card/` パッケージのみ)

### 起票済みの並行 Issue

- なし。E-3 (層) / E-4 (通貨) / E-6 (UI 基盤) はまだ起票していない (E-1 完了後 or 並行で起票)

## 直近のマージ済成果

| Commit | 内容 | 日付 |
|---|---|---|
| `c1fc9d6` | MVP コア実装一式 (#7) — mvp ブランチで完成 | 2026-05-12 |
| `b4593ab` | §15 MVP 後の機能仕様 追加 | 2026-05-13 |
| `d586129` | §15 連動 docs 整備 + PDCA レビュー反映 | 2026-05-13 |
| `c2fc491` | §15 バランス調整 + UX 改善 (#8) | 2026-05-13 |
| `4c97f0c` | **PR #9 merged**: §15 ブラッシュアップ → develop | 2026-05-14 |
| `4a9382f` | **mvp → develop マージ** (`-X ours` で衝突は develop 優先) | 2026-05-14 |
| `c5ea86d` | AI 駆動開発体制を整備 (#10) | 2026-05-14 |
| `5052dda` | コンテキスト圧縮対策の引継ぎ文書 | 2026-05-14 |
| `f62b482` | **PR #11 merged**: AI 体制整備 → develop | 2026-05-14 |

## Open Issues / PRs

| 番号 | 種類 | 状態 | 内容 |
|---|---|---|---|
| #1 | Issue | OPEN | リポジトリの土台整備 (古い、Close 候補) |
| #4 | Issue | OPEN | CI 軽量化とドキュメント微調整 (古い、Close 候補) |
| #8 | Issue | OPEN | §15 バランス調整 (PR #9 マージで実質完了、Close 推奨) |
| #12 | Issue | **OPEN** | **E-1 カードシステム実装 (§15-3)** ← 現在ここ、設計案取得済 |

Closed: #2, #3, #5, #6, #7 (MVP), #10 (PR #11 でクローズ済), #11 (merged)

## 据置き判断 (議論済、再議論不要)

詳細は [decisions.md](./decisions.md) を参照。サマリのみ:

1. **AP モデル**: §15 では使い切り型 (Slay the Spire 風)、MVP の蓄積型は廃止
2. **敵 AP**: 層番号 N に等しい (1 層 = AP 1、…)。バランス専門が C 評価したが、ユーザー判断で据置き
3. **Stats**: 6 種 (HP/速度/物攻/魔攻/物防/魔防)、MVP の 3 種から拡張
4. **ステ計算**: `max(1, カード基礎値 + 物攻 - 物防)` 加減算のみ
5. **Stop hook**: 全テスト走行 ON (ユーザー要望 b-3、「意味のあるテスト最初から書く」方針)
6. **フォント**: DotGothic16 採用、ユーザー提供 ZIP から `assets/fonts/` に配置済
7. **mvp 統合**: `-X ours` 戦略で develop 側 (§15 反映済 docs) を優先しつつ、MVP コード一式を取り込み
8. **E-7 Bestiary**: P2 (捨て候補) に降格、M1.5 では他 P0' / P1 を優先
9. **クリア条件**: 階段踏破のみ (敵全滅では遷移しない)
10. **Agent Teams**: 当面 OFF (実験的、トークン 3〜5 倍消費)

## 採用済ツール / バージョン

| ツール | バージョン |
|---|---|
| JDK | Oracle JDK 25.0.3 LTS |
| Gradle | 9.5.0 (wrapper 同梱) |
| LibGDX | 1.14.0 |
| LWJGL | 3.4.1 (LibGDX 1.14.0 の 3.3.3 を force で上書き) |
| JUnit Jupiter | 5.12.2 |
| Jackson Databind | 2.21.3 |
| Spotless | 8.4.0 |
| google-java-format | 1.35.0 |
| gdx-freetype | 1.14.0 |

JAVA_HOME: `C:\Program Files\Java\jdk-25.0.3` (Windows、永続 PATH 済)

## チーム体制 (リーダー判断)

- **リーダー (ユーザー)**: AI 駆動でコード実装
- **チームメイト**: 素材収集 (フォント / タイル / SE) + カード設計 (JSON や Excel ベース)
- **AI 駆動**: Subagent + Skills + Hooks (.claude/) で品質ゲート
- **Architect** = 本セッションの Claude (`final-architect` Agent で最終レビュー)

## AI 駆動運用の起動方法

### 通常の機能実装 (E-1〜E-10 着手時)
1. `/m1-5-start E-X 機能名` Skill 起動
2. `domain-architect` Agent で設計レビュー
3. 実装 → hook が自動で `spotlessApply` + `FooTest`
4. `test-writer` Agent でテスト補強
5. `libgdx-implementer` Agent で UI 層
6. `/architect-review` Skill で `final-architect` 最終レビュー
7. `/japanese-pr-create draft` で Draft PR
8. self-merge or チームレビュー → マージ

### 不可逆判断が必要なとき
- `decisions.md` に ADR を追記してから実行

### 失敗 / 判断ミスをしたとき
- `lessons.md` に CLAUDE.md ルール準拠でエントリ追加 (`/lessons-add` Skill)

## スケジュール

- **2026-05-12**: MVP 完成
- **2026-05-13**: §15 仕様策定 + バランス調整
- **2026-05-14 (今日)**: PR #9 merge、mvp → develop、AI 体制整備 PR #11 (進行中)
- **2026-05-15 (金)**: 本来の MVP 締切 (M1)、達成済
- **2026-05-15〜21**: M1.5 並列実装 (E-1〜E-10 のうち P0' を優先)
- **2026-05-22**: 統合テスト + デモ録画
- **2026-05-23-24 (土日)**: ハッカソン本番 (M2、提出 + デモ)

## 関連ドキュメントへの動線

- 全体仕様: [docs/GAME_DESIGN.md §15](../../docs/GAME_DESIGN.md)
- 実装俯瞰: [docs/SystemSummary.md](../../docs/SystemSummary.md)
- M1.5 並列プラン: [tasks/todo.md](../todo.md) の Phase 9
- 設計判断ログ: [decisions.md](./decisions.md)
- AI 失敗ログ: [lessons.md](./lessons.md)
- 過去レビュー: [phase_6_5_review.md](./phase_6_5_review.md)
- プロジェクト記憶: [../../CLAUDE.md](../../CLAUDE.md)
