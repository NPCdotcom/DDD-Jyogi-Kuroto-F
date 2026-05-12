package core.domain.dungeon;

import core.domain.common.Position;
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
}
