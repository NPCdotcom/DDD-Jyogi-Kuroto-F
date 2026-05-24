package core.domain.layer;

import java.util.Objects;

/**
 * ダンジョンの階層 (§15-6)。{@code number} は 1 始まりの層番号、{@code displayName} は HUD 表示用 (例: "1 層")。
 *
 * <p>各層は BSP 生成された 1 枚の連続マップで、層内はシームレスに移動する。次層へは階段 (層 1・2) またはボス撃破 (層 3) で進む。ADR-06「敵 AP = 層番号
 * N」と整合し、層が進むほど敵が強化される。
 *
 * <p>本 record は階層メタ情報のみを保持し、マップ生成・敵生成は {@code InitialStateFactory} に委譲する (副作用と純粋値の分離)。{@link
 * #first()} で 1 層目、{@link #next()} で次層を表現する。
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

  /** 次の層を返す。層番号 +1、displayName を再生成。 */
  public Layer next() {
    int nextNumber = number + 1;
    return new Layer(nextNumber, nextNumber + " 層");
  }
}
