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
