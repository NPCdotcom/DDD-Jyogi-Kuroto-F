package core.presentation.render;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.badlogic.gdx.graphics.Color;
import core.domain.card.CardElement;
import org.junit.jupiter.api.Test;

class CardRendererTest {

  @Test
  void canonicalDimensionsMatchSpecification() {
    assertEquals(120f, CardRenderer.CANONICAL_WIDTH, 1e-4f);
    assertEquals(168f, CardRenderer.CANONICAL_HEIGHT, 1e-4f);
  }

  @Test
  void elementColorReturnsNonNullForAllElements() {
    for (CardElement element : CardElement.values()) {
      Color color = CardRenderer.elementColor(element);
      assertNotNull(color, "CardElement " + element + " のアクセントカラーは non-null");
    }
  }

  @Test
  void elementColorHandlesNullSafely() {
    Color color = CardRenderer.elementColor(null);
    assertNotNull(color, "null element でもフォールバックカラーを返すこと");
  }

  @Test
  void drawCardHandlesNullTextureSafely() {
    assertDoesNotThrow(() -> CardRenderer.drawCard(null, null, 0, 0, 120, 168, Color.WHITE));
  }

  @Test
  void drawCardCompositeHandlesNullArgumentsSafely() {
    assertDoesNotThrow(
        () ->
            CardRenderer.drawCardComposite(
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                120,
                168,
                Color.WHITE,
                CardRenderMode.FULL,
                true));
  }

  @Test
  void renderModesAreCovered() {
    assertEquals(3, CardRenderMode.values().length);
    assertNotNull(CardRenderMode.valueOf("FULL"));
    assertNotNull(CardRenderMode.valueOf("THUMBNAIL"));
    assertNotNull(CardRenderMode.valueOf("ICON"));
  }
}
