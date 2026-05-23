package core.presentation.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.infrastructure.save.UiPreset;
import org.junit.jupiter.api.Test;

/**
 * {@link RenderLayout#showExtendedHud} / {@link RenderLayout#showPersistentHint} の単体テスト (§15-1 /
 * §15-8)。
 *
 * <p>両メソッドは {@link UiPreset} を受け取って boolean を返す純関数であり、LibGDX に依存しない。
 * プリセット別の表示制御仕様を明文化し、将来の仕様変更時に検出する。
 */
class RenderLayoutUiPresetTest {

  // =========================================================================
  // showExtendedHud: MINIMAL = 非表示、STANDARD / INFO_RICH = 表示
  // =========================================================================

  @Test
  void showExtendedHudReturnsFalseForMinimal() {
    assertFalse(
        RenderLayout.showExtendedHud(UiPreset.MINIMAL), "MINIMAL では Soul/Phase/Gold を非表示 (§15-8)");
  }

  @Test
  void showExtendedHudReturnsTrueForStandard() {
    assertTrue(
        RenderLayout.showExtendedHud(UiPreset.STANDARD), "STANDARD では Soul/Phase/Gold を表示 (§15-8)");
  }

  @Test
  void showExtendedHudReturnsTrueForInfoRich() {
    assertTrue(
        RenderLayout.showExtendedHud(UiPreset.INFO_RICH),
        "INFO_RICH では Soul/Phase/Gold を表示 (§15-8)");
  }

  // =========================================================================
  // showPersistentHint: INFO_RICH のみ常時表示、他は動的表示に委ねる
  // =========================================================================

  @Test
  void showPersistentHintReturnsFalseForMinimal() {
    assertFalse(
        RenderLayout.showPersistentHint(UiPreset.MINIMAL), "MINIMAL では操作ヒントを常時表示しない (§15-8)");
  }

  @Test
  void showPersistentHintReturnsFalseForStandard() {
    assertFalse(
        RenderLayout.showPersistentHint(UiPreset.STANDARD), "STANDARD では操作ヒントを常時表示しない (§15-8)");
  }

  @Test
  void showPersistentHintReturnsTrueOnlyForInfoRich() {
    assertTrue(
        RenderLayout.showPersistentHint(UiPreset.INFO_RICH), "INFO_RICH のみ操作ヒントを常時表示する (§15-8)");
  }
}
