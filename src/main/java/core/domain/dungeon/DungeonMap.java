package core.domain.dungeon;

import core.domain.common.Position;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * ダンジョン1階層の静的マップ。
 *
 * <p>外部からは座標で問い合わせる API のみ公開し、内部の Tile[][] を直接渡さない (カプセル化)。 テキストマップ表現は「上 = y
 * が大きい」とするため、行の並びを反転して格納する。
 */
public final class DungeonMap {

  private final int width;
  private final int height;
  private final Tile[][] tilesByYx;

  private DungeonMap(int width, int height, Tile[][] tilesByYx) {
    this.width = width;
    this.height = height;
    this.tilesByYx = tilesByYx;
  }

  /**
   * テキスト行のリストからマップを生成する。
   *
   * <p>{@code rowsTopToBottom.get(0)} が最上段 (y = height-1)。
   */
  public static DungeonMap of(List<String> rowsTopToBottom) {
    Objects.requireNonNull(rowsTopToBottom, "rowsTopToBottom");
    if (rowsTopToBottom.isEmpty()) {
      throw new IllegalArgumentException("rows must not be empty");
    }
    int height = rowsTopToBottom.size();
    int width = rowsTopToBottom.get(0).length();
    if (width == 0) {
      throw new IllegalArgumentException("row width must be positive");
    }
    Tile[][] grid = new Tile[height][width];
    for (int i = 0; i < height; i++) {
      String row = rowsTopToBottom.get(i);
      if (row.length() != width) {
        throw new IllegalArgumentException(
            "row %d length mismatch: expected %d, got %d".formatted(i, width, row.length()));
      }
      int y = height - 1 - i;
      for (int x = 0; x < width; x++) {
        grid[y][x] = Tile.fromGlyph(row.charAt(x));
      }
    }
    return new DungeonMap(width, height, grid);
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

  public boolean inBounds(Position p) {
    Objects.requireNonNull(p, "p");
    return p.x() >= 0 && p.x() < width && p.y() >= 0 && p.y() < height;
  }

  public Tile tileAt(Position p) {
    if (!inBounds(p)) {
      throw new IllegalArgumentException("out of bounds: " + p);
    }
    return tilesByYx[p.y()][p.x()];
  }

  public boolean isWalkable(Position p) {
    return inBounds(p) && tilesByYx[p.y()][p.x()].walkable();
  }

  /**
   * {@code from} から {@code to} へ歩いて到達できるかを 4 近傍 BFS で判定する (§15-6 ダンジョン 生成のソフトロック検証用)。壁を貫通しない経路が 1
   * つでもあれば true。
   *
   * <p>純粋関数 (状態を変更しない)。両端のいずれかが {@link #isWalkable} でない場合は false。 {@code from} と {@code to} が同一かつ
   * walkable なら true。
   */
  public boolean reachable(Position from, Position to) {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    if (!isWalkable(from) || !isWalkable(to)) {
      return false;
    }
    int[] dx = {1, -1, 0, 0};
    int[] dy = {0, 0, 1, -1};
    boolean[][] visited = new boolean[height][width];
    Deque<Position> queue = new ArrayDeque<>();
    queue.add(from);
    visited[from.y()][from.x()] = true;
    while (!queue.isEmpty()) {
      Position cur = queue.removeFirst();
      if (cur.equals(to)) {
        return true;
      }
      for (int d = 0; d < 4; d++) {
        Position next = new Position(cur.x() + dx[d], cur.y() + dy[d]);
        if (isWalkable(next) && !visited[next.y()][next.x()]) {
          visited[next.y()][next.x()] = true;
          queue.add(next);
        }
      }
    }
    return false;
  }
}
