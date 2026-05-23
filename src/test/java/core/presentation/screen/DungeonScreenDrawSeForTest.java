package core.presentation.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import core.domain.card.Card;
import core.domain.card.CardEffect;
import core.domain.card.CardElement;
import core.domain.card.CardId;
import core.domain.card.CardRarity;
import core.domain.card.CardTag;
import core.domain.card.Hand;
import core.infrastructure.audio.SeKind;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Wave 11 W11-β: 手札の最高 rarity に応じて CARD_DRAW SE が分岐することの純関数テスト。 */
class DungeonScreenDrawSeForTest {

  private static Card cardWith(String id, CardRarity rarity) {
    return new Card(
        CardId.of(id),
        "テスト " + id,
        1,
        CardTag.ATTACK,
        CardElement.PHYSICAL,
        new CardEffect.Damage(1),
        Optional.of(rarity));
  }

  private static Card cardWithoutRarity(String id) {
    return new Card(
        CardId.of(id),
        "テスト " + id,
        1,
        CardTag.ATTACK,
        CardElement.PHYSICAL,
        new CardEffect.Damage(1));
  }

  @Test
  void handWithRareCardSelectsRareSe() {
    Hand hand =
        new Hand(
            List.of(
                cardWithoutRarity("c1"),
                cardWith("c2", CardRarity.UNCOMMON),
                cardWith("c3", CardRarity.RARE)));
    assertEquals(SeKind.CARD_DRAW_R, DungeonScreen.drawSeFor(hand));
  }

  @Test
  void handWithUncommonButNoRareSelectsUncommonSe() {
    Hand hand = new Hand(List.of(cardWithoutRarity("c1"), cardWith("c2", CardRarity.UNCOMMON)));
    assertEquals(SeKind.CARD_DRAW_U, DungeonScreen.drawSeFor(hand));
  }

  @Test
  void handWithOnlyCommonSelectsCommonSe() {
    Hand hand = new Hand(List.of(cardWithoutRarity("c1"), cardWith("c2", CardRarity.COMMON)));
    assertEquals(SeKind.CARD_DRAW_C, DungeonScreen.drawSeFor(hand));
  }

  @Test
  void emptyHandFallsBackToCommonSe() {
    assertEquals(SeKind.CARD_DRAW_C, DungeonScreen.drawSeFor(Hand.empty()));
  }
}
