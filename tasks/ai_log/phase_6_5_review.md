# ai_log/phase_6_5_review.md

サブエージェントによる独立レビューの記録。Phase 2 (ドメイン層) → Phase 3〜4 (全体) → Phase 6.5 (再レビュー + テスト + コード) の順で実施。

| 日時 | フェーズ | レビュー結果サマリ |
|---|---|---|
| 2026-05-12 | Phase 2 (ドメイン層) | 総合 **A**。修正必須 2 件と改善 2 件を反映済み |
| 2026-05-12 | Phase 3〜4 (アプリ + インフラ + プレゼン) | 総合 **B → 修正後 A 相当**。修正必須 3 件を反映済み |
| 2026-05-12 | Phase 5 (実機検証) | `gradlew test` 51 件 PASS / `gradlew run` で 1m23s 起動 + exit 0。M1 全項目クリア |
| 2026-05-12 | Phase 6.5-1〜6.5-2/6.5-5 (バージョン照合 + テスト/コード再レビュー) | サブエージェント評価 A×2。コード 28→26 ファイル (Actor/RunResult 削除)、テスト 51→61 件 (全 PASS)、層越境を解消 |
| 2026-05-12 | Phase 6.5-3/6.5-4/6.5-6 (素材方針 + 日本語化 + システムまとめ) | docs/AssetGuidelines.md と LICENSES/INDEX.md を整備。DotGothic16 (ピクセル日本語) を配置、Fonts/Strings で日英自動切替を実装。docs/SystemSummary.md でアーキテクチャ俯瞰 + 拡張ロードマップ |
| 2026-05-12 | チームメイト視点レビュー (共有準備) | 「コードは綺麗、ドキュメント側に AI 丸投げ感が残る」総合診断。修正 5 件 + 議論の種 5 件 + 良い点 3 件を抽出 |

---

## 2026-05-12 — Phase 2 ドメイン層レビュー詳細

レビュー実施: 別エージェント (general-purpose) に独立評価を依頼。

**修正必須 (反映済み)**:
1. `TurnEngine.resolveEnemyAction` に phase ガードが無く GAME_OVER 後でも敵が動けてしまう
   → 先頭で `state.phase() != TurnPhase.ENEMY_TURN` ならば reject するよう修正
2. `Stats.power` が未使用 (YAGNI 違反候補)
   → power フィールドを削除し record を 3 引数化、関連テストと fixture を全て修正

**改善余地 (反映済み)**:
1. `TurnEngine` 内の `replaceEnemy` / `removeEnemy` ヘルパーが責務として外れている
   → `DungeonState.withEnemyReplaced(Enemy)` / `withEnemyRemoved(ActorId)` に移管。ID 不一致時は `IllegalStateException` でガード
2. `EnemyAi` が AP 不足や slot 不在でもスキル/移動を提案して呼び出し側 loop が刺さるリスク
   → `canUseSkill` / `canSpend(1)` で事前に判定し、不可ならば `Wait` を返す

**改善余地 (未反映 / MVP では保留)**:
- `BattleAction.UseSkill` に対象方向を持たせる構成は将来検討 (現状は隣接した最初の敵を自動選択)
- `DungeonState.findEnemy*` の線形探索は敵 1〜2 体の MVP では問題なし

**特に良かった点**:
- `TurnEngine.reject` が引数の state をそのまま返し、ActionRejected event のみ追加する純粋設計
- `DungeonMap` の `Tile[][]` を private + 静的ファクトリ経由のみで外部に渡さない
- `BattleAction` / `SkillEffect` / `RunResult` を sealed にして将来の追加時に switch の網羅性で守る

---

## 2026-05-12 — Phase 3〜4 全体 (アプリ + インフラ + プレゼン) レビュー詳細

レビュー実施: 別エージェントに独立評価を依頼。観点はビルド構成 / LibGDX API / 状態遷移 / プレゼン層 / 設計原則。

**修正必須 (反映済み)**:
1. クリア条件が「敵全滅」になっていて、敵 1 体配置の MVP では撃破直後に毎回 CLEARED が走り、戦闘してすぐに終わってしまう
   → `TurnEngine.applyPlayerMove` で「`STAIRS_DOWN` に踏み込んだら CLEARED」に変更。敵全滅では遷移しないようにロジック修正。テストも追従させ、`steppingOntoStairsTriggersCleared` を追加
2. `DungeonScreen` が `final TurnDirector director` を保持していて、新ラン後に古い `GameContext` を見続けるリスク
   → `DddGame` 側で `GameContext` と `TurnDirector` を 1 か所に集約し、`startNewRun()` で両方作り直す。Screen は都度 `game.director()` で取得する形に
3. `build.gradle` の `resources.srcDirs` に空の `assets/` を含んでいて、`processResources` の空入力警告を招く
   → MVP では `assets/` を srcDirs から外し、リソース導入と同時に再度入れる方針に切り替え

**追加対応 (合わせて実施)**:
- `InitialStateFactory.firstFloor()` の敵を 1 体 → 2 体配置に変更。階段前と中央付近に置いて、戦闘を経るプレイ感を担保

---

## 2026-05-12 — Phase 6.5 テスト妥当性レビュー詳細

**修正必須 (反映済み)**:
1. `EnemyAiTest.blockedEnemyWaits` に未使用変数と誤コメント
   → 未使用 `Enemy e` を削除、コメントを実態に合わせて書き直し
2. `TurnEngineTest.enemyAttackKillsPlayerTriggersGameOver` が「死亡時ソウル保持」を検証していない
   → `enemyAttackKillsPlayerTriggersGameOverAndKeepsSoul` に改名し、ソウル 7 を事前付与して死亡後も保持されることを assert
3. `ActionPointsTest` で「max 張り付きから再 regenerate」の冪等性テストが無い
   → `regenerateAtMaxStaysAtMax`, `regenerateZeroIsNoOp` を追加

**追加テスト (反映済み)**:
- `playerWithZeroApRejectsActions` — AP 0 で各アクションが reject されること
- `killingOneEnemyOfTwoDoesNotChangePhase` — 敵 1/2 撃破でフェーズが変わらないこと
- `adjacentEnemyWithoutApWaits` — EnemyAi が AP 不足時 Wait を返すこと
- `withEnemyReplaced/Removed ThrowsWhenIdNotFound` — DungeonState の ID 不一致例外
- `subtractReturnsRemainder` — Soul の正常系
- `towardsSamePositionReturnsRight` — Direction の境界条件

---

## 2026-05-12 — Phase 6.5 コードレビュー詳細

**修正必須 (反映済み)**:
1. `DddGame` が `application` パッケージにあって `presentation/screen/TitleScreen` を直接 new していて層越境
   → `core.presentation.screen.DddGame` に移動。`application` 層は LibGDX 非依存の `GameContext` / `TurnDirector` のみに
2. `core.domain.meta.RunResult` が src/main で 1 件も参照されていない (YAGNI 違反)
   → 削除
3. `core.domain.entity.Actor` が sealed interface として宣言のみで参照ゼロ
   → 削除し `Player` / `Enemy` から `implements Actor` を外す

**改善余地 (反映済み)**:
- `GameOverScreen.soulSnapshot` の確定タイミングをコンストラクタ → `show()` に
- `TurnDirector.ENEMY_STEP_HARD_LIMIT` に算出根拠コメント
- `Direction.towards` に TODO コメント (将来 A* に差し替え予定)

---

## 2026-05-12 — チームメイト視点レビュー (共有準備)

レビュー観点: 「Java 初〜中級者で Git 学習中の 4 人ハッカソンチームメンバー」が clone した時の印象。

**直しておくべき点 (反映済み)**:
1. README の clone URL を実リポジトリと照合 → OK 確認済
2. `gradlew` をリポジトリにコミット + README から「`gradle wrapper` 手動実行」案内を削除
3. `presentation/effect`, `presentation/ui` の空パッケージに着手予告コメント (`package-info.java`)
4. `ContributingGuide.md` / `TechSelectionMemo.md` の「MVP 着手時に追加」「.gitkeep 暫定」表記を最新の状態に更新
5. `tasks/lessons.md` と `tasks/todo.md` のレビュー記録を `tasks/ai_log/` に切り出し、`todo.md` 本体は「これからやること」中心にスリム化

**議論の種 (チームに残す)**:
1. Doko-demo (Android 対応) のタイミング — スローガンの核なのに MVP は Desktop のみ
2. キーマッピング — WASD/矢印 vs hjkl 派の存在
3. スキル装着数 — 枠 4 つあるのに MVP は 2 個しか使ってない (リプレイ性弱い)
4. クリア条件 — 階段踏破にしたので敵を避けて直行が最適戦略になりうる、敵配置の再検討
5. Java 25 機能 (sealed/record/switch pattern matching) のハードル — 初学者がどこまで触る前提か

**素直に良い点**:
- `SystemSummary §6` ドメイン用語 ↔ 実装名マッピング表
- `TurnEngine` の reject 設計 (失敗が状態を壊さない構造)
- フォント fallback (チームメイトがフォント未配置でも `gradlew run` が必ず動く)
