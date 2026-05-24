package core.domain.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import core.domain.card.Card;
import core.domain.card.CardEffect;
import core.domain.card.CardElement;
import core.domain.card.CardId;
import core.domain.card.CardTag;
import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentId;
import core.domain.equipment.EquipmentSlot;
import core.domain.equipment.StatsBonus;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * {@link NodeResolveContext} の単体テスト (Wave 3 Task A)。
 *
 * <p>本 record の責務は「cards / equipments の resolver を 1 つにまとめる」だけ。 compact constructor の null
 * 拒否と、record accessor が渡した Function をそのまま返すことを検証。
 */
class NodeResolveContextTest {

  private static Card dummyCard() {
    return new Card(
        CardId.of("dummy"),
        "ダミー",
        1,
        CardTag.ATTACK,
        CardElement.PHYSICAL,
        new CardEffect.Damage(1));
  }

  private static Equipment dummyEquipment() {
    return new Equipment(
        EquipmentId.of("dummy_eq"),
        "ダミー装備",
        EquipmentSlot.HAND,
        new StatsBonus(0, 0, 0, 0, 0, 0),
        List.of());
  }

  @Test
  void rejectsNullCardsResolver() {
    Function<EquipmentId, Equipment> eqs = id -> dummyEquipment();
    assertThrows(NullPointerException.class, () -> new NodeResolveContext(null, eqs));
  }

  @Test
  void rejectsNullEquipmentsResolver() {
    Function<CardId, Card> cards = id -> dummyCard();
    assertThrows(NullPointerException.class, () -> new NodeResolveContext(cards, null));
  }

  @Test
  void accessorsReturnTheSameFunctions() {
    Function<CardId, Card> cards = id -> dummyCard();
    Function<EquipmentId, Equipment> eqs = id -> dummyEquipment();
    NodeResolveContext ctx = new NodeResolveContext(cards, eqs);
    assertSame(cards, ctx.cards());
    assertSame(eqs, ctx.equipments());
  }

  @Test
  void resolversAreInvokedWithGivenId() {
    Card expectedCard = dummyCard();
    Equipment expectedEq = dummyEquipment();
    NodeResolveContext ctx = new NodeResolveContext(id -> expectedCard, id -> expectedEq);
    assertEquals(expectedCard, ctx.cards().apply(CardId.of("any")));
    assertEquals(expectedEq, ctx.equipments().apply(EquipmentId.of("any")));
  }
}
