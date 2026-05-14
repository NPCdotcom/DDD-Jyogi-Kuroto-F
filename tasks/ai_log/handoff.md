# ai_log/handoff.md

> **Claude セッションが圧縮 / 再開した時に最初に読む 1 枚スナップショット**。
> ここを読めば「今ここから何をすべきか」が即座に分かる構造にする。
> 状況が動くたびに 1 行ずつ更新する (古い行は decisions.md / lessons.md に流す)。

---

## 最終更新

- **日時**: 2026-05-14 (火) 夜 (E-1 + 依存 A / B / D マージ完了 = 「カードで敵を殴れる」がドメイン層で実現)
- **更新者**: Claude (本セッション)

## アクティブブランチ

`chore/post-D-handoff-update` (handoff.md 更新コミット用、軽量 PR で develop に出す予定)

ローカルメインリポジトリ: `C:\.program\DDD-Jyogi-Kuroto-F`

## 進行中の作業 (圧縮後はここから再開)

**現在地**: **M1.5 コア機能 (カード戦闘) のドメイン層がすべて develop に統合済**。本セッションで以下 4 PR をすべて self-merge:

- **PR #13 merged** (`48033de`): E-1 カードシステムのドメイン層 (`core.domain.card/`)
- **PR #16 merged** (`bbaa630`): 依存 B (Stats 6 ステ + `CardEffect.Damage.resolve`)
- **PR #17 merged** (`d77432e`): 依存 A (ActionPoints 使い切り化、`refilledTo(int)`)
- **PR #19 merged** (`42c10b0`): 依存 D (BattleAction.UseCard + TurnEngine カード解決統合)

**ドメイン層完了** = `BattleAction.UseCard(int handIndex, Direction direction)` を渡せば `CardEffect.Damage.resolve(...)` 経由で敵に最終ダメが入る。`gradlew test` 全 **155 件 PASS**。final-architect レビューですべて **A 判定**。

### 圧縮後の Claude が最初にやること

1. `gh pr list -R NPCdotcom/DDD-Jyogi-Kuroto-F --state open` で未マージ PR 確認 (本 handoff 更新 PR が残っている可能性)
2. 以下の **どれを進めるか** を判断:
   - (A) **最小 UI 結合** (`HudRenderer` 手札テキスト表示 + `PlayerInputs` キー 1-9 でカード使用) — `libgdx-implementer` Agent に依頼、推定 1〜2h
   - (B) **依存 E (Equipment) / E-5** で Deck 接続 — 装備固有カードを `Player.cardPileState` に持ち込む構造、別 Issue 起票
   - (C) **依存 C (Direction8)** — 罠 8 方向用、Damage カード単体では不要なので E-2 ソウルツリーや E-3 層構造が先で OK
   - (D) **E-2 (ソウルツリー) / E-3 (層構造) / E-4 (通貨 Gold) / E-6 (ポップアップ UI 基盤)** — handoff.md Phase 9 の P0' 並列着手
3. ユーザーが「今日中完成」と宣言したスコープ (d 案 = カードで敵を殴れる + 最小 UI) は **ドメイン層側はすべて完了**。残りは UI 結合のみ

### 本セッションの実装サマリ

| 機能 | コミット | 概要 |
|---|---|---|
| E-1 カードシステム | `48033de` | `core.domain.card/` 11 ファイル、Card / CardEffect (sealed: Damage/Move/Buff/Trap) / TrapLifetime / Deck / DrawPile / DiscardPile / Hand / CardPileState |
| 依存 B (Stats 6 ステ + Damage.resolve) | `bbaa630` | Stats を 3 → 7 引数 record 化、`CardEffect.Damage.resolve(Stats, Stats, CardElement) -> int` (式: `max(1, base + 攻 - 防)`) |
| 依存 A (AP 使い切り化) | `d77432e` | `ActionPoints.regenerate` 削除、`refilledTo(int)` 新設、`max >= 0` 緩和 |
| 依存 D (UseCard 統合) | `42c10b0` | `Player` を 6→7 引数化 (`CardPileState` 追加)、`BattleAction.UseCard` 追加、`TurnEngine.applyPlayerUseCard` / `resolveCardDamage` 実装、`resolveDamageToEnemy/Player` を int finalDamage 統一 |

テスト件数: 61 → 136 (PR #13) → 147 (#16) → 149 (#17) → **155 (#19)**

### 依存事項 A〜E の状況 (M1.5 P0')

| 依存 | 内容 | Issue / PR | 状態 |
|---|---|---|---|
| **A** | ActionPoints 使い切り型 | #14 / PR #17 | ✅ **MERGED** |
| **B** | Stats 6 ステ + Damage.resolve | #15 / PR #16 | ✅ **MERGED** |
| C | Direction8 (罠 8 方向用) | 未起票 | 後回し (Damage カードで不要) |
| **D** | BattleAction.UseCard 統合 | #18 / PR #19 | ✅ **MERGED** |
| E | Equipment 新設 (装備固有カード) | 未起票 | E-5 と統合起票推奨 |

依存関係: D だけが A+B+C 全部に依存していたが、C をスキップして D を実装 (Damage カードは 4 方向 Direction で十分、ADR-18)。**M1.5 最小ゲーム化 (A + D) 完了**。

## 直近のマージ済成果

| Commit | 内容 | 日付 |
|---|---|---|
| `c1fc9d6` | MVP コア実装一式 (#7) | 2026-05-12 |
| `4c97f0c` | **PR #9 merged**: §15 ブラッシュアップ | 2026-05-14 朝 |
| `4a9382f` | mvp → develop マージ (`-X ours`) | 2026-05-14 朝 |
| `f62b482` | **PR #11 merged**: AI 体制整備 | 2026-05-14 朝 |
| `48033de` | **PR #13 merged**: E-1 カードシステムドメイン層 (#12) | 2026-05-14 昼 |
| `bbaa630` | **PR #16 merged**: 依存 B (Stats 6 ステ + Damage.resolve) (#15) | 2026-05-14 午後 |
| `d77432e` | **PR #17 merged**: 依存 A (ActionPoints 使い切り化) (#14) | 2026-05-14 午後 |
| `42c10b0` | **PR #19 merged**: 依存 D (UseCard + TurnEngine 統合) (#18) | 2026-05-14 夜 |

## Open Issues / PRs (本 handoff 更新時点)

| 番号 | 種類 | 状態 | 内容 |
|---|---|---|---|
| #1 | Issue | OPEN | リポジトリの土台整備 (古い、Close 候補) |
| #4 | Issue | OPEN | CI 軽量化とドキュメント微調整 (古い、Close 候補) |
| #8 | Issue | OPEN | §15 バランス調整 (PR #9 マージで実質完了、Close 推奨) |
| #12 | Issue | OPEN | E-1 カードシステム実装 (依存 A/B/D マージ済、C/E のみ未着手で実質完了) |

依存 #14 (A) / #15 (B) / #18 (D) はすべて MERGED PR で実質完了済 (Issue 自体は手動 close 待ち)。

## 据置き判断 (議論済、再議論不要)

詳細は [decisions.md](./decisions.md) を参照。**ADR-17 / ADR-18 が本セッションで追加**:

1. **AP モデル**: §15 使い切り型 (Slay the Spire 風)、ADR-01 確定、PR #17 で実装
2. **敵 AP**: 層番号 N に等しい
3. **Stats**: 6 種 (HP/速度/物攻/魔攻/物防/魔防)、ADR-02 確定、PR #16 で実装
4. **ステ計算**: `max(1, カード基礎値 + 物攻 - 物防)`、**ADR-17** で `CardEffect.Damage.resolve` 配置確定、PR #16 で実装
5. **Stop hook**: 全テスト走行 ON
6. **フォント**: DotGothic16 採用
7. **mvp 統合**: `-X ours` 戦略
8. **E-7 Bestiary**: P2 (捨て候補) に降格
9. **クリア条件**: 階段踏破のみ
10. **Agent Teams**: 当面 OFF
11. **E-1 ドメイン設計**: ADR-16 確定、PR #13 で実装
12. **依存事項の起票戦略**: A+B 先行起票、C/D/E は段階起票
13. **ダメージ計算配置**: `CardEffect.Damage.resolve` (card 層内)、DamageFormula 廃案 — **ADR-17**
14. **Skill 経路不変保持**: `SkillEffect.Damage` は固定ダメ継続 — ADR-17
15. **CardPileState の所有**: **Player record に内蔵 (案 X)**、3 並列レビュー結論 — **ADR-18**
16. **Move/Buff/Trap カード**: 本 PR ではすべて reject、別 Issue で順次実装 — ADR-18
17. **BattleEvent**: `CardUsed` 新設せず `SkillUsed` 流用 — ADR-18
18. **ダメージヘルパ**: Skill/Card 両経路で `int finalDamage` 受け取り統一 (DRY) — ADR-18

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

1. `/m1-5-start E-X 機能名` Skill で Issue 起票 + ブランチ作成 (実行結果は `git status` / `gh issue view` で main session でも検証)
2. **`domain-architect` Agent で設計レビュー → ユーザー確認**。大きな設計判断は **3 並列レビュー** (domain-architect 自己再評価 / general-purpose docs 横断 / final-architect 8 原則) を推奨 — 本セッションで ADR-17 / ADR-18 を救済した運用パターン
3. ADR 記録 (`decisions.md` 追記) — 不可逆判断は実装前に記録
4. 実装 (Edit/Write) → hook で `spotlessApply` + 関連テスト自動実行
5. `test-writer` Agent でテスト補強 (境界値 + 例外パス + ハッピーパス)
6. (LibGDX 依存時のみ) `libgdx-implementer` Agent でプレゼン層
7. **`final-architect` Agent を Agent ツールから直接呼ぶ** — `/architect-review` Skill は使わない (worktree 問題、lessons.md 参照)
8. **`gh pr create` を main session から実行** — `/japanese-pr-create` Skill はテンプレ参照のみ、実行は必ず `-C C:/.program/DDD-Jyogi-Kuroto-F` 付き Bash で
9. self-merge (ADR-14、リーダー権限) または チームレビュー → マージ

### Skill 利用時の注意 (lessons.md エントリ参照)

- `context: fork` 付き Skill は worktree を生成し、後続 Skill の cwd 汚染が連鎖する
- **Skill 内の git 出力は信用しない**、必ず main session で `git -C <main-repo-path>` 明示パスで再確認
- `gh pr create` や `git commit` の不可逆操作は必ず `-C C:/.program/DDD-Jyogi-Kuroto-F` 付き

### 不可逆判断が必要なとき

- `decisions.md` に ADR を追記 (現在 ADR-18 まで蓄積済)

### 失敗 / 判断ミスをしたとき

- `lessons.md` にエントリ追加

## 残タスク (5/15 以降に進める想定)

### 今日中完成スコープ (d) の残り

- **最小 UI 結合** (P0'): `HudRenderer` で手札テキスト表示 (例: `[1] 斬撃 (AP1) [2] 魔法弾 (AP2)` の縦並び)、`PlayerInputs` で キー 1〜9 + 方向キーで `BattleAction.UseCard(handIndex, direction)` 発行
  - 担当: `libgdx-implementer` Agent
  - 推定: 1〜2h
  - 完了後、`gradlew run` で実機動作確認、5 分のデモが回せる

### M1.5 残機能 (5/15 - 5/21 想定)

- **E-3 層構造 + ノード分岐** (P0'): `core.domain.layer/`
- **E-4 通貨 Gold** (P0'): `core.domain.meta/Gold.java`
- **E-6 ポップアップ UI 基盤** (P0'): `core.presentation.window/`
- **E-2 ソウルツリー** (P0'): `core.domain.tree/` + `SoulTreeScreen`
- **E-5 装備システム + 依存 E** (P1): `core.domain.equipment/`、Deck 接続
- **依存 C (Direction8)** (P1): 罠カード 8 方向用、E-2 と並列
- E-8 / E-9 / E-10 (P1)

### スケジュール

- **2026-05-14 (今日)** ✅: E-1 + 依存 A / B / D 完成、ドメイン層で「カードで敵を殴れる」実現
- **2026-05-15 (金)**: 最小 UI 結合 → `gradlew run` でカード使用デモ可、E-3 / E-4 / E-6 並列着手
- **2026-05-16〜18**: E-2 ソウルツリー、E-5 装備、依存 C、依存 E
- **2026-05-19〜21**: E-8 演出、E-10 チュートリアル
- **2026-05-22**: 統合テスト + デモ録画
- **2026-05-23〜24**: ハッカソン本番

## 関連ドキュメントへの動線

- 全体仕様: [docs/GAME_DESIGN.md §15](../../docs/GAME_DESIGN.md)
- 実装俯瞰: [docs/SystemSummary.md](../../docs/SystemSummary.md)
- M1.5 並列プラン: [tasks/todo.md](../todo.md) の Phase 9
- 設計判断ログ: [decisions.md](./decisions.md) (ADR-18 まで蓄積)
- AI 失敗ログ: [lessons.md](./lessons.md) (Skill worktree 問題ほか)
- 過去レビュー: [phase_6_5_review.md](./phase_6_5_review.md)
- プロジェクト記憶: [../../CLAUDE.md](../../CLAUDE.md)
