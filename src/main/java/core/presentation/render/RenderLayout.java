package core.presentation.render;

/**
 * 画面レイアウトの座標定数。マップ・HUD・ログ領域の配置を 1 か所にまとめる。
 *
 * <p>「驚き最小」: 描画コードでは値ではなくここの定数を参照する。
 *
 * <p>仮想解像度は 1920×1080 (16:9)。{@link com.badlogic.gdx.utils.viewport.FitViewport} で物理ピクセルに
 * マッピングするため、全座標は 1920×1080 の仮想空間で定義する。
 */
public final class RenderLayout {

  private RenderLayout() {}

  public static final int SCREEN_WIDTH = 1920;
  public static final int SCREEN_HEIGHT = 1080;

  /**
   * タイルサイズ (ピクセル)。48px を採用。
   *
   * <p>根拠: 10×10 マップ = 480px、余白 (1920-480)/2 = 720px → マップを水平中央配置できる。
   * 32 より大きく視認性が上がり、64 より小さくマップが画面を圧迫しない。
   * DotGothic16 (16px 倍数フォント) に対して 3 倍で整合する。
   */
  public static final int TILE_SIZE = 48;

  /**
   * ダンジョン描画開始位置 (仮想座標系の左下基準)。
   *
   * <p>MAP_ORIGIN_X: (1920 - 10 * 48) / 2 = 720 で水平中央。
   * MAP_ORIGIN_Y: 1080 の中央付近 (300) に配置し、上部にログ・右にHUDを展開。
   */
  public static final int MAP_ORIGIN_X = 720;

  public static final int MAP_ORIGIN_Y = 300;

  /**
   * HUD (HP/AP/Soul/Phase) 描画位置。画面右側に配置。
   *
   * <p>マップ右端: 720 + 10*48 = 1200。HUD_X=1380 で large (32px) フォントの長い文字列
   * (例「AP: 5 / 5 (速度 3 (+1))」≈ 520px) を画面右端まで収める。
   */
  public static final int HUD_X = 1380;

  // HUD は画面上部に集約。large (32px) フォントで描画するため、行間は 48px (LARGE_LINE_HEIGHT 相当) を取る。
  // BitmapFont は Y 座標がベースラインなので、32px フォントなら描画範囲は (Y-32, Y) を占有する。
  public static final int HUD_Y_HP = 1040;

  public static final int HUD_Y_AP = 992;
  public static final int HUD_Y_SOUL = 944;
  public static final int HUD_Y_PHASE = 896;

  /**
   * 移動権残量表示の Y 座標 (ADR-21 §15-5)。
   *
   * <p>HUD_Y_PHASE の 1 行下に配置する。pendingMoveCount > 0 のときのみ描画される。
   */
  public static final int HUD_Y_MOVE_TOKEN = 848;

  /**
   * 操作ヒント (WASD/矢印: 移動 等) の Y 座標。
   *
   * <p>large (32px) で画面左下に左寄せで配置。ログ最下 (Y=222) と HAND_LABEL (Y=112) の間に置く。
   * pendingCardIndex / movementToken / cleared モード時はここで HAND_HINT 等に切り替えるため、
   * drawHand 側のヒント描画は削除して二重表示を防ぐ。
   */
  public static final int HUD_Y_HINT = 170;

  /**
   * メッセージログ表示開始位置 (下方向に展開)。
   *
   * <p>画面下部、操作ヒントの上に配置。large (32px) で行間 LARGE_LINE_HEIGHT=48 を取り、2 行表示する
   * (大型化により情報視認性を上げる。3 行以上はヒント / 手札との衝突を起こす)。
   */
  public static final int LOG_X = 40;

  public static final int LOG_TOP_Y = 270;
  public static final int LOG_LINE_HEIGHT = 48;
  public static final int LOG_LINES_VISIBLE = 2;

  /**
   * 手札表示の Y 座標。ログ領域より上、画面下部に配置する。
   *
   * <p>large (32px) フォント前提。HAND_Y=64 でラベル (Y=112) が HUD_Y_HINT (170) と十分離れる。
   * 手札選択中のヒントは drawControlsHint 側に統合したため、HAND_Y 直下の余白は不要。
   */
  public static final int HAND_Y = 64;

  /**
   * 手札カード 1 文字あたりの概算ピクセル幅 (large 32px フォント基準)。
   *
   * <p>初期手札は数枚なので画面幅に収まる前提。9 枚揃うシナリオでは右側がはみ出る可能性があるが、
   * §15-3 の MAX_HAND_SIZE 想定でも実用上問題ないレベル。
   */
  public static final int HAND_CARD_GLYPH_WIDTH = 20;

  // --- TitleScreen / GameOverScreen 用レイアウト定数 ---

  /** タイトル文字列の描画 X 座標 (水平中央付近)。 */
  public static final int TITLE_TEXT_X = 680;

  /** タイトル文字列の描画 Y 座標 (画面上部 65%)。 */
  public static final int TITLE_TEXT_Y = (int) (SCREEN_HEIGHT * 0.65f);

  /** サブタイトル / SUBTITLE の X 座標。 */
  public static final int SUBTITLE_X = 720;

  /** サブタイトルの Y 座標。 */
  public static final int SUBTITLE_Y = TITLE_TEXT_Y - 60;

  /** START_HINT の X 座標 (中央付近)。 */
  public static final int START_HINT_X = 820;

  /** START_HINT の Y 座標。 */
  public static final int START_HINT_Y = (int) (SCREEN_HEIGHT * 0.43f);

  /**
   * large フォント (32px) の行高 (px)。
   *
   * <p>BitmapFont.getLineHeight() に対し視認性余白を加えた値。タイトル / 操作説明ブロック内の行送りに使う。
   */
  public static final int LARGE_LINE_HEIGHT = 48;

  /**
   * 「[T] ソウルツリーを開く」ヒントの Y 座標 (§15-7 / E-2)。
   *
   * <p>「ENTER で出発する」の 1 行下に配置し、出発 / ツリー解放という 2 つの出口を視覚的にまとめる。
   */
  public static final int TITLE_OPEN_TREE_HINT_Y = START_HINT_Y - LARGE_LINE_HEIGHT;

  /** コントロール説明の X 座標 (中央付近)。 */
  public static final int CONTROLS_HEADER_X = 840;

  /**
   * コントロール説明ヘッダーの Y 座標。
   *
   * <p>TITLE_OPEN_TREE_HINT (≒ 416) との視覚分離のため 80px 余白を空ける。
   */
  public static final int CONTROLS_HEADER_Y = TITLE_OPEN_TREE_HINT_Y - 80;

  /** コントロール各行の X 座標。 */
  public static final int CONTROLS_X = 720;

  /**
   * コントロール 1 行目の Y 座標。
   *
   * <p>「操作」ヘッダーとの間に 56px (LARGE_LINE_HEIGHT + 8) の余白を確保。
   */
  public static final int CONTROLS_ROW1_Y = CONTROLS_HEADER_Y - 56;

  /** コントロール行間隔 (px)。 large フォント前提。 */
  public static final int CONTROLS_LINE_HEIGHT = LARGE_LINE_HEIGHT;

  // --- GameOverScreen 用 ---

  /** ゲームオーバー / CLEARED ヘッダーの X 座標。 */
  public static final int GAMEOVER_HEADER_X = 760;

  /** ゲームオーバー / CLEARED ヘッダーの Y 座標。 */
  public static final int GAMEOVER_HEADER_Y = (int) (SCREEN_HEIGHT * 0.62f);

  /** ソウル数表示の X 座標。 */
  public static final int GAMEOVER_SOUL_X = 720;

  /** ソウル数表示の Y 座標。 */
  public static final int GAMEOVER_SOUL_Y = (int) (SCREEN_HEIGHT * 0.48f);

  /** NEW_RUN_HINT の X 座標。 */
  public static final int GAMEOVER_HINT_X = 760;

  /** NEW_RUN_HINT の Y 座標。 */
  public static final int GAMEOVER_HINT_Y = (int) (SCREEN_HEIGHT * 0.37f);
}
