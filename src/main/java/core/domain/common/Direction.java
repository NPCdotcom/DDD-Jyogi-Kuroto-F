package core.domain.common;

import java.util.Objects;

/** 4 方向の移動。各方向の dx/dy オフセットを提供する。 */
public enum Direction {
  UP(0, 1),
  DOWN(0, -1),
  LEFT(-1, 0),
  RIGHT(1, 0);

  private final int dx;
  private final int dy;

  Direction(int dx, int dy) {
    this.dx = dx;
    this.dy = dy;
  }

  public int dx() {
    return dx;
  }

  public int dy() {
    return dy;
  }

  /**
   * {@code from} から {@code to} へ向かう 1 ステップを返す。
   *
   * <p>X 軸と Y 軸で残距離が大きい方を優先し、同距離なら X 軸優先 (同地点の場合は {@link #RIGHT})。
   *
   * <p>TODO: 障害物回避や経路探索を導入する段階で、本メソッドはより賢いステップ選択 (A* 等) に差し替える。 現状は MVP の四角部屋を前提とした単純実装。
   */
  public static Direction towards(Position from, Position to) {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    int dxAbs = Math.abs(to.x() - from.x());
    int dyAbs = Math.abs(to.y() - from.y());
    if (dxAbs >= dyAbs) {
      return to.x() >= from.x() ? RIGHT : LEFT;
    }
    return to.y() >= from.y() ? UP : DOWN;
  }
}
