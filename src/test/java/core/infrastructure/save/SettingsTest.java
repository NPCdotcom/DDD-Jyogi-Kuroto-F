package core.infrastructure.save;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link Settings} の値オブジェクト仕様を検証する (§15-1)。 */
class SettingsTest {

  // --- デフォルト値 ---

  @Test
  void default_bgmVolume() {
    assertEquals(0.7f, Settings.DEFAULT.bgmVolume(), 0.001f);
  }

  @Test
  void default_seVolume() {
    assertEquals(0.8f, Settings.DEFAULT.seVolume(), 0.001f);
  }

  @Test
  void default_fullscreen_isFalse() {
    assertFalse(Settings.DEFAULT.fullscreen());
  }

  @Test
  void default_uiPreset_isStandard() {
    assertEquals(UiPreset.STANDARD, Settings.DEFAULT.uiPreset());
  }

  // --- バリデーション ---

  @ParameterizedTest
  @ValueSource(floats = {-0.1f, 1.1f, Float.NaN})
  void constructor_throwsOnInvalidBgmVolume(float vol) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Settings(vol, 0.5f, false, UiPreset.STANDARD, ThemeMode.DARK));
  }

  @ParameterizedTest
  @ValueSource(floats = {-0.1f, 1.1f, Float.NaN})
  void constructor_throwsOnInvalidSeVolume(float vol) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Settings(0.5f, vol, false, UiPreset.STANDARD, ThemeMode.DARK));
  }

  @Test
  void constructor_acceptsBoundaryVolumes() {
    assertDoesNotThrow(() -> new Settings(0.0f, 1.0f, false, UiPreset.STANDARD, ThemeMode.DARK));
    assertDoesNotThrow(() -> new Settings(1.0f, 0.0f, false, UiPreset.STANDARD, ThemeMode.DARK));
  }

  // --- with* メソッド (immutable コピー) ---

  @Test
  void withBgmVolume_returnsNewInstance() {
    Settings original = Settings.DEFAULT;
    Settings updated = original.withBgmVolume(0.3f);
    assertNotSame(original, updated);
    assertEquals(0.3f, updated.bgmVolume(), 0.001f);
    // 他のフィールドは変わらない
    assertEquals(original.seVolume(), updated.seVolume(), 0.001f);
    assertEquals(original.fullscreen(), updated.fullscreen());
    assertEquals(original.uiPreset(), updated.uiPreset());
  }

  @Test
  void withSeVolume_returnsNewInstance() {
    Settings updated = Settings.DEFAULT.withSeVolume(0.4f);
    assertEquals(0.4f, updated.seVolume(), 0.001f);
  }

  @Test
  void withFullscreen_returnsNewInstance() {
    Settings updated = Settings.DEFAULT.withFullscreen(true);
    assertTrue(updated.fullscreen());
  }

  @Test
  void withUiPreset_returnsNewInstance() {
    Settings updated = Settings.DEFAULT.withUiPreset(UiPreset.MINIMAL);
    assertEquals(UiPreset.MINIMAL, updated.uiPreset());
  }

  @Test
  void withBgmVolume_clampsToMin() {
    Settings updated = Settings.DEFAULT.withBgmVolume(-0.5f);
    assertEquals(0.0f, updated.bgmVolume(), 0.001f);
  }

  @Test
  void withBgmVolume_clampsToMax() {
    Settings updated = Settings.DEFAULT.withBgmVolume(1.5f);
    assertEquals(1.0f, updated.bgmVolume(), 0.001f);
  }

  @Test
  void withUiPreset_throwsOnNull() {
    assertThrows(NullPointerException.class, () -> Settings.DEFAULT.withUiPreset(null));
  }

  // --- UiPreset プリセット適用ロジック ---

  @Test
  void renderLayout_showExtendedHud_minimal_isFalse() {
    assertFalse(core.presentation.render.RenderLayout.showExtendedHud(UiPreset.MINIMAL));
  }

  @Test
  void renderLayout_showExtendedHud_standard_isTrue() {
    assertTrue(core.presentation.render.RenderLayout.showExtendedHud(UiPreset.STANDARD));
  }

  @Test
  void renderLayout_showExtendedHud_infoRich_isTrue() {
    assertTrue(core.presentation.render.RenderLayout.showExtendedHud(UiPreset.INFO_RICH));
  }

  @Test
  void renderLayout_showPersistentHint_minimal_isFalse() {
    assertFalse(core.presentation.render.RenderLayout.showPersistentHint(UiPreset.MINIMAL));
  }

  @Test
  void renderLayout_showPersistentHint_standard_isFalse() {
    assertFalse(core.presentation.render.RenderLayout.showPersistentHint(UiPreset.STANDARD));
  }

  @Test
  void renderLayout_showPersistentHint_infoRich_isTrue() {
    assertTrue(core.presentation.render.RenderLayout.showPersistentHint(UiPreset.INFO_RICH));
  }

  // --- UiPreset 表示名の非null保証 ---

  @Test
  void uiPreset_displayNames_nonNull() {
    for (UiPreset preset : UiPreset.values()) {
      assertNotNull(preset.displayNameJa(), "Ja name null for " + preset);
      assertNotNull(preset.displayNameEn(), "En name null for " + preset);
      assertNotNull(preset.descriptionJa(), "Ja desc null for " + preset);
      assertNotNull(preset.descriptionEn(), "En desc null for " + preset);
    }
  }
}
