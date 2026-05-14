package core.domain.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** ActionPoints の §15-3 使い切り型仕様 (ADR-01) と既存 spend/canSpend 意味論の検証。 */
class ActionPointsTest {

  // =========================================================
  // refilledTo — §15-3 使い切り型ターン頭再充填
  // =========================================================

  @Test
  void refilledToResetsCurrentToNewMax() {
    // 残 AP 2 のところに新 max 3 を指定すると current も 3 まで「全」充填される (蓄積ではない)
    ActionPoints ap = new ActionPoints(2, 5);
    ActionPoints refilled = ap.refilledTo(3);
    assertEquals(3, refilled.current());
    assertEquals(3, refilled.max());
  }

  @Test
  void refilledToShrinksMaxWhenSpeedDecreased() {
    // 速度ステが下がった (debuff 等) を想定: 旧 max=5、新 max=2 に縮小
    ActionPoints full = ActionPoints.full(5);
    ActionPoints shrunk = full.refilledTo(2);
    assertEquals(2, shrunk.current());
    assertEquals(2, shrunk.max());
  }

  @Test
  void refilledToWithZeroProducesEmptyMaxZero() {
    // 速度ステ 0 を想定: max=0 / current=0、isEmpty が true
    ActionPoints zeroSpeed = ActionPoints.full(5).refilledTo(0);
    assertEquals(0, zeroSpeed.current());
    assertEquals(0, zeroSpeed.max());
    assertTrue(zeroSpeed.isEmpty());
  }

  @Test
  void refilledToWithNegativeRejected() {
    ActionPoints ap = ActionPoints.full(5);
    assertThrows(IllegalArgumentException.class, () -> ap.refilledTo(-1));
  }

  // =========================================================
  // spend / canSpend / isEmpty — 既存意味論据置の確認
  // =========================================================

  @Test
  void spendDecrementsCurrent() {
    ActionPoints ap = ActionPoints.full(5);
    assertEquals(2, ap.spend(3).current());
  }

  @Test
  void spendBeyondCurrentThrows() {
    ActionPoints ap = new ActionPoints(2, 5);
    assertThrows(IllegalStateException.class, () -> ap.spend(3));
  }

  @Test
  void canSpendBoundaryChecks() {
    ActionPoints ap = new ActionPoints(2, 5);
    assertTrue(ap.canSpend(0));
    assertTrue(ap.canSpend(2));
    assertFalse(ap.canSpend(3));
    assertFalse(ap.canSpend(-1));
  }

  @Test
  void isEmptyOnlyAtZero() {
    assertTrue(ActionPoints.empty(5).isEmpty());
    assertFalse(new ActionPoints(1, 5).isEmpty());
  }

  // =========================================================
  // compact constructor 境界 (max >= 0 への緩和を含む)
  // =========================================================

  @Test
  void constructorRejectsNegativeMax() {
    assertThrows(IllegalArgumentException.class, () -> new ActionPoints(0, -1));
  }

  @Test
  void constructorAllowsMaxZero() {
    // §15-3 で速度ステ 0 のキャラを表現するため、max=0 を許容 (旧仕様の max > 0 から緩和)
    ActionPoints zero = new ActionPoints(0, 0);
    assertTrue(zero.isEmpty());
    assertEquals(0, zero.max());
  }

  @Test
  void constructorRejectsCurrentOutOfRange() {
    assertThrows(IllegalArgumentException.class, () -> new ActionPoints(-1, 5));
    assertThrows(IllegalArgumentException.class, () -> new ActionPoints(6, 5));
  }
}
