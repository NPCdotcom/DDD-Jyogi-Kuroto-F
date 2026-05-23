package core.domain.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.card.CardId;
import core.domain.entity.EnemyKind;
import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentId;
import core.domain.equipment.EquipmentSlot;
import core.domain.equipment.StatsBonus;
import core.domain.tree.NodeId;
import core.domain.tree.SoulTree;
import core.infrastructure.bootstrap.InitialStateFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** {@link PlayerProgress} の単体テスト (Wave 5 W5-β、ラン外永続進捗の集約 record)。 */
class PlayerProgressTest {

  /** Wave 5 W5-γ: SoulTree.allNodes() を呼ぶテストがあるため Supplier を初期化。 */
  @BeforeAll
  static void initSoulTreeProvider() {
    SoulTree.setNodeProvider(InitialStateFactory::soulTreeNodes);
  }

  private static Map<EquipmentSlot, Equipment> oneSlotLoadout() {
    Map<EquipmentSlot, Equipment> m = new HashMap<>();
    Equipment dagger =
        new Equipment(
            EquipmentId.of("test_dagger"),
            "テスト短剣",
            EquipmentSlot.HAND,
            new StatsBonus(0, 0, 1, 0, 0, 0),
            List.of(),
            Optional.empty());
    m.put(EquipmentSlot.HAND, dagger);
    return m;
  }

  @Test
  void initialReturnsAllDefaults() {
    PlayerProgress p = PlayerProgress.initial(oneSlotLoadout());
    assertEquals(Soul.zero(), p.playerSoul());
    assertEquals(0, p.runCount());
    assertFalse(p.tutorialSeen());
    assertTrue(p.obtainedCards().isEmpty());
    assertTrue(p.bestiary().defeatedKinds().isEmpty());
    assertEquals(1, p.loadout().size());
    assertEquals(SoulTree.empty(), p.soulTree());
  }

  @Test
  void compactConstructorRejectsNullFields() {
    Map<EquipmentSlot, Equipment> ld = oneSlotLoadout();
    assertThrows(
        NullPointerException.class,
        () -> new PlayerProgress(null, 0, false, Set.of(), Bestiary.empty(), ld, SoulTree.empty()));
    assertThrows(
        NullPointerException.class,
        () ->
            new PlayerProgress(
                Soul.zero(), 0, false, null, Bestiary.empty(), ld, SoulTree.empty()));
    assertThrows(
        NullPointerException.class,
        () -> new PlayerProgress(Soul.zero(), 0, false, Set.of(), null, ld, SoulTree.empty()));
  }

  @Test
  void compactConstructorRejectsNegativeRunCount() {
    Map<EquipmentSlot, Equipment> ld = oneSlotLoadout();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PlayerProgress(
                Soul.zero(), -1, false, Set.of(), Bestiary.empty(), ld, SoulTree.empty()));
  }

  @Test
  void compactConstructorDefensivelyCopiesObtainedCards() {
    Set<CardId> mutable = new HashSet<>();
    mutable.add(CardId.of("dummy_card"));
    PlayerProgress p =
        new PlayerProgress(
            Soul.zero(), 0, false, mutable, Bestiary.empty(), oneSlotLoadout(), SoulTree.empty());

    mutable.add(CardId.of("intruder"));
    assertEquals(1, p.obtainedCards().size(), "外部 Set 変更は防御コピーで遮断");
    assertFalse(p.obtainedCards().contains(CardId.of("intruder")));
  }

  @Test
  void compactConstructorDefensivelyCopiesLoadout() {
    Map<EquipmentSlot, Equipment> mutable = oneSlotLoadout();
    PlayerProgress p =
        new PlayerProgress(
            Soul.zero(), 0, false, Set.of(), Bestiary.empty(), mutable, SoulTree.empty());

    mutable.clear();
    assertEquals(1, p.loadout().size(), "外部 Map 変更は防御コピーで遮断");
  }

  @Test
  void withPlayerSoulReturnsNewInstanceWithOtherFieldsPreserved() {
    PlayerProgress p = PlayerProgress.initial(oneSlotLoadout()).withRunCount(3);
    PlayerProgress after = p.withPlayerSoul(new Soul(42));

    assertNotSame(p, after);
    assertEquals(new Soul(42), after.playerSoul());
    assertEquals(3, after.runCount(), "他フィールドは保持される");
  }

  @Test
  void withRunCountAllowsIncrement() {
    PlayerProgress p = PlayerProgress.initial(oneSlotLoadout());
    assertEquals(5, p.withRunCount(5).runCount());
  }

  @Test
  void withTutorialSeenTogglesFlag() {
    PlayerProgress p = PlayerProgress.initial(oneSlotLoadout());
    assertTrue(p.withTutorialSeen(true).tutorialSeen());
    assertFalse(p.withTutorialSeen(false).tutorialSeen());
  }

  @Test
  void withObtainedCardsReplacesSet() {
    PlayerProgress p = PlayerProgress.initial(oneSlotLoadout());
    Set<CardId> newCards = Set.of(CardId.of("a"), CardId.of("b"));
    PlayerProgress after = p.withObtainedCards(newCards);
    assertEquals(2, after.obtainedCards().size());
    assertTrue(after.obtainedCards().contains(CardId.of("a")));
  }

  @Test
  void withBestiaryReplacesRecord() {
    PlayerProgress p = PlayerProgress.initial(oneSlotLoadout());
    Bestiary withSlime = Bestiary.empty().withDefeated(EnemyKind.SLIME);
    PlayerProgress after = p.withBestiary(withSlime);
    assertSame(withSlime, after.bestiary());
  }

  @Test
  void withSoulTreeReplacesTree() {
    PlayerProgress p = PlayerProgress.initial(oneSlotLoadout());
    SoulTree unlocked = SoulTree.empty().unlock(NodeId.of("hp_up_1"), new Soul(100)).newTree();
    PlayerProgress after = p.withSoulTree(unlocked);
    assertEquals(unlocked, after.soulTree());
    assertEquals(SoulTree.empty(), p.soulTree(), "元インスタンスは不変");
  }
}
