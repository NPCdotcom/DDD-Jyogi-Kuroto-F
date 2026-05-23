# ai_log/lessons.md

> AI (＝Claude) が起こした判断ミス・修正パターンの記録。
> ユーザーから受けた指摘・サブエージェントレビューの結果・自身の失敗を 1 行ルール化する。
> チーム共有資産ではない (AI 運用専用)。

---

## 形式

```
### YYYY-MM-DD: 短いタイトル

- **状況**: 何をしていたか
- **指摘 / 失敗**: 何を間違えたか
- **学び**: 次回どう動くべきか（一般化したルール）
- **適用範囲**: いつこのルールが効くか
```

---

## エントリ

### 2026-05-12: ビルドツールの常駐確認をしてから依存を書く

- **状況**: MVP の build.gradle を書こうとした
- **判断**: ローカルに java / gradle が未インストールでも、依存記述自体は静的に書ける
- **学び**: ローカル実行環境とソース成果物は別レイヤー。コードレビュー段階では「動くか」と「書かれている内容が妥当か」を分けて検証する
- **適用範囲**: 依存管理ファイルを更新するときすべて

### 2026-05-12: メインリポジトリで直接作業する（worktree を使わない）

- **状況**: Claude Code が自動で worktree を作成した状態でセッションが始まった
- **方針**: ユーザー指示・グローバルメモリで「worktree を使わず通常のリポジトリディレクトリで作業する」と明示
- **学び**: ファイル操作は `C:\.program\DDD-Jyogi-Kuroto-F\` への絶対パスで行う。worktree 側を変更しない
- **適用範囲**: 本プロジェクトの全セッション

### 2026-05-12: ビルドツールの「Java 対応バージョン」を最初に確認する

- **状況**: Java 25 + Gradle 8.14 で wrapper 生成を試したら `Unsupported class file major version 69` で失敗
- **指摘 / 失敗**: Gradle 8.14 のリリースノートには「Java 24 support」と明記されていた。最新版を選んだつもりでも、対象 JDK の major version をチェックしないと事故る
- **学び**: 新 JDK を使う場合は「ビルドツール (Gradle / Maven) の対応 Java バージョン」と「プラグイン (Spotless 等) の対応 Java/Gradle バージョン」を最初に表で確認する。今回は Gradle 9.5 + Spotless 7.2.1 が Java 25 対応の正解
- **適用範囲**: JDK / Kotlin / Scala など、最新版を採用する判断をする度に

### 2026-05-12: メジャー越えと「2.x 系の最新パッチ」を分けて検討する

- **状況**: 依存ライブラリの最新版を調査したら、JUnit は **5.12.2** (現行系) と **6.0.3** (新メジャー) が並走、Jackson も **2.21.3** (`com.fasterxml...`) と **3.1.3** (`tools.jackson...`) の二系統が同時にメンテされていた
- **学び**: 「最新版」と一括りにせず、(a) 現行マイナー/パッチの最新 と (b) 新メジャー線 を分けて把握する。MVP / 業務側コードの安定性を優先するなら (a)、新機能 API を取り込みたい時だけ (b) を選ぶ
- **適用範囲**: あらゆる依存ライブラリの更新判断。バージョン跨ぎは package 名や groupId が変わることが多く、コスト評価が別タスクになる

### 2026-05-12: Spotless 7→8 / google-java-format 1.22→1.35 の境界で生じるスタイル差

- **状況**: Spotless 8.4.0 + GJF 1.35.0 を入れ直したら `spotlessCheck` が 24 ファイル分の差分を返した
- **内容**: Javadoc 内 `<ul>` の前に空行を強制する、連続コメント行を 1 行に折り畳む、フィールド宣言の前後に空行を入れる等
- **学び**: GJF のマイナーアップでも整形ルールは静かに変わる。バージョン上げる時は必ず `spotlessApply` → 差分目視 → テスト の順で確認する。CI で `spotlessCheck` を強制している間は、移行 PR で必ずスタイル差分のみのコミットを分離する
- **適用範囲**: Spotless / Prettier / Black など、フォーマッタを CI で強制している全プロジェクト

### 2026-05-12: 外部素材の取得先は「公式 or ユーザー承認済み」のみ

- **状況**: Noto Sans JP フォントを取得するため、jsdelivr → GitHub raw → frappe/fonts の順に試行。frappe/fonts (第三者ミラー) で取得を試みたところ、permission deny。
- **指摘 / 失敗**: 公式リポジトリでファイルが見つからなかった (404) と分かった時点で「探索の幅を広げる」のではなく「ユーザーに公式取得を依頼する」べきだった。第三者ミラーは改竄リスクがゼロではなく、ライセンス上問題なくてもセキュリティ的に許諾なしに使ってはいけない
- **学び**: コードに同梱されるバイナリ (フォント・画像・音) の取得元は「公式 1 次配布元」または「ユーザーが事前に承認した URL」のみ。当てが外れたら、私が代行先を変えるのではなく、AskUserQuestion で「公式取得手段」か「ミラー承認」かを選んでもらう
- **適用範囲**: 全プロジェクトの素材取得作業

### 2026-05-12: 複数 srcDirs の同名ファイルは Gradle 9 で fatal

- **状況**: `resources.srcDirs = ['src/main/resources', 'assets']` にして両方に `.gitkeep` があったため、`processResources` が `Entry .gitkeep is a duplicate` で BUILD FAILED
- **学び**: 複数 srcDirs を 1 つの sourceSet に束ねるときは、`tasks.withType(Copy).configureEach { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }` を必ず設定する。後から見落としやすい
- **適用範囲**: LibGDX に限らず、Gradle で `assets/` や複数 resource ディレクトリを統合するすべての場面

### 2026-05-12: 「チームに渡す」観点を持つレビューを別立てる

- **状況**: 3 回のサブエージェントレビュー (ドメイン / 全体 / コード再) はすべて「シニア視点のコード品質」だった。最後にユーザーから「チームメイト視点でレビューして」と依頼され、初めて docs/ の AI 丸投げ感や lessons.md の私物臭が露呈
- **学び**: 「外部から見た時に説明コストが高いか」「ドキュメントと実装が乖離していないか」は、コード品質レビューでは検出されない別軸。プロジェクトを誰かに渡す節目では **チームメイト視点 / 新入社員視点** のレビューを別途回す
- **適用範囲**: PR を出すとき、ハンドオフのとき、OSS 化のとき

### 2026-05-14: 整備プランは「整備対象の前提が成立しているか」を先に検証する

- **状況**: AI 開発体制整備 (`.claude/agents/skills/hooks/`) を develop ブランチで開始しようとした
- **指摘 / 失敗**: サブエージェント PM レビューが C 判定で「整備対象の Java ファイルが develop に 1 件も無い、テスト 0 件、tasks/ai_log/ も無い → Hook が空回りで偽の安心感」と指摘。整備プランは「mvp ブランチに MVP コードがある前提」で書いたが、整備先 (develop) は空状態だった
- **学び**: 整備系のタスク (CI / Lint / Hook / テンプレ) を開始する前に「整備対象 (=コード / テスト / 既存資産) が **そのブランチに存在するか**」を必ず確認する。前提が成立してなければ、整備の前に「前提合わせ」フェーズを 1 つ挟む (今回は mvp → develop マージ)
- **適用範囲**: あらゆる「環境整備 / CI 設定 / Hook 導入 / テンプレ作成」系のタスク

### 2026-05-14: コンテキスト圧縮対策として handoff スナップショットを文書化する

- **状況**: Claude Code セッションが長くなり、auto-compaction が近づいた。ユーザーが「圧縮後も作業継続できるか」と懸念
- **判断**: サブエージェントで「直前会話を一切知らない新セッションの Claude」を想定して判定 → B 評価 (技術的には可能だが、直近意思決定 4 点が分散していて把握しづらい)
- **学び**: 長セッションで意思決定が積み重なるプロジェクトでは、**圧縮前に「handoff.md (スナップショット) + decisions.md (ADR 風)」を整備する**。圧縮後の新セッションが 2 ファイル読めば「今ここから何をすべきか」が即座に分かる構造を作る
- **適用範囲**: 1 セッションで複数の不可逆判断を行うプロジェクト全般 (ハッカソン / 中規模リファクタリング / OSS メジャーバージョンアップ等)

### 2026-05-14: Skill の `context: fork` で worktree が残留し cwd 汚染が連鎖する

- **状況**: `architect-review` Skill (`context: fork`) を起動後、後続の `japanese-pr-create` Skill (fork 指定なし) でも `claude/loving-proskuriakova-5c4dd1` worktree が cwd になり、`git diff origin/develop --stat` が「100 ファイル変更、6299 deletions」の偽差分を表示した
- **指摘 / 失敗**: Skill 内の `!`git status`/`git diff`` 出力をそのまま信頼して PR 作成していたら、develop の最新コードをほぼ全部消す災害的 PR になっていた。`context: fork` 指定が無い Skill でも、過去の fork で生成された worktree が残っていると cwd 汚染が後続 Skill にも連鎖する。さらに `git worktree remove --force` を試みても **Claude Code セッション中はファイルロックで Permission denied** になり、セッション終了まで残留する
- **学び**: (1) Skill 内の git 出力は信用せず、必ず main session 側で `git -C <main-repo-path> ...` の明示パスで再確認する。(2) `context: fork` 付き Skill 実行後は `git worktree list` で残留確認 → 不要なら `git worktree remove --force <path>` で削除する (セッション中に失敗したらセッション終了後に手動削除)。これは Claude Code 既知バグ ([GitHub Issue #40968](https://github.com/anthropics/claude-code/issues/40968))。(3) `gh pr create` / `git commit` 等の **不可逆操作を含む Bash 実行は必ず `-C <main-repo-path>` 付き** で行う
- **適用範囲**: `context: fork` を持つすべての Skill (現状 `~/.claude/skills/architect-review/` のみ)、および git 情報を出力する全 Skill (`japanese-pr-create` を含む)

### 2026-05-23: LibGDX `new FitViewport()` 直後の `viewport.update()` 漏れで描画が見えない

- **状況**: `SoulNodeUnlockDialog` のコンストラクタで `this.viewport = new FitViewport(1920, 1080, camera)` を呼んだ直後に `viewport.update(...)` を呼んでおらず、後で `viewport.apply()` した時に内部の `screenWidth/Height` が 0 のまま → `Gdx.gl.glViewport(0, 0, 0, 0)` で 0×0 領域に描画 → 画面に何も見えないが render() コード自体は走る
- **指摘 / 失敗**: 「ダイアログの render() が走っているのに見えない」という症状の真因を、最初は「描画順序」「描画完走の中断」「unproject 誤差」と仮定して 4 回別の修正を試した。実機ログで `render() call × 4` が出ていたのに見えない事実から「viewport 初期化漏れ」に辿り着くまで 3 セッション費やした
- **学び**: (1) LibGDX で**動的生成された Popup / Dialog** (LibGDX framework の `resize()` ライフサイクル外) は、コンストラクタで `viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true)` を必ず呼ぶ。(2) Scene2D の `Stage(viewport)` 経由なら Stage コンストラクタが内部で update() を呼ぶので不要。自前 `SpriteBatch + ShapeRenderer` 構成のときだけ要注意。(3) 「描画コードは走るが画面に何も出ない」症状 = ほぼ間違いなく `glViewport(0,0,0,0)` か `setColor` で透明化、見たら viewport の初期化を疑う
- **適用範囲**: LibGDX で `new FitViewport / ScreenViewport / ExtendViewport` 等を直接 new するすべてのコンポーネント (Screen サブクラス以外)

### 2026-05-23: ハッカソン直前の record signature 変更は M2 送り判断

- **状況**: A6 final-architect が「LayerEndNode.Shop が Card 直接保持、NodeEffect.CardGrantEffect は CardId + cardResolver、表現が非統一」と指摘。本セッションで統一する候補だった
- **指摘 / 失敗**: ハッカソン本番前日 (5/23) に record signature を変更すると、シリアライズパス・既存テスト・複数 callsite の連鎖修正が大規模になりリスクが大きい
- **学び**: ハッカソン本番直前 (T-24h 以内) の **record signature 変更 / sealed permit 追加 / コンストラクタ引数追加** は原則 M2 送り。すでに動いているテスト 545 件を緑に保つ方が優先。「驚き最小」の整合性は M2 で取る
- **適用範囲**: ハッカソン / 提出締切前の最終リファクタフェーズ全般

### 2026-05-23: domain 層のマスタ JSON 化で「依存方向違反」と「static API 互換」のトレードオフ

- **状況**: M2 Wave 2 Task A で SoulTree.allNodes() (23 ノード Java ハードコード) を tree.json + SoulTreeCatalog (infrastructure 層) に分離。新実装で `SoulTree.allNodes()` が `InitialStateFactory.soulTreeNodes()` を呼ぶ形になった
- **指摘 / 失敗**: 技術的には CLAUDE.md ルール「domain は他層に依存しない」の違反。ただし代替案 (Supplier 注入 / instance method 化) は既存 callsite 5+ 箇所 (SoulTree.unlock / SaveDataConverter / SoulTreeScreen / Fonts) を全て変更する大規模リファクタになる
- **判断**: 現状の static API 互換性 + 既存テスト 579 件互換を優先し、依存方向違反を**意図的に許容**。Wave 5 (DddGame 集約抽出時) で Supplier 注入パターン (`SoulTree.setNodeProvider(Supplier)` を `DddGame.create()` で呼ぶ) に置換する流れを M2 backlog に記録
- **学び**: 設計原則違反は「全部直す or 全部我慢」ではなく「コストと範囲を計算して段階的に解消」が正解。Wave 単位で「今やる / 後で」を明確化することで進捗と品質を両立できる。違反は lessons.md と m2_backlog.md に記録して忘却防止
- **適用範囲**: 大規模リファクタで静的 API の透過置換 vs 依存注入のいずれかを選ぶ判断全般 (特に Tier 1 ルール違反だが Tier 2 互換性を優先する状況)

### 2026-05-23: Resolver 単体ではなく Context record でラップする (NodeResolveContext パターン)

- **状況**: M2 Wave 3 Task A で LayerEndNode.Shop を `Card` 直接保持から `CardId + cardResolver` に変更する計画。最初の plan では `apply(Player, Function<CardId, Card>)` と直接 Function を引数にする案だった
- **指摘 (ユーザー)**: 「Function を直接渡すと将来 equipmentResolver や itemResolver を追加するとき LayerEndNode.apply の全 variant + 全 callsite の signature が破壊される。Resolver のペアを `NodeResolveContext` record でラップして 1 引数にすれば、将来の resolver 追加は record フィールド追加だけで signature 不変」
- **判断 (受け入れ)**: Task A の段階で `NodeResolveContext(Function<CardId, Card> cards, Function<EquipmentId, Equipment> equipments)` を新設し、すべての LayerEndNode variant が `apply(Player, NodeResolveContext)` を受ける設計に変更。これにより Task B (ShopEquipment) で context.equipments() を読むだけで本物の装備名表示ができ、追加引数の連鎖修正を避けられた
- **学び**: **Resolver / Service が複数になる予感があるなら、最初から Context record でラップする**。「今は 1 つだから Function でいい」と思って単体注入すると、2 つ目を追加する時に全箇所書き換えが必要になる (OCP 違反)。Context record にしておけば「フィールド追加 + null チェック追加」だけで済む。これは Wave 3 Task A → Task B の連続実装で実際に効果を確認 (Task B では引数 signature を一切変えずに displayName が完璧に解決できた)
- **適用範囲**: domain layer の純関数 API で「外部リソース解決のための Function 引数」を複数渡したくなる場面全般 (cardResolver / equipmentResolver / itemResolver / buffResolver 等)。「1 つでも将来 2 つになる予感があるなら Context record」をデフォルトに

### 2026-05-23: God Object の段階的分割 (Wave 4 α/β/γ 5 段階パターン)

- **状況**: M2 Wave 4 で DungeonScreen 697 行 (God Object) を責務分割。最初の plan では「責務 3 つを一気に切り出す」案だったが、ユーザー判断で「α/β/γ と小さく区切りながら全体を通して開発」に変更
- **判断**: 純粋データ (W4-α: EnemyKindMemory) → 副作用エフェクト (W4-β: ScreenEffects) → 複雑な lifecycle (W4-γ: EliteRewardOrchestrator) の順で段階的に切り出し、各段階で `gradlew check` 緑を維持しながらコミット
- **効果**: 697 → 615 (W4-α、-12 行) → 533 (W4-β、-82 行) → 463 (W4-γ、-70 行) と 3 段階で計 ~34% 削減。各段階で BUILD SUCCESSFUL を保ち、最終的に大規模リファクタを完成
- **学び**: **God Object 分割は「一気にやらず段階的に」**。理由 (1) 各段階でテスト緑を確認できる (2) Git の commit 単位で「どこで何を切り出したか」が後から追える (3) 切り出した独立クラスは個別にテスト追加が容易 (W4-α で +7 テスト、W4-β で +9 テスト)。
- **段階順の原則**: **純粋データ → 副作用集約 → 複雑な lifecycle** の順。純粋データは依存が少なく失敗時の影響が局所的。副作用集約は描画の begin/end ライフサイクル制御が要 (ユーザー指摘で `batch.begin/end` 跨ぎ禁止ルールを記録)。Lifecycle 分離は最も複雑、最後に着手
- **適用範囲**: 500+ 行の Screen / Manager / Controller クラスの責務分割全般

### 2026-05-23: BitmapFont.setColor / batch.setColor のリーク防止

- **状況**: Wave 4 W4-ε で装備テーマ変動 UI 実装時、HudRenderer / DungeonScreen の各描画メソッドで `font.setColor(theme.textColor())` の後にリセットしていない箇所が複数発覚
- **指摘 (ユーザー)**: 「LibGDX BitmapFont の color フィールドはミュータブル。setColor 後にリセットしないと他の描画箇所に色がリークして予期せぬバグになる」
- **判断 (受け入れ)**: 各 draw メソッドの末尾で `font.setColor(Color.WHITE)` (または既定色) にリセットすることを徹底するパターンを採用。Wave 4 W4-ε で drawLog 末尾にリセット追加、W4-δ BestiaryScreen でも徹底
- **学び**: **LibGDX のミュータブル Color フィールド (BitmapFont.color / SpriteBatch.color / ShapeRenderer.color) は描画メソッドの内側で setColor したら、必ず末尾で reset する**。grep で確認する方法: `grep -n "setColor" <ファイル>` で setColor 直後の対称的な reset があるか目視確認
- **適用範囲**: LibGDX の BitmapFont / SpriteBatch / ShapeRenderer の color プロパティを使う全描画コード

### 2026-05-23: Logger 級の Static initialization setter は許容パターン

- **状況**: Wave 5 W5-γ で SoulTree.allNodes() の domain → infrastructure 依存方向違反 (InitialStateFactory.soulTreeNodes() 直接呼出) を Supplier 注入パターンで解消
- **判断**: `private static Supplier<Map<NodeId, TreeNode>> nodeProvider;` フィールド + `public static void setNodeProvider(...)` で起動時に 1 度だけ注入。mutable static state 禁止ルールの**例外**として許容する条件 (1) 初期化フェーズのみ set (2) read 以降の state 変更なし (3) テストで初期化可能 (`@BeforeAll`)
- **学び**: **「Logger / Properties 級の単発 initialization setter」は許容**。java.util.logging.Logger.getLogger() や System.setProperty() と同じ位置付けで「JVM 起動時に 1 度設定し、以降は read-only」が守られるなら mutable static 禁止ルールの例外として認める。代わりに以下のガードを必須に: (a) Javadoc で「Logger 級の単発 setter」と明記 (b) null チェックで起動初期化漏れを早期検出 (`throw new IllegalStateException(...)`)(c) テストで `@BeforeAll` で provider を注入
- **並列テスト時の注意**: Gradle 並列テスト実行時、static フィールドは全テストで共有される。通常は同一 provider で問題ないが、**カタログモックテスト等で setProvider を上書きする場合**は `@AfterEach` で前 provider に復元する (try-finally スコープ限定 setter ヘルパが理想)
- **適用範囲**: domain layer から infrastructure layer への直接呼出を解消したいときの第一手 (依存逆転の代替案)。コンストラクタ注入が困難な「事実上のシングルトン」(record の static method 等) で特に有効

### 2026-05-23: TurnEngine 段階的分割 (Movement → CardResolver → SkillResolver の依存最小順)

- **状況**: Wave 5 W5-α-1/2/3 で TurnEngine 663 行 (God Object) を 3 クラスに分割
- **判断**: 依存最小から順に切り出し: (1) W5-α-1 TurnEngineMovement (applyPlayerMove / applyEnemyMove、移動 + 罠踏み) → (2) W5-α-2 TurnEngineCardResolver (CardEffect 4 種 dispatcher + resolver) → (3) W5-α-3 TurnEngineSkillResolver (Skill 関連 + 効果適用)。各段階で `gradlew check` 緑を保ちつつコミット
- **効果**: 663 → 622 (W5-α-1、-41 行) → 472 (W5-α-2、-150 行) → 330 (W5-α-3、-142 行) と 3 段階で計 ~50% 削減
- **共通ヘルパの扱い**: `reject` / `resolveDamageToEnemy` / `checkAndTriggerTrap` は複数 resolver から呼ばれるため TurnEngine に**残し package-private に降格**。Plan では「checkAndTriggerTrap も SkillResolver に移動」と書いていたが、Movement と Skill 両方から呼ばれるため Movement → Skill の不自然な依存方向を回避する**妥当な逸脱**として TurnEngine に残置を選択
- **学び**: **God Object 分割は「依存最小から」「共通ヘルパは残置」の 2 原則を守る**。理由 (1) 依存最小から切り出すと chain refactor が起きにくい (2) 共通ヘルパを安易に新クラスに移すと「Movement → SkillResolver」のような奇妙な依存方向ができてしまう (3) package-private 降格は同パッケージ内クラスからは呼べるため、副作用なく分離可能
- **Plan からの妥当な逸脱の判断基準**: Plan の文字通りに従うと依存方向が悪化する場合は逸脱してよい。ただし**コミットメッセージとクラス Javadoc に「Plan からの逸脱理由」を明記**することで後追い可能にする
- **適用範囲**: 500+ 行のドメイン層 utility / engine クラスを分割するとき全般

### 2026-05-23: 集約 record の段階的導入 (内部統合は次 Wave へ)

- **状況**: Wave 5 W5-β で DddGame の 7 ラン外フィールド (playerSoul / runCount / tutorialSeen / obtainedCards / bestiary / loadout / soulTree) を PlayerProgress record に集約する計画。Plan では「内部リファクタも実施」と書いていたが、影響範囲が広く 644 件のテスト破壊リスクが高い
- **判断**: **段階的アプローチを採用**。Wave 5 W5-β では PlayerProgress record + テスト 11 件のみ作成し、DddGame の内部統合 (フィールド統合 + setter / getter 書換) は Wave 6 で実施する m2_backlog に記録
- **学び**: **集約 record の導入は 2 段階に分ける**。(1) 第 1 段階: record + テスト追加で「集約の形」を確立 (低リスク、テスト 0 件追加なら破壊しようがない) (2) 第 2 段階: 既存コードの内部統合 (中リスク、setter / getter を順次中継メソッドに置き換え)。Plan に「両方同 Wave」と書かれていても、テスト破壊リスクが高ければ 2 Wave に分割するのが安全
- **判断基準**: 影響範囲が 5 ファイル以上 / 既存テスト 100 件以上を経由するなら段階分割を検討。1 Wave で完了せず複数 Wave に渡る場合は m2_backlog に明示しておくと後追いが容易
- **適用範囲**: 大規模 record 抽出 / 値オブジェクト集約 / DTO 統合などの破壊的変更全般

### 2026-05-24: i18n 段階的移管の収束パターン

- **状況**: Wave 1 Task 4 で 4 ペア (例外/通知メッセージ) を i18n 化したあと、Screen 内に少量のハードコード日本語が残存。Wave 6 W6-α で完了させた
- **判断**: 残ハードコードを「Strings.java の Ja / En 両エントリに追加 → Screen で `jp ? Strings.Ja.FOO : Strings.En.FOO` 経由」に書き換え。9 箇所を 1 コミットでクリア
- **学び**: **i18n 化は「一度で全部」を狙わず、Wave をまたいで段階的に収束させる**。理由 (1) コード全体を 1 度に書き換えると差分が膨大で review しにくい (2) 各 Wave で「残ハードコードを数える → 移管」のミニループを回すと、Strings.java のキー命名規約が育って一貫性が高まる (Wave 6 では `BONUS_STAT_*_SHORT` のような短縮形カテゴリを命名できた)
- **収束判定**: `grep "[一-龯ぁ-んァ-ヶ]+"` で Screen ディレクトリを scan して「該当箇所 0」になれば収束。例外メッセージ (throw new) は domain 層に残しても OK (IllegalArgumentException 等はログ用、ユーザー目に触れない)
- **適用範囲**: i18n / l10n の段階的導入全般。「全面移行」は coredump 級の差分を生むので、機能単位 / 画面単位 / 例外単位の分割が必須

### 2026-05-24: SaveData migration の graceful pattern (v1 → v2 で確立)

- **状況**: Wave 6 W6-β で SaveData に `defeatedEnemyKinds: List<String>` と `tutorialSeen: boolean` を追加。`schemaVersion=1` の旧セーブを読み込めるよう v2 化
- **判断**: 3 層の防御で graceful migration を実現:
  - **層 1 (Jackson レベル)**: 欠落フィールドは null (List) / false (boolean default) で来る。`@JsonProperty` で命名すれば自動で対応
  - **層 2 (compact constructor レベル)**: `defeatedEnemyKinds = defeatedEnemyKinds != null ? List.copyOf(defeatedEnemyKinds) : List.of();` で null → empty 正規化。compact constructor は record の中央集権ゲートなので、ここで完結
  - **層 3 (Converter レベル)**: 未知の Enum 名 (将来削除された敵種) は `EnumKind.valueOf(name)` の `IllegalArgumentException` を try-catch + WARN + graceful skip。`toLoadout` / `toObtainedCards` と同型パターン
- **学び**: **永続データの schema migration は「3 層 graceful」を必ず備える**。1 層だけでは将来の field 追加 / 削除 / typo を吸収しきれない。3 層 = (a) Jackson default、(b) compact constructor normalize、(c) Converter で valueOf catch。これで「旧セーブを開いてクラッシュ」を構造的に排除できる
- **CTO レビュー (ユーザー指摘) 反映**: `Set<EnemyKind>` → `List<String>` 直列化は **必ず `.name()` を使う** (既存 `unlockedNodeIds` / `obtainedCardIds` と同型パターン)。`Enum.toString()` は override 可能なので避ける
- **適用範囲**: あらゆる永続データの schema migration (SaveData / Settings / Catalog 系 JSON)。新フィールド追加時は必ず 3 層を組む

### 2026-05-24: タイガーリリー戦略 — 集約 record の内部統合

- **状況**: Wave 6 W6-γ で Wave 5 残置の DddGame PlayerProgress 内部統合を実施。7 ラン外フィールド (soulTree / playerSoul / tutorialSeen / runCount / obtainedCards / bestiary / loadout) を 1 record に集約
- **判断**: 「タイガーリリー戦略」を採用 = **1 フィールドずつ progress.withXxx(...) 化し、各段階で `gradlew check` 緑を確認する**ミニループ。1 度に全置換せず、フィールドごとに修正 → check → 次のフィールド、と繰り返す。実際は 7 フィールド + 関連箇所 (startNewRun / save / load) を 1 ファイル 1 コミット相当の差分で完成
- **CTO レビュー (ユーザー指摘) 反映**: `progress = progress.withSoulTree(...).withPlayerSoul(...)` のような **メソッドチェインで書ききる**。一時 PlayerProgress インスタンスは複数生成されるが、JVM の Escape Analysis により Young Generation / Stack 上で高速 GC される (パフォーマンス懸念なし)。「withFoo してから一時変数に入れて withBar する」と冗長に書くより、チェインで意図が明瞭になる
- **公開 API 互換維持**: Screen 側の `game.playerSoul()` / `game.loadout()` 等の呼出は変えない。内部で `return progress.playerSoul();` の中継メソッドにすることで、影響範囲を「DddGame 1 ファイルのみ」に閉じ込める。Screen 側の `game.progress()` 直接公開化は Wave 7 以降に breaking change として段階的に
- **学び**: **大規模な集約 record 内部統合は (a) タイガーリリー戦略 (1 フィールドずつ置換 + check) + (b) 中継 getter で公開 API 互換維持 + (c) チェインメソッドで美しく書ききる、の 3 セットで進める**。これで影響範囲を最小化しつつ、テスト破壊リスクなく完了できる
- **適用範囲**: God Object の集約 record 化全般。3 件以上のラン外永続フィールドを 1 record に統合するとき

### 2026-05-24: 同パッケージ package-private + Helpers クラスの分離パターン (2 段階リファクタの完了形)

- **状況**: Wave 5 W5-α 系で TurnEngine を Movement / CardResolver / SkillResolver に分割した際、共通ヘルパ (reject / resolveDamageToEnemy / checkAndTriggerTrap) は package-private で TurnEngine 本体に残置していた。Wave 7 W7-α で独立クラス TurnEngineHelpers に集約
- **判断**: 2 段階リファクタの完了形を採用:
  - **第 1 段階 (Wave 5)**: 主要責務 (Movement / Card / Skill) を切り出し、共通ヘルパは元クラスに package-private 化して残置 → 「同パッケージから呼べる + 外部 API は変えない」中間状態
  - **第 2 段階 (Wave 7)**: 残置していた共通ヘルパも独立クラスに集約 → 「全責務が分離 + 元クラスはフロー制御だけに絞られる」完了形
- **削減効果**: TurnEngine 663 → 330 (Wave 5) → 197 (Wave 7) 行、計約 70% 削減
- **学び**: **大規模クラス分割は 1 Wave で完璧を目指さず、2 段階リファクタで段階的に進める**。理由 (1) 第 1 段階で「主要責務分離」を完了 → 第 2 段階で「ヘルパ集約」、と分けると各 Wave のコミット粒度が小さく review しやすい (2) 第 1 段階で package-private 残置にすると「依存方向が崩れない」中間状態が保てる (Movement → Helpers のような変な依存が出ない) (3) 第 2 段階のタイミングは「共通ヘルパが 3 つ以上たまったら」を目安にする
- **判断基準**: 元クラスが 200 行を切ったら第 2 段階のリファクタは打ち切る (それ以上分割するとオーバー engineering)
- **適用範囲**: 500+ 行の utility / engine クラスの責務分割全般、Wave またぎの段階的リファクタが許容される場合

### 2026-05-24: 中継 getter からの卒業 (機械的置換による breaking change の完遂)

- **状況**: Wave 6 W6-γ で DddGame の 7 ラン外フィールドを PlayerProgress に集約した際、Screen 公開 API は中継 getter (game.playerSoul() 等) で互換維持にした。Wave 7 W7-β で完全撤去し、game.progress() 1 メソッドに集約
- **判断**: 4 ステップを 1 コミット内で実施:
  - (1) `progress()` 公開メソッド追加
  - (2) Screen / テストコードの呼出 16 箇所を機械的置換 (`game.xxx()` → `game.progress().xxx()`)
  - (3) 全 grep で残無し確認 (`grep "game\.playerSoul(" src` 等)
  - (4) 中継 getter 7 件を削除
- **CTO レビュー反映**: 中継 getter 撤去前に **grep でテストコード側の呼出を一網打尽**に列挙すること。今回はテストコード呼出ゼロだったが、確認は必須。「Screen メインコードだけ」のつもりがテストにも呼出があると、コンパイルエラーで気づくまで時間ロス
- **学び**: **breaking change の機械的置換は 4 ステップを 1 コミットに収める**。理由 (1) 中継残置 + コール置換が混在する中間状態は「意図不明」になりがち (2) 1 コミット 4 ステップなら git revert で元に戻せる安全性 (3) `grep` で 0 件を確認してから中継削除する手順を踏むと安心 (4) Screen 公開 API の縮小 (7 メソッド → 1 メソッド) で「DddGame は何を持っているか」が明示的になる
- **適用範囲**: 互換維持中継パターンの最終撤去全般 (record 直接公開化 / Builder pattern 解消 / DTO 中継削除 等)

### 2026-05-24: presentation 層テストの境界 (純粋関数のみ対象、LibGDX 描画は対象外)

- **状況**: Wave 7 W7-γ で presentation 層テスト補強。HudRenderer / RenderLayout / UiThemeResolver / BuffKindLabels / NodeIconPathResolver から純粋関数を抽出してテスト追加
- **判断**: テスト対象は **以下の条件を全て満たすメソッドのみ**:
  - LibGDX オブジェクト (`batch.draw(...)` / `font.setColor(...)` / `Texture` / `Stage`) を呼ばない純関数
  - 引数と戻り値だけで動作が決まる (副作用なし、static 純関数)
  - Color の比較は `r` / `g` / `b` フィールド値で代替 (`assertEquals(expected.r, actual.r, 0.001f)`)、`Color.equals()` は ε 比較できないので避ける
- **対象外**: `font.draw(batch, text, x, y)` 等の LibGDX 描画呼出を含むメソッドはヘッドレス LibGDX が必要 = テストコスト overkill。Mockito で SpriteBatch を mock するのも検証価値が低い (描画呼出の引数を assert しても何の品質保証にもならない)
- **学び**: **presentation 層テストは「純粋関数のラインを明確に分ける」**。具体的には: (1) LibGDX 描画呼出 = 視覚的 QA で確認 (テスト不要) (2) 文字列フォーマット / 色解決 / レイアウト計算 = 単体テスト対象 (Wave 7 で +22 件追加) (3) Screen ライフサイクル (show / render / dispose) = ヘッドレス LibGDX 環境で必要なら別途。これで「テストカバレッジを上げるためだけのコスト」を避け、本当に回帰防止になるテストだけが残る
- **適用範囲**: LibGDX / Unity / その他 GUI フレームワーク依存の presentation 層全般。純粋関数の比率が低い場合は「Screen 群はテスト不可」と明示的に M2 送りにする選択も OK

### 2026-05-24: domain 層の表示ラベル汚染 — 段階的剥離パターン (Wave 1/6/8 の 3 Wave 完結)

- **状況**: Wave 1 Task 4 で例外メッセージの i18n 移管 (4 ペア)、Wave 6 W6-α で Screen ハードコード 9 箇所を Strings に集約、Wave 8 W8-α で LayerEndNode#displayName を撤去。3 Wave に分けて domain 層から日本語を完全排除
- **判断**: 各 Wave で「影響範囲が手頃なスコープ」だけ移管する。一気に全部やると差分が膨大、各 Wave で挙動変化を確認しながら積み上げる
- **学び**: **domain 層の表示文字列汚染は「層 (domain / presentation) 境界を侵犯する種類の負債」なので、ハッカソン期は意図的に残置 + M2 段階で剥離が正解**。理由 (1) ハッカソン期は「displayName で文字列直接取得」が圧倒的に楽 (2) M2 で本格的 i18n を導入する際に「presentation 層に Labels クラスを集約 + Strings の Ja/En キー化」を順次進める (3) 完全撤去は「Event の displayLabel: String field を Enum に置換」のような構造変更を要するので、最後の Wave に持ってくる
- **Event の field 仕様変更の威力**: `Event(int, int, int, String)` → `Event(int, int, int, EventKind)` への変更で「外部から日本語 String を注入できる悪い設計」が構造的に不可能になる。compact constructor で「kind と delta の整合性」を validation することで、`new Event(99, 99, 99, EventKind.HEALING_SPRING)` のような不整合呼出を早期検出 + `Event.of(kind)` factory で kind だけから自動構築可能
- **適用範囲**: domain 層の表示文字列負債全般、特に「ハッカソン期に意図的に残した日本語 displayName」の最終撤去

### 2026-05-24: LibGDX リソース系のコンポーネント集約パターン — record ではなく final class が筋

- **状況**: Wave 8 W8-β で DddGame の Fonts / SoundManager / CardImageRegistry の 3 LibGDX リソースを GameResources final class に集約
- **判断**: record にはできない。理由は dispose() メソッドが必要で、record は immutable value object 専用 (Disposable interface 実装は record でも可能だが「リソースを保持し、自分の生死を管理する」のは record の責務と直交する)
- **完全なる防衛的 dispose**: 各 dispose 呼出を try-catch で個別に囲み、1 つが例外を投げても後続のリソース解放がスキップされない構造に。LibGDX の dispose は内部状態によっては例外を投げる可能性があり (Texture 未ロード状態、Music 既 close 等)、ハッカソン後の長期運用 (Phase D Android) でのクラッシュ率に直結する
- **解放順序**: CardImageRegistry (Texture[]) → SoundManager (Music + Sound) → Fonts (FreeTypeFontGenerator + BitmapFont)。順序判断は「重い・無依存なリソースから順、フォントは最後 (テキスト系のクラッシュログを最後まで出せる)」
- **学び**: **リソースライフサイクル管理クラスは record ではなく final class、dispose は防衛的 try-catch で個別解放**。理由 (1) record は immutable 思想 + value 比較が前提で、リソースの生死を扱うのと合わない (2) Disposable パターンは LibGDX のお作法、自前実装でも順序と例外を意識した実装が必須 (3) DddGame からは `if (resources != null) resources.dispose()` の 2 行に集約され、リソース系の責務が DddGame から完全分離される
- **適用範囲**: GUI / ゲームフレームワーク (LibGDX / JavaFX / Swing) のリソース集約全般。dispose / close ライフサイクルがある型の集約は record ではなく final class
