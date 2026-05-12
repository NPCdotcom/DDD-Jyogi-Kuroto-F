package core.domain.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SoulTest {

  @Test
  void zeroIsZero() {
    assertEquals(0, Soul.zero().amount());
  }

  @Test
  void addCombinesAmounts() {
    Soul total = new Soul(3).add(new Soul(7));
    assertEquals(10, total.amount());
  }

  @Test
  void subtractEnforcesNonNegative() {
    assertThrows(IllegalStateException.class, () -> new Soul(1).subtract(new Soul(2)));
  }

  @Test
  void negativeAmountRejected() {
    assertThrows(IllegalArgumentException.class, () -> new Soul(-1));
  }

  @Test
  void subtractReturnsRemainder() {
    assertEquals(7, new Soul(10).subtract(new Soul(3)).amount());
    assertEquals(0, new Soul(5).subtract(new Soul(5)).amount());
  }
}
