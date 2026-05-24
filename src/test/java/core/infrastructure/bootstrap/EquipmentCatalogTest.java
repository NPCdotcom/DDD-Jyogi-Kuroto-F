package core.infrastructure.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.card.CardId;
import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentId;
import org.junit.jupiter.api.Test;

/** {@link EquipmentCatalog} が equipment.json をロードし、装備の付与カードが全て cards.json に存在することの検証。 */
class EquipmentCatalogTest {

  private static final EquipmentCatalog CATALOG = EquipmentCatalog.load();

  @Test
  void loadsAllEquipment() {
    assertFalse(CATALOG.all().isEmpty(), "equipment.json から装備がロードされる");
    assertTrue(CATALOG.all().size() >= 15, "チーム装備 15 種以上 (実数 " + CATALOG.all().size() + ")");
  }

  @Test
  void everyGrantedCardResolvesInCardCatalog() {
    // 装備の grantedCards が cards.json に無いと、その装備でラン開始時に resolveCard が例外を投げる。
    for (Equipment eq : CATALOG.all()) {
      for (CardId cid : eq.grantedCards()) {
        assertEquals(
            cid,
            InitialStateFactory.cardCatalog().get(cid).id(),
            eq.id().value() + " の付与カード " + cid.value() + " は cards.json に存在する");
      }
    }
  }

  @Test
  void getUnknownIdThrows() {
    assertThrows(
        IllegalArgumentException.class, () -> CATALOG.get(EquipmentId.of("no_such_equipment")));
  }
}
