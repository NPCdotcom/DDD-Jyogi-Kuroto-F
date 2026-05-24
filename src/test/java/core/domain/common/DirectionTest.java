package core.domain.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DirectionTest {

  @Test
  void offsetsMatchAxes() {
    assertEquals(0, Direction.UP.dx());
    assertEquals(1, Direction.UP.dy());
    assertEquals(-1, Direction.LEFT.dx());
    assertEquals(0, Direction.LEFT.dy());
  }

  @Test
  void towardsPrefersAxisWithLargerDelta() {
    Position from = new Position(0, 0);
    assertEquals(Direction.RIGHT, Direction.towards(from, new Position(5, 1)));
    assertEquals(Direction.UP, Direction.towards(from, new Position(1, 5)));
    assertEquals(Direction.LEFT, Direction.towards(from, new Position(-5, -1)));
    assertEquals(Direction.DOWN, Direction.towards(from, new Position(0, -3)));
  }

  @Test
  void towardsBreaksTieByPreferringHorizontal() {
    Position from = new Position(0, 0);
    // dx == dy のときは X 軸優先
    assertEquals(Direction.RIGHT, Direction.towards(from, new Position(3, 3)));
  }

  @Test
  void towardsSamePositionReturnsRight() {
    // dxAbs == dyAbs == 0 のとき、ガード条件で RIGHT が返る (現状契約)
    Position p = new Position(3, 3);
    assertEquals(Direction.RIGHT, Direction.towards(p, p));
  }
}
