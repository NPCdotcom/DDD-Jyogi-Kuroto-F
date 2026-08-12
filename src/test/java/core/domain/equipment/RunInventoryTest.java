package core.domain.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link RunInventory} の単体テスト (EQUIP-00、§15-9 死亡時の装備喪失)。
 *
 * <p>ラン 1 回ぶんの所持品を「持込品」「未確定品」「現在装備」「保護品」に分けた不変条件を固定する。 未確定品 = ラン中に金貨で購入した装備で、クリア時のみ所有化され、死亡時は Soul
 * へ変換せず失う。
 */
class RunInventoryTest {

  private static final EquipmentId STARTER = EquipmentOwnership.STARTER_EQUIPMENT_ID;
  private static final EquipmentId BOOTS = EquipmentId.of("dash_boots");
  private static final EquipmentId BLADE = EquipmentId.of("dark_blade");
  private static final EquipmentId ARMOR = EquipmentId.of("dragon_scale_armor");

  // ---------------- 持込品と未確定品の分離 ----------------

  @Test
  void carriedInAndUnconfirmedMustNotOverlap() {
    // 同じ ID を 1 つのランで重複取得できない。
    assertThrows(
        IllegalArgumentException.class,
        () -> new RunInventory(Set.of(BOOTS), Set.of(BOOTS), Set.of(), Optional.empty()));
  }

  @Test
  void disjointCarriedInAndUnconfirmedAreAccepted() {
    RunInventory inventory =
        new RunInventory(Set.of(STARTER, BOOTS), Set.of(BLADE), Set.of(), Optional.empty());
    assertEquals(Set.of(STARTER, BOOTS), inventory.carriedIn());
    assertEquals(Set.of(BLADE), inventory.unconfirmed());
  }

  // ---------------- 現在装備 ----------------

  @Test
  void equippedMustBeCarriedInOrUnconfirmed() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RunInventory(Set.of(BOOTS), Set.of(BLADE), Set.of(), Optional.of(ARMOR)));
  }

  @Test
  void equippedFromUnconfirmedIsAccepted() {
    // ラン中に購入した装備はその場で装着できる。
    RunInventory inventory =
        new RunInventory(Set.of(STARTER), Set.of(BLADE), Set.of(), Optional.of(BLADE));
    assertEquals(Optional.of(BLADE), inventory.equipped());
  }

  @Test
  void emptyEquippedIsAccepted() {
    RunInventory inventory =
        new RunInventory(Set.of(STARTER), Set.of(), Set.of(), Optional.empty());
    assertTrue(inventory.equipped().isEmpty());
  }

  // ---------------- 保護指定 ----------------

  @Test
  void protectedIdsMustBeCarriedIn() {
    // 未確定品は保護対象にできない。死亡時は保護の有無に関わらず失う (§15-9)。
    assertThrows(
        IllegalArgumentException.class,
        () -> new RunInventory(Set.of(BOOTS), Set.of(BLADE), Set.of(BLADE), Optional.empty()));
  }

  @Test
  void starterCannotBeProtected() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RunInventory(Set.of(STARTER, BOOTS), Set.of(), Set.of(STARTER), Optional.empty()));
  }

  // ---------------- 所有との突き合わせ ----------------

  @Test
  void startRunRejectsProtectionBeyondCapacity() {
    EquipmentOwnership ownership =
        new EquipmentOwnership(Set.of(BOOTS, BLADE), Set.of(), RetentionCapacity.of(1));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            RunInventory.startRun(
                ownership, Set.of(BOOTS, BLADE), Set.of(BOOTS, BLADE), Optional.empty()));
  }

  @Test
  void startRunRejectsCarryingUnownedEquipment() {
    EquipmentOwnership ownership = EquipmentOwnership.initial();
    assertThrows(
        IllegalArgumentException.class,
        () -> RunInventory.startRun(ownership, Set.of(STARTER, BLADE), Set.of(), Optional.empty()));
  }

  @Test
  void startRunAcceptsProtectionWithinCapacity() {
    EquipmentOwnership ownership =
        new EquipmentOwnership(Set.of(BOOTS), Set.of(), RetentionCapacity.of(1));
    RunInventory inventory =
        RunInventory.startRun(ownership, Set.of(STARTER, BOOTS), Set.of(BOOTS), Optional.of(BOOTS));
    assertEquals(Set.of(BOOTS), inventory.protectedIds());
  }

  // ---------------- 喪失対象の判定 ----------------

  @Test
  void unprotectedCarriedInIsLostOnDeath() {
    RunInventory inventory =
        new RunInventory(Set.of(STARTER, BOOTS, BLADE), Set.of(), Set.of(BOOTS), Optional.empty());
    // 初期短剣は常に残り、保護品も残る。未保護の持込品だけが喪失対象。
    assertEquals(Set.of(BLADE), inventory.unprotectedCarriedIn());
  }

  @Test
  void starterIsNeverLostOnDeath() {
    RunInventory inventory =
        new RunInventory(Set.of(STARTER), Set.of(), Set.of(), Optional.empty());
    assertTrue(inventory.unprotectedCarriedIn().isEmpty());
  }

  // ---------------- 不変性 ----------------

  @Test
  void collectionsAreDefensivelyCopied() {
    Set<EquipmentId> mutable = new LinkedHashSet<>();
    mutable.add(BOOTS);
    RunInventory inventory = new RunInventory(mutable, Set.of(), Set.of(), Optional.empty());
    mutable.add(BLADE);
    assertFalse(inventory.carriedIn().contains(BLADE));
  }

  @Test
  void rejectsNullArguments() {
    assertThrows(
        NullPointerException.class,
        () -> new RunInventory(null, Set.of(), Set.of(), Optional.empty()));
    assertThrows(
        NullPointerException.class,
        () -> new RunInventory(Set.of(), null, Set.of(), Optional.empty()));
    assertThrows(
        NullPointerException.class,
        () -> new RunInventory(Set.of(), Set.of(), null, Optional.empty()));
    assertThrows(
        NullPointerException.class, () -> new RunInventory(Set.of(), Set.of(), Set.of(), null));
  }
}
