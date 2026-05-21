package core.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.battle.ActionPoints;
import core.domain.card.ActiveBuff;
import core.domain.card.CardEffect;
import core.domain.card.CardId;
import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentId;
import core.domain.equipment.EquipmentSlot;
import core.domain.equipment.StatsBonus;
import core.domain.skill.SkillSlot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** {@link PlayerStatuses} の単体テスト (§15-4 / ADR-25)。 */
class PlayerStatusesTest {

  private static Stats baseStats() {
    return new Stats(10, 10, 3, 2, 2, 1, 1);
  }

  private static SkillSlot emptySlot() {
    return new SkillSlot(List.of(), 4);
  }

  // ---------------- ファクトリ ----------------

  @Test
  void ofProducesEmptyEquipmentAndBuffs() {
    PlayerStatuses ps = PlayerStatuses.of(baseStats(), ActionPoints.full(5), emptySlot());
    assertTrue(ps.equipment().isEmpty(), "装備未装着で初期化される");
    assertEquals(0, ps.activeBuffs().size(), "Buff なしで初期化される");
    assertEquals(0, ps.equipmentCards().size(), "装備固有カード 0 件");
  }

  // ---------------- compact constructor ----------------

  @Test
  void compactConstructorRejectsNullFields() {
    Stats s = baseStats();
    ActionPoints ap = ActionPoints.full(5);
    SkillSlot slot = emptySlot();
    assertThrows(
        NullPointerException.class, () -> new PlayerStatuses(null, ap, slot, Map.of(), List.of()));
    assertThrows(
        NullPointerException.class, () -> new PlayerStatuses(s, null, slot, Map.of(), List.of()));
    assertThrows(
        NullPointerException.class, () -> new PlayerStatuses(s, ap, null, Map.of(), List.of()));
    assertThrows(
        NullPointerException.class, () -> new PlayerStatuses(s, ap, slot, null, List.of()));
    assertThrows(NullPointerException.class, () -> new PlayerStatuses(s, ap, slot, Map.of(), null));
  }

  @Test
  void compactConstructorRejectsSlotKeyMismatch() {
    // Map のキーと Equipment.slot() が一致しない場合は IllegalArgumentException
    Equipment boots =
        new Equipment(
            EquipmentId.of("boots"),
            "ぼろ靴",
            EquipmentSlot.FEET,
            new StatsBonus(0, 1, 0, 0, 0, 0),
            List.of());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PlayerStatuses(
                baseStats(),
                ActionPoints.full(5),
                emptySlot(),
                Map.of(EquipmentSlot.HAND, boots), // FEET 装備を HAND キーに置く
                List.of()));
  }

  // ---------------- effectiveStats: 装備・Buff なし ----------------

  @Test
  void effectiveStatsEqualsBaseStatsWhenNoEquipmentNoBuffs() {
    PlayerStatuses ps = PlayerStatuses.of(baseStats(), ActionPoints.full(5), emptySlot());
    Stats eff = ps.effectiveStats();

    assertEquals(baseStats().maxHp(), eff.maxHp());
    assertEquals(baseStats().speed(), eff.speed());
    assertEquals(baseStats().physicalAttack(), eff.physicalAttack());
    assertEquals(baseStats().physicalDefense(), eff.physicalDefense());
  }

  // ---------------- effectiveStats: 装備のみ ----------------

  @Test
  void effectiveStatsCombinesEquipmentBonus() {
    Equipment boots =
        new Equipment(
            EquipmentId.of("tattered_boots"),
            "ぼろ靴",
            EquipmentSlot.FEET,
            new StatsBonus(0, 1, 0, 0, 0, 0),
            List.of(CardId.of("dash")));
    PlayerStatuses ps =
        new PlayerStatuses(
            baseStats(),
            ActionPoints.full(5),
            emptySlot(),
            Map.of(EquipmentSlot.FEET, boots),
            List.of());

    Stats eff = ps.effectiveStats();
    assertEquals(baseStats().speed() + 1, eff.speed(), "装備 speed +1 が反映される");
    assertEquals(baseStats().physicalAttack(), eff.physicalAttack(), "他ステは素ステ通り");
  }

  @Test
  void effectiveStatsCombinesMultipleEquipmentSlots() {
    // 2 部位装備 (FEET + HAND): ぼろ靴 (speed +1) + ぼろい短剣 (物攻 +1)
    Equipment boots =
        new Equipment(
            EquipmentId.of("boots"),
            "ぼろ靴",
            EquipmentSlot.FEET,
            new StatsBonus(0, 1, 0, 0, 0, 0),
            List.of(CardId.of("dash")));
    Equipment dagger =
        new Equipment(
            EquipmentId.of("dagger"),
            "ぼろい短剣",
            EquipmentSlot.HAND,
            new StatsBonus(0, 0, 1, 0, 0, 0),
            List.of(CardId.of("zangeki")));
    PlayerStatuses ps =
        new PlayerStatuses(
            baseStats(),
            ActionPoints.full(5),
            emptySlot(),
            Map.of(EquipmentSlot.FEET, boots, EquipmentSlot.HAND, dagger),
            List.of());

    Stats eff = ps.effectiveStats();
    assertEquals(baseStats().speed() + 1, eff.speed(), "ぼろ靴の speed +1");
    assertEquals(baseStats().physicalAttack() + 1, eff.physicalAttack(), "ぼろい短剣の 物攻 +1");
  }

  @Test
  void equipmentCardsConcatenatesAllGrantedCards() {
    // 2 部位装備の grantedCards を連結 (ADR-26、装備固有デッキ生成用)
    Equipment boots =
        new Equipment(
            EquipmentId.of("boots"),
            "ぼろ靴",
            EquipmentSlot.FEET,
            StatsBonus.zero(),
            List.of(CardId.of("dash")));
    Equipment dagger =
        new Equipment(
            EquipmentId.of("dagger"),
            "ぼろい短剣",
            EquipmentSlot.HAND,
            StatsBonus.zero(),
            List.of(CardId.of("zangeki"), CardId.of("strong_strike")));
    PlayerStatuses ps =
        new PlayerStatuses(
            baseStats(),
            ActionPoints.full(5),
            emptySlot(),
            Map.of(EquipmentSlot.FEET, boots, EquipmentSlot.HAND, dagger),
            List.of());

    List<CardId> cards = ps.equipmentCards();
    assertEquals(3, cards.size(), "ぼろ靴 1 枚 + ぼろい短剣 2 枚 = 3 枚");
    assertTrue(cards.contains(CardId.of("dash")));
    assertTrue(cards.contains(CardId.of("zangeki")));
    assertTrue(cards.contains(CardId.of("strong_strike")));
  }

  // ---------------- effectiveStats: Buff のみ ----------------

  @Test
  void effectiveStatsCombinesBuffSum() {
    ActiveBuff atkBuff = new ActiveBuff(CardEffect.BuffKind.PHYSICAL_ATTACK_UP, 2, 3);
    PlayerStatuses ps =
        new PlayerStatuses(
            baseStats(), ActionPoints.full(5), emptySlot(), Map.of(), List.of(atkBuff));

    Stats eff = ps.effectiveStats();
    assertEquals(baseStats().physicalAttack() + 2, eff.physicalAttack(), "Buff 物攻 +2 が反映される");
    assertEquals(baseStats().speed(), eff.speed(), "他ステは素ステ通り");
  }

  // ---------------- effectiveStats: 装備 + Buff 合算 ----------------

  @Test
  void effectiveStatsCombinesEquipmentAndBuffsTogether() {
    Equipment dagger =
        new Equipment(
            EquipmentId.of("rusty_dagger"),
            "錆びた短剣",
            EquipmentSlot.HAND,
            new StatsBonus(0, 0, 1, 0, 0, 0), // 物攻 +1
            List.of());
    ActiveBuff atkBuff = new ActiveBuff(CardEffect.BuffKind.PHYSICAL_ATTACK_UP, 2, 3); // 物攻 +2
    ActiveBuff spdBuff = new ActiveBuff(CardEffect.BuffKind.SPEED_UP, 1, 2); // 速度 +1
    PlayerStatuses ps =
        new PlayerStatuses(
            baseStats(),
            ActionPoints.full(5),
            emptySlot(),
            Map.of(EquipmentSlot.HAND, dagger),
            List.of(atkBuff, spdBuff));

    Stats eff = ps.effectiveStats();
    assertEquals(
        baseStats().physicalAttack() + 1 + 2, eff.physicalAttack(), "装備 +1 + Buff +2 = +3");
    assertEquals(baseStats().speed() + 1, eff.speed(), "Buff speed +1");
  }

  // ---------------- 多種 BuffKind の合算 ----------------

  @Test
  void effectiveStatsSumsAllFiveBuffKinds() {
    PlayerStatuses ps =
        new PlayerStatuses(
            baseStats(),
            ActionPoints.full(5),
            emptySlot(),
            Map.of(),
            List.of(
                new ActiveBuff(CardEffect.BuffKind.PHYSICAL_ATTACK_UP, 1, 1),
                new ActiveBuff(CardEffect.BuffKind.MAGICAL_ATTACK_UP, 2, 1),
                new ActiveBuff(CardEffect.BuffKind.PHYSICAL_DEFENSE_UP, 3, 1),
                new ActiveBuff(CardEffect.BuffKind.MAGICAL_DEFENSE_UP, 4, 1),
                new ActiveBuff(CardEffect.BuffKind.SPEED_UP, 5, 1)));

    Stats eff = ps.effectiveStats();
    assertEquals(baseStats().physicalAttack() + 1, eff.physicalAttack());
    assertEquals(baseStats().magicalAttack() + 2, eff.magicalAttack());
    assertEquals(baseStats().physicalDefense() + 3, eff.physicalDefense());
    assertEquals(baseStats().magicalDefense() + 4, eff.magicalDefense());
    assertEquals(baseStats().speed() + 5, eff.speed());
  }

  // ---------------- with* ----------------

  @Test
  void withStatsReturnsNewInstanceLeavingOriginalIntact() {
    PlayerStatuses ps = PlayerStatuses.of(baseStats(), ActionPoints.full(5), emptySlot());
    Stats reduced = baseStats().damaged(3);

    PlayerStatuses updated = ps.withStats(reduced);

    assertNotSame(ps, updated);
    assertEquals(baseStats().currentHp(), ps.stats().currentHp(), "元は変化しない");
    assertEquals(baseStats().currentHp() - 3, updated.stats().currentHp());
  }

  @Test
  void withActiveBuffsReplacesList() {
    PlayerStatuses ps = PlayerStatuses.of(baseStats(), ActionPoints.full(5), emptySlot());
    ActiveBuff buff = new ActiveBuff(CardEffect.BuffKind.SPEED_UP, 1, 2);

    PlayerStatuses updated = ps.withActiveBuffs(List.of(buff));

    assertEquals(0, ps.activeBuffs().size(), "元は変化しない");
    assertEquals(1, updated.activeBuffs().size());
  }

  // ---------------- 防御コピー ----------------

  @Test
  void activeBuffsListIsDefensivelyCopied() {
    java.util.List<ActiveBuff> mutable = new java.util.ArrayList<>();
    mutable.add(new ActiveBuff(CardEffect.BuffKind.SPEED_UP, 1, 2));
    PlayerStatuses ps =
        new PlayerStatuses(baseStats(), ActionPoints.full(5), emptySlot(), Map.of(), mutable);

    mutable.add(new ActiveBuff(CardEffect.BuffKind.PHYSICAL_ATTACK_UP, 1, 1));

    assertEquals(1, ps.activeBuffs().size(), "外部リスト変更は防御コピーで遮断される");
  }
}
