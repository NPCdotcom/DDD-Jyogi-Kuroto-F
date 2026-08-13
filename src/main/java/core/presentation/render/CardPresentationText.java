package core.presentation.render;

import core.domain.card.Card;
import core.domain.card.CardElement;

/** 選択中カードの詳細表示文言を生成する、LibGDX 非依存の純粋ヘルパ。 */
public final class CardPresentationText {

  private CardPresentationText() {}

  /** カード名、AP、element、効果全文を既存 HUD と同じ書式で返す。 */
  public static String format(Card card, boolean japanese) {
    String displayName = japanese ? card.displayName() : asciiTitleCase(card.id().value());
    return "%s  AP:%d  %s\n - %s"
        .formatted(
            displayName,
            card.apCost(),
            elementLabel(card.element(), japanese),
            CardDescriber.describe(card, japanese));
  }

  private static String asciiTitleCase(String snakeCase) {
    StringBuilder title = new StringBuilder(snakeCase.length());
    boolean capitalize = true;
    for (int i = 0; i < snakeCase.length(); i++) {
      char current = snakeCase.charAt(i);
      if (current == '_') {
        title.append(' ');
        capitalize = true;
      } else {
        title.append(
            capitalize && current >= 'a' && current <= 'z' ? (char) (current - 32) : current);
        capitalize = false;
      }
    }
    return title.toString();
  }

  private static String elementLabel(CardElement element, boolean japanese) {
    return switch (element) {
      case PHYSICAL ->
          japanese ? Strings.Ja.CARD_ELEMENT_PHYSICAL : Strings.En.CARD_ELEMENT_PHYSICAL;
      case MAGICAL -> japanese ? Strings.Ja.CARD_ELEMENT_MAGICAL : Strings.En.CARD_ELEMENT_MAGICAL;
    };
  }
}
