package core.domain.dungeon;

import core.domain.common.Direction;
import core.domain.common.Position;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
   * 指定座標のタイルを差し替えた新 {@link DungeonMap} を返す (Wave 11 W11-α、record-like immutable update)。
   *
   * <p>「外枠 (周囲 1 マス、x = 0 / x = width-1 / y = 0 / y = height-1)」を差し替えようとした場合は {@link
   * IllegalArgumentException} で弾く。外枠は地形の不変条件 (= 必ず WALL で囲まれている) を 保証するためのドメインモデル上の境界で、これを壊すと敵 AI
   * の経路探索やテキスト 描画が破綻するため、安全弁として多層防御する。
   *
   * <p>境界内なら、tilesByYx を行単位でディープコピーした上で 1 マスだけ書換える。元の {@code this} は 不変のまま。
   */
  public DungeonMap withTileAt(Position p, Tile newTile) {
    Objects.requireNonNull(p, "p");
    Objects.requireNonNull(newTile, "newTile");
    if (!inBounds(p)) {
      throw new IllegalArgumentException("out of bounds: " + p);
    }
    if (p.x() == 0 || p.x() == width - 1 || p.y() == 0 || p.y() == height - 1) {
      throw new IllegalArgumentException("cannot modify outer border tile: " + p);
    }
    Tile[][] grid = new Tile[height][];
    for (int y = 0; y < height; y++) {
      grid[y] = Arrays.copyOf(tilesByYx[y], width);
    }
    grid[p.y()][p.x()] = newTile;
    return new DungeonMap(width, height, grid);
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

  /**
   * {@code from} から {@code to} への最短経路の「最初の 1 歩」の向きを 4 近傍 BFS で求める (敵 AI の壁迂回用)。
   *
   * <p>{@link #reachable} と同型の BFS。各セルに「from から最初に踏んだ向き」を記録し、{@code to} に到達した
   * 時点でその向きを返す。壁を貫通しない経路が存在しなければ {@link Optional#empty()}。{@code from == to}、 いずれかが歩行不可、到達不能の場合も
   * empty。純粋関数 (状態を変更しない)。
   */
  public Optional<Direction> firstStepToward(Position from, Position to) {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    if (from.equals(to) || !isWalkable(from) || !isWalkable(to)) {
      return Optional.empty();
    }
    Direction[] dirs = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
    boolean[][] visited = new boolean[height][width];
    Direction[][] firstStep = new Direction[height][width];
    Deque<Position> queue = new ArrayDeque<>();
    queue.add(from);
    visited[from.y()][from.x()] = true;
    while (!queue.isEmpty()) {
      Position cur = queue.removeFirst();
      for (Direction d : dirs) {
        Position next = new Position(cur.x() + d.dx(), cur.y() + d.dy());
        if (!isWalkable(next) || visited[next.y()][next.x()]) {
          continue;
        }
        visited[next.y()][next.x()] = true;
        firstStep[next.y()][next.x()] = cur.equals(from) ? d : firstStep[cur.y()][cur.x()];
        if (next.equals(to)) {
          return Optional.of(firstStep[next.y()][next.x()]);
        }
        queue.add(next);
      }
    }
    return Optional.empty();
  }

  /**
   * 値等価性: width / height / 全タイル内容が一致すれば等しい (record ではないため明示実装)。
   *
   * <p>2 次元配列の比較は {@link Arrays#deepEquals} を使う (要素の {@code Tile} まで再帰比較)。
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DungeonMap other)) {
      return false;
    }
    return width == other.width
        && height == other.height
        && Arrays.deepEquals(tilesByYx, other.tilesByYx);
  }

  @Override
  public int hashCode() {
    return Objects.hash(width, height, Arrays.deepHashCode(tilesByYx));
  }
}
