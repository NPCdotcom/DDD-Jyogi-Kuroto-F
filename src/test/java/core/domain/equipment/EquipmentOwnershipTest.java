package core.domain.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link RetentionCapacity} / {@link EquipmentOwnership} の単体テスト (EQUIP-00、§15-9 死亡時の装備喪失)。
 *
 * <p>恒久状態 (Profile) 側の不変条件を固定する。ラン側の持込・未確定は {@link RunInventoryTest} が担当する。
 */
class EquipmentOwnershipTest {

  private static final EquipmentId STARTER = EquipmentOwnership.STARTER_EQUIPMENT_ID;
  private static final EquipmentId BOOTS = EquipmentId.of("dash_boots");
  private static final EquipmentId BLADE = EquipmentId.of("dark_blade");

  // ---------------- RetentionCapacity ----------------

  @Test
  void retentionCapacityAcceptsZeroToTwo() {
    assertEquals(0, RetentionCapacity.of(0).value());
    assertEquals(1, RetentionCapacity.of(1).value());
    assertEquals(2, RetentionCapacity.of(2).value());
  }

  @Test
  void retentionCapacityRejectsNegative() {
    assertThrows(IllegalArgumentException.class, () -> RetentionCapacity.of(-1));
  }

  @Test
  void retentionCapacityRejectsAboveMax() {
    // 保護枠は「魂の刻印 I」「魂の刻印 II」の 2 ノードで最大 2 枠 (§15-7)。
    assertThrows(IllegalArgumentException.class, () -> RetentionCapacity.of(3));
  }

  @Test
  void retentionCapacityNoneIsZero() {
    assertEquals(0, RetentionCapacity.none().value());
  }

  // ---------------- EquipmentOwnership: 初期短剣 ----------------

  @Test
  void initialOwnershipHoldsOnlyStarterWithNoCapacity() {
    EquipmentOwnership ownership = EquipmentOwnership.initial();
    assertEquals(Set.of(STARTER), ownership.ownedIds());
    assertEquals(0, ownership.capacity().value());
    assertTrue(ownership.protectedIds().isEmpty());
  }

  @Test
  void starterIsAddedEvenWhenOmittedFromOwnedIds() {
    // 初期短剣は常に再支給される (§15-9)。所有一覧から欠けていても補われる。
    EquipmentOwnership ownership =
        new EquipmentOwnership(Set.of(BOOTS), Set.of(), RetentionCapacity.none());
    assertTrue(ownership.ownedIds().contains(STARTER));
    assertTrue(ownership.ownedIds().contains(BOOTS));
  }

  @Test
  void starterCannotBeProtected() {
    // 初期短剣は喪失しないため保護枠を消費させない (§15-9)。
    assertThrows(
        IllegalArgumentException.class,
        () -> new EquipmentOwnership(Set.of(STARTER), Set.of(STARTER), RetentionCapacity.of(1)));
  }

  // ---------------- EquipmentOwnership: 保護指定 ----------------

  @Test
  void protectedIdsMustBeOwned() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new EquipmentOwnership(Set.of(BOOTS), Set.of(BLADE), RetentionCapacity.of(1)));
  }

  @Test
  void protectedIdsMustNotExceedCapacity() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new EquipmentOwnership(
                Set.of(BOOTS, BLADE), Set.of(BOOTS, BLADE), RetentionCapacity.of(1)));
  }

  @Test
  void protectedIdsAtExactlyCapacityIsAccepted() {
    EquipmentOwnership ownership =
        new EquipmentOwnership(Set.of(BOOTS, BLADE), Set.of(BOOTS, BLADE), RetentionCapacity.of(2));
    assertEquals(Set.of(BOOTS, BLADE), ownership.protectedIds());
  }

  // ---------------- EquipmentOwnership: 不変性 ----------------

  @Test
  void ownedIdsAreDefensivelyCopied() {
    Set<EquipmentId> mutable = new LinkedHashSet<>();
    mutable.add(BOOTS);
    EquipmentOwnership ownership =
        new EquipmentOwnership(mutable, Set.of(), RetentionCapacity.none());
    mutable.add(BLADE);
    assertFalse(ownership.ownedIds().contains(BLADE));
  }

  @Test
  void ownedIdsAreUnmodifiable() {
    EquipmentOwnership ownership = EquipmentOwnership.initial();
    assertThrows(UnsupportedOperationException.class, () -> ownership.ownedIds().add(BOOTS));
  }

  @Test
  void rejectsNullArguments() {
    assertThrows(
        NullPointerException.class,
        () -> new EquipmentOwnership(null, Set.of(), RetentionCapacity.none()));
    assertThrows(
        NullPointerException.class,
        () -> new EquipmentOwnership(Set.of(), null, RetentionCapacity.none()));
    assertThrows(
        NullPointerException.class, () -> new EquipmentOwnership(Set.of(), Set.of(), null));
  }

  // ---------------- EquipmentOwnership: 所有の追加 ----------------

  @Test
  void withOwnedAddsIdWithoutMutatingOriginal() {
    EquipmentOwnership before = EquipmentOwnership.initial();
    EquipmentOwnership after = before.withOwned(BOOTS);
    assertFalse(before.ownedIds().contains(BOOTS));
    assertTrue(after.ownedIds().contains(BOOTS));
  }

  @Test
  void withoutOwnedRemovesIdAndItsProtection() {
    EquipmentOwnership before =
        new EquipmentOwnership(Set.of(BOOTS, BLADE), Set.of(BOOTS), RetentionCapacity.of(1));
    EquipmentOwnership after = before.withoutOwned(BOOTS);
    assertFalse(after.ownedIds().contains(BOOTS));
    assertFalse(after.protectedIds().contains(BOOTS));
  }

  @Test
  void withoutOwnedNeverRemovesStarter() {
    // 未保護品の喪失処理で初期短剣まで失わせない (§15-9)。
    EquipmentOwnership after = EquipmentOwnership.initial().withoutOwned(STARTER);
    assertTrue(after.ownedIds().contains(STARTER));
  }
}
