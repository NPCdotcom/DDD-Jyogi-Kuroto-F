package core.presentation.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.common.Direction;
import core.domain.common.Position;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Wave 14 W14-α: マウスクリック位置からプレイヤー方向への変換テスト (純関数 directionToward)。 */
class RenderLayoutDirectionTowardTest {

  private static final Position ORIGIN = new Position(5, 5);

  // CTO チェックポイント #2: 自分マスクリックでは Optional.empty()、誤って RIGHT 発火しない (自爆クリック回避)

  @Test
  void samePositionReturnsEmpty() {
    assertEquals(Optional.empty(), RenderLayout.directionToward(ORIGIN, ORIGIN));
  }

  // 真上 / 真下 / 真右 / 真左 (基本方向)

  @Test
  void rightClickReturnsRight() {
    assertEquals(
        Optional.of(Direction.RIGHT), RenderLayout.directionToward(ORIGIN, new Position(7, 5)));
  }

  @Test
  void leftClickReturnsLeft() {
    assertEquals(
        Optional.of(Direction.LEFT), RenderLayout.directionToward(ORIGIN, new Position(3, 5)));
  }

  @Test
  void upClickReturnsUp() {
    assertEquals(
        Optional.of(Direction.UP), RenderLayout.directionToward(ORIGIN, new Position(5, 8)));
  }

  @Test
  void downClickReturnsDown() {
    assertEquals(
        Optional.of(Direction.DOWN), RenderLayout.directionToward(ORIGIN, new Position(5, 2)));
  }

  // 斜め: |dx| > |dy| なら横優先、|dy| > |dx| なら縦優先

  @Test
  void diagonalDxGreaterReturnsHorizontal() {
    // dx=3, dy=1 → 横優先 (RIGHT)
    assertEquals(
        Optional.of(Direction.RIGHT), RenderLayout.directionToward(ORIGIN, new Position(8, 6)));
  }

  @Test
  void diagonalDyGreaterReturnsVertical() {
    // dx=1, dy=3 → 縦優先 (UP)
    assertEquals(
        Optional.of(Direction.UP), RenderLayout.directionToward(ORIGIN, new Position(6, 8)));
  }

  @Test
  void equalDxDyPicksHorizontalForDeterminism() {
    // dx=2, dy=2 → 完全な斜め、横優先で安定 (テスト再現性)
    assertEquals(
        Optional.of(Direction.RIGHT), RenderLayout.directionToward(ORIGIN, new Position(7, 7)));
  }

  @Test
  void negativeEqualDxDyPicksHorizontal() {
    // dx=-2, dy=-2 → 横優先 (LEFT)
    assertEquals(
        Optional.of(Direction.LEFT), RenderLayout.directionToward(ORIGIN, new Position(3, 3)));
  }

  // CTO #2 回帰防止: 自分マス + 比較式の素通り防止

  @Test
  void samePositionMustNotFallThroughComparison() {
    // dx=0, dy=0 で |dx| >= |dy| (0 >= 0) を素通りすると RIGHT が誤発火する。
    // メソッド冒頭の short-circuit ガードで empty を返すことを保証。
    Optional<Direction> result =
        RenderLayout.directionToward(new Position(0, 0), new Position(0, 0));
    assertTrue(result.isEmpty(), "自分マスクリックは empty (RIGHT 自爆発火しない)");
  }
}
