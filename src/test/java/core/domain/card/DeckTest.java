package core.domain.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import core.domain.support.DomainFixtures;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Deck の不変条件テスト。
 *
 * <p>ADR-16 検証ポイント 8: 空デッキで例外、1 枚以上で成功。 GAME_DESIGN §15-3: 空デッキは禁止 (初期デッキは装備固有カードで最低 1 枚保証)。
 */
class DeckTest {

  // ハッピーパス

  @Test
  void deckWithOneCardIsAccepted() {
    // 検証ポイント 8: 1 枚で成功 (最小有効 Deck)
    Deck deck = new Deck(List.of(DomainFixtures.attackCard("c1")));
    assertEquals(1, deck.size());
  }

  @Test
  void deckWithMultipleCardsIsAccepted() {
    List<Card> cards =
        List.of(
            DomainFixtures.attackCard("c1"),
            DomainFixtures.magicCard("c2"),
            DomainFixtures.moveCard("c3"));
    Deck deck = new Deck(cards);
    assertEquals(3, deck.size());
  }

  // 境界値 / 例外パス

  @Test
  void emptyDeckThrowsIllegalArgumentException() {
    // 検証ポイント 8: 空リストは禁止
    assertThrows(IllegalArgumentException.class, () -> new Deck(List.of()));
  }

  @Test
  void nullCardListThrowsNullPointerException() {
    assertThrows(NullPointerException.class, () -> new Deck(null));
  }

  @Test
  void compactConstructorPerformsDefensiveCopy() {
    // 不変性: 外部の List を変更しても Deck 内部に影響しない
    List<Card> mutable = new ArrayList<>();
    mutable.add(DomainFixtures.attackCard("c1"));
    Deck deck = new Deck(mutable);
    mutable.clear();
    assertEquals(1, deck.size(), "外部 List の変更が Deck 内部に伝播してはならない");
  }
}
