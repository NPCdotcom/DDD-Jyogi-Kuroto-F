# UI/UX 静的監査 — 初めて遊ぶプレイヤーの視点

監査日: 2026-10-30  
対象: 提供された local main (60ac172) の読み取り専用コピー。基準ディレクトリは {S}/main。以下の file:line はすべてこのコピーからの相対パスである。

**High 2 件: F15（報酬表示中に戦闘が進む）、F21（結果画面の異常系に退路・説明がない）。** F21 は精算不正・保存障害時の指摘で、正常プレイでその状態が発生することを確認したものではない。コード・表示文・座標・定数を照合した監査であり、GUI、Gradle、ゲーム実行、ネットワーク、有料 API、git 書き込みは実施していない。実際の描画、入力機器別の挙動、音の聞こえ方は未検証である。

分類は [静的確認済み] = 分岐・文言・数値から確認できる事実、[有力な仮説] = コード上の原因はあるが体感・発生条件に未確認部分があるもの、[実機確認要] = 描画や体感の確認が必要なもの、とする。重大度 High = 進行・安全な意思決定を大きく損なう、Med = 誤操作や理解阻害、Low = 局所的な不便。規模 S = 文言・ガード・小さな描画/入力修正、M = 複数箇所の連携変更、L = 方式変更。規模は作業量の相対評価であり、動作検証の工数は含まない。「仕様変更」は GAME_DESIGN.md のルール・画面方針を変える必要性を示す。

## 既知の手動所見との照合

出所: docs/20260813_manual_findings_plan.md:15、:16、:17、:18、:19。

| 既知項目 | 現行コード上の状態 | 根拠 |
| --- | --- | --- |
| #1 敵ターン表示が小さい | 一部改善。32px の継続表示はあるが、提案された大きな開始通知は未実装。待機間隔は固定 0.10 秒から可変化されている。K01 として扱い、新規の別件には数えない。 | src/main/java/core/presentation/render/HudRenderer.java:328、:334 / src/main/java/core/presentation/screen/DungeonScreen.java:65、:271 / docs/20260813_manual_findings_plan.md:114 |
| #2 壊れる壁の位置 | 既知の優先候補・fallback ロジックが残る。対向 FLOOR 条件があることは確認できるが、最短経路が短くなる保証・分布の証拠にはならない。解決済みとは判定しない。 | src/main/java/core/infrastructure/bootstrap/InitialStateFactory.java:435、:458、:479 / docs/20260813_manual_findings_plan.md:222、:230 |
| #3 装備の全開放 | 対策済み。画面は所有 ID にある装備のみを列挙し、装着 API でも所有チェックを行う。未解放品をすべて選べる状態ではない。実機回帰は未確認。 | src/main/java/core/presentation/screen/EquipmentScreen.java:71、:81 / src/main/java/core/presentation/screen/DddGame.java:186 |
| #4 ツリーが一個飛ばしに見える | 原因となった 3 ノードに 105° / 225° / 315° のオフセットが入り、座標も SoulTreeLayout に集約済み。旧問題の原因への対策は確認済み。全ラベルの視覚 QA まで合格したという意味ではない。 | src/main/java/core/presentation/render/SoulTreeLayout.java:41 / src/main/java/core/presentation/screen/SoulTreeScreen.java:147 / docs/20260813_manual_findings_plan.md:68 |
| #5 スケルトン相当がほぼ攻撃しない | SWIFT_SLIME の実値が AGGRESSIVE になっており、旧往復挙動の設定原因は対策済み。近くの「CAUTIOUS」とするコメントは古いが、コメントを実装の根拠にしない。 | src/main/java/core/domain/entity/EnemyKind.java:17、:20 / src/main/java/core/presentation/screen/DungeonScreen.java:176 / docs/20260813_manual_findings_plan.md:159 |

# 1. 初見で操作が理解できるか

初回はプリセット選択、その後タイトル上にチュートリアルを表示する。移動の AP、カード選択→方向指定、移動権、待機、ターン終了、階段、層末選択を日英で説明しており、導入自体は存在する。根拠: src/main/java/core/presentation/screen/DddGame.java:776 / src/main/java/core/presentation/screen/TitleScreen.java:65 / src/main/java/core/presentation/render/Strings.java:170、:397。

### F01 — 操作説明を閉じると、設定から読み直せない

- 判定: [静的確認済み] / 重大度: Med / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/screen/TitleScreen.java:201、:211 / src/main/java/core/presentation/screen/DddGame.java:282 / src/main/java/core/presentation/screen/SettingsScreen.java:31、:178、:205。チュートリアル生成は未読時のみ、設定の 5 項目には再表示がない。
- 初見への影響: 全文を理解する前に閉じた人が、カードや移動権の操作を再確認できない。タイトルの短い説明には移動権の詳細がない。
- 小さな修正案: 設定に「操作説明を再表示」を 1 項目追加し、既存 TutorialOverlay を再利用する。既読フラグはそのまま保つ。
- 規模: S / 仕様変更: 不要。docs/GAME_DESIGN.md:765 に再表示が明記されている。

### F02 — 初心者向けミニマルで、操作状態からの抜け方まで消える

- 判定: [静的確認済み] / 重大度: Med / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/render/HudRenderer.java:320、:323 / src/main/java/core/presentation/render/RenderLayout.java:34 / src/main/java/core/presentation/input/PlayerInputs.java:131、:145。MINIMAL では CLEARED / ENEMY_TURN 以外の操作ヒントをすべて抑制する。
- 初見への影響: 選択中カードの ESC キャンセルや、移動権中は SPACE が無効で ENTER が脱出手段であることが見えなくなる。HP/AP だけで遊べる簡潔さと、モードの説明不足が混在する。
- 小さな修正案: 通常時の説明だけを省略し、カード選択中・移動権保持中のヒントはプリセットによらず表示する。
- 規模: S / 仕様変更: 不要。docs/GAME_DESIGN.md:419 の初心者向け方針を保つ補助表示で、戦闘ルールは変えない。

# 2. 説明文と実際のキー・マウス操作の一致

主要割当は一致する。移動は WASD/矢印、数字 1〜9 は実在する手札の選択、SPACE は 1 AP の待機、ENTER はターン終了、ESC はカード選択解除である。「SPACE = ターン終了」という旧誤記は現行チュートリアルには残っていない。根拠: src/main/java/core/presentation/input/PlayerInputs.java:145、:190、:207、:221 / src/main/java/core/presentation/input/InputAction.java:16 / src/main/java/core/presentation/render/Strings.java:23、:182、:184。

### F03 — 休憩の「HP 全回復 / Full HP heal」は実装と一致しない

- 判定: [静的確認済み] / 重大度: Med / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/render/Strings.java:139、:365 / src/main/java/core/presentation/render/LayerEndNodeLabels.java:41 / src/main/java/core/domain/layer/LayerEndNode.java:92、:98。
- 初見への影響: 全回復を見込んで休憩を選ぶが、実際は基礎最大 HP を 3 で整数除算した量（最低 1）しか回復しない。最大 HP 30・現在 HP 5 なら 15 になる。コードのコメントにある「30%・切り上げ」とも数式は違う。
- 小さな修正案: まず表示を実計算と一致する「休憩: HP +10」等の予測値にする。回復率や端数処理の変更は別のバランス判断として切り離す。
- 規模: S / 仕様変更: 不要。docs/GAME_DESIGN.md:621 は HP 回復とだけ定めており、現行計算に表示を合わせる提案である。

### F04 — 新規開始の放棄確認で「L で再開」と読めるが L は処理されない

- 判定: [静的確認済み] / 重大度: Low / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/render/Strings.java:165、:392 / src/main/java/core/presentation/window/ConfirmationDialog.java:80 / src/main/java/core/presentation/screen/TitleScreen.java:216、:240、:260。
- 初見への影響: ダイアログ本文の案内に従って L を押しても反応せず、再開できないように見える。親タイトルの L 分岐へはモーダル表示中に到達しない。
- 小さな修正案: 本文を「N / ESC で戻り、L で再開」に直す。
- 規模: S / 仕様変更: 不要。docs/GAME_DESIGN.md:712 の再開導線に関する文言整合のみ。

# 3. カード効果・AP 消費・使用可能状態

選択中カードには名前・AP・属性・効果全文を表示し、射程/範囲も条件付きで補足する。AP 不足で使用しても AP や手札を消費せず、ActionRejected が返る点は問題なし。根拠: src/main/java/core/presentation/render/CardPresentationText.java:12 / src/main/java/core/presentation/render/CardDescriber.java:31、:66 / src/main/java/core/domain/battle/TurnEngineCardResolver.java:38。

### F05 — AP 不足を選択前に区別できず、拒否理由はログだけ

- 判定: [静的確認済み] / 重大度: Med / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/render/HudRenderer.java:224、:232、:247、:397 / src/main/java/core/domain/battle/TurnEngineCardResolver.java:46 / src/main/java/core/presentation/input/PlayerInputs.java:164。カード画像の色は選択中か否かだけで、所持 AP とコストを比べた使用不可表示がない。方向を指定すると、成功/拒否を問わず選択も解除される。
- 初見への影響: 使えないカードも同じように見え、選ぶ→方向指定→何も起きない、を繰り返す。画像経路には手札位置に対応した動的な数字ラベルもなく、複数枚になると 1〜9 の対応を数えて探す必要がある。画像内の固定文字の読みやすさ自体は未検証。
- 小さな修正案: 各カードに動的な「番号 / AP」小ラベルを重ね、不足時は減光と「AP 不足」を併記する。使用拒否時にも短い理由を選択欄へ表示する。
- 規模: S / 仕様変更: 不要。docs/GAME_DESIGN.md:502 のコスト制約を可視化するだけで、実行可能条件は変えない。

### F06 — 「物理ダメージ N」が最終ダメージのように読める

- 判定: [静的確認済み] / 重大度: Low / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/render/CardDescriber.java:34 / docs/GAME_DESIGN.md:507。表示している N は baseValue で、物攻・物防を加減する前の値である。
- 初見への影響: カードの表示数と実際の与ダメージが違う理由が分かりにくい。
- 小さな修正案: 「基礎威力 N」へ言い換え、操作説明に「基礎値 + 攻撃 - 防御、最低 1」を 1 行追加する。
- 規模: S / 仕様変更: 不要。既存ダメージ仕様の説明。

# 4. 情報の優先順位・視認性・文字サイズ・重なり・余白・コントラスト

HP/AP はプリセットにかかわらず常時表示し、32px フォント・48px 行間を確保する。カード詳細は 16px、幅 944px で折り返す。これらの値は確認できるが、16px は 1280×720 への Fit で約 10.7 物理 px になるため、効果全文の読みやすさは実機確認対象である。根拠: src/main/java/core/presentation/render/HudRenderer.java:47、:69、:250 / src/main/java/core/presentation/render/RenderLayout.java:119、:182、:199。手札・詳細の基準位置は分離されており、この 2 領域の単純な座標重複は見つからない（RenderLayout.java:173、:190）。

### F07 — ライトテーマの濃色文字を黒い HUD 背景へ描く

- 判定: [静的確認済み] / 重大度: Med / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/render/UiTheme.java:74 / src/main/java/core/presentation/render/UiThemeResolver.java:31 / src/main/java/core/presentation/screen/DddGame.java:161 / src/main/java/core/presentation/screen/DungeonScreen.java:467、:470 / src/main/java/core/presentation/render/HudRenderer.java:58、:195。
- 初見への影響: ライト設定にすると文字は RGB 約 0.1/0.1/0.12 に変わるが、下部背景は不透明の黒のまま。ターン終了の文字や HP/AP の背景との明暗差を損ねる。実画面の見え方は未検証だが、前景・背景の設定の不整合は確定している。
- 小さな修正案: HUD パネルの背景色もテーマで切り替える。最小の暫定対応なら、黒背景上の essential テキストを明色に保つ。
- 規模: S / 仕様変更: 不要。docs/GAME_DESIGN.md:254 のテーマ演出の表示修正で、ルールは不変。

### F08 — ログ・操作ヒントの背後に可読性用パネルがない

- 判定: [有力な仮説] / 重大度: Med / 既知手動所見: #1 の視認性とも関連するが、背景の不足は別の根拠。
- 根拠: src/main/java/core/presentation/screen/DungeonScreen.java:462、:468、:471 / src/main/java/core/presentation/render/RenderLayout.java:142、:155 / src/main/java/core/presentation/render/HudRenderer.java:350、:355。下部背景の上端は y=300、ヒントは y=350、ログは y=520/472。右上パネルにも入らない。
- 初見への影響: 32px に拡大されていても、移動するマップや敵画像の上に灰色のログ・説明が直接重なり、AP 不足や操作ヒントを見落とす可能性がある。実際のコントラストは地形画像・位置次第であり未確定。
- 小さな修正案: 各テキスト帯の直下に薄い背景を足す、またはログ/ヒントを既存パネル内へ移す。
- 規模: S / 仕様変更: 不要。UI の可読性改善。

### F09 — クレジットの後半が仮想画面の下へ出る

- 判定: [静的確認済み] / 重大度: Low / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/screen/CreditsScreen.java:24、:64、:121、:128 / src/main/java/core/presentation/render/RenderLayout.java:233。日英とも 25 行を y=800 から 48px 間隔で配置し、最終行は y=-352。戻るヒントの y=80 とも途中行が重なる。
- 初見への影響: 音素材や末尾案内を読めず、画面が途中で切れたように見える。進行は ESC で戻れるため阻害しない。
- 小さな修正案: 空行を減らして 2 列にするか、既存一覧と同じスクロール方式へ寄せる。
- 規模: S（2 列化） / 仕様変更: 不要。docs/GAME_DESIGN.md:689 の UI の内容を変える提案ではない。

# 5. ウィンドウサイズ変更と解像度

DesktopLauncher は 1920×1080・リサイズ可能で、確認した Screen は FitViewport と resize/update を持つ。単純な縦横引き伸ばし描画ではない点は問題なし。根拠: src/main/java/core/infrastructure/desktop/DesktopLauncher.java:22 / src/main/java/core/presentation/screen/DungeonScreen.java:150、:665 / src/main/java/core/presentation/screen/FirstRunPresetScreen.java:44、:137 / src/main/java/core/presentation/screen/SoulTreeScreen.java:706。

### F10 — 描画は Fit、手札・終了ボタン・初回選択のクリックは stretch 換算

- 判定: [静的確認済み] / 重大度: Med / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/render/ButtonBounds.java:39、:48、:53 / src/main/java/core/presentation/screen/DungeonScreen.java:601、:620 / src/main/java/core/presentation/screen/FirstRunPresetScreen.java:112、:121。
- 初見への影響: 16:9 以外へリサイズすると、見えているカードやボタンを押しても反応しない/別の操作になる。例として 1280×1024 では Fit の黒帯は上下 152px。終了ボタン中心の仮想 y=60 は物理下端から 192px に描かれるが、単純換算は y=202.5 と判定してボタン範囲 y=30〜90 を外す。
- 小さな修正案: 固定 HUD 用の FitViewport から unproject する経路に統一する。FirstRunPresetScreen は既存 viewport を ButtonBounds の Viewport 付き overload に渡せる。黒帯上のクリックも除外する。
- 規模: M（各クリック入口の統一） / 仕様変更: 不要。docs/GAME_DESIGN.md:411 のリサイズを正しく支える修正。

### F11 — Elite 報酬ポップアップへ resize が伝播しない

- 判定: [静的確認済み] / 重大度: Med / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/screen/DungeonScreen.java:665 / src/main/java/core/presentation/screen/EliteRewardOrchestrator.java:108 / src/main/java/core/presentation/window/NodeChoicePopup.java:173。親は層末選択とステータスへだけ resize を転送し、EliteRewardOrchestrator には転送口がない。
- 初見への影響: 報酬を読んでいる間にサイズを変えると、その Stage の画面座標・クリック判定が以前のサイズに残り、選択肢の位置とマウスが合わなくなる。実際の描画崩れの程度は未確認。
- 小さな修正案: EliteRewardOrchestrator.resize を追加して、存在する popup に委譲する。
- 規模: S / 仕様変更: 不要。UI ライフサイクルの補完。

### F23 — 設定変更で手動リサイズしたウィンドウが元に戻る可能性

- 判定: [有力な仮説] / 重大度: Med / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/screen/SettingsScreen.java:190 / src/main/java/core/presentation/screen/DddGame.java:590、:602。音量やテーマだけを変更しても、windowed 設定なら毎回 setWindowedMode(1920,1080) を呼ぶ。
- 初見への影響: 見やすいサイズに調整してから音量を下げただけで、画面サイズまで戻る可能性がある。実際の OS/backend の適用結果は未検証。
- 小さな修正案: 画面モードの副作用は fullscreen の値が変わった時に限定し、以前の windowed サイズを維持する。
- 規模: S / 仕様変更: 不要。docs/GAME_DESIGN.md:411 のリサイズ設定を尊重する修正。

# 6. 日本語/英語とレイアウト

言語設定を切り替えるメニューはなく、フォントファイルの有無で日本語/英語を選ぶ実装である（src/main/java/core/presentation/render/Fonts.java:111、:121 / src/main/java/core/presentation/screen/SettingsScreen.java:31）。そのため「英語切替」の監査は、フォント欠落時の fallback 経路として行った。日本語側は Strings.Ja、カード、装備、敵、ツリーの表示名を事前グリフ集合へ集める対策がある（Fonts.java:200）。

### F12 — 英語 fallback に日本語の説明・固有名・確認文が残る

- 判定: [静的確認済み] / 重大度: Med / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/render/Fonts.java:123、:133 / src/main/java/core/presentation/window/NodeChoicePopup.java:120 / src/main/java/core/presentation/window/SoulNodeUnlockDialog.java:123、:129、:145 / src/main/java/core/presentation/screen/CardCollectionScreen.java:131 / src/main/java/core/presentation/render/CardDescriber.java:26 / src/main/java/core/presentation/screen/EquipmentScreen.java:190、:264 / src/main/java/core/presentation/render/LayerEndNodeLabels.java:45。
- 初見への影響: ASCII 専用フォントへ fallback しても、ノードのクリック案内、解放のコスト・確定文、図鑑のカード効果や名前などが日本語のまま。英語だけで読めず、日本語グリフも用意されない箇所がある。
- 小さな修正案: hardcode された文を Strings.Ja/En に移し、図鑑にも CardDescriber.describe(card, jp) を使う。名前は選択カードの CardPresentationText が既に行う英字 ID 整形を必要箇所へ再利用する。
- 規模: M / 仕様変更: 不要。既存の日英表示経路の整合で、別の言語切替機能を新設する提案ではない。

はみ出しの限界: TutorialOverlay は本文 Label を wrap/最大幅指定せず pack し（src/main/java/core/presentation/window/TutorialOverlay.java:61、:71）、NodeChoicePopup も同様（NodeChoicePopup.java:99、:124）。日英の長文幅はフォントの実測が必要であり、この監査では個別の横はみ出しを断定しない。F09 の縦はみ出しだけは座標計算で確定している。

# 7. マウスとキーボード操作の一貫性

### F13 — タイトルの任意クリックが新規開始になり、各画面の操作方式も混在する

- 判定: [静的確認済み] / 重大度: Med / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/screen/TitleScreen.java:243、:260、:271 / src/main/java/core/presentation/screen/EquipmentScreen.java:122、:133 / src/main/java/core/presentation/screen/SoulTreeScreen.java:384、:415 / src/main/java/core/presentation/screen/SettingsScreen.java:178。
- 初見への影響: 見えている「設定」「つづき」をクリックしても新規開始になる。進行中ランがある場合は確認で保護されるが、目的のメニューを開けない。マウスで遊び始めてからキーが必要になり、逆にキーボードでは装備やツリーの選択を完結できない。
- 小さな修正案: まずタイトルの各メニュー文字にクリック矩形を割り当て、空白クリックを無効にする。次段階で一覧の選択/確定、戻るボタンを各画面へ揃える。
- 規模: S（タイトルのみ） / M（全画面の入力統一） / 仕様変更: 不要。docs/GAME_DESIGN.md:712 の既存導線の入力手段を揃える。

| 画面/操作 | キーボード | マウス | 根拠 |
| --- | --- | --- | --- |
| 初回プリセット | 1/2/3 | 行クリック | src/main/java/core/presentation/screen/FirstRunPresetScreen.java:98 |
| 通常の移動/カード/終了 | WASD・数字・ENTER | マップ・カード・終了ボタン | src/main/java/core/presentation/screen/DungeonScreen.java:252 / src/main/java/core/presentation/input/PlayerInputs.java:79 |
| タイトルの再開/設定/図鑑等 | L/S/C/E/T/K/B | 対応領域なし、クリックは Start | src/main/java/core/presentation/screen/TitleScreen.java:243 |
| 装備一覧 | 上下/WS はスクロール、ESC は戻る | 装着/解除はクリックのみ、ホイール経路なし | src/main/java/core/presentation/screen/EquipmentScreen.java:105、:122、:133 |
| ツリー | パン/ズーム/R/ESC | 解放/ドラッグパン。解放確定には Y が必要 | src/main/java/core/presentation/screen/SoulTreeScreen.java:384、:415 / src/main/java/core/presentation/window/SoulNodeUnlockDialog.java:90 |
| 設定 | 上下・左右・ESC | 値の変更経路なし | src/main/java/core/presentation/screen/SettingsScreen.java:178 |
| カード/敵図鑑、クレジット | スクロール対応画面は上下、戻るは ESC | 戻る/ホイール経路なし | src/main/java/core/presentation/screen/CardCollectionScreen.java:73、:89 / src/main/java/core/presentation/screen/BestiaryScreen.java:60、:76 / src/main/java/core/presentation/screen/CreditsScreen.java:44 |

# 8. ダイアログの閉じ方・フォーカス・二重入力

| ウィンドウ | 実装上の閉じ方/確定 | 評価と根拠 |
| --- | --- | --- |
| TutorialOverlay | ENTER / ESC / 任意クリック | 閉じたフレームは親が return し、直後に Start しない。src/main/java/core/presentation/screen/TitleScreen.java:201 |
| StatusPopup | Tab / ESC | 開けるのは自ターンかつ他 popup がない場合。開いている間は進行停止。src/main/java/core/presentation/screen/DungeonScreen.java:200、:228 |
| NodeChoicePopup | 1〜3 / 行クリックで選択確定 | ESC による無選択閉じはない。層末の必須選択なので、それ自体をソフトロックとは扱わない。src/main/java/core/presentation/screen/DungeonScreen.java:321 / src/main/java/core/presentation/window/NodeChoicePopup.java:108 |
| ConfirmationDialog | Y で確定、N / ESC で中止 | マウス用ボタン/右クリック閉じはない。src/main/java/core/presentation/window/ConfirmationDialog.java:80 |
| SoulNodeUnlockDialog | 所持ソウルが足りる時 Y、N / ESC で中止 | モーダル中のパン/ポインタ処理を止め、閉じる時にポインタ状態をリセット済み。src/main/java/core/presentation/window/SoulNodeUnlockDialog.java:90 / src/main/java/core/presentation/screen/SoulTreeScreen.java:379、:415、:571 |

### F14 — マウスで開いた確認をマウスで確定/中止できない

- 判定: [静的確認済み] / 重大度: Low / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/window/ConfirmationDialog.java:80、:112 / src/main/java/core/presentation/window/SoulNodeUnlockDialog.java:90、:145。
- 初見への影響: ツリーノードをクリックした直後や新規開始確認で、ラベルを押しても反応しない。マウス操作を続けている人には停止に見える。
- 小さな修正案: Y/N と等価の可視ボタンを置く。中止方法の表記を ESC とボタンで統一する。右クリックで閉じるかは操作ポリシーとして明示してから揃える。
- 規模: S / 仕様変更: 不要。確認内容や消費条件は変えない。

### F15 — Elite 報酬表示中も背後で戦闘が進み、数字キーを二重に扱う

- 判定: [静的確認済み] / 重大度: High / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/screen/DungeonScreen.java:200、:204、:207、:246、:267、:270 / src/main/java/core/presentation/screen/EliteRewardOrchestrator.java:69 / src/main/java/core/presentation/input/PlayerInputs.java:200。
- 初見への影響: 報酬を読んでいる間も ENEMY_TURN のステップが進むため、残った敵に攻撃されうる。自ターンでは 1〜3 が通常手札の選択にも使われ、その後同じフレームで報酬の選択にも使われる。報酬選択後に意図せずカード選択状態が残る経路がある。
- 静的な手順: Elite 撃破で popup が作られる → 次フレームも statusPanelOpen だけが更新停止条件 → updateState が敵を動かす、または PlayerInputs.poll が数字を読む → その後 eliteReward.handleInput も同じ数字を読む。Mouse 向けには isActive ガードがあるが、poll と敵更新を囲むガードはない。
- 小さな修正案: Elite 報酬が active の間は戦闘更新と通常入力の両方を止め、報酬入力だけを処理する。開閉時にカード選択も reset する。
- 規模: M / 仕様変更: 不要。docs/GAME_DESIGN.md:586 の報酬選択を安全に行う UI の入力排他で、AP/敵行動ルールを変えない。

NodeChoicePopup の consume は取得後に選択を消し、dispose 時には自身が InputProcessor の場合だけ解除するため、同じ結果の単純な二重適用は防いでいる（src/main/java/core/presentation/window/NodeChoicePopup.java:148、:178）。F15 はその仕組みの外側で通常入力が同時に走る問題である。

# 9. イベント結果のフィードバック

### F16 — 資源不足の泉/ショップを選んでも層を進み、選択をやり直せない

- 判定: [静的確認済み] / 重大度: Med / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/domain/layer/LayerEndNode.java:123、:185、:234 / src/main/java/core/domain/layer/EventKind.java:13 / src/main/java/core/presentation/screen/DddGame.java:497、:534 / src/main/java/core/presentation/screen/DungeonScreen.java:340、:342、:358。
- 初見への影響: 治療の泉はソウル 10 未満だと無変化のまま次層へ進む。ショップも購入できずに進む。ショップは不足 flash があるが、泉には対応通知がない。回復を選んだのに回復せず、層末の機会も失ったように見える。
- 小さな修正案: 消費可能かを選択確定前に調べ、不可なら理由を表示して同じ 3 択を維持する。選択肢にも所持/必要数と無効状態を出す。
- 規模: S / 仕様変更: 不要。docs/GAME_DESIGN.md:616 の「提示から 1 選択」を、効果が成立する選択として扱う修正。回復量・価格は変えない。

### F17 — 泉・休憩・祠・宝箱の結果を具体的に伝える通知がない

- 判定: [静的確認済み] / 重大度: Low / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/screen/DddGame.java:491、:541 / src/main/java/core/presentation/screen/DungeonScreen.java:330 / src/main/java/core/domain/layer/LayerEndNode.java:95、:182。効果は Player の更新として適用し即遷移する。専用の成功通知はなく、ステ強化の SE とショップ不足時の flash だけが分岐する。
- 初見への影響: HP 上限で泉の実回復が 20 未満だった時や、祠で HP を失った時、何がどれだけ変わったか追いづらい。MINIMAL は Soul/Gold 表示もないため特に確認しにくい。
- 小さな修正案: 適用前後の差分から「HP +8 / ソウル -10」「金貨 +50」等を既存の 2.5 秒 flash で表示する。
- 規模: S / 仕様変更: 不要。docs/GAME_DESIGN.md:620 の効果を伝える UI 表示。

# 10. 戦闘テンポ・待機・アニメーション

敵の間隔は clamp(1.2 / 初期 AP 合計, 0.03, 0.10) 秒、1 フレーム最大 10 ステップ、残 AP がなくなれば最後のフェーズ遷移を待たせない。旧所見の「常に 0.10 × AP 合計」は現行にはそのまま当てはまらない。ただし AP 合計が 40 を超えると下限 0.03 秒のため約 1.2 秒は厳密な上限ではない。根拠: src/main/java/core/presentation/screen/DungeonScreen.java:65、:272、:283、:292。

ダメージ数字は 1.0 秒、シェイクは 0.18 秒、通常 flash は 2.5 秒、最後の 0.5 秒でフェードする。これらの演出自体を待つ入力ロックは見当たらない。根拠: src/main/java/core/presentation/effect/DamagePopup.java:22 / src/main/java/core/presentation/screen/ScreenEffects.java:40、:87、:168 / src/main/java/core/presentation/screen/DungeonScreen.java:199。音と視認に十分な間隔かは [実機確認要] であり、静的監査から速度変更を即断しない。

### K01 — 【既知 #1】敵ターン開始を見分ける強い合図は未実装

- 判定: [静的確認済み] / 重大度: Med / 既知手動所見: docs/20260813_manual_findings_plan.md:106。
- 根拠: src/main/java/core/presentation/render/HudRenderer.java:334 / src/main/java/core/presentation/screen/DungeonScreen.java:537 / docs/20260813_manual_findings_plan.md:114。現行はオレンジの継続ヒント。フェーズ変化の処理はプレイヤーターンのドロー SE を扱うが、敵ターン開始の大きな短時間表示はない。
- 初見への影響: 入力が効かない原因が敵ターンなのか分かりづらい可能性が残る。待ち時間の可変化は対策済みなので、旧「長すぎる待機」のまま重複評価しない。
- 小さな修正案: 新たな待機を増やさず、既存フェーズ表示をパネル内で短く強調する。F08 の背景対策とまとめて確認する。
- 規模: S / 仕様変更: 不要。ターン表示の補助で、docs/GAME_DESIGN.md:543 のシームレスな進行は維持する。

# 11. 成長・報酬獲得時の達成感

撃破 SE、与被ダメージ数字とシェイク、Elite の LEVEL_UP SE とカード獲得 flash、ステ強化の STATUS_UP SE、ツリー解放名の 2.5 秒 flash は実装済みである。根拠: src/main/java/core/presentation/screen/DungeonScreen.java:510、:518、:532 / src/main/java/core/presentation/screen/EliteRewardOrchestrator.java:85 / src/main/java/core/presentation/screen/DddGame.java:541 / src/main/java/core/presentation/screen/SoulTreeScreen.java:533、:700。実際の音素材・聞こえ方は起動していないため未検証。

### F18 — 結果の「持ち帰ったソウル」は、そのランの獲得量でなく累計残高

- 判定: [静的確認済み] / 重大度: Med / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/screen/GameOverScreen.java:53、:54、:92 / src/main/java/core/presentation/screen/DddGame.java:346、:358 / src/main/java/core/presentation/render/Strings.java:46 / src/main/java/core/domain/save/SoulSettlement.java:41。
- 初見への影響: 最初の 1 回は違いが出にくいが、ソウルを貯めた 2 回目以降に「今回の成果」と「強化に使える総額」を取り違える。前より多く稼げたか分かりにくい。
- 小さな修正案: 「今回の増減」と「所持合計」を分けて表示する。精算前の残高を保存し、精算後との差分を使えば既存計算を流用できる。
- 規模: S / 仕様変更: 不要。docs/GAME_DESIGN.md:160 の死亡/生還後の成果表示を明確にするだけで、精算額は変えない。

# 12. 操作ミスからの回復

カード選択は ESC/同じ数字/同じカードクリックで解除でき、ツリー解放・リセット・進行中ランの放棄には確認がある。最後の装備を外して空デッキにする経路もガード済みである。根拠: src/main/java/core/presentation/input/PlayerInputs.java:145、:181 / src/main/java/core/presentation/screen/SoulTreeScreen.java:489、:659 / src/main/java/core/presentation/screen/TitleScreen.java:247 / src/main/java/core/presentation/screen/DddGame.java:202。実行済みの戦闘行動の巻き戻しを追加する提案はしない。

### F19 — 装備は横位置を問わず行を切り替え、拒否時にも決定音だけ鳴る

- 判定: [静的確認済み] / 重大度: Med / 既知手動所見: 未掲載（既知 #3 の全開放とは別）。
- 根拠: src/main/java/core/presentation/screen/EquipmentScreen.java:133、:139、:143、:155 / src/main/java/core/presentation/screen/DddGame.java:202。選択の判定は y の範囲と行番号のみで、x の制限がない。最後の装備の解除は DddGame が return するが、呼出側は常に決定音を鳴らす。
- 初見への影響: 空白の右側をクリックして別装備へ切り替える、唯一の短剣を外そうとしても理由不明の無反応になる。
- 小さな修正案: 行に明示的な矩形を設けて表示と一致させる。解除できない場合は「最後の装備は外せません」と表示して成功音を鳴らさない。
- 規模: S / 仕様変更: 不要。空デッキ防止は保ち、入力と結果表示だけを直す。

### F20 — 選択して持ち上がったカードの上端がクリック判定に含まれず、マップ操作へ抜ける

- 判定: [静的確認済み] / 重大度: Med / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/render/RenderLayout.java:173、:176 / src/main/java/core/presentation/render/HudRenderer.java:159、:230 / src/main/java/core/presentation/screen/DungeonScreen.java:253、:265、:625 / src/main/java/core/presentation/input/PlayerInputs.java:88。
- 初見への影響: 通常カードの y 範囲は 110〜278、選択中の見た目は 130〜298 なのに、ヒット範囲は元のまま。見えている上端 20px を再クリックするとカード解除ではなく、選択カードの方向指定/使用へ回りうる。カード以外の HUD 空白もマップクリックから除外されていない。
- 小さな修正案: 選択状態込みの描画矩形をヒット判定と共有し、HUD パネルや黒帯上のクリックはマップ操作として扱わない。
- 規模: S / 仕様変更: 不要。誤入力防止のみ。

### F24 — 右クリック取消の経路がなく、通常行動に回る可能性

- 判定: [有力な仮説] / 重大度: Med / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/input/PlayerInputs.java:116 / src/main/java/core/presentation/screen/DungeonScreen.java:594、:608。通常操作の入口はボタン種別を絞らず justTouched を使い、右クリックを取消専用として扱う分岐はない。
- 初見への影響: 右クリックをカード選択の取消だと想定した人が、通常行動を発生させる可能性がある。右ボタンがこの経路を通る実際の入力挙動は未検証であり、確定した誤射としては数えない。
- 小さな修正案: F20 と合わせ、行動を左クリックに限定し、右クリックは選択取消へ割り当ててヒントにも明記する。
- 規模: S / 仕様変更: 要（入力割当の追加を §15-10 の説明へ補足）。AP・行動効果は変更しない。

# 13. 進行不能になりうる画面遷移

### F21 — 結果画面の保存障害・精算不正がユーザーに説明されず、不正状態には退路がない

- 判定: [静的確認済み] / 重大度: High / 既知手動所見: 未掲載。異常系限定。
- 根拠: src/main/java/core/presentation/screen/GameOverScreen.java:53、:99、:108、:116、:121 / src/main/java/core/presentation/screen/DddGame.java:337、:351 / src/main/java/core/domain/save/SoulSettlement.java:40。
- 初見への影響: SAVE_FAILED でも通常の「ENTER でソウルツリーへ」が表示され、押すと裏で再試行するだけ。INVALID_STATE ではその入力自体が no-op で、ESC/タイトルへ戻る経路もない。プレイヤーにはフリーズと区別できず、アプリを閉じるしかない。
- 発生の限界: 保存失敗や精算の不正が返った場合の画面処理は確定しているが、その障害を通常の初回プレイで発生させたわけではない。整数上限/不正な残高などの精算拒否条件は SoulSettlement に実在する。
- 小さな修正案: 結果状態別に「保存失敗・再試行」「精算できません・データを保持してタイトルへ戻る」を表示する。成功前のソウルツリー移動や不正データの上書きを許して回避してはならない。
- 規模: M / 仕様変更: 不要。docs/GAME_DESIGN.md:767 の保存仕様を維持した障害時の復旧導線。

### F22 — ダンジョン中に説明・設定・タイトルへ戻る入口がない

- 判定: [静的確認済み] / 重大度: Med / 既知手動所見: 未掲載。
- 根拠: src/main/java/core/presentation/screen/DungeonScreen.java:199、:228、:653 / src/main/java/core/presentation/input/PlayerInputs.java:145、:190。ESC はカード選択やステータスを閉じるだけで、未選択状態の ESC に画面遷移はない。
- 初見への影響: チュートリアルを見直したい、音量を変えたい、途中で離れたい時に OS の終了操作へ頼る。これ単独で戦闘進行が止まるソフトロックとは扱わないが、回復・離脱の見通しが悪い。
- 小さな修正案: カード未選択時の ESC で「続行 / 操作説明 / 設定 / タイトルへ」を開く。タイトルへは「この層の途中状態は保存されず、直近の層開始から再開」と明示する。
- 規模: M / 仕様変更: 要（§15-8 の画面導線を補足）。docs/GAME_DESIGN.md:772 の層境界だけの保存ルールは変更しない。

正常系について、以下はコード上で脱出手段があり、「ESC で抜けない」だけを理由に進行不能とはしない。

| 状態 | 脱出/進行手段と根拠 |
| --- | --- |
| 初回プリセットで ESC が効かない | 数字 1/2/3 または行クリックで選択すればタイトルへ。src/main/java/core/presentation/screen/FirstRunPresetScreen.java:98、:129 |
| 空スロットのカードを選んで方向待ちになる | 実在する手札数に制限するガードあり。src/main/java/core/presentation/input/PlayerInputs.java:200 |
| 移動権が残っているが四方を塞がれた | ENTER で移動権を放棄しターン終了できる。SPACE 無効は F02 の説明不足に注意。src/main/java/core/presentation/input/PlayerInputs.java:131 |
| 層末選択を ESC で閉じられない | 1〜3/行クリックで選び、resolveLayerEndChoice から次層へ進む。ただし失敗選択は F16。src/main/java/core/presentation/screen/DungeonScreen.java:321 / src/main/java/core/presentation/screen/DddGame.java:534 |
| ツリー確認を ESC で閉じた直後に再び同じ確認が開く | モーダル中と消費後に pointerDown/dragged をリセットする対策あり。src/main/java/core/presentation/screen/SoulTreeScreen.java:415、:571 |
| 装備を全部外して攻撃手段ゼロで開始 | 最後の 1 装備を外せないガードあり。F19 の理由表示は不足。src/main/java/core/presentation/screen/DddGame.java:200 |
| 敵が拒否行動を繰り返してターン終了しない | 拒否時にその敵の AP を使い切らせる経路あり。src/main/java/core/application/TurnDirector.java:183 |

# 体験価値が大きく変更が小さい改善 — 優先上位 8 件

重大度に加え、誤操作の回復性・遭遇しやすさ・修正のまとまりで優先した。いずれもこの監査では未実装である。

| 優先 | 改善 | 対応 | 規模 | 期待できる体験価値 / 仕様変更 |
| --- | --- | --- | --- | --- |
| 1 | 報酬表示中は戦闘と通常入力を止める | F15 / High | M | 読んでいる間の被弾と二重入力を防ぐ。仕様変更不要。 |
| 2 | 結果画面に保存失敗・再試行・退路を表示 | F21 / High | M | 障害時の無反応と行き止まりを解消。異常系限定だが進行保護に直結。精算ルール変更不要。 |
| 3 | 休憩を「実回復量」の表示へ直す | F03 / Med | S | 選択の前提を正し、回復への信頼を取り戻す。回復量自体は変えない。 |
| 4 | 泉/ショップの支払い可否を確定前に確認 | F16 / Med | S | 何も得ず層末選択を失う体験を防ぐ。価格・効果は維持。 |
| 5 | ライトテーマと HUD 背景の色を揃える | F07 / Med | S | HP/AP と終了操作の明暗差を確保する。ルール変更不要。 |
| 6 | HUD のクリック座標を FitViewport に揃える | F10 / Med | M | リサイズしても狙った操作が通る。F11 の resize 委譲も同時確認。仕様変更不要。 |
| 7 | タイトルの各文字をクリックで開けるようにする | F13 / Med | S | 「設定を押したら出発」を防ぎ、最初の画面で操作モデルを統一。仕様変更不要。 |
| 8 | 手札へ番号・AP・不足状態を動的表示 | F05 / Med | S | カードの選びやすさと失敗理由を改善。実行可能条件は維持。 |

次点の低コスト改善は F01 のチュートリアル再表示、F20 の持ち上げカードとヒット矩形の一致、F17 のイベント差分 flash。既知 #1 の表示調整は F08/K01 とまとめ、#2 の壁配置は既知資料どおり計測後に判断する。

## 検証範囲と残る確認

- 実施: 指定コピーのソース・表示文・仕様・既知所見の静的照合、座標/時間定数の計算、13 観点の根拠と提案の整理。
- 未実施: Gradle/テスト/ビルド、GUI 起動、実プレイ、ネットワーク照会。したがって実機での動作保証や「問題が再現した」という報告ではない。
- 将来の実機確認候補: 初回説明→カード選択→移動権の理解、Elite 報酬中の被弾/数字入力、1280×1024 と横長ウィンドウでのクリック、LIGHT での HP/AP/終了ラベル、英語 fallback の全 popup、選択カード上端クリック、泉の支払不足と成功時の差分、保存失敗/精算拒否時の復旧。GUI は今回の制約では起動していない。
- 変更した成果物はこの報告書のみ。指定コピーおよび元リポジトリへの変更・git 書き込みは行っていない。
