package core.domain.dungeon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.common.Position;
import java.util.List;
import org.junit.jupiter.api.Test;

class DungeonMapTest {

  private static final List<String> THREE_BY_THREE = List.of("###", "#.>", "###");

  @Test
  void widthAndHeightAreInferredFromRows() {
    DungeonMap m = DungeonMap.of(THREE_BY_THREE);
    assertEquals(3, m.width());
    assertEquals(3, m.height());
  }

  @Test
  void topRowMapsToHighestY() {
    // 入力の最初の行 = y=2 (height-1)
    DungeonMap m = DungeonMap.of(THREE_BY_THREE);
    assertEquals(Tile.WALL, m.tileAt(new Position(0, 2)));
    assertEquals(Tile.FLOOR, m.tileAt(new Position(1, 1)));
    assertEquals(Tile.STAIRS_DOWN, m.tileAt(new Position(2, 1)));
    assertEquals(Tile.WALL, m.tileAt(new Position(0, 0)));
  }

  @Test
  void walkableMirrorsTileWalkability() {
    DungeonMap m = DungeonMap.of(THREE_BY_THREE);
    assertTrue(m.isWalkable(new Position(1, 1)));
    assertTrue(m.isWalkable(new Position(2, 1))); // STAIRS_DOWN も walk 可
    assertFalse(m.isWalkable(new Position(0, 1)));
  }

  @Test
  void outOfBoundsIsNotWalkable() {
    DungeonMap m = DungeonMap.of(THREE_BY_THREE);
    assertFalse(m.isWalkable(new Position(-1, 0)));
    assertFalse(m.isWalkable(new Position(0, 99)));
  }

  @Test
  void tileAtOutOfBoundsThrows() {
    DungeonMap m = DungeonMap.of(THREE_BY_THREE);
    assertThrows(IllegalArgumentException.class, () -> m.tileAt(new Position(99, 0)));
  }

  @Test
  void mismatchedRowWidthRejected() {
    assertThrows(IllegalArgumentException.class, () -> DungeonMap.of(List.of("###", "##")));
  }

  @Test
  void emptyRowsRejected() {
    assertThrows(IllegalArgumentException.class, () -> DungeonMap.of(List.of()));
  }

  @Test
  void unknownGlyphRejected() {
    assertThrows(IllegalArgumentException.class, () -> DungeonMap.of(List.of("###", "#@#", "###")));
  }
}
