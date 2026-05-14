# ai_log/handoff.md

> **Claude セッションが圧縮 / 再開した時に最初に読む 1 枚スナップショット**。
> ここを読めば「今ここから何をすべきか」が即座に分かる構造にする。
> 状況が動くたびに 1 行ずつ更新する (古い行は decisions.md / lessons.md に流す)。

---

## 最終更新

- **日時**: 2026-05-14 (火) (E-1 PR #13 merged + 依存 B 実装完了 + PR #16 Draft 作成)
- **更新者**: Claude (本セッション)

## アクティブブランチ

`feat/#15/B-stats-6stat` (Issue #15 依存事項 B 実装ブランチ、PR #16 で develop に Draft 出している)

ローカルメインリポジトリ: `C:\.program\DDD-Jyogi-Kuroto-F`

## 進行中の作業 (圧縮後はここから再開)

**現在地**: 依存事項 **B (Stats 6 ステ化)** のドメイン層実装が完了、PR #16 を develop ベースで Draft 出している。final-architect レビュー判定 **A** (修正必須 0 件)。

3 並列サブエージェントレビューを経て、初版設計案 (DamageFormula 新クラス) を修正版 (`CardEffect.Damage.resolve` メソッドを card 層内に追加) に変更した。設計判断は **ADR-17** で確定。

### 圧縮後の Claude が最初にやること

1. `gh pr view 16 -R NPCdotcom/DDD-Jyogi-Kuroto-F` で PR #16 の状況確認 (self-merge 可否)
2. `gh issue view 14 -R NPCdotcom/DDD-Jyogi-Kuroto-F` で依存 A の内容確認
3. 以下のいずれかに着手:
   - **PR #16 を self-merge** (`gh pr ready 16 && gh pr merge 16 --merge --delete-branch`) して develop に統合
   - **A (Issue #14, ActionPoints 使い切り化)** を別ブランチで着手 (B と独立、PR #16 マージ後でも前でも OK)
   - C (Direction8) / D (BattleAction.UseCard) / E (Equipment) の Issue 起票判断
4. 依存 D は A+B+C 全部に依存するため、最後に着手する想定 (A/B/C/E は並列可)

### Issue #15 (B) 実装サマリ (commit 41b06f4)

- `core.domain.entity.Stats` を 3 引数 → **7 引数 record** に拡張 (新 4 フィールド非負検証)
- `core.domain.card.CardEffect.Damage` に **`int resolve(Stats, Stats, CardElement)`** メソッド追加
  - 式: `max(1, baseValue + 物攻/魔攻 - 物防/魔防)` (最低 1 ダメ保証)
- `DomainFixtures` / `InitialStateFactory` / `TurnEngineTest` の `new Stats(...)` を 7 引数化、新 4 フィールドは **暫定 0 埋め** (ADR-17、キャラビルドは別 Issue)
- `SkillEffect.Damage` は **意図的に触らない** (固定ダメ継続、対称性確保)
- 新規テスト 11 件 (Stats +3 件 / CardEffect.Damage.resolve +8 件)、`gradlew test` で **全 147 件 PASS**
- 設計判断: **ADR-17** (3 サブエージェント並列レビュー統合結論)

### 依存事項 A〜E の状況

| 依存 | 内容 | Issue / PR | 工数 | 状態 |
|---|---|---|---|---|
| **A** | `ActionPoints` 蓄積→使い切り型 (§15-3) | #14 OPEN | M | 起票済、**未着手** |
| **B** | `Stats` 6 ステ化 (§15-4) + Damage.resolve | #15 / **PR #16 Draft** | M | **実装完了、PR レビュー中** |
| C | `Direction8` 新設 (罠 8 方向用) | 未起票 | S | PR #16 マージ後検討 |
| D | `BattleAction.UseCard` 追加 + TurnEngine 修正 | 未起票 | L | A+B+C 完了後に起票 (依存集中) |
| E | `Equipment` 新設 (装備固有カード) | 未起票 | S | E-5 (装備システム) と統合起票推奨 |

依存関係: **D だけが A+B+C 全部に依存**。A/B/C/E は相互独立で並列着手可。

## 直近のマージ済成果

| Commit | 内容 | 日付 |
|---|---|---|
| `c1fc9d6` | MVP コア実装一式 (#7) — mvp ブランチで完成 | 2026-05-12 |
| `4c97f0c` | **PR #9 merged**: §15 ブラッシュアップ → develop | 2026-05-14 |
| `4a9382f` | **mvp → develop マージ** (`-X ours`) | 2026-05-14 |
| `f62b482` | **PR #11 merged**: AI 体制整備 → develop | 2026-05-14 |
| `48033de` | **PR #13 merged**: E-1 カードシステムのドメイン層 (#12) → develop | 2026-05-14 |
| `41b06f4` | **feat: Stats 6 ステ化 + CardEffect.Damage.resolve (#15)** ← 本セッション、PR #16 で出している | 2026-05-14 |

## Open Issues / PRs

| 番号 | 種類 | 状態 | 内容 |
|---|---|---|---|
| #1 | Issue | OPEN | リポジトリの土台整備 (古い、Close 候補) |
| #4 | Issue | OPEN | CI 軽量化とドキュメント微調整 (古い、Close 候補) |
| #8 | Issue | OPEN | §15 バランス調整 (PR #9 マージで実質完了、Close 推奨) |
| #12 | Issue | OPEN | E-1 カードシステム実装 (PR #13 でドメイン層完了、依存 A〜E 残り) |
| #14 | Issue | OPEN | **依存 A**: ActionPoints 使い切り化 (P0' breaking-change、未着手) |
| #15 | Issue | OPEN | **依存 B**: Stats 6 ステ化 (PR #16 で対応中) |
| **#16** | **PR** | **Draft** | **feat: Stats 6 ステ化 + CardEffect.Damage.resolve (#15)** ← 本セッション、レビュー / マージ待ち |

Closed: #2, #3, #5, #6, #7 (MVP), #10 (PR #11 でクローズ済), #11 (merged), **#13 (merged 本日)**

## 据置き判断 (議論済、再議論不要)

詳細は [decisions.md](./decisions.md) を参照。サマリのみ:

1. **AP モデル**: §15 では使い切り型 (Slay the Spire 風)、MVP の蓄積型は廃止 (Issue #14 で実装予定)
2. **敵 AP**: 層番号 N に等しい (バランス専門が C 評価したが、ユーザー判断で据置き)
3. **Stats**: 6 種 (HP/速度/物攻/魔攻/物防/魔防)、MVP の 2 種から拡張 (**PR #16 で実装完了**)
4. **ステ計算**: `max(1, カード基礎値 + 物攻 - 物防)` 加減算のみ (**PR #16 で `CardEffect.Damage.resolve` として実装**)
5. **Stop hook**: 全テスト走行 ON
6. **フォント**: DotGothic16 採用
7. **mvp 統合**: `-X ours` 戦略で develop 側を優先
8. **E-7 Bestiary**: P2 (捨て候補) に降格
9. **クリア条件**: 階段踏破のみ
10. **Agent Teams**: 当面 OFF
11. **E-1 ドメイン設計**: ADR-16 で確定、PR #13 でマージ
12. **依存事項の起票戦略**: A+B のみ先行起票 (戦略 Z)、C/D/E は段階起票
13. **ダメージ計算の置き場所**: `CardEffect.Damage.resolve` (card 層内)、DamageFormula 新クラスは廃案 (**ADR-17**)
14. **Skill 経路の不変保持**: `SkillEffect.Damage` は固定ダメ継続、Card と Skill の意味論を分離 (ADR-17)

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
2. **`domain-architect` Agent で設計レビュー → ユーザー確認**。大きな設計判断は **3 並列レビュー** (domain-architect 自己再評価 / general-purpose docs 横断 / final-architect 8 原則) を推奨 — 本セッションで ADR-17 を救済した運用パターン
3. ADR 記録 (`decisions.md` 追記) — 不可逆判断は実装前に記録
4. 実装 (Edit/Write) → hook で `spotlessApply` + 関連テスト自動実行
5. `test-writer` Agent でテスト補強 (境界値 + 例外パス + ハッピーパス)
6. (LibGDX 依存時のみ) `libgdx-implementer` Agent でプレゼン層
7. **`final-architect` Agent を Agent ツールから直接呼ぶ** — `/architect-review` Skill は使わない (worktree 問題で実行ブランチが汚染される、lessons.md 参照)
8. **`gh pr create` を main session から実行** — `/japanese-pr-create` Skill はテンプレ参照のみ、実行は必ず `-C C:/.program/DDD-Jyogi-Kuroto-F` 付き Bash で
9. self-merge (ADR-14、リーダー権限) または チームレビュー → マージ

### Skill 利用時の注意 (lessons.md エントリ参照)

- `context: fork` 付き Skill (現 `architect-review` のみ) は worktree を生成し、後続 Skill の cwd 汚染が連鎖する
- **Skill 内の git 出力は信用しない**、必ず main session で `git -C <main-repo-path>` 明示パスで再確認
- `gh pr create` や `git commit` の不可逆操作は必ず `-C C:/.program/DDD-Jyogi-Kuroto-F` 付き

### 不可逆判断が必要なとき

- `decisions.md` に ADR を追記 (現在 ADR-17 まで蓄積済)

### 失敗 / 判断ミスをしたとき

- `lessons.md` にエントリ追加

## スケジュール

- **2026-05-12**: MVP 完成
- **2026-05-13**: §15 仕様策定 + バランス調整
- **2026-05-14 (今日)**: PR #9 / #11 / #13 merged、依存 B 実装完了 + PR #16 Draft
- **2026-05-15 (金)**: 本来の MVP 締切 (M1)、達成済 / PR #16 マージ + 依存 A 着手予定
- **2026-05-15〜21**: M1.5 並列実装 (依存 A 着手 → C/D/E 起票 → E-2/E-3/E-4/E-6 並列)
- **2026-05-22**: 統合テスト + デモ録画
- **2026-05-23-24 (土日)**: ハッカソン本番 (M2)

## 関連ドキュメントへの動線

- 全体仕様: [docs/GAME_DESIGN.md §15](../../docs/GAME_DESIGN.md)
- 実装俯瞰: [docs/SystemSummary.md](../../docs/SystemSummary.md)
- M1.5 並列プラン: [tasks/todo.md](../todo.md) の Phase 9
- 設計判断ログ: [decisions.md](./decisions.md) (最新: ADR-17 ダメージ計算配置)
- AI 失敗ログ: [lessons.md](./lessons.md) (最新: Skill worktree 問題)
- 過去レビュー: [phase_6_5_review.md](./phase_6_5_review.md)
- プロジェクト記憶: [../../CLAUDE.md](../../CLAUDE.md)
