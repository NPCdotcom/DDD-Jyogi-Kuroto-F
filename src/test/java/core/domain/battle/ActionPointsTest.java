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
  // refilled — max を保ったまま current を満タンに (敵ターン頭リフィル、ADR-06)
  // =========================================================

  @Test
  void refilledRestoresCurrentToMaxWithoutChangingMax() {
    // 残 AP 1 / max 5 → current だけ 5 に回復、max は据置 (refilledTo と違い max を上書きしない)
    ActionPoints ap = new ActionPoints(1, 5);
    ActionPoints refilled = ap.refilled();
    assertEquals(5, refilled.current());
    assertEquals(5, refilled.max());
  }

  @Test
  void refilledOnEmptyApRestoresToMax() {
    ActionPoints refilled = ActionPoints.empty(3).refilled();
    assertEquals(3, refilled.current());
    assertEquals(3, refilled.max());
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

  // =========================================================
  // P3-6: refilled() と refilledTo() の違いを明示
  // =========================================================

  /** ADR-06: 敵 AP は層番号で固定。refilled() は max を変えず current を max に戻す (敵ターン頭用)。 */
  @Test
  void refilledPreservesMaxWhileRestoringCurrentToMax() {
    // AP 残 0 / max 3 → refilled → current=3, max=3 (max は 3 のまま)
    ActionPoints ap = new ActionPoints(0, 3);
    ActionPoints refilled = ap.refilled();

    assertEquals(3, refilled.max(), "max は変化しない");
    assertEquals(3, refilled.current(), "current は max まで回復");
  }

  /**
   * refilled と refilledTo の違い: refilledTo は max も上書きするが refilled は max 据置。
   *
   * <p>敵に refilledTo(speed) を使うと「敵 AP = 層番号」仕様が崩れるため、敵ターン頭は refilled() を使う (ADR-06)。
   */
  @Test
  void refilledDoesNotChangMaxUnlikeRefilledTo() {
    ActionPoints ap = new ActionPoints(1, 5);

    ActionPoints byRefilled = ap.refilled(); // max 据置
    ActionPoints byRefilledTo = ap.refilledTo(3); // max を 3 に書き換え

    assertEquals(5, byRefilled.max(), "refilled は max を据置 (5 のまま)");
    assertEquals(3, byRefilledTo.max(), "refilledTo は max を新値 (3) に変更する");
    assertEquals(5, byRefilled.current(), "refilled の current は元 max (5) に回復");
    assertEquals(3, byRefilledTo.current(), "refilledTo の current は新 max (3) に回復");
  }
}
