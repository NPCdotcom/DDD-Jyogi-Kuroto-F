package core.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.equipment.StatsBonus;
import org.junit.jupiter.api.Test;

class StatsTest {

  @Test
  void constructorRejectsInvalidCoreValues() {
    // maxHp <= 0
    assertThrows(IllegalArgumentException.class, () -> new Stats(0, 0, 1, 0, 0, 0, 0));
    // currentHp < 0
    assertThrows(IllegalArgumentException.class, () -> new Stats(-1, 10, 1, 0, 0, 0, 0));
    // currentHp > maxHp
    assertThrows(IllegalArgumentException.class, () -> new Stats(11, 10, 1, 0, 0, 0, 0));
    // speed < 0
    assertThrows(IllegalArgumentException.class, () -> new Stats(5, 10, -1, 0, 0, 0, 0));
  }

  @Test
  void constructorRejectsNegativeAttackOrDefenseValues() {
    // 物攻 / 魔攻 / 物防 / 魔防 すべて非負 (§15-4 / ADR-17)
    assertThrows(IllegalArgumentException.class, () -> new Stats(5, 10, 1, -1, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new Stats(5, 10, 1, 0, -1, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new Stats(5, 10, 1, 0, 0, -1, 0));
    assertThrows(IllegalArgumentException.class, () -> new Stats(5, 10, 1, 0, 0, 0, -1));
  }

  @Test
  void allSevenAccessorsExposeConstructorValues() {
    Stats s = new Stats(7, 20, 4, 3, 2, 5, 1);
    assertEquals(7, s.currentHp());
    assertEquals(20, s.maxHp());
    assertEquals(4, s.speed());
    assertEquals(3, s.physicalAttack());
    assertEquals(2, s.magicalAttack());
    assertEquals(5, s.physicalDefense());
    assertEquals(1, s.magicalDefense());
  }

  @Test
  void damagedFloorsAtZero() {
    Stats s = new Stats(5, 10, 2, 0, 0, 0, 0);
    assertEquals(0, s.damaged(100).currentHp());
    assertFalse(s.damaged(100).isAlive());
  }

  @Test
  void healedCeilingsAtMax() {
    Stats s = new Stats(5, 10, 2, 0, 0, 0, 0);
    assertEquals(10, s.healed(999).currentHp());
  }

  @Test
  void damagedReturnsNewInstanceLeavingOriginalIntact() {
    Stats original = new Stats(5, 10, 2, 0, 0, 0, 0);
    Stats reduced = original.damaged(2);
    assertEquals(5, original.currentHp(), "元の Stats は変化してはならない");
    assertEquals(3, reduced.currentHp());
  }

  @Test
  void damagedAndHealedPreserveAttackAndDefenseFields() {
    // damaged / healed は HP だけを変え、他 4 フィールドは保持する (record の値伝播確認)
    Stats s = new Stats(5, 10, 2, 7, 6, 5, 4);

    Stats damaged = s.damaged(2);
    assertEquals(3, damaged.currentHp());
    assertEquals(7, damaged.physicalAttack());
    assertEquals(6, damaged.magicalAttack());
    assertEquals(5, damaged.physicalDefense());
    assertEquals(4, damaged.magicalDefense());

    Stats healed = s.healed(3);
    assertEquals(8, healed.currentHp());
    assertEquals(7, healed.physicalAttack());
    assertEquals(6, healed.magicalAttack());
    assertEquals(5, healed.physicalDefense());
    assertEquals(4, healed.magicalDefense());
  }

  @Test
  void isAliveTrueAboveZero() {
    assertTrue(new Stats(1, 10, 1, 0, 0, 0, 0).isAlive());
    assertFalse(new Stats(0, 10, 1, 0, 0, 0, 0).isAlive());
  }

  @Test
  void negativeDamageOrHealRejected() {
    Stats s = new Stats(5, 10, 1, 0, 0, 0, 0);
    assertThrows(IllegalArgumentException.class, () -> s.damaged(-1));
    assertThrows(IllegalArgumentException.class, () -> s.healed(-1));
  }

  // ---------------- plus(StatsBonus) (ADR-25) ----------------

  @Test
  void plusAddsAllSixFieldsFromBonus() {
    Stats base = new Stats(5, 10, 2, 1, 1, 0, 0);
    Stats up = base.plus(new StatsBonus(3, 1, 2, 1, 1, 2));

    assertEquals(13, up.maxHp(), "maxHp +3");
    assertEquals(5, up.currentHp(), "currentHp は据置 (新 maxHp 内なら不変)");
    assertEquals(3, up.speed(), "speed +1");
    assertEquals(3, up.physicalAttack(), "physicalAttack +2");
    assertEquals(2, up.magicalAttack(), "magicalAttack +1");
    assertEquals(1, up.physicalDefense(), "physicalDefense +1");
    assertEquals(2, up.magicalDefense(), "magicalDefense +2");
  }

  @Test
  void plusWithZeroIsIdentity() {
    Stats base = new Stats(5, 10, 2, 1, 1, 0, 0);
    assertEquals(base, base.plus(StatsBonus.zero()));
  }

  @Test
  void plusClampsCurrentHpToNewMaxHpWhenMaxDecreases() {
    // 装備の負補正で maxHp が下がる場合、currentHp は新 maxHp でキャップされる (上限張り付き防止)
    Stats base = new Stats(10, 10, 2, 1, 1, 0, 0);
    Stats reduced = base.plus(new StatsBonus(-5, 0, 0, 0, 0, 0));

    assertEquals(5, reduced.maxHp(), "maxHp 10 - 5 = 5");
    assertEquals(5, reduced.currentHp(), "currentHp は 5 にキャップ");
  }

  @Test
  void plusEnforcesMinimumMaxHpOfOne() {
    // 装備の負補正が大きすぎても maxHp は最低 1 を保証 (record コンストラクタの maxHp > 0 制約を満たす)
    Stats base = new Stats(5, 10, 2, 1, 1, 0, 0);
    Stats minimal = base.plus(new StatsBonus(-999, 0, 0, 0, 0, 0));

    assertEquals(1, minimal.maxHp());
    assertEquals(1, minimal.currentHp());
  }

  @Test
  void plusClampsNegativeAttackDefenseFieldsToZero() {
    // 4 攻防ステは負値を弾く (record コンストラクタの非負制約を満たす)
    Stats base = new Stats(5, 10, 1, 1, 1, 1, 1);
    Stats stripped = base.plus(new StatsBonus(0, -10, -10, -10, -10, -10));

    assertEquals(0, stripped.speed());
    assertEquals(0, stripped.physicalAttack());
    assertEquals(0, stripped.magicalAttack());
    assertEquals(0, stripped.physicalDefense());
    assertEquals(0, stripped.magicalDefense());
  }

  @Test
  void plusReturnsNewInstanceLeavingOriginalIntact() {
    Stats base = new Stats(5, 10, 2, 1, 1, 0, 0);
    Stats up = base.plus(new StatsBonus(5, 1, 1, 1, 1, 1));

    assertEquals(10, base.maxHp(), "元 Stats は変化しない");
    assertEquals(15, up.maxHp());
  }

  @Test
  void plusRejectsNullBonus() {
    Stats base = new Stats(5, 10, 2, 1, 1, 0, 0);
    assertThrows(NullPointerException.class, () -> base.plus(null));
  }

  // ---------------- withMaxHpRaised (§15-8 層末ノード HP 強化用) ----------------

  @Test
  void withMaxHpRaisedIncreasesMaxHpAndCurrentHpByAmount() {
    // §15-8: 上限上昇分が即座に空きスロットを埋める体験
    Stats s = new Stats(5, 10, 2, 0, 0, 0, 0);
    Stats raised = s.withMaxHpRaised(5);
    assertEquals(15, raised.maxHp(), "maxHp が amount 分増加する");
    assertEquals(10, raised.currentHp(), "currentHp も amount 分増加する");
  }

  @Test
  void withMaxHpRaisedByZeroIsIdentity() {
    Stats s = new Stats(5, 10, 2, 0, 0, 0, 0);
    Stats unchanged = s.withMaxHpRaised(0);
    assertEquals(10, unchanged.maxHp());
    assertEquals(5, unchanged.currentHp());
  }

  @Test
  void withMaxHpRaisedPreservesOtherFields() {
    Stats s = new Stats(5, 10, 3, 7, 6, 5, 4);
    Stats raised = s.withMaxHpRaised(3);
    assertEquals(3, raised.speed());
    assertEquals(7, raised.physicalAttack());
    assertEquals(6, raised.magicalAttack());
    assertEquals(5, raised.physicalDefense());
    assertEquals(4, raised.magicalDefense());
  }

  @Test
  void withMaxHpRaisedReturnsNewInstanceLeavingOriginalIntact() {
    Stats original = new Stats(5, 10, 2, 0, 0, 0, 0);
    Stats raised = original.withMaxHpRaised(10);
    assertEquals(10, original.maxHp(), "元 Stats は変化しない");
    assertEquals(5, original.currentHp(), "元 currentHp は変化しない");
    assertEquals(20, raised.maxHp());
    assertEquals(15, raised.currentHp());
  }

  @Test
  void withMaxHpRaisedNegativeAmountRejected() {
    Stats s = new Stats(5, 10, 2, 0, 0, 0, 0);
    assertThrows(IllegalArgumentException.class, () -> s.withMaxHpRaised(-1));
  }

  // ---------------- withSpeedRaised (§15-8 層末ノード速度強化用) ----------------

  @Test
  void withSpeedRaisedIncreasesSpeedByAmount() {
    // §15-8: 速度 = 1 ターンの最大 AP (§15-4)
    Stats s = new Stats(5, 10, 3, 0, 0, 0, 0);
    Stats raised = s.withSpeedRaised(2);
    assertEquals(5, raised.speed(), "speed が amount 分増加する");
  }

  @Test
  void withSpeedRaisedByZeroIsIdentity() {
    Stats s = new Stats(5, 10, 3, 0, 0, 0, 0);
    assertEquals(3, s.withSpeedRaised(0).speed());
  }

  @Test
  void withSpeedRaisedDoesNotChangeHpFields() {
    Stats s = new Stats(5, 10, 3, 0, 0, 0, 0);
    Stats raised = s.withSpeedRaised(1);
    assertEquals(5, raised.currentHp(), "currentHp は変化しない");
    assertEquals(10, raised.maxHp(), "maxHp は変化しない");
  }

  @Test
  void withSpeedRaisedPreservesAttackAndDefenseFields() {
    Stats s = new Stats(5, 10, 3, 7, 6, 5, 4);
    Stats raised = s.withSpeedRaised(1);
    assertEquals(7, raised.physicalAttack());
    assertEquals(6, raised.magicalAttack());
    assertEquals(5, raised.physicalDefense());
    assertEquals(4, raised.magicalDefense());
  }

  @Test
  void withSpeedRaisedReturnsNewInstanceLeavingOriginalIntact() {
    Stats original = new Stats(5, 10, 3, 0, 0, 0, 0);
    Stats raised = original.withSpeedRaised(2);
    assertEquals(3, original.speed(), "元 Stats は変化しない");
    assertEquals(5, raised.speed());
  }

  @Test
  void withSpeedRaisedNegativeAmountRejected() {
    Stats s = new Stats(5, 10, 3, 0, 0, 0, 0);
    assertThrows(IllegalArgumentException.class, () -> s.withSpeedRaised(-1));
  }
}
