package core.presentation.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import core.domain.card.Card;
import core.domain.card.CardElement;

/**
 * カードを枠 + イラスト + コスト + 名前 + element 色 で合成描画するユーティリティ (Wave 10 W10-γ)。
 *
 * <p>元 100×100 px の {@code card_frame.png} を任意の表示サイズにスケールし、各枠の相対座標も同スケールで乗算する。 描画順は Z-Index 重要 (CTO
 * レビュー #5):
 *
 * <ol>
 *   <li>奥: イラスト枠の背景 (既存カード Texture、イラスト枠内)
 *   <li>中: card_frame.png を重ねて端を綺麗に隠す
 *   <li>手前: element 色枠 + コスト + 名前を最前面
 * </ol>
 *
 * <p>名前枠の文字描画は {@link GlyphLayout} で幅計測し、はみ出す場合は文字列を切り詰める (CTO レビュー #3 / #4)。
 *
 * <p>純粋静的ユーティリティ。状態を保持しない (NAME_LAYOUT は local-scope 上書き専用)。
 */
public final class CardRenderer {

  // ---- 元 100×100 px 座標 (描画時に scale 乗算) ----
  private static final float FRAME_BASE = 100f;
  private static final float COST_X = 4f;
  private static final float COST_Y = 4f;
  private static final float COST_W = 13f;
  private static final float COST_H = 13f;
  private static final float RARITY_X = 78f;
  private static final float RARITY_Y = 2f;
  private static final float RARITY_W = 15f;
  private static final float RARITY_H = 15f;
  private static final float ART_X = 16f;
  private static final float ART_Y = 16f;
  private static final float ART_W = 63f;
  private static final float ART_H = 63f;
  private static final float NAME_X = 10f;
  private static final float NAME_Y_TOP = 99f; // 名前枠上端 (元 100 px 座標、Y は下から上に増える)
  private static final float NAME_W = 75f;

  // ---- element 色枠 (Card record に rarity 無いため CardElement で 2 色のみ、Wave 11+ で Rarity 拡張) ----
  private static final Color PHYSICAL_COLOR = new Color(0.7f, 0.7f, 0.7f, 1f); // 灰
  private static final Color MAGICAL_COLOR = new Color(0.75f, 0.45f, 0.95f, 1f); // 紫

  /**
   * フォント幅計測用の static GlyphLayout (アロケーション 0、CTO レビュー #4 反映)。
   *
   * <p>同一スレッド (LibGDX render) の直列スコープでのみ使用、再利用時は {@link GlyphLayout#setText(BitmapFont,
   * CharSequence)} で完全上書き。
   */
  private static final GlyphLayout NAME_LAYOUT = new GlyphLayout();

  private CardRenderer() {}

  /**
   * カードを枠 + イラスト + コスト + 名前 + element 色 で合成描画する (Z-Index 順序遵守)。
   *
   * @param batch SpriteBatch (begin 済み)
   * @param font カード名 / コスト用フォント (typically Fonts.hud or large)
   * @param card 描画対象カード
   * @param cardImage カードイラスト (CardImageRegistry から取得、null 可 = イラスト枠空白)
   * @param frame card_frame.png Texture
   * @param x 描画 X (左上)
   * @param y 描画 Y (左下、LibGDX 標準)
   * @param w 描画幅 (px、100 → scale 乗算用)
   * @param h 描画高 (px、100 → scale 乗算用)
   * @param tintColor 全体ティント色 (選択中の黄色等、Color.WHITE で通常描画)
   */
  public static void drawCard(
      SpriteBatch batch,
      BitmapFont font,
      Card card,
      Texture cardImage,
      Texture frame,
      float x,
      float y,
      float w,
      float h,
      Color tintColor) {
    float scale = w / FRAME_BASE; // 元 100 px に対するスケール (height ベースでも可、w = h 前提なら同じ)

    // (1) 奥: イラスト枠の背景 = カード Texture (元 ART_Y は上端、LibGDX Y は下から なので反転)
    if (cardImage != null) {
      batch.setColor(tintColor);
      float artDrawX = x + ART_X * scale;
      // 元 100px 座標は Y 軸下向き = LibGDX (下から上に Y+) に変換するため反転計算
      float artDrawY = y + h - (ART_Y + ART_H) * scale;
      batch.draw(cardImage, artDrawX, artDrawY, ART_W * scale, ART_H * scale);
    }

    // (2) 中: card_frame.png を重ね描画 (イラスト端を綺麗に隠す)
    batch.setColor(tintColor);
    batch.draw(frame, x, y, w, h);

    // (3) 手前: element 色枠 (レアリティ枠位置)、batch.setColor の stateful 性に注意し、必ず Color.WHITE リセット
    Color elementColor = (card.element() == CardElement.PHYSICAL) ? PHYSICAL_COLOR : MAGICAL_COLOR;
    batch.setColor(elementColor);
    float rarityDrawY = y + h - (RARITY_Y + RARITY_H) * scale;
    // 単色四角描画は SpriteBatch だと frame の一部を流用する形にしたいが、四角だけの Texture が無いので
    // frame Texture を 1×1 で薄く塗る方式は避け、ShapeRenderer 経由は別 begin/end が必要 → ここでは frame の
    // 隅の単色領域を流用する (rarity 枠は frame 内に既に存在すれば視覚的に重なるだけ、欠落しても許容)
    batch.draw(
        frame,
        x + RARITY_X * scale,
        rarityDrawY,
        RARITY_W * scale,
        RARITY_H * scale,
        0,
        0,
        1,
        1); // 1×1 サンプリングで単色塗り (frame の左上 1px を引き伸ばす)
    batch.setColor(Color.WHITE); // ★ 必ずリセット (色リーク防止、Wave 4 / W10-β CTO レビュー #2)

    // 手前: コスト数値 (font 描画、batch は引き続き begin 中)
    font.setColor(Color.WHITE);
    float costDrawY = y + h - COST_Y * scale; // フォントは baseline、Y 上端付近に描画
    font.draw(batch, String.valueOf(card.apCost()), x + COST_X * scale, costDrawY);

    // 手前: 名前 (GlyphLayout 計測 + はみ出し時切り詰め、static 再利用)
    float nameWidthPx = NAME_W * scale;
    String drawText = card.displayName();
    NAME_LAYOUT.setText(font, drawText);
    if (NAME_LAYOUT.width > nameWidthPx) {
      // 1 文字ずつ削って "…" 追加 (KISS、W10-γ 戦略 2)
      for (int len = drawText.length() - 1; len > 0; len--) {
        String candidate = drawText.substring(0, len) + "…";
        NAME_LAYOUT.setText(font, candidate);
        if (NAME_LAYOUT.width <= nameWidthPx) {
          drawText = candidate;
          break;
        }
      }
    }
    float nameDrawY = y + h - NAME_Y_TOP * scale + NAME_LAYOUT.height; // baseline 調整
    font.draw(batch, drawText, x + NAME_X * scale, nameDrawY);
    font.setColor(Color.WHITE); // 念のためリセット
  }
}
