package core.domain.layer;

import java.util.Objects;

/**
 * ダンジョンの階層 (§15-6)。{@code number} は 1 始まりの層番号、{@code roomIndex} は層内の現在部屋 番号 (1 始まり)、{@code
 * displayName} は HUD 表示用 (例: "1 層")。
 *
 * <p>§15-6「N 層目 = N 部屋構成」: 層 N は N 個の部屋を持ち、{@code roomIndex} が 1..N と進む。 部屋数そのものは {@code number}
 * から導出する (roomCount == number、DRY)。
 *
 * <p>ADR-06「敵 AP = 層番号 N」と整合、層が進むほど敵が強化される。本 record は階層メタ情報のみを 保持し、敵生成・マップ生成は {@code
 * InitialStateFactory} に委譲する (副作用と純粋値の分離)。
 *
 * <p>{@link #first()} で 1 層目 1 部屋目、{@link #next()} で次層 (部屋番号は 1 にリセット)、 {@link #nextRoom()}
 * で同層の次部屋を表現する。
 */
public record Layer(int number, int roomIndex, String displayName) {

  public Layer {
    if (number < 1) {
      throw new IllegalArgumentException("number must be >= 1: " + number);
    }
    if (roomIndex < 1) {
      throw new IllegalArgumentException("roomIndex must be >= 1: " + roomIndex);
    }
    Objects.requireNonNull(displayName, "displayName");
    if (displayName.isBlank()) {
      throw new IllegalArgumentException("displayName must not be blank");
    }
  }

  /** 1 層目・1 部屋目を返す。InitialStateFactory.firstFloor の初期状態に使う。 */
  public static Layer first() {
    return new Layer(1, 1, "1 層");
  }

  /** 次の層を返す。層番号 +1、部屋番号は 1 にリセット、displayName を再生成。 */
  public Layer next() {
    int nextNumber = number + 1;
    return new Layer(nextNumber, 1, nextNumber + " 層");
  }

  /** 同じ層の次の部屋を返す。部屋番号 +1、層番号・displayName は据置。 */
  public Layer nextRoom() {
    return new Layer(number, roomIndex + 1, displayName);
  }
}
