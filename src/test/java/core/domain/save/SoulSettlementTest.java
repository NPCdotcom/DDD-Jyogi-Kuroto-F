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
  void settleAcceptsFinalLessThanInitialBecauseSoulCanBeSpentInRun() {
    // 治療の泉 (HP+20 / Soul-10) 等でラン中に消費できるため、final < initial は破損ではない。
    // 旧試験はこれを拒否対象としていたが、泉を使って死んだプレイヤーの精算を丸ごと失う挙動だった。
    assertEquals(90, SoulSettlement.settle(100, 90, 100).orElseThrow());
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
