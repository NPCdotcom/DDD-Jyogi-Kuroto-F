package core.presentation.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Wave 14 W14-β: ButtonBounds の contains 境界値テスト (純関数のみ、LibGDX 依存なし)。 */
class ButtonBoundsTest {

  private static final ButtonBounds BUTTON = new ButtonBounds(100, 200, 50, 30);

  @Test
  void containsCornerInclusive() {
    // 左下隅・右上隅は内部扱い (境界を含む)
    assertTrue(BUTTON.contains(100f, 200f), "左下角");
    assertTrue(BUTTON.contains(150f, 230f), "右上角");
  }

  @Test
  void containsInsidePoint() {
    assertTrue(BUTTON.contains(125f, 215f), "中央付近");
  }

  @Test
  void doesNotContainPointJustOutside() {
    // 1 px ずれは外部
    assertFalse(BUTTON.contains(99f, 215f), "左端 -1 px");
    assertFalse(BUTTON.contains(151f, 215f), "右端 +1 px");
    assertFalse(BUTTON.contains(125f, 199f), "下端 -1 px");
    assertFalse(BUTTON.contains(125f, 231f), "上端 +1 px");
  }

  @Test
  void doesNotContainFarPoint() {
    assertFalse(BUTTON.contains(0f, 0f), "原点");
    assertFalse(BUTTON.contains(1000f, 1000f), "遠方");
  }
}
