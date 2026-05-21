package core.infrastructure.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.card.Card;
import core.domain.card.CardEffect;
import core.domain.card.CardId;
import org.junit.jupiter.api.Test;

/** {@link CardCatalog} が cards.json を正しくロードし、既存システムの参照 ID を全て解決することの検証。 */
class CardCatalogTest {

  private static final CardCatalog CATALOG = CardCatalog.load();

  @Test
  void loadsManyCards() {
    assertFalse(CATALOG.all().isEmpty(), "cards.json から複数カードがロードされる");
    assertTrue(CATALOG.all().size() >= 50, "チームカード込みで 50 枚以上 (実数 " + CATALOG.all().size() + ")");
  }

  @Test
  void resolvesEquipmentAndSoulTreeReferencedIds() {
    // 装備 grantedCards + ソウルツリー CardGrantEffect が参照する ID を cards.json が全てカバーする。
    for (String id :
        new String[] {
          "dash", "zangeki", "magic_bolt", "strong_strike", "fireball", "iron_skin", "arcane_veil"
        }) {
      assertNotNull(CATALOG.get(CardId.of(id)), id + " は cards.json に存在する");
    }
  }

  @Test
  void getReturnsCardWithMatchingId() {
    assertEquals("dash", CATALOG.get(CardId.of("dash")).id().value());
  }

  @Test
  void unknownIdThrows() {
    assertThrows(IllegalArgumentException.class, () -> CATALOG.get(CardId.of("no_such_card_xyz")));
  }

  @Test
  void parsesAllFourEffectTypes() {
    // cards.json には Damage / Move / Buff / Trap の 4 効果が全て含まれる。
    assertTrue(CATALOG.get(CardId.of("zangeki")).effect() instanceof CardEffect.Damage);
    assertTrue(CATALOG.get(CardId.of("dash")).effect() instanceof CardEffect.Move);
    assertTrue(CATALOG.get(CardId.of("iron_skin")).effect() instanceof CardEffect.Buff);
    assertTrue(CATALOG.get(CardId.of("flame_circle")).effect() instanceof CardEffect.Trap);
  }

  @Test
  void allIdsAreUnique() {
    long distinct = CATALOG.all().stream().map(Card::id).distinct().count();
    assertEquals(CATALOG.all().size(), distinct, "カード ID は一意");
  }
}
