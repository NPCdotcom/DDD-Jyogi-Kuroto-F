package core.infrastructure.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.card.Card;
import core.domain.card.CardEffect;
import core.domain.card.CardId;
import core.domain.card.CardRarity;
import java.util.Optional;
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

  // Wave 11 W11-β: rarity graceful 読込

  @Test
  void rarityFieldIsParsedWhenPresent() {
    // cards.json で W11-β 時点に rarity 明示済の代表カード (動作確認用)
    assertEquals(Optional.of(CardRarity.RARE), CATALOG.get(CardId.of("blaze_nova")).rarity());
    assertEquals(Optional.of(CardRarity.RARE), CATALOG.get(CardId.of("meteor_drop")).rarity());
    assertEquals(Optional.of(CardRarity.UNCOMMON), CATALOG.get(CardId.of("teleport")).rarity());
    assertEquals(
        Optional.of(CardRarity.UNCOMMON), CATALOG.get(CardId.of("overhead_smash")).rarity());
  }

  @Test
  void rarityIsEmptyWhenAbsentInJson() {
    // 大多数のカードは rarity 未指定 → Optional.empty()、rarityOrDefault() = COMMON にフォールバック
    Card zangeki = CATALOG.get(CardId.of("zangeki"));
    assertEquals(Optional.empty(), zangeki.rarity());
    assertEquals(CardRarity.COMMON, zangeki.rarityOrDefault());
  }

  // Wave 12 W12-α: Damage の range / areaRadius graceful 読込

  @Test
  void damageRangeAndAreaRadiusDefaultToOneAndZeroWhenAbsent() {
    // cards.json で大多数のカードは range / areaRadius 未指定 → デフォルト (1 / 0) = 近接単体
    CardEffect.Damage zangeki = (CardEffect.Damage) CATALOG.get(CardId.of("zangeki")).effect();
    assertEquals(1, zangeki.range(), "range 未指定なら 1 (近接) にフォールバック");
    assertEquals(0, zangeki.areaRadius(), "areaRadius 未指定なら 0 (単体) にフォールバック");
  }
}
