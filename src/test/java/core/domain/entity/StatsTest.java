package core.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
