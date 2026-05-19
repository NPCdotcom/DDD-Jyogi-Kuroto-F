package core.domain.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import core.domain.card.Card;
import core.domain.card.CardEffect;
import core.domain.card.CardElement;
import core.domain.card.CardId;
import core.domain.card.CardTag;
import core.domain.common.Position;
import core.domain.entity.Player;
import core.domain.equipment.StatsBonus;
import core.domain.support.DomainFixtures;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** {@link NodeEffect} 4 種の単体テスト (§15-7 / E-2)。 */
class NodeEffectTest {

  private static final Function<CardId, Card> DUMMY_RESOLVER =
      id ->
          new Card(
              id,
              "解決済み:" + id.value(),
              1,
              CardTag.ATTACK,
              CardElement.PHYSICAL,
              new CardEffect.Damage(1));

  private static Player basePlayer() {
    return DomainFixtures.playerAt(new Position(1, 1));
  }

  // ---------------- None ----------------

  @Test
  void noneReturnsSameInstance() {
    Player base = basePlayer();
    Player after = NodeEffect.None.INSTANCE.apply(base, DUMMY_RESOLVER);
    assertSame(base, after, "None 効果は Player を変えない");
  }

  // ---------------- StatsBonusEffect ----------------

  @Test
  void statsBonusEffectAddsBonusToPlayerStats() {
    Player base = basePlayer();
    int beforePhyAtk = base.stats().physicalAttack();

    Player after =
        new NodeEffect.StatsBonusEffect(new StatsBonus(0, 0, 2, 0, 0, 0))
            .apply(base, DUMMY_RESOLVER);

    assertEquals(beforePhyAtk + 2, after.stats().physicalAttack(), "素ステに直接加算");
  }

  @Test
  void statsBonusEffectRejectsNullBonus() {
    assertThrows(NullPointerException.class, () -> new NodeEffect.StatsBonusEffect(null));
  }

  // ---------------- CardGrantEffect ----------------

  @Test
  void cardGrantEffectAddsCardToDrawPile() {
    Player base = basePlayer();
    int beforeDeckSize =
        base.cardPileState().drawPile().size()
            + base.cardPileState().hand().size()
            + base.cardPileState().discardPile().size();

    Player after =
        new NodeEffect.CardGrantEffect(CardId.of("test_card")).apply(base, DUMMY_RESOLVER);

    int afterDeckSize =
        after.cardPileState().drawPile().size()
            + after.cardPileState().hand().size()
            + after.cardPileState().discardPile().size();
    assertEquals(beforeDeckSize + 1, afterDeckSize, "デッキ枚数 +1");
    // 追加先は DrawPile
    assertEquals(
        base.cardPileState().drawPile().size() + 1,
        after.cardPileState().drawPile().size(),
        "DrawPile に追加");
  }

  @Test
  void cardGrantEffectRejectsNullCardId() {
    assertThrows(NullPointerException.class, () -> new NodeEffect.CardGrantEffect(null));
  }

  // ---------------- SlotExpandEffect ----------------

  @Test
  void slotExpandEffectIncreasesSkillSlotMaxSize() {
    Player base = basePlayer();
    int beforeMax = base.skillSlot().maxSize();

    Player after = new NodeEffect.SlotExpandEffect(2).apply(base, DUMMY_RESOLVER);

    assertEquals(beforeMax + 2, after.skillSlot().maxSize(), "maxSize +2");
  }

  @Test
  void slotExpandEffectRejectsZeroOrNegativeAmount() {
    assertThrows(IllegalArgumentException.class, () -> new NodeEffect.SlotExpandEffect(0));
    assertThrows(IllegalArgumentException.class, () -> new NodeEffect.SlotExpandEffect(-1));
  }
}
