package core.presentation.render;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import core.domain.common.Direction;
import core.domain.common.Position;
import core.infrastructure.save.UiPreset;
import java.util.Optional;

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

  // =========================================================================
  // §15-1 / §15-8 UI プリセット参照ヘルパー
  // =========================================================================

  /**
   * 指定プリセットで HP / AP 以外の HUD 行 (Soul / Phase / Gold) を表示すべきかを返す。
   *
   * <p>MINIMAL では非表示、STANDARD / INFO_RICH では表示する。
   *
   * @param preset 現在の UI プリセット
   * @return true なら Soul / Phase / Gold を描画する
   */
  public static boolean showExtendedHud(UiPreset preset) {
    return preset != UiPreset.MINIMAL;
  }

  /**
   * 指定プリセットで操作ヒント行を常時表示すべきかを返す。
   *
   * <p>INFO_RICH のみ常時表示。MINIMAL / STANDARD はゲーム内状態に応じた動的表示に委ねる。
   *
   * @param preset 現在の UI プリセット
   * @return true なら操作ヒントを常時描画する
   */
  public static boolean showPersistentHint(UiPreset preset) {
    return preset == UiPreset.INFO_RICH;
  }

  // =========================================================================
  // Wave 14 W14-α: マウス入力 → タイル座標 / 方向 の変換ヘルパ
  // =========================================================================

  /**
   * スクリーン座標 (Gdx.input.getX/Y の生値) を Viewport.unproject 経由でタイル座標へ変換する (Wave 14 W14-α)。
   *
   * <p>CTO チェックポイント #1: ウィンドウサイズ変更時の Viewport レターボックス (黒帯) を考慮するため、 必ず {@code viewport.unproject}
   * 経由でワールド座標化する。{@code camera.unproject} 単体ではビューポートの screenX/Y/Width/Height
   * を考慮できず、リサイズ後にクリック位置がズレる。
   *
   * @param mapViewport マップカメラに紐付くビューポート (FitViewport 等)
   * @param screenX {@link com.badlogic.gdx.Gdx#input}.getX() の生値
   * @param screenY {@link com.badlogic.gdx.Gdx#input}.getY() の生値 (下向き、viewport.unproject が内部反転)
   * @return タイル座標 (床/壁関わらず、マップ範囲内外も問わず純粋な算出)
   */
  public static Position screenToTile(Viewport mapViewport, int screenX, int screenY) {
    Vector2 worldPos = mapViewport.unproject(new Vector2(screenX, screenY));
    int tileX = (int) Math.floor((worldPos.x - MAP_ORIGIN_X) / TILE_SIZE);
    int tileY = (int) Math.floor((worldPos.y - MAP_ORIGIN_Y) / TILE_SIZE);
    return new Position(tileX, tileY);
  }

  /**
   * 起点 {@code from} から目標 {@code to} への 4 方向のうち主要方向を返す純関数 (Wave 14 W14-α)。
   *
   * <p>CTO チェックポイント #2: {@code dx == 0 && dy == 0} (自分マス) は最初のガードで {@code Optional.empty()}
   * を返す。これを怠ると {@code |0| >= |0|} 比較が true を通過し、RIGHT 誤発火による自爆移動バグが発生する。
   *
   * <p>方向決定ルール: {@code |dx| >= |dy|} なら横方向 (dx > 0 → RIGHT, dx < 0 → LEFT)、それ以外は縦方向 (dy &gt; 0 →
   * UP, dy &lt; 0 → DOWN)。完全な斜め (|dx| == |dy|) は横優先で安定する (テスト再現性)。
   *
   * @return 主要方向、または同一マスなら {@link Optional#empty()}
   */
  public static Optional<Direction> directionToward(Position from, Position to) {
    int dx = to.x() - from.x();
    int dy = to.y() - from.y();
    if (dx == 0 && dy == 0) {
      return Optional.empty();
    }
    if (Math.abs(dx) >= Math.abs(dy)) {
      return Optional.of(dx > 0 ? Direction.RIGHT : Direction.LEFT);
    }
    return Optional.of(dy > 0 ? Direction.UP : Direction.DOWN);
  }

  public static final int SCREEN_WIDTH = 1920;
  public static final int SCREEN_HEIGHT = 1080;

  /**
   * タイルサイズ (ピクセル)。80px を採用。
   *
   * <p>BSP マップ (層 1≈14×12 〜 層 3≈26×20) をプレイヤー追従カメラで表示する前提。80px なら層 2 以降の マップが 1920×1080
   * の視界を超えてスクロールが機能し、かつタイル 1 つが十分大きく視認できる。
   */
  public static final int TILE_SIZE = 80;

  /** ダンジョン描画原点。マップはワールド座標 (0,0) 始点で描画し、画面内の位置決めは {@code DungeonScreen} の プレイヤー追従カメラが担う (§15-6)。 */
  public static final int MAP_ORIGIN_X = 0;

  public static final int MAP_ORIGIN_Y = 0;

  /**
   * HUD (HP/AP/Soul/Phase) 描画位置。画面右上に固定カメラで配置 (§15-6 追従カメラのマップに重畳)。
   *
   * <p>HUD_X=1380 で large (32px) フォントの長い文字列 (例「AP: 5 / 5 (速度 3 (+1))」≈ 520px) を画面右端まで収める。
   */
  public static final int HUD_X = 1380;

  // HUD は画面上部に集約。large (32px) フォントで描画するため、行間は 48px (LARGE_LINE_HEIGHT 相当) を取る。
  // BitmapFont は Y 座標がベースラインなので、32px フォントなら描画範囲は (Y-32, Y) を占有する。
  public static final int HUD_Y_HP = 1040;

  public static final int HUD_Y_AP = 992;
  public static final int HUD_Y_SOUL = 944;
  public static final int HUD_Y_GOLD = 896;
  public static final int HUD_Y_PHASE = 848;

  /**
   * 移動権残量表示の Y 座標 (ADR-21 §15-5)。
   *
   * <p>HUD_Y_PHASE の 1 行下に配置する。pendingMoveCount > 0 のときのみ描画される。
   */
  public static final int HUD_Y_MOVE_TOKEN = 800;

  /**
   * 操作ヒント (WASD/矢印: 移動 等) の Y 座標。
   *
   * <p>large (32px) で画面左下に左寄せで配置。ログ最下 (Y=222) と HAND_LABEL (Y=112) の間に置く。 pendingCardIndex /
   * movementToken / cleared モード時はここで HAND_HINT 等に切り替えるため、 drawHand 側のヒント描画は削除して二重表示を防ぐ。
   */
  public static final int HUD_Y_HINT = 170;

  /**
   * メッセージログ表示開始位置 (下方向に展開)。
   *
   * <p>画面中段右寄り、HUD パネル + 手札 + カード詳細テキストの上に配置。large (32px) で行間 LARGE_LINE_HEIGHT=48 を取り、 2 行表示する。
   *
   * <p>LOG_TOP_Y=520 → ログ占有範囲 Y∈[472, 520]。手札カード画像 (HAND_CARD_BOTTOM_Y=110 + HAND_CARD_HEIGHT=168 =
   * Y∈[110, 278]) およびカード詳細テキスト (HAND_DETAIL_TEXT_Y=80、画像の下に配置) と衝突せず、 HUD_Y_HINT=170 /
   * HUD_Y_MOVE_TOKEN=800 とも干渉しない安全領域。
   */
  public static final int LOG_X = 40;

  public static final int LOG_TOP_Y = 520;
  public static final int LOG_LINE_HEIGHT = 48;
  public static final int LOG_LINES_VISIBLE = 2;

  /**
   * 手札表示の Y 座標。ログ領域より上、画面下部に配置する。
   *
   * <p>large (32px) フォント前提。HAND_Y=64 でラベル (Y=112) が HUD_Y_HINT (170) と十分離れる。 手札選択中のヒントは
   * drawControlsHint 側に統合したため、HAND_Y 直下の余白は不要。
   */
  public static final int HAND_Y = 64;

  // §15-3 大型手札描画 (カード画像 120x168)。1 枚分の slot 幅 = WIDTH + MARGIN。9 枚で 1224 px、画面幅 1920 に収まる。
  public static final int HAND_CARD_WIDTH = 120;
  public static final int HAND_CARD_HEIGHT = 168;
  public static final int HAND_CARD_MARGIN = 16;

  /** 手札列の左下 Y (画面下からの余白)。 */
  public static final int HAND_CARD_BOTTOM_Y = 110;

  /** 選択中カードを縦方向に持ち上げる量 (視覚強調)。 */
  public static final int HAND_CARD_SELECTED_LIFT = 20;

  /**
   * 選択中カードの詳細テキスト (displayName + AP + 効果説明) を描画する Y 座標。
   *
   * <p>カード画像 (Y∈[110, 278]) と被らないよう、画像の**下** (画面下端側) に配置する。 large (32px) baseline Y=80 → 描画範囲
   * Y∈[48, 80]、画面下端から 48px 余白、HAND_CARD_BOTTOM_Y=110 とも 30px 余白、HUD_Y_HINT=170 とも 90px 離れて干渉なし。
   */
  public static final int HAND_DETAIL_TEXT_Y = 80;

  /** 手札 1 枚目の左下 X 座標 (9 枚センター配置: (1920 - 9*120 - 8*16) / 2 = 356)。 */
  public static final int HAND_FIRST_X = 356;

  /** 画面下の HUD パネル (手札背景) の高さ (px、§15-3 / §15-6 カメラオフセット計算で使う)。 */
  public static final int HUD_BOTTOM_PANEL_HEIGHT = 300;

  // Wave 15 W15-β / Wave 16 W16-α #6: スキル枠 HUD は完全廃止 (SkillSlot 廃止、デッキ一本化、§15-3 整合)。
  // 旧 SKILL_SLOT_FIRST_X / SKILL_SLOT_Y / SKILL_SLOT_SIZE / SKILL_SLOT_MARGIN は削除済。
  // 画面下部は HUD_BOTTOM_PANEL_HEIGHT 内で手札 (HAND_*) と操作ヒント (HUD_Y_HINT) のみで構成される。

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
  // タイトル画面: [S]/[K] 行 (= TITLE_OPEN_TREE_HINT_Y - LARGE_LINE_HEIGHT*2 = 320) との重なり回避のため
  // 「操作」ヘッダーを LARGE_LINE_HEIGHT*3 + 16 = 160 px 下へずらす。結果 Y=256、十分な余白を確保。
  public static final int CONTROLS_HEADER_Y = TITLE_OPEN_TREE_HINT_Y - LARGE_LINE_HEIGHT * 3 - 16;

  /** コントロール各行の X 座標。 */
  public static final int CONTROLS_X = 720;

  /**
   * コントロール 1 行目の Y 座標。
   *
   * <p>「操作」ヘッダーとの間に 56px (LARGE_LINE_HEIGHT + 8) の余白を確保。
   */
  public static final int CONTROLS_ROW1_Y = CONTROLS_HEADER_Y - LARGE_LINE_HEIGHT;

  /** コントロール行間隔 (px)。 large フォント前提。 */
  public static final int CONTROLS_LINE_HEIGHT = LARGE_LINE_HEIGHT;

  // --- SettingsScreen / FirstRunPresetScreen 用 ---

  /** 設定画面タイトルの X 座標。 */
  public static final int SETTINGS_TITLE_X = 820;

  /** 設定画面タイトルの Y 座標。 */
  public static final int SETTINGS_TITLE_Y = (int) (SCREEN_HEIGHT * 0.85f);

  /** 設定項目の X 座標 (ラベル)。 */
  public static final int SETTINGS_LABEL_X = 560;

  /** 設定項目の X 座標 (値)。 */
  public static final int SETTINGS_VALUE_X = 960;

  /** 設定項目リスト先頭の Y 座標。 */
  public static final int SETTINGS_ROW_TOP_Y = (int) (SCREEN_HEIGHT * 0.70f);

  /** 設定項目の行間隔 (px)。 */
  public static final int SETTINGS_ROW_HEIGHT = LARGE_LINE_HEIGHT + 16;

  /** 設定画面操作ヒントの Y 座標。 */
  public static final int SETTINGS_HINT_Y = (int) (SCREEN_HEIGHT * 0.12f);

  /** 初回プリセット選択タイトルの X 座標。 */
  public static final int PRESET_TITLE_X = 560;

  /** 初回プリセット選択タイトルの Y 座標。 */
  public static final int PRESET_TITLE_Y = (int) (SCREEN_HEIGHT * 0.78f);

  /** 初回プリセット選択肢の X 座標。 */
  public static final int PRESET_CHOICE_X = 560;

  /** 初回プリセット選択肢の先頭 Y 座標。 */
  public static final int PRESET_CHOICE_TOP_Y = (int) (SCREEN_HEIGHT * 0.62f);

  /** 初回プリセット選択肢の行間隔 (px)。 */
  public static final int PRESET_CHOICE_HEIGHT = LARGE_LINE_HEIGHT * 2 + 8;

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
