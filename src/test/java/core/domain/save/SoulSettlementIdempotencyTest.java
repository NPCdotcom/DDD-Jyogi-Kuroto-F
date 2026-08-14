package core.domain.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/**
 * 同じラン終了処理が 2 回走っても Soul が増えないことを固定する。
 *
 * <p>{@code GameOverScreen.show()} は Screen ライフサイクルで再度呼ばれうる。旧 {@code DddGame.onRunEnded()} は
 * RunSession 無しでも精算を続行しており、2 回目は {@code initialRunSoul} が 0 になるため {@code delta = 恒久総額そのもの}となって Soul
 * と runCount が二重加算されていた。
 *
 * <p>本試験は精算式そのものが「2 回目の入力」で破綻することを示す。呼出側 ({@code onRunEnded}) は RunSession
 * が無い呼び出しを精算せずスキップすることで、この入力自体を作らせない。
 */
class SoulSettlementIdempotencyTest {

  /** 恒久 500 のプレイヤーがラン中に 30 稼いだ状態。 */
  private static final int PREVIOUS_TOTAL = 500;

  private static final int INITIAL_RUN_SOUL = 500;
  private static final int FINAL_RUN_SOUL = 530;

  @Test
  void firstSettlementAddsOnlyWhatWasEarnedDuringTheRun() {
    OptionalInt settled = SoulSettlement.settle(PREVIOUS_TOTAL, FINAL_RUN_SOUL, INITIAL_RUN_SOUL);

    assertTrue(settled.isPresent());
    assertEquals(530, settled.getAsInt(), "500 + (530 - 500) = 530。持越しは二重に乗らない");
  }

  @Test
  void secondSettlementWithLostInitialSoulWouldDoubleCount() {
    // 1 回目の結果を恒久側に反映した状態で、initialRunSoul を失ったまま再精算した場合。
    // これは「RunSession 無しで onRunEnded が再入した」ときに起きていた入力そのもの。
    int afterFirst =
        SoulSettlement.settle(PREVIOUS_TOTAL, FINAL_RUN_SOUL, INITIAL_RUN_SOUL).getAsInt();

    OptionalInt doubled = SoulSettlement.settle(afterFirst, afterFirst, 0);

    assertEquals(
        1060,
        doubled.getAsInt(),
        "initialRunSoul を 0 と誤ると 530 + 530 = 1060 に膨らむ。呼出側がこの入力を作ってはならない");
  }

  @Test
  void settlementIsIdempotentWhenInitialRunSoulIsPreserved() {
    // initialRunSoul が正しく保たれている限り、同じ入力を何度通しても結果は変わらない。
    int once = SoulSettlement.settle(PREVIOUS_TOTAL, FINAL_RUN_SOUL, INITIAL_RUN_SOUL).getAsInt();
    int twice = SoulSettlement.settle(PREVIOUS_TOTAL, FINAL_RUN_SOUL, INITIAL_RUN_SOUL).getAsInt();

    assertEquals(once, twice, "同じ入力なら何度でも同じ結果");
  }

  @Test
  void inRunSoulSpendingIsSettledAsANegativeDelta() {
    // 治療の泉 (HP+20 / Soul-10) のようにラン中の消費がある。final < initial は破損ではない。
    // ここを拒否すると、泉を使って死んだプレイヤーが精算ごと失われる。
    OptionalInt settled = SoulSettlement.settle(1000, 990, 1000);

    assertTrue(settled.isPresent(), "ラン中の消費は正常な状態");
    assertEquals(990, settled.getAsInt(), "1000 + (990 - 1000) = 990");
  }

  @Test
  void overflowIsRefusedRatherThanWrappingToNegative() {
    assertEquals(
        Integer.MAX_VALUE, SoulSettlement.settle(Integer.MAX_VALUE, 0, 0).getAsInt(), "境界ちょうどは成功");
    assertTrue(SoulSettlement.settle(Integer.MAX_VALUE, 1, 0).isEmpty(), "int を超える総額は拒否する");
  }

  @Test
  void negativeSettledTotalIsRefused() {
    // 消費が恒久総額を上回るのは実際には起こりえない。破損とみなす。
    assertTrue(SoulSettlement.settle(10, 0, 100).isEmpty(), "総額が負になる入力は拒否する");
  }

  @Test
  void boundaryTableIsFixed() {
    // 0 + (MAX - 0) → MAX 成功
    assertEquals(Integer.MAX_VALUE, SoulSettlement.settle(0, Integer.MAX_VALUE, 0).getAsInt());
    // MAX + (0 - 0) → MAX 成功
    assertEquals(Integer.MAX_VALUE, SoulSettlement.settle(Integer.MAX_VALUE, 0, 0).getAsInt());
    // MAX + (1 - 0) → overflow 拒否
    assertTrue(SoulSettlement.settle(Integer.MAX_VALUE, 1, 0).isEmpty());
    // 100 + (99 - 100) → 99。ラン中消費は正常 (治療の泉)
    assertEquals(99, SoulSettlement.settle(100, 99, 100).getAsInt());
    // finalRunSoul < 0 → 拒否
    assertThrows(IllegalArgumentException.class, () -> SoulSettlement.settle(100, -1, 0));
  }
}
