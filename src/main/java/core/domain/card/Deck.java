package core.domain.card;

import java.util.List;
import java.util.Objects;

/**
 * デッキ (静的なカードマスター)。
 *
 * <p>戦闘開始時に {@link DrawPile} へシャッフルして展開される、ラン全体を通して固定の「持ち込みカードリスト」。 戦闘中の動的な山札・手札・捨て札の状態は {@link
 * CardPileState} 側で管理する (責務分離)。
 *
 * <p>空デッキは禁止 (初期デッキは装備固有カードで最低 1 枚保証される、§15-3)。
 */
public record Deck(List<Card> cards) {

  public Deck {
    Objects.requireNonNull(cards, "cards");
    if (cards.isEmpty()) {
      throw new IllegalArgumentException("Deck must contain at least 1 card");
    }
    cards = List.copyOf(cards);
  }

  public int size() {
    return cards.size();
  }
}
