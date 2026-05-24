package core.domain.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.card.CardId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** {@link Equipment} / {@link EquipmentId} の単体テスト (§15-9 / ADR-25)。 */
class EquipmentTest {

  // ---------------- EquipmentId ----------------

  @Test
  void equipmentIdRejectsBlankValue() {
    assertThrows(IllegalArgumentException.class, () -> new EquipmentId(""));
    assertThrows(IllegalArgumentException.class, () -> new EquipmentId("   "));
  }

  @Test
  void equipmentIdRejectsNullValue() {
    assertThrows(NullPointerException.class, () -> new EquipmentId(null));
  }

  @Test
  void equipmentIdOfReturnsSameValue() {
    EquipmentId id = EquipmentId.of("tattered_boots");
    assertEquals("tattered_boots", id.value());
  }

  // ---------------- Equipment ----------------

  @Test
  void equipmentAcceptsAllFiveFields() {
    Equipment boots =
        new Equipment(
            EquipmentId.of("tattered_boots"),
            "ぼろ靴",
            EquipmentSlot.FEET,
            new StatsBonus(0, 1, 0, 0, 0, 0),
            List.of(CardId.of("dash")));

    assertEquals("ぼろ靴", boots.displayName());
    assertEquals(EquipmentSlot.FEET, boots.slot());
    assertEquals(1, boots.statsBonus().speed());
    assertEquals(1, boots.grantedCards().size());
  }

  @Test
  void equipmentAllowsEmptyGrantedCards() {
    Equipment ring =
        new Equipment(
            EquipmentId.of("plain_ring"),
            "素朴な指輪",
            EquipmentSlot.HEAD,
            new StatsBonus(5, 0, 0, 0, 0, 0),
            List.of());

    assertEquals(0, ring.grantedCards().size());
  }

  @Test
  void equipmentRejectsBlankDisplayName() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Equipment(
                EquipmentId.of("x"), "", EquipmentSlot.HAND, StatsBonus.zero(), List.of()));
  }

  @Test
  void equipmentRejectsNullFields() {
    assertThrows(
        NullPointerException.class,
        () -> new Equipment(null, "x", EquipmentSlot.HAND, StatsBonus.zero(), List.of()));
    assertThrows(
        NullPointerException.class,
        () ->
            new Equipment(
                EquipmentId.of("x"), null, EquipmentSlot.HAND, StatsBonus.zero(), List.of()));
    assertThrows(
        NullPointerException.class,
        () -> new Equipment(EquipmentId.of("x"), "x", null, StatsBonus.zero(), List.of()));
    assertThrows(
        NullPointerException.class,
        () -> new Equipment(EquipmentId.of("x"), "x", EquipmentSlot.HAND, null, List.of()));
    assertThrows(
        NullPointerException.class,
        () -> new Equipment(EquipmentId.of("x"), "x", EquipmentSlot.HAND, StatsBonus.zero(), null));
  }

  @Test
  void equipmentGrantedCardsAreDefensivelyCopied() {
    // 外部リストの変更が Equipment 内部に影響しない (List.copyOf 防御コピー)
    List<CardId> mutable = new ArrayList<>();
    mutable.add(CardId.of("zangeki"));
    Equipment sword =
        new Equipment(
            EquipmentId.of("rusty_sword"),
            "錆びた剣",
            EquipmentSlot.HAND,
            new StatsBonus(0, 0, 1, 0, 0, 0),
            mutable);

    mutable.add(CardId.of("magic_bolt")); // 外部リストを変更
    assertEquals(1, sword.grantedCards().size(), "外部変更は防御コピーで遮断される");
    assertNotSame(mutable, sword.grantedCards());
  }

  // ---------------- themeName (§7-2 / W4-ε) ----------------

  @Test
  void themeNameDefaultsToEmptyWhenUsing5ArgConstructor() {
    // 5 引数コンストラクタ (後方互換) では themeName = Optional.empty()
    Equipment boots =
        new Equipment(
            EquipmentId.of("tattered_boots"),
            "ぼろ靴",
            EquipmentSlot.FEET,
            new StatsBonus(0, 1, 0, 0, 0, 0),
            List.of(CardId.of("dash")));
    assertTrue(boots.themeName().isEmpty(), "5 引数では themeName は Optional.empty()");
  }

  @Test
  void themeNameNullInCompactConstructorIsReplacedWithEmpty() {
    // compact constructor: themeName=null → Optional.empty() に正規化される
    Equipment eq =
        new Equipment(
            EquipmentId.of("test"), "テスト", EquipmentSlot.HAND, StatsBonus.zero(), List.of(), null);
    assertTrue(eq.themeName().isEmpty(), "null は Optional.empty() に正規化される");
  }

  @Test
  void themeNameIsPreservedWhenProvided() {
    Equipment eq =
        new Equipment(
            EquipmentId.of("fire_blade"),
            "炎の剣",
            EquipmentSlot.HAND,
            StatsBonus.zero(),
            List.of(),
            Optional.of("fire"));
    assertEquals("fire", eq.themeName().orElseThrow());
  }
}
