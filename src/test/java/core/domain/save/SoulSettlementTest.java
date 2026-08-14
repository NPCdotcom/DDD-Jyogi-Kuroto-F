package core.domain.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class SoulSettlementTest {

  @Test
  void settleNormalGain() {
    OptionalInt result = SoulSettlement.settle(1000, 1300, 1000);
    assertTrue(result.isPresent());
    assertEquals(1300, result.getAsInt());
  }

  @Test
  void settleWithZeroInitialAndGain() {
    OptionalInt result = SoulSettlement.settle(500, 200, 0);
    assertTrue(result.isPresent());
    assertEquals(700, result.getAsInt());
  }

  @Test
  void settleRefusesCorruptedFinalLessThanInitial() {
    OptionalInt result = SoulSettlement.settle(1000, 900, 1000);
    assertTrue(result.isEmpty(), "finalRunSoul < initialRunSoul の場合は精算拒否して empty を返すこと");
  }

  @Test
  void settleRejectsNegativePreviousTotal() {
    assertThrows(IllegalArgumentException.class, () -> SoulSettlement.settle(-1, 100, 100));
  }

  @Test
  void settleRejectsNegativeInitialRunSoul() {
    assertThrows(IllegalArgumentException.class, () -> SoulSettlement.settle(100, 100, -1));
  }
}
