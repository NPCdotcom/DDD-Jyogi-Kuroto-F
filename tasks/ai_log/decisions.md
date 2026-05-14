# ai_log/decisions.md

> **ADR (Architecture Decision Record) 風の不可逆判断ログ**。
> 一度決めて、覆すのに大きなコストが伴う判断を追記専用で蓄積する。
> Issue 本文や docs 改訂履歴に散らばる「なぜそうしたか」を集約。

---

## 形式

```
## ADR-NN: 短いタイトル

- **Status**: Accepted | Superseded by ADR-MM | Deprecated
- **Date**: YYYY-MM-DD
- **Context**: 何が問題だったか、どんな選択肢があったか
- **Decision**: 何を選んだか
- **Consequences**: その結果として何が起きるか (良い影響と悪い影響)
- **Reference**: 関連 Issue / PR / docs
```

---

## ADR-01: AP モデルを蓄積型 → 使い切り型 (§15-3) に変える

- **Status**: Accepted
- **Date**: 2026-05-13
- **Context**: MVP は AP 蓄積型 (`ActionPoints.regenerate(speed)` で上限まで貯まる)。§15 でカードデッキ構築型 (Slay the Spire 風) を採用するにあたり、AP の意味論が衝突
- **Decision**: §15 では AP **使い切り型** に変更。毎ターン頭で速度ステ分まで全リセット、AP 切れ or 手札切れで相手ターン
- **Consequences**:
  - + Slay the Spire のメンタルモデル流用可能、初心者でも分かりやすい
  - + 「今ターン使い切らないと損」の戦術的緊張感
  - − MVP コードの `ActionPoints` / `TurnEngine` を書き直し必要 (M1.5 で対応)
- **Reference**: [docs/GAME_DESIGN.md §15-3](../../docs/GAME_DESIGN.md), tasks/todo.md Phase 9 の breaking change リスト

## ADR-02: Stats を 3 ステ → 6 ステ (§15-4) に拡張

- **Status**: Accepted
- **Date**: 2026-05-13
- **Context**: MVP は HP / 速度 のみ、攻撃力 (`Stats.power`) を YAGNI で削除済。§15 でカードに物理 / 魔法属性を入れるため、防御も対応が必要
- **Decision**: HP / 速度 / 物攻 / 魔攻 / 物防 / 魔防 の 6 種。ダメージ計算は `max(1, カード基礎値 + 物攻 - 物防)` 加減算のみ (最低 1 ダメ保証)
- **Consequences**:
  - + 物理 / 魔法のキャラビルドが成立
  - + MVP の `Stats.power` 削除判断が「将来 2 つに分けて入れる」前提だったので整合
  - − `Stats` record の引数増、テスト fixture 全部書き換え
- **Reference**: [§15-4](../../docs/GAME_DESIGN.md), [phase_6_5_review.md](./phase_6_5_review.md) ドメイン層レビュー反映

## ADR-03: UI を画面遷移 → ポップアップ式に変える

- **Status**: Accepted
- **Date**: 2026-05-13
- **Context**: MVP は Screen 単位の画面遷移 (TitleScreen → DungeonScreen → GameOverScreen)。§15 で「シームレス戦闘 + 編成画面 + ソウルツリー + Bestiary」と画面要素が増加、画面遷移では UX が分断される
- **Decision**: 全 UI を **Scene2D の Window でポップアップ式** に変更。FancyMenu Mod 的なイメージ。基本解像度 1920×1080、初回起動でプリセット 3 種 (ミニマル / 標準 / 情報マシマシ) から選ばせる
- **Consequences**:
  - + モードレス UI で「戦闘中も編成画面開ける」等の体験
  - + テーマ変動 (装備系統で UI 着替え) と相性
  - − MVP の `Screen` 群を全部書き直し必要
  - − Stage / Window / Skin の習熟コスト
- **Reference**: [§15-1, §15-8](../../docs/GAME_DESIGN.md)

## ADR-04: 解像度 800×600 → 1920×1080 ベースに変更

- **Status**: Accepted
- **Date**: 2026-05-13
- **Context**: MVP の `RenderLayout` は 800×600 ハードコード。ポップアップ UI 16 種を並べるには狭い
- **Decision**: 1920×1080 ベース、ユーザー設定でリサイズ可能、ViewPort で吸収
- **Consequences**:
  - + フル HD で UI が映える、デモ映え◯
  - − 全座標を比率化、`RenderLayout` 書き直し必要
- **Reference**: [§15-1](../../docs/GAME_DESIGN.md)

## ADR-05: 戦闘モードの境界を廃止 (シームレス戦闘)

- **Status**: Accepted
- **Date**: 2026-05-13
- **Context**: 古典 JRPG は「ダンジョン探索 → 戦闘画面切替 → 戦闘終了 → 探索戻り」の境界がある。ToME / Nethack 系はそれを持たない
- **Decision**: **戦闘モードという概念を廃止**。タイル単位ターン制ローグライク。プレイヤーが動かないと敵も動かない (待ち戦術 OK)、敵が居れば敵ターン、居なければスキップ
- **Consequences**:
  - + 画面遷移なしでテンポ◯
  - + MVP の `DungeonState` 中心の設計と一致 (もともとシームレスだった)
  - − Bestiary 連動の予告表示などで「動かないと敵も動かない」の長考対策が必要
- **Reference**: [§15-5](../../docs/GAME_DESIGN.md)

## ADR-06: 敵 AP = 層番号 N を据置き (バランス専門 C 評価でも変更しない)

- **Status**: Accepted
- **Date**: 2026-05-14
- **Context**: バランス専門のサブエージェントから「敵 AP = 層番号は 3 層で敵 1 体が 12 ダメ/ターン → 3 ターンで HP 30 が溶ける、詰む」と C 評価を受けた
- **Decision**: ユーザー判断で **据置き**。「指数的難易度」の意図は「プレイヤーが追いつかれて死亡 → 強化 → 再挑戦のローグライト的サイクル」を生むため、AP の同一性で実現する設計
- **Consequences**:
  - + ローグライト的な死亡 → 再挑戦のリズム生成
  - − プレイテストで「詰む」「リトライ多すぎ」が出たら HP / 攻撃力側で再調整 (AP は変えない)
- **Reference**: [phase_6_5_review.md](./phase_6_5_review.md) のバランス専門 C 評価、[§15-5](../../docs/GAME_DESIGN.md)

## ADR-07: クリア条件は階段踏破のみ (敵全滅では遷移しない)

- **Status**: Accepted
- **Date**: 2026-05-12 (MVP 段階で確定)
- **Context**: MVP の初期実装では「敵全滅で CLEARED」だったが、敵 1 体配置の MVP では撃破直後に毎回クリア = 戦闘体験が薄い
- **Decision**: クリア条件は **階段踏破のみ**。敵全滅は単に敵がいなくなるだけ
- **Consequences**:
  - + ローグライク的踏破感
  - − 敵を避けて階段直行が最適戦略になりうる (敵配置で対策)
- **Reference**: [§15-6](../../docs/GAME_DESIGN.md), [phase_6_5_review.md](./phase_6_5_review.md) 全体レビュー反映

## ADR-08: 装備は 1 部位スタート、耐久値なし、特殊能力なし

- **Status**: Accepted
- **Date**: 2026-05-13
- **Context**: タルコフ風の「装備耐久」は MVP § で示唆されていたが、UI が煩雑、楽しみより面倒さが勝つ可能性
- **Decision**: 装備は **1 部位**、耐久なし、特殊能力なし、ステ補正 + UI テーマ変動 + 装備固有カードのみ
- **Consequences**:
  - + 実装が極シンプル
  - + 装備固有カードがソウルツリー連動 (初期装備 = 初期デッキ) で綺麗に設計統合
  - − 「装備の個性」が薄くなる、ハッカソン後にカード固有効果で差別化検討
- **Reference**: [§15-9](../../docs/GAME_DESIGN.md)

## ADR-09: ソウルツリーはリセット可能 (1 回 5 ソウル)

- **Status**: Accepted
- **Date**: 2026-05-13
- **Context**: Path of Exile 風 Passive Tree は通常リセット不可だが、ハッカソン規模ではビルド試行 / デバッグの容易性が重要
- **Decision**: **リセット可能**、1 回 5 ソウル消費、消費したソウル残量は復元、ラン中の取得カード・装備は影響なし
- **Consequences**:
  - + ビルド試行が気軽、デバッグも楽
  - − 「永続強化」の重みが薄れる、本番では微課金にして「不可逆らしさ」を残す
- **Reference**: [§15-7](../../docs/GAME_DESIGN.md)

## ADR-10: Stop hook で全テスト走行 ON (ユーザー要望)

- **Status**: Accepted
- **Date**: 2026-05-14
- **Context**: サブエージェントは「応答ごとに 17 秒待ちは体感速度損失」と OFF 推奨だったが、ユーザーは「テスト毎回 ON、仕様固めて意味のあるテスト最初から書けば AI が書いてもちゃんと動く」と主張
- **Decision**: `.claude/settings.json` で `Stop` hook により **全テスト走行を常時 ON**
- **Consequences**:
  - + テスト書き忘れや破壊変更を即検知
  - + 「意味のあるテスト」を最初から書く文化形成
  - − 応答ごとに数十秒待つ (テスト件数が増えるほど顕在化)、後で時間問題になったら見直し
- **Reference**: [.claude/settings.json](../../.claude/settings.json), [.claude/hooks/final-check.sh](../../.claude/hooks/final-check.sh)

## ADR-11: フォントは DotGothic16 採用

- **Status**: Accepted
- **Date**: 2026-05-13
- **Context**: Noto Sans JP 等の汎用日本語フォントは綺麗だが大きい (4.5MB)、ピクセル方針との相性も微妙。ユーザーが DotGothic16 (16 ドットフォント、SIL OFL 1.1) の ZIP を提供
- **Decision**: `assets/fonts/DotGothic16-Regular.ttf` を採用、Nearest フィルタ + 16 倍数サイズ (16 / 32 / 48) で pixel-perfect
- **Consequences**:
  - + ピクセル方針との一貫性、ピクセルゲームらしい外観
  - + 軽量 (約 2 MB)、SIL OFL 1.1 で再配布可
  - − 漢字カバレッジは Noto より狭い (実用上問題なし)
- **Reference**: [docs/AssetGuidelines.md](../../docs/AssetGuidelines.md), [LICENSES/INDEX.md](../../LICENSES/INDEX.md), [LICENSES/DotGothic16_OFL.txt](../../LICENSES/DotGothic16_OFL.txt)

## ADR-12: Agent Teams は当面 OFF

- **Status**: Accepted
- **Date**: 2026-05-14
- **Context**: Claude Code 2026 の Agent Teams (実験機能) は M1.5 並列実装に魅力的だが、トークン 3〜5 倍消費、不安定性、Claude Max 上限懸念
- **Decision**: **当面 OFF**。Subagent + Skills + Hooks 運用が落ち着いてから段階導入を検討
- **Consequences**:
  - + Claude Max のトークン上限を守りやすい
  - + 単一セッション + Subagent 呼出という枯れたパターンを徹底
  - − 真の並列実行はできない (1 Claude セッション内で逐次)
- **Reference**: [CLAUDE.md](../../CLAUDE.md) の「Agent Teams は当面オフ」記述、[PR #11](../../) 本文

## ADR-13: mvp ブランチを develop に `-X ours` で統合

- **Status**: Accepted
- **Date**: 2026-05-14
- **Context**: mvp ブランチに MVP コード一式 + ドキュメント (MVP 段階の説明)、develop に §15 仕様反映済 docs。`docs/` が両方で更新されていて衝突多数
- **Decision**: `git merge mvp -X ours` で **develop 側を優先**、コード一式は新規追加で取り込み
- **Consequences**:
  - + docs の §15 反映 + MVP コード + ビルド基盤 がすべて develop に揃う
  - + 衝突解消が自動 (`-X ours`)、手動コンフリクト解消ゼロ
  - − mvp 側の docs (MVP 段階の説明) は失われる (= 履歴を mvp ブランチで参照する必要、ただしリポジトリ参照ブランチとして保持)
- **Reference**: commit `4a9382f` の merge ログ、[handoff.md](./handoff.md) 直近のマージ済成果

## ADR-14: PR は Draft で出して self-merge 可 (リーダー権限)

- **Status**: Accepted
- **Date**: 2026-05-14
- **Context**: [ContributingGuide §3](../../docs/ContributingGuide.md) は「人間レビュー 1 名以上必須」だが、リーダー単独運用 (AI 駆動) の現状ではボトルネックになる
- **Decision**: 整備・docs 更新等の **低リスク PR はリーダー権限で self-merge 可**。コードの設計変更を伴う PR はチームレビュー必須を維持
- **Consequences**:
  - + リーダーの開発テンポを保てる
  - + AI レビュー (`final-architect`) で品質ゲートを担保
  - − ContributingGuide のルール (人間レビュー必須) と乖離、いずれ更新が必要
- **Reference**: [PR #11](../../), [PR #9](../../) の self-merge 運用

## ADR-15: E-7 Bestiary は P2 (捨て候補) に降格

- **Status**: Accepted
- **Date**: 2026-05-14
- **Context**: サブエージェント PM レビューで M1.5 スコープ過大 (10 機能 10 日) を指摘。E-7 Bestiary は他機能と独立性が高く、デモシナリオ §15-12 にも必須でない
- **Decision**: E-7 を **P2 (本番後)** に降格、M1.5 では P0' (E-1 / E-3 / E-4 / E-6 / E-2) を優先
- **Consequences**:
  - + M1.5 のリアルなスコープ縮小、本番までに到達可能
  - − 「敵を倒すたびに図鑑記録」というメタ進行的楽しさは本番には間に合わない
- **Reference**: [tasks/todo.md Phase 9](../todo.md), [phase_6_5_review.md](./phase_6_5_review.md) チームメイト視点レビュー

## ADR-16: E-1 カードシステムのドメイン設計を確定

- **Status**: **Accepted (ユーザー承認済、実装はコンテキスト圧縮後の次セッションで)**
- **Date**: 2026-05-14
- **Context**: M1.5 P0' の E-1 (カードシステム、§15-3) 実装にあたり、`core.domain.card/` パッケージのドメイン構造を確定する必要があった。domain-architect Agent が設計案を提案、ユーザーが承認
- **Decision**: 以下のドメイン構造で実装する:

  ```
  core.domain.card/
  ├── CardId.java          record(String value), of(String) ファクトリ
  ├── CardTag.java         enum: ATTACK, MOVEMENT, BUFF, TRAP
  ├── CardElement.java     enum: PHYSICAL, MAGICAL (ハイブリッドは YAGNI)
  ├── Card.java            record(CardId, String displayName, int apCost,
  │                                 CardTag, CardElement, CardEffect)
  │                        ※ apCost >= 1 を compact constructor で強制
  ├── CardEffect.java      sealed interface permits Damage, Move, Buff, Trap
  │                          ├ record Damage(int baseValue)
  │                          ├ record Move(int distance)
  │                          ├ record Buff(BuffKind kind, int amount, int durationTurns)
  │                          └ record Trap(int baseValue, TrapLifetime lifetime)
  ├── TrapLifetime.java    sealed interface permits UntilStepped, Turns
  │                          ├ record UntilStepped()   // 物理罠
  │                          └ record Turns(int remaining)  // 魔法罠
  ├── Deck.java            record(List<Card> cards) 静的マスター
  ├── DrawPile.java        record(List<Card> cards), drawOne(), shuffledFrom(List, Random)
  ├── DiscardPile.java     record(List<Card> cards), add(Card), reshuffleInto(Random)
  ├── Hand.java            record(List<Card> cards), MAX_SIZE=9, add(Card), remove(int)
  └── CardPileState.java   record(DrawPile, Hand, DiscardPile)
                           initialDrawCount(int totalDeckSize) static
                           drawN(int n, Random rng), playFromHand(int)
  ```

  **核となる設計判断**:
  1. Card は単一 record (8 サブクラスを作らない)、多態性は CardEffect (sealed) に集約 — KISS
  2. CardPileState で戦闘中の動的状態を局所閉包 (山札切れ → 捨て札再シャッフルもここ)
  3. Random は引数注入、ドメイン副作用ゼロ (テスト時は決定的シードで検証)
  4. TrapLifetime を sealed で物理/魔法の差を型表現 (boolean フラグにしない、驚き最小)
  5. すべての List フィールドは compact constructor で `List.copyOf` (defensive copy)

  **数値定数 (CardPileState 内 `static final`)**:
  - `MAX_HAND_SIZE = 9`
  - `INITIAL_DRAW_CAP = 3`
  - `DRAW_CONVERGENCE_DECK_SIZE = 6`

- **Consequences**:
  - + KISS / 不変性 / 副作用分離 / sealed 網羅性 を守れる
  - + テスト容易 (決定的シードで山札シャッフル検証可)
  - + 既存 `SkillEffect` (sealed) と並列で共存 (統合しない、§15-3 のスキル枠 + デッキ併存を尊重)
  - − **依存事項 A〜E** が別途必要 (本 ADR では `core.domain.card/` のみ実装):
    - **A**: `ActionPoints` 蓄積→使い切り型書き換え (§15-3 ターン終了条件)
    - **B**: `Stats` 6 ステ化 (物攻/魔攻/物防/魔防 追加、ダメージ計算で使う)
    - **C**: `Direction8` 新設 (罠の周囲 8 方向用、`Direction` は 4 方向のまま残す)
    - **D**: `BattleAction.UseCard(int handIndex, Direction)` 追加 + `TurnEngine` switch 全網羅修正
    - **E**: `core.domain.equipment.Equipment` 新設 (装備固有カードを `List<CardId>` で持つ)
  - − E-1 単体では「ゲーム内で動くカード」にはならない。動かすには A〜E の少なくとも一部が必要

- **次セッションでの作業順** (圧縮後の Claude が読む想定):
  1. ブランチ `feat/#12/E-1-card-skeleton` をチェックアウト
  2. 上記 11 ファイルを `core/domain/card/` に Write (`domain-architect` Agent に再依頼してもよい)
  3. `test-writer` Agent で `src/test/java/core/domain/card/` 配下のテスト追加 (検証ポイント 6 種):
     - `Card.apCost = 0` で例外 / `>= 1` で成功
     - `Hand` の 10 枚目 add で例外
     - `CardPileState.initialDrawCount`: deck=1→1, deck=3→3, deck=5→3, deck=6→5, deck=10→5
     - `drawN` で山札切れ時に Random で再シャッフル (固定シードで決定的検証)
     - `CardEffect` sealed switch 網羅性
     - `TrapLifetime.Turns(remaining=0)` で例外
  4. `gradlew test` で既存 61 件 + 新規 ~15 件全 PASS 確認
  5. `/architect-review` で最終レビュー (A or B 判定なら次へ)
  6. `/japanese-pr-create draft` で Draft PR
  7. 別 Issue を立てて依存 A〜E を順次着手

- **Reference**: [Issue #12](https://github.com/NPCdotcom/DDD-Jyogi-Kuroto-F/issues/12), [docs/GAME_DESIGN.md §15-3](../../docs/GAME_DESIGN.md), [.claude/agents/domain-architect.md](../../.claude/agents/domain-architect.md), domain-architect Agent 出力 (本セッション、2026-05-14)

## ADR-17: ダメージ計算は CardEffect.Damage.resolve に置く + fixture 新フィールドは暫定 0 埋め

- **Status**: Accepted (3 サブエージェント並列レビュー結論、ユーザー承認済)
- **Date**: 2026-05-14
- **Context**: ADR-02 で「Stats を 6 ステ化、ダメージ計算は `max(1, 基礎値 + 物攻 - 物防)`」と確定したが、計算ロジックをどこに置くかは未定。設計時の候補 4 案:
  - (a) `Stats` 内 static method
  - (b) `core.domain.battle.DamageFormula` 新クラス
  - (c) `TurnEngine` 内 private method
  - (d) `CardEffect.Damage` record にメソッド追加

  3 サブエージェント (domain-architect 自己再評価 / general-purpose docs 横断 / final-architect 8 原則) で並列検証した結果、(b) と (d) の対立があり、final-architect が「battle → card は層越境、card 層内に閉じる (d) が依存方向クリーン」と指摘。実コード調査で `core.domain.battle` は現状 `core.domain.card` を一切 import していないことが確認された。
- **Decision**: 以下を採用
  1. ダメージ計算は **`CardEffect.Damage.resolve(Stats attacker, Stats defender, CardElement element)`** メソッドとして `CardEffect.Damage` record 内に置く ((d) 案)
  2. `core.domain.battle.DamageFormula` 新クラスは **作らない** (KISS、層越境回避)
  3. `Stats` 内 static method は **作らない** (entity → card 逆方向依存を回避)
  4. `SkillEffect.Damage` は **触らない** (Skill = 固定ダメ継続、対称性確保 = `SkillEffect.Damage.amount` をそのまま使う)
  5. `Stats` に `withPhysicalAttack` 等の `with*` メソッドは **本 PR で追加しない** (バフ適用 Issue で必要時に追加、YAGNI)
  6. fixture (`DomainFixtures.java:63,78` / `InitialStateFactory.java:77,87`) の新フィールド 4 つは **暫定 0 埋め**。プレイヤー / 敵のキャラビルド数値は別 Issue で再設計 (本 PR スコープ外)
- **Consequences**:
  - + 依存方向が `battle → card → entity` のクリーンな単方向に保たれる
  - + `CardEffect.Damage` 自身が「自分のダメージ確定」責務を持つ自然な設計
  - + Skill 経路と Card 経路の意味論分離が明確 (Skill=固定ダメ、Card=計算式)
  - + fixture 暫定 0 埋めにより既存 TurnEngineTest 全 PASS を保てる (`max(1, 15+0-0) = 15` で等価)
  - − キャラビルド数値設計が別 Issue になり、ゲーム内バランスは本 PR で確定しない
  - − ADR-16 の `CardEffect.java` Javadoc 「application 層の解決器 (TurnEngine 等) が Stats と組み合わせて算出」と若干齟齬。実装時に Javadoc を「CardEffect.Damage.resolve が算出、TurnEngine は結果を反映」に書き換える
  - − Issue #15 のスコープが「Stats 7 引数化 + CardEffect.Damage.resolve + テスト」に狭まる (DamageFormula 廃案、with\* 廃案、fixture 数値設計は別 Issue)
- **Reference**: ADR-02, ADR-16, [Issue #15](https://github.com/NPCdotcom/DDD-Jyogi-Kuroto-F/issues/15), 3 サブエージェント並列レビュー出力 (本セッション、2026-05-14)

## ADR-18: CardPileState は Player record に内蔵 + UseCard は Damage カードのみ実装

- **Status**: Accepted (3 サブエージェント並列レビュー結論、main session 統合判断、ユーザー承認済)
- **Date**: 2026-05-14
- **Context**: 依存事項 D (BattleAction.UseCard + TurnEngine カード解決統合) の最大の設計分岐 = `CardPileState` の所有者をどうするか。初版 (domain-architect) は **案 Y (DungeonState に Map で集約)** を推奨したが、3 並列レビューで final-architect と domain-architect 自己再評価が **案 X (Player に内蔵)** を推奨。general-purpose は ADR-16 の「戦闘中の動的状態を局所閉包」を根拠に案 Y を推奨し、3 観点で意見分岐。

  分岐点の整理:
  - **案 X (Player に CardPileState 内蔵)**: `Player(ActorId, Position, Stats, ActionPoints, SkillSlot, Soul, CardPileState)` 7 引数化
  - **案 Y (DungeonState に Map で集約)**: `DungeonState(..., Map<ActorId, CardPileState>)` 5 引数化
  - **案 Z (PlayerCards 中間 record)**: 1:1 関係を 1 階層余分に包む、却下

- **Decision**: 以下を採用
  1. **案 X (Player に CardPileState 内蔵)** を採用 — 投票 2 対 1、実測コスト最小、ドメイン意味論的整合
  2. `Player` record を 6 引数 → 7 引数化 (`CardPileState cardPileState` 追加)、`withCardPileState` 1 メソッド追加、既存 4 with* で新フィールド伝播
  3. `BattleAction.UseCard(int handIndex, Direction direction)` を sealed permits に追加 (`direction` は本 Issue では Damage カードの隣接攻撃方向指定で必須)
  4. **Damage カードのみ実装**、Move/Buff/Trap は `BattleEvent.ActionRejected` で「未実装のカード効果」を明示 (本 Issue スコープ外、別 Issue で実装)
  5. `BattleEvent.CardUsed` は **新設しない**、既存 `SkillUsed(ActorId, String)` を流用してカード表示文言 (`card.displayName()`) を渡す (YAGNI)
  6. `TurnEngine.resolveDamageToEnemy/Player` を `int finalDamage` 受け取り形に統一 (DRY、Skill 経路と Card 経路で共有)
  7. `CardPileState.empty()` static factory を追加 (Player 初期生成時に空デッキ状態を渡せるよう)
  8. `InitialStateFactory.newPlayer` / `DomainFixtures.playerAt` を 7 引数化、`CardPileState.empty()` を渡す (Deck 接続は別 Issue / E-5 Equipment で後付け)
  9. TurnEngineTest に UseCard 系 6 件追加: 正常系 / AP 不足 / 対象不在 / Hand 範囲外 / MAGICAL 属性ダメ計算 / 敵 UseCard reject

- **Consequences**:
  - + 依存方向が `entity → card` 追加 (既存 `entity → skill` の前例ありで驚き最小)
  - + 実装コスト最小 (実測: `new Player(...)` 直接呼出 2 箇所 + `with*` 内部 4 箇所伝播)
  - + Player の「持ち物」「戦闘中状態」が record 内に一元化、§15-9 Equipment 系列との所有レイヤー統一
  - + Skill 経路と Card 経路の対称性確保 (固定ダメと計算式ダメの差を `int finalDamage` 受け取り形で吸収)
  - − `Player` record が 6 → 7 引数化 (fat record の懸念だが、§15 全体で見れば必然)
  - − 戦闘終了時に CardPileState を `empty` 化するロジックが将来必要 (現状はラン中ずっと保持で OK、明示クリアは別 Issue)
  - − general-purpose 指摘「Deck=永続 / CardPileState=戦闘中限り」の二層分離は、本 PR では `CardPileState` のみ Player に持たせ、Deck (永続) は別 Issue / E-5 Equipment で後付けする形で両立 (Deck 接続は本 PR スコープ外)
  - − Move/Buff/Trap カードを本 Issue で reject するため、E-1 単体ではまだゲーム内で「攻撃カードのみ」しか使えない (Move/Buff/Trap は別 Issue で順次)

- **Reference**: ADR-05 (戦闘モード境界廃止), ADR-16 (E-1 カード設計), ADR-17 (Damage.resolve 配置), [Issue #18](https://github.com/NPCdotcom/DDD-Jyogi-Kuroto-F/issues/18), 3 サブエージェント並列レビュー出力 (本セッション、2026-05-14)

## ADR-19: 毎ターンドロー 1 枚を本日実装 / 移動カード化は明日以降に E-5 装備とセットで実装

- **Status**: Accepted (3 サブエージェント並列レビュー結論、ユーザー承認済)
- **Date**: 2026-05-14
- **Context**: M1.5 コア機能 (PR #21) 完成直後、リーダーから 2 つの追加要望が来た:
  - (1) **毎ターン (プレイヤーターン頭) ドロー** — §15-3 「ターン終了条件: AP 切れ or 手札切れ」「Slay the Spire のエナジー近似」を踏まえると StS 流の毎ターン補充ドローが暗黙仕様
  - (2) **移動のカード化** — §15-5 「移動カードを切らないと動けない (Slay the Spire 純粋路線)」(L534) で明文化、§15-9 「ぼろ靴 (固有: 移動カード)」(L711) で初期装備設計

  ADR-18 では Move/Buff/Trap カード実装を「本 PR スコープ外、別 Issue で順次」と保留中。今回の要望はこの「別 Issue」の前倒し相当。

  3 並列レビュー (domain-architect / final-architect / general-purpose) の意見:
  - **毎ターンドロー**: 全員賛成、§15-3 / StS 準拠、実装規模小 (TurnEngine + TurnDirector + DddGame で 30 行程度)
  - **移動カード化**: 意見分岐 (X 完全廃止 / Y 共存 / 今日見送り)。final-architect は「ADR-18 と矛盾、E-5 装備 (ぼろ靴) + EnemyAi の Move 利用調査 + sealed permits 改修が連鎖し軽量修正にならない」と保留推奨

- **Decision**: 以下を採用
  1. **毎ターン 1 枚ドローを本日実装** — `TurnEngine.startPlayerTurn(DungeonState, Random)` に Random 引数を追加し、内部で `player.cardPileState().drawN(1, rng)` を呼ぶ。AP リフィル + ドローを 1 メソッドで担う
  2. **API 破壊許容** — `startPlayerTurn(state)` → `startPlayerTurn(state, Random)` は ADR-16「Random 引数注入」と整合するため許容。caller の `TurnDirector` / `DddGame` も Random を保持・注入する
  3. **ドロー枚数は 1 枚固定** — §15-3 仕様で「毎ターンドロー枚数」は未明文。1 枚固定が最小実装、Hand 上限 (`MAX_HAND_SIZE=9`) で自然に停止。将来「速度ステ連動」「StS 流 5 枚 (initialDrawCount 経由)」への拡張は別 Issue
  4. **山札切れ時の挙動** — `drawN` 内で自動再シャッフル (CardPileState.drawN 既設、§15-3 通り)
  5. **移動カード化は本日見送り、明日 5/15 以降に E-5 装備とセットで実装** — 仕様は §15-5 で明文化されているが、E-5 (ぼろ靴 = 装備固有カードで初期デッキ構成) + EnemyAi の Move 利用調査 + `BattleAction.Move` 廃止 (or プレイヤー側だけ reject) + `PlayerInputs.pollNormalMode` WASD 経路改修と連鎖。軽量修正のスコープを超えるため別 Issue 起票
  6. **移動カード化の最終案は別 ADR で確定する** — 案 X (完全廃止) / 案 Y (WASD = 移動カード自動プレイ) / 案 Z (ショートカット) のうちどれを採るかは、E-5 装備実装時に再評価して新 ADR で記録

- **Consequences**:
  - + StS 風カードゲームのテンポが今日のうちに成立 (毎ターンで手札補充が見える)
  - + ターン終了条件「AP 切れ or 手札切れ」の手札切れ側が現実的な閾値になる
  - + `TurnEngine` 純関数性は保たれる (Random 引数注入で副作用分離維持、ADR-16 と整合)
  - + 移動カード化を別 Issue にしたことで ADR-18 (Move/Buff/Trap は別 Issue) と矛盾せず、計画的に着手可能
  - − 移動は当面 WASD/矢印で動かせる状態が続き、§15-5 「移動カードを切らないと動けない」とは仕様乖離 (明日以降に解消予定)
  - − `TurnDirector` コンストラクタ API が変わる (`new TurnDirector(context)` → `new TurnDirector(context, Random)`)、`DddGame.startNewRun` が `new Random()` を渡すため毎ラン異なるシード = 再現性は失われる (テストでは固定シード `Random(42)` を渡す)

- **Reference**: §15-3 (L443-513), §15-5 (L529-569), ADR-01 (AP 使い切り型), ADR-16 (E-1 設計), ADR-17 (Damage.resolve), ADR-18 (Move/Buff/Trap は別 Issue)、3 サブエージェント並列レビュー (本セッション、2026-05-14)

## ADR-20: M1.5 残仕様の一括確定 (移動 α 案 / 装備折衷 / 解像度 1920×1080 / 音タグ予約 / Android デモ動画代替)

- **Status**: Accepted (3 サブエージェント並列レビュー結論 + ユーザー承認済)
- **Date**: 2026-05-14
- **Context**: M1.5 コア機能 (PR #21〜#23) 完成 + 毎ターンドロー実装後、リーダー (ユーザー) からチーム共有前提で 5 つの仕様確認・決定事項。3 並列レビュー (libgdx-implementer / domain-architect / general-purpose) で各論点を検証し、ユーザーが最終確定。
- **Decision**:
  1. **移動仕様 = 案 α** — 「移動カード 1 枚を切る → そのカードの `CardEffect.Move(distance)` の `distance` ぶん AWSD で連続移動権を得る」。`UseCard(handIndex, dir)` で「移動権取得」、その後 `PlayerInputs` の状態 (`pendingMoveCount`) を持ち、AWSD で 1 マスずつ消費。残カウント 0 で通常モード復帰。`§15-5` L534「移動カードを切らないと動けない」純粋路線を維持しつつ、方向操作の直感性を確保。案 β (WASD 自動プレイ) は「切る意識」が消えるため不採用、案 X (毎マス UseCard) は操作性最悪で不採用、案 γ (両刀共存) は §15-5 違反で不採用
  2. **装備フォーマット = 案 B (折衷)** — `Equipment(EquipmentId id, String displayName, EquipmentSlot slot, StatsBonus statsBonus, List<CardId> grantedCards)` の 5 引数 record。`grantedCards` は **空リスト可** (ステ補正のみの装備も許容、§15-9 の「装備固有カード」と整合)。装備の個性は「ステ補正の組み合わせ」 + 「装備固有カードの有無」の 2 軸で表現。初期装備例: ぼろ靴 (速度+1, grantedCards=[移動カード ID]) / ぼろい短剣 (物攻+1, grantedCards=[斬撃カード ID]) / 革の鎧 (HP+5, grantedCards=[]) など。チームメンバー案「ステ補正のみ」は「特定装備で grantedCards=空」で表現可能
  3. **画面解像度 = 1920×1080 (16:9) を今日中に着手** — `DesktopLauncher.setWindowedMode(1920, 1080)` + `setResizable(true)`、`FitViewport(1920, 1080)` で全 Screen を統一、`resize()` メソッドで `viewport.update(width, height, true)` 呼出。`RenderLayout` 定数を 1920×1080 基準に再計算、絶対座標箇所 (TitleScreen / GameOverScreen) を比率ベースに置換。素材作成は **1920×1080 基準** で。実装は本セッションで開始 (E-6 ポップアップ UI 基盤の前提整備)
  4. **音タグ仕様 = M2 以降に予約** — `docs/GAME_DESIGN.md` には明文化されていないが、ユーザー提案として「主タグ (ATTACK/MOVEMENT/BUFF/TRAP) × 副タグ (PHYSICAL/MAGICAL) で SE を出し分け」を将来仕様として記録。本セッションでは実装しない。BGM / SE は本番デモまでに 2〜3 個あれば映え◯
  5. **Android 方針 = Desktop 基本 + デモ動画代替** — §15-12 「Doko-demo (クロスプラットフォーム実演)」は本番デモの訴求軸だが、Android backend は未着手で 3〜5 日要する。**Desktop 単独で機能完成度を優先**、Android はビルドが通れば本番ライブで見せる、無理なら録画動画でクロスプラットフォーム感を補足。§15-12 のセーブ続きデモは Desktop 2 セッションで代替可能
- **Consequences**:
  - + 移動 α 案により §15-5 純粋路線 + 直感操作の両立、`UseCard` API 拡張不要 (`PlayerInputs` 状態追加のみ)
  - + 装備 B 案により §15-9 仕様準拠 + チームメンバー案 (ステ補正のみ) を「grantedCards=空」で表現可能、柔軟性最大
  - + 解像度を今日着手することで E-6 (ポップアップ UI) 着手時に解像度トラブル回避、素材作成基準も早期確定
  - + 音タグ予約により今は他機能に集中可能
  - + Android 方針確定でデモ準備にリソース集中、本番までに無理な実装を避けられる
  - − 移動 α 案は `PlayerInputs` の状態管理が複雑化 (現状の 2 ステート: 通常 / カード選択中 → 3 ステート: 通常 / カード選択中 / 移動権保持中)。テスト網羅必須
  - − 装備 5 引数 record で `Player` も `Optional<Equipment>` を持つことになり、`Player.finalStats()` で合算ロジックが必要 (E-5 実装時に対応)
  - − 解像度変更で TitleScreen / GameOverScreen / HudRenderer の絶対座標を比率化する作業発生 (libgdx-implementer 見積 標準 2〜3h)
- **Reference**: §15-1 (L405-419 解像度), §15-5 (L529-569 移動), §15-9 (L697-714 装備), §15-12 (L732-755 デモ), ADR-03 (ポップアップ UI), ADR-04 (1920×1080 確定), ADR-08 (装備 1 部位 / 耐久なし), ADR-18 (Move/Buff/Trap 別 Issue), ADR-19 (毎ターンドロー + 移動カード化予告), 3 サブエージェント並列レビュー (本セッション、2026-05-14)、ユーザー最終確定 (装備=B 案)

## ADR-21: Move カード実装 + 移動 α 案 PlayerInputs ステート (Player.pendingMoveCount 保持)

- **Status**: Accepted (3 サブエージェント並列レビュー結論、ユーザー承認済)
- **Date**: 2026-05-14 (深夜継続セッション)
- **Context**: ADR-20 で「移動 α 案: カード切る → distance ぶん AWSD で連続移動」を確定済だが、状態管理の置き場 (PlayerInputs 側 / Player record 側 / 両側) は未確定。3 並列レビューで:
  - domain-architect: PlayerInputs 側 (ドメイン非侵食、案 ii)
  - libgdx-implementer: 両側 (Player.pendingMoveCount をドメインで持つ、案 Z)
  - final-architect: スコープを Move のみに絞れ、Buff/Trap は明日

  libgdx-implementer の指摘「セーブ整合 + AP 切れ自動ターン終了との競合回避」が決定的。`TurnDirector.autoEndPlayerTurnIfApDepleted()` が AP 0 で発火し pendingMoveCount を宙ぶらりんにする競合状態を避けるため、ドメイン側 (Player) に状態を持つ判断。
- **Decision**: 以下を採用
  1. **`Player` record 8 引数化** — `int pendingMoveCount` を追加、`withPendingMoveCount(int)` 提供。compact constructor で 0 以上検証
  2. **`TurnEngine.applyPlayerUseCard` の `CardEffect.Move` case 実装** — reject 解除、`pendingMoveCount = distance` を Player に設定 + AP cost ぶん消費 + Hand→Discard 移動 (UseCard の標準処理)。**カード使用時点では移動しない** (移動は次の AWSD 入力で発生、距離 1 のカードでも UseCard 直後に AWSD が必要)
  3. **`TurnEngine.applyPlayerMove` の AP スキップ分岐** — `pendingMoveCount > 0` なら AP 消費せず 1 マス移動 + pendingMoveCount-- に。0 なら従来通り AP 1 消費の通常移動
  4. **途中ブロックは reject 統一** — distance 2 で 2 マス目に壁 / 敵がある場合、移動キー押下を reject (AP 消費なし、pendingMoveCount も減らない)。final-architect「驚き最小」推奨
  5. **`BattleEvent.MovementGranted(ActorId, int remainingSteps)` 新規追加** — UseCard(Move) で発火、HudRenderer が拾って「移動権 残 N 歩」表示
  6. **`PlayerInputs` 3 ステート拡張** — 状態 2 = `state.player().pendingMoveCount() > 0` で WASD = `BattleAction.Move(direction)` (TurnEngine 側で AP 0 処理)。ESC でドメインに「pendingMoveCount リセット」アクションを送る (AP は戻さない、移動権破棄は YAGNI で AP 戻し回避)
  7. **HudRenderer.drawHand** — `pendingMoveCount > 0` のとき画面に「移動権 残 N 歩」を CYAN で追加表示
  8. **スコープは Move のみ** — Buff (`Player.activeBuffs`) と Trap (`DungeonState.placedTraps`) は明日 5/15 以降の別 PR、本 PR では引き続き reject 維持
- **Consequences**:
  - + §15-5 「移動カードを切らないと動けない」純粋路線を達成
  - + dash カードがゲーム内で動作 (Damage 系のみだった現状から拡張)
  - + セーブ時に pendingMoveCount が自然に永続化される (層単位セーブ、§15-11 と整合)
  - + AP 切れと移動権の競合を完全に解消
  - + ADR-20 の移動 α 案を実装完了
  - − `Player` record が 8 引数化、record 引数 9 超え見直しタイミングと隣り合わせ (装備 E-5 追加で 9 引数 = 見直しライン)
  - − Buff/Trap は引き続き reject、テンプレ提示の iron_skin / spike_trap は死札のまま (明日解決)
  - − `BattleAction.Move(direction)` 自体は変更なし (AP 消費判定は TurnEngine 側に集約) で互換性維持、ただし TurnEngine の applyPlayerMove ロジックは pendingMoveCount 分岐で複雑化
  - − DomainFixtures / InitialStateFactory の `new Player(...)` 呼出 2 箇所を 8 引数版に更新が必要 (デフォルト pendingMoveCount=0)
- **Reference**: §15-5 (L529-569), ADR-18 (CardPileState を Player に内蔵), ADR-19 (毎ターンドロー + 移動カード化予告), ADR-20 (移動 α 案 / 装備 B 案 / 解像度確定), 3 サブエージェント並列レビュー (本セッション深夜、2026-05-14)、libgdx-implementer 指摘 (セーブ整合と AP 切れ競合回避)

## ADR-22: Trap カード実装 (DungeonState 5 引数化 + PlacedTrap record + 踏み判定 + ライフタイム管理)

- **Status**: Accepted (3 サブエージェント並列レビュー結論、test-writer による微修正含む)
- **Date**: 2026-05-14 (深夜継続)
- **Context**: ADR-21 で Move カードを完成させた後、ADR-18 / ADR-21 で「Move/Buff/Trap の本実装は別 Issue」と保留中の Trap を実装する。テンプレ (docs/templates/cards.json) で提示済の `spike_trap` を実動作させ、§15-3 「タグ × 属性」のうち TRAP × PHYSICAL/MAGICAL を網羅する。3 並列レビューで:
  - domain-architect: 案 P (DungeonState 5 引数化、`List<PlacedTrap>`) 推奨
  - final-architect: Gold 単独推奨 (Trap は Player 触らないが DungeonState 拡張のリスクあり、深夜帯非推奨)
  - general-purpose: A + B 推奨 (テンプレ完全動作)

  Gold (D) を 30 分で先行完了したため余力ありと判断、domain-architect 推奨に従い Trap 単独実装に着手。
- **Decision**: 以下を採用
  1. **`PlacedTrap` record 新設** — `(Position position, int baseValue, TrapLifetime lifetime, CardElement element)` の 4 引数。`decrementedLifetime()` / `isAlive()` / `resolveDamage(Stats victim)` を提供
  2. **`DungeonState` 5 引数化** — `List<PlacedTrap> placedTraps` 追加、`withPlacedTraps`、`findTrapAt`、**4 引数互換コンストラクタ** で既存呼出を破壊しない
  3. **`TurnEngine.applyPlayerUseCard` の Trap case 実装** — `reject` 解除 → `resolveCardTrap`、設置先 walkable チェック、同座標重複は **上書き** (3 並列レビュー結論「驚き最小 = 最新が優先」)、AP 消費 + Hand→Discard
  4. **`TurnEngine.applyPlayerMove` / `applyEnemyMove` で踏み判定** — `checkAndTriggerTrap` 共通ヘルパ、player/enemy 両経路で罠検出 + ダメージ適用 + UntilStepped 除去 / Turns 維持
  5. **`TurnEngine.startPlayerTurn` で Turns ライフタイム管理** — `Turns(N)` 罠を `Turns(N-1)` にデクリメント、0 で除去。`UntilStepped` 罠は据置 (踏まれるまで永続)
  6. **罠ダメージ計算** — `PlacedTrap.resolveDamage(Stats victim)` で `max(1, baseValue - 物防 or 魔防)` (element に応じた防御参照)。**設置者ステ依存なし** (KISS、設置時スナップショット保存の複雑性を回避)
  7. **`BattleEvent.TrapPlaced` / `BattleEvent.TrapTriggered` 新規追加** — sealed permits 拡張、HudRenderer の switch で日英文言対応
  8. **`TrapLifetime.Turns` の制約緩和** — `remaining < 1` で例外 → `remaining < 0` で例外に緩和。理由: `decrementedLifetime()` が `Turns(1)` を `Turns(0)` (期限切れ中間値) に変換するため、設置時 (CardEffect.Trap の compact constructor) で `>= 1` を保証しつつ内部中間値 `0` を許容する KISS 判断。`TrapLifetimeTest` の該当ケース (`turnsRemainingZeroThrowsIllegalArgumentException`) を `turnsRemainingZeroIsAcceptedAsExpiredIntermediateValue` に書き換え
- **Consequences**:
  - + テンプレ提示済 `spike_trap` が実動作、§15-3 4 タグ × 2 属性 = 8 種のうち TRAP × PHYSICAL/MAGICAL が完全動作 (Buff のみ残)
  - + 敵 AI 経路でも罠踏み判定が機能 (敵を罠タイルに誘導する戦術が成立)
  - + DungeonState 4 引数互換コンストラクタで既存呼出 (DomainFixtures / InitialStateFactory) を破壊しない
  - + `Turns(0)` を中間値として許容することで `decrementedLifetime()` が単純化、ライフタイム管理が KISS
  - − `DungeonState` 5 引数化、`Player` も 8 引数のままで合計フィールド数は増加 (ただし `DungeonState` は引数見直しライン未到達、4→5 で余裕)
  - − 罠ダメージ計算が「設置者ステ無視」なので、強化系装備で罠を強化する余地がない (将来必要なら PlacedTrap にスナップショット追加で拡張)
  - − テスト追加 7 件 (`TurnEngineTest` 6 件 + `TrapLifetimeTest` 1 件書換)、合計 176 件 PASS
- **Reference**: §15-3 (L443-513), ADR-16 (E-1 設計、Turns 制約を本 ADR で緩和), ADR-18 (Move/Buff/Trap 別 Issue 予告), ADR-21 (Move 実装、本 ADR-22 は連続実装の続き), 3 並列レビュー (本セッション深夜、2026-05-14)、test-writer による Turns(0) 中間値検出
