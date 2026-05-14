# ai_log/handoff.md

> **Claude セッションが圧縮 / 再開した時に最初に読む 1 枚スナップショット**。
> ここを読めば「今ここから何をすべきか」が即座に分かる構造にする。
> 状況が動くたびに 1 行ずつ更新する (古い行は decisions.md / lessons.md に流す)。

---

## 最終更新

- **日時**: 2026-05-14 (火) (E-1 ドメイン層実装完了 + PR #13 Draft 作成 + 依存事項 A/B Issue 起票完了)
- **更新者**: Claude (本セッション)

## アクティブブランチ

`feat/#12/E-1-card-skeleton` (Issue #12 E-1 カードシステム実装、PR #13 で develop に Draft 出している)

ローカルメインリポジトリ: `C:\.program\DDD-Jyogi-Kuroto-F`

## 進行中の作業 (圧縮後はここから再開)

**現在地**: E-1 カードシステムのドメイン層実装が完了、PR #13 (Draft) を develop ベースで出している。final-architect レビュー判定 **A** (修正必須 0 件)。依存事項のうち **A (ActionPoints 使い切り化、Issue #14)** と **B (Stats 6 ステ化、Issue #15)** を起票済。C / D / E は PR #13 マージ後に段階起票する想定。

### 圧縮後の Claude が最初にやること

1. `gh pr view 13 -R NPCdotcom/DDD-Jyogi-Kuroto-F` で PR #13 の状況確認 (チームレビュー待ち / self-merge 可否)
2. `gh issue view 14 -R NPCdotcom/DDD-Jyogi-Kuroto-F` と `gh issue view 15` で A / B Issue の内容確認
3. 以下のいずれかに着手:
   - **PR #13 を self-merge** (`gh pr ready 13 && gh pr merge 13 --merge`) して develop に統合
   - **A (Issue #14, ActionPoints 使い切り化)** または **B (Issue #15, Stats 6 ステ化)** を別ブランチで着手 (互いに独立で並列 OK、ただし PR #13 と同じ `core.domain.battle` / `core.domain.entity` に触るのでマージ衝突最小化のため PR #13 マージ後の着手を推奨)
4. C / D / E は今日は起票していない (PR #13 マージ後に判断、E は todo.md Phase 9 の **E-5 (装備システム)** と内容重複するので E-5 Issue として起票推奨)

### E-1 実装サマリ (commit 8cdfff5)

- `core.domain.card/` 11 ファイル新設 (Card / CardId / CardTag / CardElement / CardEffect (sealed) / TrapLifetime (sealed) / Deck / DrawPile / DiscardPile / Hand / CardPileState)
- テスト 9 ファイル、新規 75 件 (`gradlew test` で全 136 件 PASS)
- DomainFixtures に card 用 fixture 7 メソッド追加
- 設計は **ADR-16** で確定、final-architect レビュー判定 **A**

### 依存事項 A〜E の状況

| 依存 | 内容 | Issue | 工数 | 破壊テスト | 状態 |
|---|---|---|---|---|---|
| **A** | `ActionPoints` 蓄積→使い切り型 | **#14 OPEN** | M | 約 10 件 | 起票済、未着手 |
| **B** | `Stats` 6 ステ化 (物攻/魔攻/物防/魔防) | **#15 OPEN** | M | 約 10〜15 件 | 起票済、未着手 |
| C | `Direction8` 新設 (罠 8 方向用) | 未起票 | S | 0 件 | PR #13 マージ後検討 |
| D | `BattleAction.UseCard` 追加 + TurnEngine 修正 | 未起票 | L | 約 5〜8 件 | A+B+C 完了後に起票 (依存集中) |
| E | `Equipment` 新設 (装備固有カード) | 未起票 | S | 0 件 | todo.md Phase 9 の E-5 (装備システム) と統合起票推奨 |

依存関係: D だけが A+B+C 全部に依存。A/B/C/E は相互独立で並列着手可。

## 直近のマージ済成果

| Commit | 内容 | 日付 |
|---|---|---|
| `c1fc9d6` | MVP コア実装一式 (#7) — mvp ブランチで完成 | 2026-05-12 |
| `4c97f0c` | **PR #9 merged**: §15 ブラッシュアップ → develop | 2026-05-14 |
| `4a9382f` | **mvp → develop マージ** (`-X ours`) | 2026-05-14 |
| `f62b482` | **PR #11 merged**: AI 体制整備 → develop | 2026-05-14 |
| `620c21d` | docs: E-1 設計を ADR-16 として確定 | 2026-05-14 |
| `8cdfff5` | **feat: E-1 カードシステムのドメイン層を実装 (#12)** ← 本セッション、PR #13 で出している | 2026-05-14 |

## Open Issues / PRs

| 番号 | 種類 | 状態 | 内容 |
|---|---|---|---|
| #1 | Issue | OPEN | リポジトリの土台整備 (古い、Close 候補) |
| #4 | Issue | OPEN | CI 軽量化とドキュメント微調整 (古い、Close 候補) |
| #8 | Issue | OPEN | §15 バランス調整 (PR #9 マージで実質完了、Close 推奨) |
| #12 | Issue | OPEN | **E-1 カードシステム実装** (PR #13 で対応中、ドメイン層は完成) |
| **#13** | **PR** | **Draft** | **feat: E-1 カードシステムのドメイン層を実装 (#12)** ← レビュー / マージ待ち |
| **#14** | **Issue** | **OPEN** | **依存 A**: ActionPoints 使い切り化 (§15-3、P0' breaking-change) |
| **#15** | **Issue** | **OPEN** | **依存 B**: Stats 6 ステ化 (§15-4、P0' breaking-change) |

Closed: #2, #3, #5, #6, #7 (MVP), #10 (PR #11 でクローズ済), #11 (merged)

## 据置き判断 (議論済、再議論不要)

詳細は [decisions.md](./decisions.md) を参照。サマリのみ:

1. **AP モデル**: §15 では使い切り型 (Slay the Spire 風)、MVP の蓄積型は廃止 (Issue #14 で実装)
2. **敵 AP**: 層番号 N に等しい (バランス専門が C 評価したが、ユーザー判断で据置き)
3. **Stats**: 6 種 (HP/速度/物攻/魔攻/物防/魔防)、MVP の 3 種から拡張 (Issue #15 で実装)
4. **ステ計算**: `max(1, カード基礎値 + 物攻 - 物防)` 加減算のみ
5. **Stop hook**: 全テスト走行 ON
6. **フォント**: DotGothic16 採用
7. **mvp 統合**: `-X ours` 戦略で develop 側を優先
8. **E-7 Bestiary**: P2 (捨て候補) に降格
9. **クリア条件**: 階段踏破のみ
10. **Agent Teams**: 当面 OFF
11. **E-1 ドメイン設計**: ADR-16 で確定、PR #13 で実装
12. **依存事項の起票戦略**: A+B のみ先行起票 (戦略 Z、サブエージェント PM 推奨)、C/D/E は段階起票

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

1. `/m1-5-start E-X 機能名` Skill (Issue 起票 + ブランチ作成)
2. `domain-architect` Agent で設計レビュー → ユーザー確認
3. 実装 (Edit/Write) → hook で `spotlessApply` + 関連テスト
4. `test-writer` Agent でテスト補強
5. (LibGDX 依存時) `libgdx-implementer` Agent
6. `final-architect` Agent で最終レビュー (※ `/architect-review` Skill は worktree 問題あり、直接 Agent 推奨。lessons.md 参照)
7. `gh pr create` で日本語 Draft PR (※ `/japanese-pr-create` Skill も worktree 問題あり、テンプレ参照のみ、実行は main session)
8. self-merge or チームレビュー → マージ

### Skill 利用時の注意 (本セッションで判明、lessons.md エントリ追記済)

- `context: fork` 付き Skill (現 `architect-review` のみ) は worktree を生成し、後続 Skill の cwd 汚染が連鎖する
- **Skill 内の git 出力は信用しない**、必ず main session で `git -C <main-repo-path>` 明示パスで再確認
- `gh pr create` や `git commit` の不可逆操作は必ず `-C C:/.program/DDD-Jyogi-Kuroto-F` 付き

### 不可逆判断が必要なとき

- `decisions.md` に ADR を追記

### 失敗 / 判断ミスをしたとき

- `lessons.md` にエントリ追加 (`/lessons-add` Skill or 直接 Edit)

## スケジュール

- **2026-05-12**: MVP 完成
- **2026-05-13**: §15 仕様策定 + バランス調整
- **2026-05-14 (今日)**: PR #9 / #11 merged、E-1 ドメイン層実装完了 + PR #13 Draft、Issue #14 / #15 起票
- **2026-05-15 (金)**: 本来の MVP 締切 (M1)、達成済 / PR #13 マージ予定
- **2026-05-15〜21**: M1.5 並列実装 (依存 A/B 着手 → C/D/E 起票 → E-2/E-3/E-4/E-6 並列)
- **2026-05-22**: 統合テスト + デモ録画
- **2026-05-23-24 (土日)**: ハッカソン本番 (M2)

## 関連ドキュメントへの動線

- 全体仕様: [docs/GAME_DESIGN.md §15](../../docs/GAME_DESIGN.md)
- 実装俯瞰: [docs/SystemSummary.md](../../docs/SystemSummary.md)
- M1.5 並列プラン: [tasks/todo.md](../todo.md) の Phase 9
- 設計判断ログ: [decisions.md](./decisions.md) (最新は ADR-16: E-1 ドメイン設計)
- AI 失敗ログ: [lessons.md](./lessons.md) (最新: Skill worktree 問題)
- 過去レビュー: [phase_6_5_review.md](./phase_6_5_review.md)
- プロジェクト記憶: [../../CLAUDE.md](../../CLAUDE.md)
