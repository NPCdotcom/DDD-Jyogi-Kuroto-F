package core.domain.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  void corruptedCheckpointIsRefusedRatherThanSilentlyClamped() {
    // final < initial は Checkpoint 破損。0 へ丸めず精算自体を拒否する。
    assertTrue(
        SoulSettlement.settle(PREVIOUS_TOTAL, 100, 500).isEmpty(),
        "finalRunSoul < initialRunSoul は精算を拒否する");
  }
}
