package core.domain.dungeon;

/** ダンジョン1マスの種類。glyph はテキストマップ表現に用いる。 */
public enum Tile {
  FLOOR('.', true),
  WALL('#', false),
  STAIRS_DOWN('>', true);

  private final char glyph;
  private final boolean walkable;

  Tile(char glyph, boolean walkable) {
    this.glyph = glyph;
    this.walkable = walkable;
  }

  public char glyph() {
    return glyph;
  }

  public boolean walkable() {
    return walkable;
  }

  public static Tile fromGlyph(char glyph) {
    for (Tile t : values()) {
      if (t.glyph == glyph) {
        return t;
      }
    }
    throw new IllegalArgumentException("unknown tile glyph: '" + glyph + "'");
  }
}
