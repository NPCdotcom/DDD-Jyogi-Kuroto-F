package core.presentation.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import core.domain.card.Card;
import core.domain.card.CardElement;

/**
 * コード描画によるカード描画基盤 (Phase 5-3 / 5-4 / 5-5)。
 *
 * <p>1x1 white texture とコード描画により、枠線・ヘッダー・属性色・コスト・アート・詳細テキストを 解像度に依存しない Canonical Layout (基準
 * 120x168) で拡縮合成描画する。
 */
public final class CardRenderer {

  /** 基準幅 (px)。 */
  public static final float CANONICAL_WIDTH = 120f;

  /** 基準高 (px)。 */
  public static final float CANONICAL_HEIGHT = 168f;

  /** カード背景色 (暗めのスレートグレー)。 */
  private static final Color CARD_BG_COLOR = new Color(0.12f, 0.12f, 0.16f, 0.95f);

  /** 枠線色 (メタリックシルバー)。 */
  private static final Color CARD_BORDER_COLOR = new Color(0.75f, 0.75f, 0.82f, 1f);

  private CardRenderer() {}

  /**
   * 単一カードテクスチャを描画する互換メソッド (Wave 10 互換)。
   *
   * @param batch SpriteBatch (begin 済み)
   * @param cardImage カード画像 (null なら no-op)
   * @param x 描画 X (左下)
   * @param y 描画 Y (左下)
   * @param w 描画幅 (px)
   * @param h 描画高 (px)
   * @param tintColor 全体ティント色
   */
  public static void drawCard(
      SpriteBatch batch, Texture cardImage, float x, float y, float w, float h, Color tintColor) {
    if (cardImage == null) {
      return;
    }
    batch.setColor(tintColor);
    batch.draw(cardImage, x, y, w, h);
    batch.setColor(Color.WHITE); // 色リーク防止
  }

  /**
   * カードをコード枠合成で描画する (Phase 5-4)。
   *
   * @param batch SpriteBatch (begin 済み)
   * @param whiteTexture 1x1 白テクスチャ
   * @param font テキスト描画用フォント (null 可)
   * @param card カードドメインエンティティ
   * @param artTexture イラストテクスチャ (null 可)
   * @param x 描画 X (左下)
   * @param y 描画 Y (左下)
   * @param w 描画幅 (px)
   * @param h 描画高 (px)
   * @param tintColor 全体ティント色 (選択時は薄黄等)
   * @param mode 描画モード (FULL / THUMBNAIL / ICON)
   * @param isJapanese 日本語表示フラグ
   */
  public static void drawCardComposite(
      SpriteBatch batch,
      Texture whiteTexture,
      BitmapFont font,
      Card card,
      Texture artTexture,
      float x,
      float y,
      float w,
      float h,
      Color tintColor,
      CardRenderMode mode,
      boolean isJapanese) {
    if (batch == null || whiteTexture == null || card == null) {
      return;
    }

    float scaleX = w / CANONICAL_WIDTH;
    float scaleY = h / CANONICAL_HEIGHT;
    float borderWidth = Math.max(1f, 3f * Math.min(scaleX, scaleY));

    // 1. 外枠 (Border) 描画
    batch.setColor(multiplyColors(CARD_BORDER_COLOR, tintColor));
    batch.draw(whiteTexture, x, y, w, h);

    // 2. カード背景描画 (内側 Rect)
    batch.setColor(multiplyColors(CARD_BG_COLOR, tintColor));
    batch.draw(
        whiteTexture, x + borderWidth, y + borderWidth, w - borderWidth * 2f, h - borderWidth * 2f);

    if (mode == CardRenderMode.ICON) {
      // ICON モード: アート中心描画
      if (artTexture != null) {
        batch.setColor(tintColor);
        batch.draw(
            artTexture,
            x + borderWidth * 2f,
            y + borderWidth * 2f,
            w - borderWidth * 4f,
            h - borderWidth * 4f);
      }
      batch.setColor(Color.WHITE);
      return;
    }

    // 3. ヘッダー帯 (属性カラー) 描画
    Color elementColor = elementColor(card.element());
    float headerHeight = 24f * scaleY;
    float headerY = y + h - borderWidth - headerHeight;
    batch.setColor(multiplyColors(elementColor, tintColor));
    batch.draw(whiteTexture, x + borderWidth, headerY, w - borderWidth * 2f, headerHeight);

    // 4. アート領域描画 (中央上寄り)
    float artHeight = 72f * scaleY;
    float artY = headerY - artHeight - 2f * scaleY;
    float artWidth = w - borderWidth * 4f;
    float artX = x + borderWidth * 2f;

    if (artTexture != null) {
      batch.setColor(tintColor);
      batch.draw(artTexture, artX, artY, artWidth, artHeight);
    } else {
      // プレースホルダーイラスト背景
      Color placeholderColor = new Color(0.2f, 0.22f, 0.28f, 1f);
      batch.setColor(multiplyColors(placeholderColor, tintColor));
      batch.draw(whiteTexture, artX, artY, artWidth, artHeight);
    }

    // 5. テキスト描画 (FULL モードのみ)
    if (mode == CardRenderMode.FULL && font != null) {
      font.setColor(Color.WHITE);
      // コスト描画 (左上)
      font.draw(batch, "AP" + card.apCost(), x + borderWidth + 4f * scaleX, y + h - 6f * scaleY);
      // カード名描画 (ヘッダー内)
      font.draw(batch, card.displayName(), x + borderWidth + 42f * scaleX, y + h - 6f * scaleY);
    }

    // 色リーク防止 (Wave 4 W4-ε 原則)
    batch.setColor(Color.WHITE);
  }

  /** 属性に応じたアクセントカラーを返す (PHYSICAL: 鉄灰, MAGICAL: 藍紫)。 */
  public static Color elementColor(CardElement element) {
    if (element == null) {
      return new Color(0.6f, 0.6f, 0.65f, 1f);
    }
    return switch (element) {
      case PHYSICAL -> new Color(0.72f, 0.72f, 0.76f, 1f); // 鉄灰
      case MAGICAL -> new Color(0.45f, 0.55f, 0.88f, 1f); // 藍紫
    };
  }

  private static Color multiplyColors(Color base, Color tint) {
    if (tint == null || tint.equals(Color.WHITE)) {
      return base;
    }
    return new Color(base.r * tint.r, base.g * tint.g, base.b * tint.b, base.a * tint.a);
  }
}
