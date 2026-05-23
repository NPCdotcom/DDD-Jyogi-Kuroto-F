package core.presentation.effect;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import core.presentation.render.RenderLayout;

/**
 * HP 低下警告演出 (§7-2 / C-2)。HP 比率が閾値以下のとき、画面端に脈動する赤いビネット (フレーム枠) を描画する。 Hades 風の juicy
 * フィードバックで「やばい」感を可視化する。
 *
 * <p>純粋な描画ユーティリティ。アニメ時刻 (累積秒) は呼出側 (DungeonScreen) が管理する。 ShapeRenderer の Filled モードで使用する。
 */
public final class LowHpWarning {

  /** この比率以下で警告表示 (= HP が maxHp の 30% 以下)。 */
  public static final float HP_THRESHOLD = 0.30f;

  /** フレーム枠の厚み (px、画面端から内側に塗る矩形)。 */
  private static final float FRAME_THICKNESS = 80f;

  /** アルファ脈動の中心値と振幅 (0.10 〜 0.50 の範囲で揺れる)。 */
  private static final float ALPHA_BASE = 0.30f;

  private static final float ALPHA_AMP = 0.20f;

  /** 脈動速度 (rad/秒)、約 0.75 サイクル/秒。 */
  private static final float PULSE_SPEED = 4.7f;

  private LowHpWarning() {}

  /**
   * 警告を描画すべきか判定する。HP が 0 (= 死亡) の場合は GameOverScreen が担当するため警告を出さない。
   *
   * @param currentHp 現在 HP
   * @param maxHp 最大 HP
   * @return HP 比率が閾値以下かつ生存中なら true
   */
  public static boolean shouldDraw(int currentHp, int maxHp) {
    if (maxHp <= 0 || currentHp <= 0) {
      return false;
    }
    return ((float) currentHp / (float) maxHp) <= HP_THRESHOLD;
  }

  /**
   * 警告フレーム (画面端の赤い枠) を描画する。{@code shapes.begin(Filled)} 〜 {@code end()} は呼出側が管理する。 α ブレンドのため {@code
   * GL_BLEND} が有効になっている必要がある。
   *
   * @param shapes 描画用 ShapeRenderer (Filled モードで begin 済み)
   * @param animTimeSeconds アニメ時刻 (累積秒)
   */
  public static void draw(ShapeRenderer shapes, float animTimeSeconds) {
    float alpha = ALPHA_BASE + ALPHA_AMP * (float) Math.sin(animTimeSeconds * PULSE_SPEED);
    if (alpha < 0f) {
      alpha = 0f;
    }
    shapes.setColor(0.85f, 0.15f, 0.20f, alpha);
    float w = RenderLayout.SCREEN_WIDTH;
    float h = RenderLayout.SCREEN_HEIGHT;
    float t = FRAME_THICKNESS;
    // 上下左右の 4 帯 (中央は素抜け)。重複描画を避けるため上下は full width、左右は上下を除く高さ。
    shapes.rect(0f, 0f, w, t); // 下
    shapes.rect(0f, h - t, w, t); // 上
    shapes.rect(0f, t, t, h - 2f * t); // 左
    shapes.rect(w - t, t, t, h - 2f * t); // 右
  }
}
