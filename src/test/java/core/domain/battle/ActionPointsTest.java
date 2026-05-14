package core.domain.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ActionPointsTest {

  @Test
  void regenerateCapsAtMax() {
    ActionPoints ap = new ActionPoints(3, 5);
    assertEquals(5, ap.regenerate(10).current());
  }

  @Test
  void regenerateAccumulatesBelowMax() {
    ActionPoints ap = new ActionPoints(1, 5);
    assertEquals(3, ap.regenerate(2).current());
  }

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

  @Test
  void negativeRegenerateRejected() {
    ActionPoints ap = ActionPoints.full(5);
    assertThrows(IllegalArgumentException.class, () -> ap.regenerate(-1));
  }

  @Test
  void regenerateAtMaxStaysAtMax() {
    // current == max のときに更に regenerate しても max のまま (冪等)
    ActionPoints full = new ActionPoints(5, 5);
    assertEquals(5, full.regenerate(3).current());
    assertEquals(5, full.regenerate(3).regenerate(3).current());
  }

  @Test
  void regenerateZeroIsNoOp() {
    ActionPoints ap = new ActionPoints(3, 5);
    assertEquals(3, ap.regenerate(0).current());
  }
}
