package core.presentation.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link ScreenEffects} の純粋ロジック部分の単体テスト (Wave 4 W4-β)。
 *
 * <p>Texture / SpriteBatch / ShapeRenderer を使う描画系は LibGDX headless 環境が必要なため省略し、 状態管理 (flash /
 * shake) のロジックのみ検証する。
 */
class ScreenEffectsTest {

  // -------------------------------------------------------------------------
  // showFlash / isShowingFlash / flashAlpha
  // -------------------------------------------------------------------------

  @Test
  void freshEffectsHasNoFlash() {
    ScreenEffects e = new ScreenEffects();
    assertFalse(e.isShowingFlash());
    assertNull(e.currentFlashMessage());
    assertEquals(0f, e.flashAlpha());
  }

  @Test
  void showFlashSetsMessageAndTimer() {
    ScreenEffects e = new ScreenEffects();
    e.showFlash("hello");
    assertTrue(e.isShowingFlash());
    assertEquals("hello", e.currentFlashMessage());
    // flashTimer = 2.5, alpha = min(1, 2.5/0.5) = 1.0
    assertEquals(1f, e.flashAlpha(), 0.0001f);
  }

  @Test
  void showFlashOverwritesPreviousMessage() {
    ScreenEffects e = new ScreenEffects();
    e.showFlash("first");
    e.showFlash("second");
    assertEquals("second", e.currentFlashMessage());
  }

  @Test
  void advanceEffectsDecrementsFlashTimerAndClearsMessageWhenExpired() {
    ScreenEffects e = new ScreenEffects();
    e.showFlash("msg");
    // 2.5 秒より少し多めに進める → flashTimer = 0 → flashMessage = null
    e.advanceEffects(2.6f);
    assertFalse(e.isShowingFlash());
    assertNull(e.currentFlashMessage());
  }

  @Test
  void flashAlphaFadesInLastHalfSecond() {
    ScreenEffects e = new ScreenEffects();
    e.showFlash("fade");
    // flashTimer = 2.5 → 0.25 秒残し (0.25 / 0.5 = 0.5)
    e.advanceEffects(2.25f);
    assertEquals(0.5f, e.flashAlpha(), 0.01f);
  }

  @Test
  void advanceEffectsDoesNotGoNegativeForFlashTimer() {
    ScreenEffects e = new ScreenEffects();
    e.showFlash("msg");
    e.advanceEffects(100f);
    assertFalse(e.isShowingFlash());
    assertNull(e.currentFlashMessage());
  }

  // -------------------------------------------------------------------------
  // isShaking (shake の状態は spawnPopup が libgdx 依存のため直接操作不可)
  // advanceEffects のシェイクデクリメントのみ検証する
  // -------------------------------------------------------------------------

  @Test
  void freshEffectsIsNotShaking() {
    ScreenEffects e = new ScreenEffects();
    assertFalse(e.isShaking());
  }

  // -------------------------------------------------------------------------
  // advanceEffects: lowHpAnimTime 累積
  // lowHpAnimTime は private なので isShaking / isShowingFlash を通した間接確認のみ
  // -------------------------------------------------------------------------

  @Test
  void advanceEffectsMultipleCallsAreAdditive() {
    ScreenEffects e = new ScreenEffects();
    e.showFlash("x");
    e.advanceEffects(1.0f);
    e.advanceEffects(1.0f);
    // flashTimer = 2.5 - 2.0 = 0.5 → まだ表示中
    assertTrue(e.isShowingFlash());
    e.advanceEffects(0.6f);
    // flashTimer = 0.5 - 0.6 <= 0 → 消える
    assertFalse(e.isShowingFlash());
  }

  // -------------------------------------------------------------------------
  // MAX_POPUPS 定数の存在確認 (ScreenEffects が定数を公開していること)
  // -------------------------------------------------------------------------

  @Test
  void maxPopupsConstantHasExpectedValue() {
    assertEquals(16, ScreenEffects.MAX_POPUPS);
  }

  @Test
  void shakeDurationConstantHasExpectedValue() {
    assertEquals(0.18f, ScreenEffects.SHAKE_DURATION, 0.0001f);
  }
}
