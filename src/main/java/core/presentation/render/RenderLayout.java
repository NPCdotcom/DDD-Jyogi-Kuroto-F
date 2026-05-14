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
   * HUD (HP/AP/Soul/Phase) 描画位置。画面右側 (x=1420) に配置。
   *
   * <p>マップ右端: 720 + 10*48 = 1200。HUD_X=1420 でマップとの間に余白 220px を確保。
   */
  public static final int HUD_X = 1420;

  // HUD は画面上部に集約 (final-architect レビュー指摘で、マップ上端 Y=780 と
  // 重ならないよう 1000+ に再配置)。BitmapFont は Y 座標がベースラインなので
  // 16px フォントなら描画範囲は (Y-16, Y) を占有する。
  public static final int HUD_Y_HP = 1040;

  public static final int HUD_Y_AP = 1010;
  public static final int HUD_Y_SOUL = 980;
  public static final int HUD_Y_PHASE = 950;

  /**
   * 移動権残量表示の Y 座標 (ADR-21 §15-5)。
   *
   * <p>HUD_Y_PHASE=950 の 1 行下 (950 - 30 = 920) に配置する。pendingMoveCount > 0 のときのみ描画される。
   */
  public static final int HUD_Y_MOVE_TOKEN = 920;

  /**
   * メッセージログ表示開始位置 (下方向に展開)。
   *
   * <p>画面下部に配置。LOG_TOP_Y=230 から下に向かって各行を展開。
   */
  public static final int LOG_X = 40;

  public static final int LOG_TOP_Y = 230;
  public static final int LOG_LINE_HEIGHT = 28;
  public static final int LOG_LINES_VISIBLE = 6;

  /**
   * 手札表示の Y 座標。ログ領域より下、画面最下部付近に配置する。
   *
   * <p>LOG_TOP_Y=230、LOG_LINE_HEIGHT=28、LOG_LINES_VISIBLE=6 で最低 Y=230-(6-1)*28=90。
   * HAND_Y=50 なら 1 行分の余白を確保できる。
   */
  public static final int HAND_Y = 50;

  /**
   * 手札カード 1 文字あたりの概算ピクセル幅 (HUD_SIZE=16px フォント基準)。
   *
   * <p>1920px 幅なら 1920 / 12 = 160 文字相当で最大 9 枚が並ぶ。
   */
  public static final int HAND_CARD_GLYPH_WIDTH = 12;

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

  /** コントロール説明の X 座標 (中央付近)。 */
  public static final int CONTROLS_HEADER_X = 840;

  /** コントロール説明ヘッダーの Y 座標。 */
  public static final int CONTROLS_HEADER_Y = (int) (SCREEN_HEIGHT * 0.37f);

  /** コントロール各行の X 座標。 */
  public static final int CONTROLS_X = 720;

  /** コントロール 1 行目の Y 座標。 */
  public static final int CONTROLS_ROW1_Y = (int) (SCREEN_HEIGHT * 0.33f);

  /** コントロール行間隔 (px)。 */
  public static final int CONTROLS_LINE_HEIGHT = 32;

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
