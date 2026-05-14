package core.domain.layer;

import java.util.Objects;

/**
 * ダンジョンの階層 (§15-6)。{@code number} は 1 始まりの層番号、{@code displayName} は HUD 表示用 (例: "1 層")。
 *
 * <p>ADR-06 「敵 AP = 層番号 N」と整合、層が進むほど敵が強化される。本 record は階層メタ情報のみを保持し、敵生成・マップ生成は {@code
 * InitialStateFactory.advanceLayer} に委譲する (副作用と純粋値の分離)。
 *
 * <p>{@link #first()} で 1 層目を、{@link #next()} で次層への遷移を表現する。
 */
public record Layer(int number, String displayName) {

  public Layer {
    if (number < 1) {
      throw new IllegalArgumentException("number must be >= 1: " + number);
    }
    Objects.requireNonNull(displayName, "displayName");
    if (displayName.isBlank()) {
      throw new IllegalArgumentException("displayName must not be blank");
    }
  }

  /** 1 層目を返す。InitialStateFactory.firstFloor の初期状態に使う。 */
  public static Layer first() {
    return new Layer(1, "1 層");
  }

  /** 次の層を返す。number++ し、displayName を再生成。 */
  public Layer next() {
    int nextNumber = number + 1;
    return new Layer(nextNumber, nextNumber + " 層");
  }
}
