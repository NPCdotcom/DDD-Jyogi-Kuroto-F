package core.domain.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PositionTest {

  @Test
  void moveDelegatesToDirectionOffsets() {
    Position origin = new Position(2, 3);
    assertEquals(new Position(2, 4), origin.move(Direction.UP));
    assertEquals(new Position(2, 2), origin.move(Direction.DOWN));
    assertEquals(new Position(1, 3), origin.move(Direction.LEFT));
    assertEquals(new Position(3, 3), origin.move(Direction.RIGHT));
  }

  @Test
  void manhattanDistanceIsAbsSumOfAxisDifferences() {
    assertEquals(0, new Position(0, 0).manhattanDistanceTo(new Position(0, 0)));
    // |1-4| + |2-(-2)| = 3 + 4 = 7
    assertEquals(7, new Position(1, 2).manhattanDistanceTo(new Position(4, -2)));
    assertEquals(4, new Position(-1, -1).manhattanDistanceTo(new Position(1, 1)));
  }

  @Test
  void isAdjacentIsTrueOnlyForOrthogonalNeighbor() {
    Position p = new Position(5, 5);
    assertTrue(p.isAdjacentTo(new Position(5, 6)));
    assertTrue(p.isAdjacentTo(new Position(4, 5)));
    assertFalse(p.isAdjacentTo(p));
    assertFalse(p.isAdjacentTo(new Position(6, 6))); // 斜めは隣接ではない
    assertFalse(p.isAdjacentTo(new Position(5, 7)));
  }

  @Test
  void moveRejectsNullDirection() {
    assertThrows(NullPointerException.class, () -> new Position(0, 0).move(null));
  }
}
