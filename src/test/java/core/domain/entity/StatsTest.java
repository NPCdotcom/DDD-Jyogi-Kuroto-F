package core.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StatsTest {

  @Test
  void constructorRejectsInvalidValues() {
    assertThrows(IllegalArgumentException.class, () -> new Stats(0, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> new Stats(-1, 10, 1));
    assertThrows(IllegalArgumentException.class, () -> new Stats(11, 10, 1));
    assertThrows(IllegalArgumentException.class, () -> new Stats(5, 10, -1));
  }

  @Test
  void damagedFloorsAtZero() {
    Stats s = new Stats(5, 10, 2);
    assertEquals(0, s.damaged(100).currentHp());
    assertFalse(s.damaged(100).isAlive());
  }

  @Test
  void healedCeilingsAtMax() {
    Stats s = new Stats(5, 10, 2);
    assertEquals(10, s.healed(999).currentHp());
  }

  @Test
  void damagedReturnsNewInstanceLeavingOriginalIntact() {
    Stats original = new Stats(5, 10, 2);
    Stats reduced = original.damaged(2);
    assertEquals(5, original.currentHp(), "元の Stats は変化してはならない");
    assertEquals(3, reduced.currentHp());
  }

  @Test
  void isAliveTrueAboveZero() {
    assertTrue(new Stats(1, 10, 1).isAlive());
    assertFalse(new Stats(0, 10, 1).isAlive());
  }

  @Test
  void negativeDamageOrHealRejected() {
    Stats s = new Stats(5, 10, 1);
    assertThrows(IllegalArgumentException.class, () -> s.damaged(-1));
    assertThrows(IllegalArgumentException.class, () -> s.healed(-1));
  }
}
