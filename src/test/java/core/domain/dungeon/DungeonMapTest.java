package core.domain.dungeon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.common.Direction;
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

  // §15-6: 到達可能性 BFS (DungeonGenerator のソフトロック検証に使う)

  @Test
  void reachableTrueForConnectedFloors() {
    DungeonMap m = DungeonMap.of(List.of("#####", "#...#", "#...#", "#...#", "#####"));
    assertTrue(m.reachable(new Position(1, 1), new Position(3, 3)));
  }

  @Test
  void reachableTrueForSameWalkablePosition() {
    DungeonMap m = DungeonMap.of(List.of("#####", "#...#", "#...#", "#...#", "#####"));
    assertTrue(m.reachable(new Position(2, 2), new Position(2, 2)));
  }

  @Test
  void reachableTrueDetouringAroundPillar() {
    // 中央 (2,2) のピラーを迂回して (1,1) → (3,3) に到達できる
    DungeonMap m = DungeonMap.of(List.of("#####", "#...#", "#.#.#", "#...#", "#####"));
    assertTrue(m.reachable(new Position(1, 1), new Position(3, 3)));
  }

  @Test
  void reachableFalseWhenWalledOff() {
    // x=2 の壁列が左右の床領域を完全分断する
    DungeonMap m = DungeonMap.of(List.of("#####", "#.#.#", "#.#.#", "#.#.#", "#####"));
    assertFalse(m.reachable(new Position(1, 1), new Position(3, 3)));
  }

  @Test
  void reachableFalseWhenEndpointIsWall() {
    DungeonMap m = DungeonMap.of(List.of("#####", "#...#", "#...#", "#...#", "#####"));
    assertFalse(m.reachable(new Position(1, 1), new Position(0, 0)));
  }

  @Test
  void reachableFalseForOutOfBoundsEndpoint() {
    DungeonMap m = DungeonMap.of(List.of("#####", "#...#", "#...#", "#...#", "#####"));
    assertFalse(m.reachable(new Position(1, 1), new Position(99, 99)));
  }

  // §15-6: BFS 経路の 1 歩目 (敵 AI の壁迂回)

  @Test
  void firstStepTowardReturnsDirectStepInOpenRoom() {
    DungeonMap m = DungeonMap.of(List.of("#####", "#...#", "#...#", "#...#", "#####"));
    assertEquals(
        Direction.RIGHT, m.firstStepToward(new Position(1, 1), new Position(3, 1)).orElseThrow());
  }

  @Test
  void firstStepTowardDetoursAroundWall() {
    // 中央列 x=2 が y=2,3 で壁。(3,3) → (1,3) の直線 LEFT は不可、迂回経路の 1 歩目は DOWN。
    DungeonMap m = DungeonMap.of(List.of("#####", "#.#.#", "#.#.#", "#...#", "#####"));
    assertEquals(
        Direction.DOWN, m.firstStepToward(new Position(3, 3), new Position(1, 3)).orElseThrow());
  }

  @Test
  void firstStepTowardEmptyWhenWalledOff() {
    DungeonMap m = DungeonMap.of(List.of("#####", "#.#.#", "#.#.#", "#.#.#", "#####"));
    assertTrue(m.firstStepToward(new Position(1, 1), new Position(3, 3)).isEmpty());
  }

  @Test
  void firstStepTowardEmptyForSamePosition() {
    DungeonMap m = DungeonMap.of(List.of("#####", "#...#", "#...#", "#...#", "#####"));
    assertTrue(m.firstStepToward(new Position(2, 2), new Position(2, 2)).isEmpty());
  }
}
