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
