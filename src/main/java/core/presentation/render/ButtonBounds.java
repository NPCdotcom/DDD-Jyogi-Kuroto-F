package core.presentation.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * 矩形クリック判定の純粋値オブジェクト (Wave 14 W14-β)。HUD 仮想空間 (1920×1080) 基準の矩形を持ち、 マウス入力との衝突判定を統一する。
 *
 * <p>CTO チェックポイント #3: 呼出側は **必ず HUD Viewport.unproject 経由で生スクリーン座標 (Gdx.input.getX/Y) を
 * 仮想座標に正規化してから {@link #contains}** を叩く。ウィンドウサイズ変更時に ButtonBounds 側 (固定 1920×1080) と 生スクリーン座標
 * (デバイスピクセル) が単位ずれするのを防ぐ。便利メソッド {@link #containsScreenInput} は 内部で {@code viewport.unproject}
 * を実施するため、各画面で重複させない (DRY)。
 */
public record ButtonBounds(int x, int y, int width, int height) {

  /**
   * 仮想座標 (1920×1080 空間) が矩形内かを判定する純関数。
   *
   * @param virtualX viewport.unproject 済の X (HUD 仮想 X)
   * @param virtualY viewport.unproject 済の Y (HUD 仮想 Y、上向き)
   * @return 矩形内 (境界を含む) なら true
   */
  public boolean contains(float virtualX, float virtualY) {
    return virtualX >= x && virtualX <= x + width && virtualY >= y && virtualY <= y + height;
  }

  /**
   * 生スクリーン座標 (Gdx.input.getX / Y) を HUD Viewport.unproject で仮想座標に正規化してから {@link #contains}
   * 判定する便利メソッド (CTO チェックポイント #3)。
   *
   * <p>各画面で「unproject → contains」を繰り返さないため DRY 集約。ウィンドウリサイズ・フルスクリーン切替後でも ピクセル単位の精度を保つ。
   *
   * @param hudViewport HUD カメラに紐付くビューポート (FitViewport 等)
   * @param rawScreenX {@link com.badlogic.gdx.Gdx#input}.getX() の生値
   * @param rawScreenY {@link com.badlogic.gdx.Gdx#input}.getY() の生値 (下向き、unproject が反転)
   * @return 矩形内なら true
   */
  public boolean containsScreenInput(Viewport hudViewport, int rawScreenX, int rawScreenY) {
    Vector3 tmp = new Vector3(rawScreenX, rawScreenY, 0);
    hudViewport.unproject(tmp);
    return contains(tmp.x, tmp.y);
  }

  /**
   * HUD 用 Viewport を持たない画面 (固定カメラのみで描画する Screen 群) 向けの簡易版 (Wave 14 W14-β)。
   *
   * <p>{@code Gdx.graphics.getWidth/Height} で生スクリーン解像度を取得し、1920×1080 仮想空間との 単純な stretch 比率変換で
   * contains 判定する。aspect 比保持 (黒帯) には非対応だが、既存 {@code DungeonScreen.handleHandMouseClick}
   * と同型の変換のため統合して使える。aspect 保持が必要な画面では 別途 HUD Viewport を持って {@link #containsScreenInput(Viewport,
   * int, int)} を使う。
   */
  public boolean containsScreenInput(int rawScreenX, int rawScreenY) {
    float screenW = Gdx.graphics.getWidth();
    float screenH = Gdx.graphics.getHeight();
    if (screenW <= 0 || screenH <= 0) {
      return false;
    }
    float virtualX = rawScreenX * (RenderLayout.SCREEN_WIDTH / screenW);
    float virtualY =
        RenderLayout.SCREEN_HEIGHT - (rawScreenY * (RenderLayout.SCREEN_HEIGHT / screenH));
    return contains(virtualX, virtualY);
  }
}
