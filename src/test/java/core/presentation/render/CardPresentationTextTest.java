package core.presentation.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.card.Card;
import core.domain.card.CardEffect;
import core.domain.card.CardElement;
import core.domain.card.CardId;
import core.domain.card.CardTag;
import core.infrastructure.bootstrap.CardCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardPresentationTextTest {

  @Test
  void formatsCompleteJapanesePhysicalCardDetails() {
    Card card =
        new Card(
            CardId.of("heavy_slash"),
            "強斬撃",
            3,
            CardTag.ATTACK,
            CardElement.PHYSICAL,
            new CardEffect.Damage(5, 3));

    assertEquals("強斬撃  AP:3  物\n - 物理ダメージ 5 (射程 3)", CardPresentationText.format(card, true));
  }

  @Test
  void formatsCompleteEnglishMagicalCardDetails() {
    Card card =
        new Card(
            CardId.of("arc_burst"),
            "爆炎",
            2,
            CardTag.ATTACK,
            CardElement.MAGICAL,
            new CardEffect.Damage(9, 4, 1));

    assertEquals(
        "Arc Burst  AP:2  Magical\n - Magical damage 9 (Range 4 / Area 1)",
        CardPresentationText.format(card, false));
  }

  @Test
  void allCatalogEnglishDetailsAreAsciiOnly() {
    List<Card> cards = CardCatalog.load().all();
    assertEquals(59, cards.size(), "検証対象は現在の cards.json 全 59 枚");

    for (Card card : cards) {
      String text = CardPresentationText.format(card, false);
      assertTrue(
          text.codePoints()
              .allMatch(codePoint -> codePoint == '\n' || codePoint >= 0x20 && codePoint <= 0x7e),
          () -> card.id().value() + " の英語詳細に非 ASCII 文字がある: " + text);
    }
  }

  @Test
  void allCatalogDetailsUseExactlyTwoLinesWithinConservativeHudFontWidth() {
    List<Card> cards = CardCatalog.load().all();
    assertEquals(59, cards.size(), "検証対象は現在の cards.json 全 59 枚");

    for (Card card : cards) {
      for (boolean japanese : List.of(true, false)) {
        String text = CardPresentationText.format(card, japanese);
        String[] lines = text.split("\\n", -1);
        assertEquals(
            RenderLayout.HAND_DETAIL_LINE_COUNT,
            lines.length,
            () -> card.id().value() + " の詳細行数: " + text);
        for (String line : lines) {
          int conservativeWidth =
              line.codePointCount(0, line.length()) * RenderLayout.HAND_DETAIL_FONT_SIZE;
          assertTrue(
              conservativeWidth <= RenderLayout.HAND_DETAIL_TEXT_WIDTH,
              () ->
                  "%s の詳細行が幅超過: %dpx > %dpx: %s"
                      .formatted(
                          card.id().value(),
                          conservativeWidth,
                          RenderLayout.HAND_DETAIL_TEXT_WIDTH,
                          line));
        }
      }
    }
  }
}
