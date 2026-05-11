# プロジェクト概要

> ゲーム仕様の詳細は [GAME_DESIGN.md](./GAME_DESIGN.md) を参照。本ドキュメントは **プロジェクトの位置付け（why / who / when）** に絞る。

## 1. コンセプト
- スローガン: **「誰でも | どれでも | どんな | 場所でも」 動くゲーム**
- 「D」の意味: **Doko-demo（どこでも動く）**
- 動機: 部内ハッカソンの定番フォーマット（JS + Web）から外し、Java でマルチプラットフォーム対応に挑戦する
- 差別化軸: 「Java単体で PC・スマホで動く」という技術的インパクトを審査員に訴求
- ターゲット: 情報機器を持っているすべての人（第一）/ 部内ハッカソン審査員（第二）
- 詳細: [GAME_DESIGN.md §1](./GAME_DESIGN.md)

## 2. ゲーム性（サマリ）
- ジャンル: **ローグライト**（ローグライク + メタ進行）
- コア体験: **「死を繰り返して強くなる」**
- 1プレイの長さ: **3〜5分**
- 動的生成する要素: マップ / 敵配置 / アイテム / UIレイアウト（優先順）
- 戦闘システム・死亡継承・ソウル経済などの詳細: [GAME_DESIGN.md §4–§6](./GAME_DESIGN.md)

## 3. 技術スタック（サマリ）
- 言語: **Java 25 LTS (Oracle JDK 25)**
- フレームワーク: LibGDX
- ビルド: Gradle (Wrapper 同梱)
- IDE: IntelliJ IDEA Community 推奨
- 配布形式: **Desktop (Win/Mac/Linux) + Android**（Web/iOS はスコープ外）
- 詳細: [GAME_DESIGN.md §3](./GAME_DESIGN.md) / [TechSelectionMemo.md](./TechSelectionMemo.md)

## 4. やらないこと（MVP段階のスコープ外）
- 動的マップ生成
- Android対応
- 動的UI/UX演出（テーマ変動、警告演出など）
- サウンド
- 装備システム
- ソウルの使用UI（スキル習得 / 枠拡張 / ステ強化）
- マルチエンディング
- セーブ機能の作り込み
- ※ MVP後に段階的に取り込む。詳細は [GAME_DESIGN.md §11-3](./GAME_DESIGN.md)

## 5. 成功条件

### MVP（今週金曜）
- [ ] 2Dダンジョン1階層 が描画される
- [ ] プレイヤーが移動・戦闘できる（AP制）
- [ ] 死亡判定が動く
- [ ] 死亡時にソウルが残る最小ループが回る
- [ ] Desktop（Windows）で起動する

### 最終（5/24 提出時）
- [ ] Win/Mac/Linuxで起動する
- [ ] 1プレイが完結する
- [ ] デモで3〜5分間見せられる
- [ ] 公開できると良き

## 6. リスクと対策
- リスク1: 「誰でもどれでも」の穴を突かれて動かないものを出される可能性
  - 対策: すべての環境を想定し、Desktop + Android の動作テストを CI 化する（build.gradle 追加後）
- リスク2: Git スキル差による事故（誤マージ、競合解消ミス）
  - 対策: ブランチ保護 + PR テンプレ + Git 初心者向けチートシート（[BranchingStrategy.md §5](./BranchingStrategy.md)）
- リスク3: 仕様の認識ズレ
  - 対策: GAME_DESIGN.md を Single Source of Truth とし、議論は HackMD、確定は PR で本リポジトリへ
