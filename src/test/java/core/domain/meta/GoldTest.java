package core.domain.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GoldTest {

  @Test
  void zeroIsZero() {
    assertEquals(0, Gold.zero().amount());
  }

  @Test
  void addCombinesAmounts() {
    Gold total = new Gold(5).add(new Gold(15));
    assertEquals(20, total.amount());
  }

  @Test
  void subtractEnforcesSufficient() {
    assertThrows(IllegalStateException.class, () -> new Gold(3).subtract(new Gold(5)));
  }

  @Test
  void negativeAmountRejected() {
    assertThrows(IllegalArgumentException.class, () -> new Gold(-1));
  }

  @Test
  void subtractReturnsRemainder() {
    assertEquals(45, new Gold(50).subtract(new Gold(5)).amount());
    assertEquals(0, new Gold(20).subtract(new Gold(20)).amount());
  }

  @Test
  void addReturnsNewInstanceLeavingOriginalIntact() {
    // 不変性検証 (record は構造的に不変だが、API の意図を明示)
    Gold original = new Gold(10);
    Gold added = original.add(new Gold(5));
    assertEquals(10, original.amount(), "元の Gold は変化してはならない");
    assertEquals(15, added.amount());
    assertNotSame(original, added);
  }

  @Test
  void addAndSubtractNullArgRejected() {
    Gold g = new Gold(10);
    assertThrows(NullPointerException.class, () -> g.add(null));
    assertThrows(NullPointerException.class, () -> g.subtract(null));
  }
}
