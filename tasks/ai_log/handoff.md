# ai_log/handoff.md

> **Claude セッションが圧縮 / 再開した時に最初に読む 1 枚スナップショット**。
> ここを読めば「今ここから何をすべきか」が即座に分かる構造にする。
> 状況が動くたびに 1 行ずつ更新する (古い行は decisions.md / lessons.md に流す)。

---

## 最終更新

- **日時**: 2026-05-14 (火) 深夜 (M1.5 コア機能 = カードで敵を殴れる が `gradlew run` で実機動作可能)
- **更新者**: Claude (本セッション)

## アクティブブランチ

`chore/post-ui-handoff-update` (handoff.md 更新用、低リスク docs PR)

ローカルメインリポジトリ: `C:\.program\DDD-Jyogi-Kuroto-F`

## 進行中の作業 (圧縮後はここから再開)

**現在地**: **M1.5 コア機能完成、`gradlew run` で「カードで敵を殴れる」がエンドツーエンドで動作可能**。本セッションで以下 6 PR を self-merge:

| # | PR | 内容 | Commit |
|---|---|---|---|
| 1 | **#13** | E-1 カードシステムドメイン層 (`core.domain.card/` 11 ファイル) | `48033de` |
| 2 | **#16** | 依存 B: Stats 6 ステ + `CardEffect.Damage.resolve` | `bbaa630` |
| 3 | **#17** | 依存 A: ActionPoints 使い切り化 (`refilledTo(int)`) | `d77432e` |
| 4 | **#19** | 依存 D: BattleAction.UseCard + TurnEngine 統合 | `42c10b0` |
| 5 | **#20** | handoff.md 依存 D 完了状態更新 | `adbebea` |
| 6 | **#21** | **最小 UI 結合 + 初期手札注入 + 6 ステ初期値設定** | `b2c86c3` |

`gradlew test` 全 **155 件 PASS**、final-architect レビュー全 PR で **A 判定** (#21 は B → 修正 2 件で A 相当)。

### 圧縮後の Claude が最初にやること

1. **`gradlew run` で実機確認** (ユーザー依頼) — 移動 (WASD/矢印) / カード選択 (1-9) / 方向キーでカード使用 / SPACE 待機 / ENTER ターン終了 が動くか
2. (実機確認 OK なら) **次のフェーズに着手**:
   - (A) **E-3 層構造 + ノード分岐** (`core.domain.layer/`) - P0'、§15-6
   - (B) **E-4 通貨 Gold** (`core.domain.meta/Gold.java`) - P0'、§15-2
   - (C) **E-6 ポップアップ UI 基盤** (`core.presentation.window/`) - P0'、ADR-03 で全 UI を Scene2D Window 化
   - (D) **E-2 ソウルツリー** (`core.domain.tree/` + `SoulTreeScreen`) - P0'、§15-7
   - (E) **E-5 装備 + 依存 E**: 装備固有カードで Deck 動的化 - P1
3. (実機確認で問題あれば) バグ調査 — `bug-hunter` Agent に依頼

### 本セッションの実装サマリ

```
┌─────────────────────────────────────────────────────┐
│   gradlew run (DungeonScreen)                       │
│   ├── PlayerInputs (2 ステート: 数字→方向)            │
│   ├── HudRenderer (手札テキスト + 選択中ハイライト)    │
│   └── BattleAction.UseCard(handIndex, direction)    │
│         ↓                                            │
│   TurnEngine.applyPlayerUseCard                     │
│   ├── Hand 範囲チェック / AP コストチェック             │
│   ├── CardEffect で switch                          │
│   │   ├── Damage  → resolve(attacker, defender, element) → Stats.damaged  │
│   │   ├── Move    → reject (未実装)                  │
│   │   ├── Buff    → reject (未実装)                  │
│   │   └── Trap    → reject (未実装)                  │
│   ├── Hand → Discard 移動 (CardPileState.playFromHand)│
│   ├── AP 消費 (ActionPoints.spend)                  │
│   └── ターン頭で AP 全リセット (ActionPoints.refilledTo)│
└─────────────────────────────────────────────────────┘
```

### 残スコープ (5/15 以降)

#### M1.5 P0' 機能 (5/15-18 想定)

- **E-3** 層構造 + ノード分岐 (`core.domain.layer/`) — §15-6
- **E-4** 通貨 Gold (`core.domain.meta/Gold.java`) — §15-2
- **E-6** ポップアップ UI 基盤 (`core.presentation.window/`、Scene2D Window) — §15-1, §15-8、ADR-03
- **E-2** ソウルツリー (`core.domain.tree/` + `SoulTreeScreen`) — §15-7
- **E-5** 装備システム + 依存 E (装備固有カードで Deck 動的化) — §15-9 + ADR-18 で予告

#### P1 (5/19-21 想定)

- **E-8** シームレス戦闘演出 — §15-5
- **E-9** セーブ最小 — §15-11
- **E-10** チュートリアル — §15-10
- **依存 C** Direction8 (罠 8 方向用) — 罠カード実装時のみ必要

#### スキップ済 (時間切れ判定)

- **E-7** Bestiary — P2 (ADR-15 で降格)
- **Move/Buff/Trap カード実装** — Damage で十分デモ可能、別 Issue

## 直近のマージ済成果

| Commit | 内容 | 日付 |
|---|---|---|
| `c1fc9d6` | MVP コア実装一式 (#7) | 2026-05-12 |
| `4c97f0c` | **PR #9 merged**: §15 ブラッシュアップ | 2026-05-14 朝 |
| `f62b482` | **PR #11 merged**: AI 体制整備 | 2026-05-14 朝 |
| `48033de` | **PR #13 merged**: E-1 カードシステム (#12) | 2026-05-14 昼 |
| `bbaa630` | **PR #16 merged**: 依存 B Stats 6 ステ (#15) | 2026-05-14 午後 |
| `d77432e` | **PR #17 merged**: 依存 A ActionPoints 使い切り (#14) | 2026-05-14 午後 |
| `42c10b0` | **PR #19 merged**: 依存 D UseCard 統合 (#18) | 2026-05-14 夜 |
| `b2c86c3` | **PR #21 merged**: 最小 UI 結合 + 初期手札注入 + 6 ステ初期値 | 2026-05-14 夜 |

## Open Issues / PRs

| 番号 | 種類 | 状態 | 内容 |
|---|---|---|---|
| #1 | Issue | OPEN | リポジトリの土台整備 (古い、Close 候補) |
| #4 | Issue | OPEN | CI 軽量化とドキュメント微調整 (古い、Close 候補) |
| #8 | Issue | OPEN | §15 バランス調整 (PR #9 マージで実質完了、Close 推奨) |
| #12 | Issue | OPEN | E-1 カードシステム実装 (ドメイン + UI 結合完了、Close 候補) |

依存 #14 (A) / #15 (B) / #18 (D) はすべて MERGED PR で実質完了 (手動 close 待ち)。

## 据置き判断 (議論済、再議論不要)

詳細は [decisions.md](./decisions.md) を参照。**ADR-17 / ADR-18 が本セッションで追加**:

1. **AP モデル**: §15 使い切り型 — ADR-01、PR #17 で実装
2. **敵 AP**: 層番号 N に等しい
3. **Stats**: 6 種 — ADR-02、PR #16 で実装
4. **ステ計算**: `max(1, base + 物攻 - 物防)` — **ADR-17** で `CardEffect.Damage.resolve` 配置確定、PR #16 で実装
5. **Stop hook**: 全テスト走行 ON
6. **フォント**: DotGothic16 採用
7. **mvp 統合**: `-X ours` 戦略
8. **E-7 Bestiary**: P2 降格
9. **クリア条件**: 階段踏破のみ
10. **Agent Teams**: 当面 OFF
11. **E-1 ドメイン設計**: ADR-16、PR #13 で実装
12. **依存事項の起票戦略**: A+B 先行、C/D/E 段階起票
13. **ダメージ計算配置**: `CardEffect.Damage.resolve` (card 層内)、DamageFormula 廃案 — ADR-17
14. **Skill 経路不変保持**: SkillEffect = 固定ダメ継続 — ADR-17
15. **CardPileState の所有**: **Player record に内蔵 (案 X)** — **ADR-18**
16. **Move/Buff/Trap カード**: 本 PR では reject、別 Issue で順次 — ADR-18
17. **BattleEvent**: `CardUsed` 新設せず `SkillUsed` 流用 — ADR-18
18. **ダメージヘルパ**: Skill/Card 両経路で `int finalDamage` 受け取り統一 — ADR-18
19. **UI 形式**: 本日は MVP テキスト UI、E-6 で Scene2D Window 化 — ADR-03 中間実装 (PR #21)
20. **初期手札**: ハードコード 3 枚 (`InitialStateFactory`)、E-5 Equipment で動的化予定 — PR #21
21. **NUM_1〜9**: UseCard に完全置換、UseSkill 一時無効化 (Shift+1〜4 等で将来復活可) — PR #21

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

1. `/m1-5-start E-X 機能名` Skill で Issue 起票 + ブランチ作成
2. **`domain-architect` Agent で設計レビュー → ユーザー確認**。大きな設計判断は **3 並列レビュー** 推奨 (本セッションで ADR-17 / ADR-18 を救済した運用パターン)
3. ADR 記録 (`decisions.md` 追記) — 不可逆判断は実装前に記録
4. 実装 (Edit/Write) → hook で `spotlessApply` + 関連テスト自動実行
5. `test-writer` Agent でテスト補強
6. (LibGDX 依存時のみ) `libgdx-implementer` Agent でプレゼン層
7. **`final-architect` Agent を Agent ツールから直接呼ぶ** — `/architect-review` Skill は使わない (worktree 問題)
8. **`gh pr create` を main session から実行** — `/japanese-pr-create` Skill はテンプレ参照のみ
9. self-merge (ADR-14) または チームレビュー → マージ

### Skill 利用時の注意 (lessons.md エントリ参照)

- `context: fork` 付き Skill は worktree を生成し、後続 Skill の cwd 汚染が連鎖する
- **Skill 内の git 出力は信用しない**、必ず `git -C <main-repo-path>` 明示パスで再確認
- `gh pr create` や `git commit` の不可逆操作は必ず `-C C:/.program/DDD-Jyogi-Kuroto-F` 付き

### 不可逆判断が必要なとき

- `decisions.md` に ADR を追記 (現在 **ADR-18** まで蓄積済)

### 失敗 / 判断ミスをしたとき

- `lessons.md` にエントリ追加

## スケジュール

- **2026-05-12**: MVP 完成
- **2026-05-13**: §15 仕様策定 + バランス調整
- **2026-05-14 (今日)** ✅: **M1.5 コア機能完成** (E-1 + 依存 A/B/D + UI 結合、6 PR self-merge、`gradlew run` で実機動作)
- **2026-05-15 (金)**: 実機確認 + E-3 / E-4 / E-6 並列着手
- **2026-05-16〜18**: E-2 ソウルツリー、E-5 装備 + 依存 E、E-6 完成
- **2026-05-19〜21**: E-8 演出、E-9 セーブ、E-10 チュートリアル、依存 C
- **2026-05-22**: 統合テスト + デモ録画
- **2026-05-23〜24**: ハッカソン本番

## 関連ドキュメントへの動線

- 全体仕様: [docs/GAME_DESIGN.md §15](../../docs/GAME_DESIGN.md)
- 実装俯瞰: [docs/SystemSummary.md](../../docs/SystemSummary.md)
- M1.5 並列プラン: [tasks/todo.md](../todo.md) の Phase 9
- 設計判断ログ: [decisions.md](./decisions.md) (ADR-18 まで)
- AI 失敗ログ: [lessons.md](./lessons.md)
- 過去レビュー: [phase_6_5_review.md](./phase_6_5_review.md)
- プロジェクト記憶: [../../CLAUDE.md](../../CLAUDE.md)
