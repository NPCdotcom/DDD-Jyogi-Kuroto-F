package core.presentation.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * カード手札の 1 枚分を描画するユーティリティ。
 *
 * <p>2026-05-24: 全カードを完成形カード絵 (枠 + AP + 名前 + イラスト + element マーク すべて画像に焼き込み) に統一する方針に伴い、 旧 Wave 10
 * W10-γ の合成描画 (frame 重ね + element 色枠 + AP コスト + 名前 GlyphLayout) を完全廃止。 本実装は {@code card_XX.png} を
 * tintColor 付きで 1 枚描画するだけ。
 *
 * <p>選択中ハイライト等の動的フィードバックは tintColor で表現する (黄色ティント等は呼出側 {@link HudRenderer} の判断)。
 */
public final class CardRenderer {

  private CardRenderer() {}

  /**
   * カード画像を 1 枚描画する。
   *
   * @param batch SpriteBatch (begin 済み)
   * @param cardImage カード画像 (null なら no-op)
   * @param x 描画 X (左下)
   * @param y 描画 Y (左下、LibGDX 標準)
   * @param w 描画幅 (px)
   * @param h 描画高 (px)
   * @param tintColor 全体ティント色 (選択中は薄黄等、通常時は Color.WHITE)
   */
  public static void drawCard(
      SpriteBatch batch, Texture cardImage, float x, float y, float w, float h, Color tintColor) {
    if (cardImage == null) {
      return;
    }
    batch.setColor(tintColor);
    batch.draw(cardImage, x, y, w, h);
    batch.setColor(Color.WHITE); // 色リーク防止 (Wave 4 W4-ε 同型原則)
  }
}
