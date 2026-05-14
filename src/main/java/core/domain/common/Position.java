package core.domain.common;

import java.util.Objects;

/**
 * 2D 座標を表す不変値オブジェクト。
 *
 * <p>座標系: 原点 (0,0) は左下。x は右方向、y は上方向に増加する。
 */
public record Position(int x, int y) {

  public Position move(Direction direction) {
    Objects.requireNonNull(direction, "direction");
    return new Position(x + direction.dx(), y + direction.dy());
  }

  public int manhattanDistanceTo(Position other) {
    Objects.requireNonNull(other, "other");
    return Math.abs(x - other.x) + Math.abs(y - other.y);
  }

  public boolean isAdjacentTo(Position other) {
    return manhattanDistanceTo(other) == 1;
  }
}
