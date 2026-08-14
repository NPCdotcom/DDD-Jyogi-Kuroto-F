package core.domain.save;

import java.util.OptionalInt;
import java.util.logging.Logger;

/**
 * ラン終了時および放棄時のソウル精算計算を行う純関数クラス (SAVE-03B / Phase 1)。
 *
 * <p>正常系数式: {@code settledSoulTotal = previousTotal + (finalRunSoul - initialRunSoul)}
 *
 * <p>異常時: {@code finalRunSoul < initialRunSoul} の場合は壊れた checkpoint とみなし、 0
 * へクランプして精算成功させるのではなく、empty を返して Profile / Checkpoint の更新を拒否する。
 */
public final class SoulSettlement {

  private static final Logger LOG = Logger.getLogger(SoulSettlement.class.getName());

  private SoulSettlement() {}

  /**
   * ソウル精算計算を行う。
   *
   * @param previousTotal 精算前の Profile 恒久ソウル (>= 0)
   * @param finalRunSoul ラン終了/放棄時点の最終所持ソウル
   * @param initialRunSoul ラン開始/ロード時点の初期持越しソウル
   * @return 精算成功時は新しい Profile 恒久ソウル、異常 checkpoint 時は empty
   */
  public static OptionalInt settle(int previousTotal, int finalRunSoul, int initialRunSoul) {
    if (previousTotal < 0) {
      throw new IllegalArgumentException("previousTotal must be >= 0 (got " + previousTotal + ")");
    }
    if (initialRunSoul < 0) {
      throw new IllegalArgumentException(
          "initialRunSoul must be >= 0 (got " + initialRunSoul + ")");
    }
    if (finalRunSoul < initialRunSoul) {
      LOG.severe(
          "Refusing to settle corrupted checkpoint: finalRunSoul ("
              + finalRunSoul
              + ") is less than initialRunSoul ("
              + initialRunSoul
              + ")");
      return OptionalInt.empty();
    }
    int netEarned = finalRunSoul - initialRunSoul;
    return OptionalInt.of(previousTotal + netEarned);
  }
}
