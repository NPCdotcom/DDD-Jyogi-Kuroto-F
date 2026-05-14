package core.presentation.render;

/**
 * 画面レイアウトの座標定数。マップ・HUD・ログ領域の配置を 1 か所にまとめる。
 *
 * <p>「驚き最小」: 描画コードでは値ではなくここの定数を参照する。
 */
public final class RenderLayout {

  private RenderLayout() {}

  public static final int SCREEN_WIDTH = 800;
  public static final int SCREEN_HEIGHT = 600;
  public static final int TILE_SIZE = 32;

  /** ダンジョン描画開始位置 (画面ピクセル座標の左下基準)。 */
  public static final int MAP_ORIGIN_X = 40;

  public static final int MAP_ORIGIN_Y = 240;

  /** HUD (HP/AP/Soul) 描画位置。 */
  public static final int HUD_X = 40;

  public static final int HUD_Y_HP = 210;
  public static final int HUD_Y_AP = 190;
  public static final int HUD_Y_SOUL = 170;
  public static final int HUD_Y_PHASE = 150;

  /** メッセージログ表示開始位置 (下方向に展開)。 */
  public static final int LOG_X = 40;

  public static final int LOG_TOP_Y = 120;
  public static final int LOG_LINE_HEIGHT = 18;
  public static final int LOG_LINES_VISIBLE = 5;

  /**
   * 手札表示の Y 座標。ログ領域より下、画面最下部付近に配置する。
   *
   * <p>LOG_TOP_Y=120、LOG_LINE_HEIGHT=18、LOG_LINES_VISIBLE=5 で最低 Y 座標は 120-(5-1)*18=48 程度。
   * HAND_Y=30 なら 1 行分の余白を確保でき、手札行と重ならない。
   */
  public static final int HAND_Y = 30;
}
