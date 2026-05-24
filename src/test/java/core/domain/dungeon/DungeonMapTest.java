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

  // Phase 1 回帰: equals / hashCode (値等価性の明示実装)

  @Test
  void equalMapsAreEqual() {
    // 同じ行リストから生成した 2 つの DungeonMap は値等価
    DungeonMap a = DungeonMap.of(THREE_BY_THREE);
    DungeonMap b = DungeonMap.of(THREE_BY_THREE);
    assertEquals(a, b, "同じ行リストから生成した DungeonMap は等しい");
  }

  @Test
  void equalMapsHaveSameHashCode() {
    DungeonMap a = DungeonMap.of(THREE_BY_THREE);
    DungeonMap b = DungeonMap.of(THREE_BY_THREE);
    assertEquals(a.hashCode(), b.hashCode(), "等しい DungeonMap は同一の hashCode を持つ");
  }

  @Test
  void equalityIsSymmetric() {
    DungeonMap a = DungeonMap.of(THREE_BY_THREE);
    DungeonMap b = DungeonMap.of(THREE_BY_THREE);
    assertEquals(a, b);
    assertEquals(b, a, "equals は対称的");
  }

  @Test
  void mapsWithDifferentWidthAreNotEqual() {
    // 幅が異なる → 非等価
    DungeonMap wide = DungeonMap.of(List.of("####", "#..#", "####"));
    DungeonMap narrow = DungeonMap.of(List.of("###", "#.#", "###"));
    assertFalse(wide.equals(narrow), "幅が異なる DungeonMap は等しくない");
  }

  @Test
  void mapsWithDifferentHeightAreNotEqual() {
    // 高さが異なる → 非等価
    DungeonMap tall = DungeonMap.of(List.of("###", "#.#", "#.#", "###"));
    DungeonMap short_ = DungeonMap.of(List.of("###", "#.#", "###"));
    assertFalse(tall.equals(short_), "高さが異なる DungeonMap は等しくない");
  }

  @Test
  void mapsWithDifferentTileContentAreNotEqual() {
    // タイル内容が 1 つでも違う → 非等価
    DungeonMap withFloor = DungeonMap.of(List.of("###", "#.#", "###"));
    DungeonMap withStairs = DungeonMap.of(List.of("###", "#>#", "###"));
    assertFalse(withFloor.equals(withStairs), "タイル内容が異なる DungeonMap は等しくない");
  }

  @Test
  void mapDoesNotEqualNull() {
    DungeonMap m = DungeonMap.of(THREE_BY_THREE);
    assertFalse(m.equals(null), "DungeonMap は null と等しくない");
  }

  @Test
  void mapDoesNotEqualOtherType() {
    DungeonMap m = DungeonMap.of(THREE_BY_THREE);
    assertFalse(m.equals("not a DungeonMap"), "DungeonMap は異なる型のオブジェクトと等しくない");
  }

  // Wave 11 W11-α: withTileAt 不変性 + 境界防御

  @Test
  void withTileAtReturnsNewMapWithSingleTileChanged() {
    DungeonMap original = DungeonMap.of(List.of("#####", "#...#", "#...#", "#...#", "#####"));
    DungeonMap modified = original.withTileAt(new Position(2, 2), Tile.BREAKABLE_WALL);
    assertEquals(Tile.BREAKABLE_WALL, modified.tileAt(new Position(2, 2)));
    // 元の DungeonMap は不変
    assertEquals(Tile.FLOOR, original.tileAt(new Position(2, 2)));
    // 他のマスは変わっていない
    assertEquals(Tile.FLOOR, modified.tileAt(new Position(1, 1)));
    assertEquals(Tile.FLOOR, modified.tileAt(new Position(3, 3)));
    assertEquals(Tile.WALL, modified.tileAt(new Position(0, 0)));
  }

  @Test
  void withTileAtRejectsOuterBorderTiles() {
    // CTO チェックポイント #1: 外枠 (x=0 / x=width-1 / y=0 / y=height-1) は不変条件で IAE
    DungeonMap m = DungeonMap.of(List.of("#####", "#...#", "#...#", "#...#", "#####"));
    assertThrows(
        IllegalArgumentException.class, () -> m.withTileAt(new Position(0, 2), Tile.FLOOR));
    assertThrows(
        IllegalArgumentException.class,
        () -> m.withTileAt(new Position(m.width() - 1, 2), Tile.FLOOR));
    assertThrows(
        IllegalArgumentException.class, () -> m.withTileAt(new Position(2, 0), Tile.FLOOR));
    assertThrows(
        IllegalArgumentException.class,
        () -> m.withTileAt(new Position(2, m.height() - 1), Tile.FLOOR));
  }

  @Test
  void withTileAtRejectsOutOfBounds() {
    DungeonMap m = DungeonMap.of(List.of("#####", "#...#", "#...#", "#...#", "#####"));
    assertThrows(
        IllegalArgumentException.class, () -> m.withTileAt(new Position(99, 2), Tile.FLOOR));
  }

  // Wave 13 W13-α: hasLineOfSight + canSeeWithin

  @Test
  void hasLineOfSightTrueForSamePosition() {
    DungeonMap m = DungeonMap.of(List.of("#####", "#...#", "#...#", "#...#", "#####"));
    assertTrue(m.hasLineOfSight(new Position(2, 2), new Position(2, 2)));
  }

  @Test
  void hasLineOfSightTrueOnOpenStraightLine() {
    // (1,1) → (3,1) は床直線、視線通る
    DungeonMap m = DungeonMap.of(List.of("#####", "#...#", "#...#", "#...#", "#####"));
    assertTrue(m.hasLineOfSight(new Position(1, 1), new Position(3, 1)));
  }

  @Test
  void hasLineOfSightTrueOnDiagonal() {
    // (1,1) → (3,3) 斜め、すべて床
    DungeonMap m = DungeonMap.of(List.of("#####", "#...#", "#...#", "#...#", "#####"));
    assertTrue(m.hasLineOfSight(new Position(1, 1), new Position(3, 3)));
  }

  @Test
  void hasLineOfSightFalseBlockedByWall() {
    // (1,2) → (3,2)、間の (2,2) が壁
    DungeonMap m = DungeonMap.of(List.of("#####", "#...#", "#.#.#", "#...#", "#####"));
    assertFalse(m.hasLineOfSight(new Position(1, 2), new Position(3, 2)));
  }

  @Test
  void hasLineOfSightFalseBlockedByBreakableWall() {
    // CTO #2: BREAKABLE_WALL も視線遮断 (Wave 11 一貫性)
    DungeonMap m = DungeonMap.of(List.of("#####", "#...#", "#.B.#", "#...#", "#####"));
    assertFalse(m.hasLineOfSight(new Position(1, 2), new Position(3, 2)));
  }

  @Test
  void canSeeWithinFalseWhenOutOfRange() {
    DungeonMap m =
        DungeonMap.of(List.of("########", "#......#", "#......#", "#......#", "########"));
    // (1,2) → (6,2) はチェビシェフ距離 5、視界半径 4 では見えない
    assertFalse(m.canSeeWithin(new Position(1, 2), new Position(6, 2), 4));
    // 視界半径 5 にすれば見える
    assertTrue(m.canSeeWithin(new Position(1, 2), new Position(6, 2), 5));
  }

  @Test
  void canSeeWithinFalseWhenBlockedEvenIfInRange() {
    // 距離は範囲内だが間に壁があれば見えない
    DungeonMap m = DungeonMap.of(List.of("#####", "#...#", "#.#.#", "#...#", "#####"));
    assertFalse(m.canSeeWithin(new Position(1, 2), new Position(3, 2), 5));
  }
}
