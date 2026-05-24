package core.infrastructure.save;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link SettingsManager} の save → exists → load、破損時フォールバックを検証する (§15-1)。 */
class SettingsManagerTest {

  @TempDir File tempDir;

  private SettingsManager manager;
  private File settingsFile;

  @BeforeEach
  void setUp() {
    settingsFile = new File(tempDir, "test_settings.json");
    manager = new SettingsManager(settingsFile);
  }

  @Test
  void existsReturnsFalse_whenNoFile() {
    assertFalse(manager.exists());
  }

  @Test
  void load_returnsDefault_whenNoFile() {
    Settings result = manager.load();
    assertEquals(Settings.DEFAULT, result);
  }

  @Test
  void save_createsFile() {
    manager.save(Settings.DEFAULT);
    assertTrue(settingsFile.exists());
    assertTrue(manager.exists());
  }

  @Test
  void saveAndLoad_roundTrip_default() {
    manager.save(Settings.DEFAULT);
    Settings loaded = manager.load();

    assertEquals(Settings.DEFAULT.bgmVolume(), loaded.bgmVolume(), 0.001f);
    assertEquals(Settings.DEFAULT.seVolume(), loaded.seVolume(), 0.001f);
    assertEquals(Settings.DEFAULT.fullscreen(), loaded.fullscreen());
    assertEquals(Settings.DEFAULT.uiPreset(), loaded.uiPreset());
  }

  @Test
  void saveAndLoad_roundTrip_customValues() {
    Settings custom = new Settings(0.3f, 0.5f, true, UiPreset.INFO_RICH, ThemeMode.DARK);
    manager.save(custom);
    Settings loaded = manager.load();

    assertEquals(0.3f, loaded.bgmVolume(), 0.001f);
    assertEquals(0.5f, loaded.seVolume(), 0.001f);
    assertTrue(loaded.fullscreen());
    assertEquals(UiPreset.INFO_RICH, loaded.uiPreset());
  }

  @Test
  void saveAndLoad_roundTrip_allPresets() {
    for (UiPreset preset : UiPreset.values()) {
      Settings s = Settings.DEFAULT.withUiPreset(preset);
      manager.save(s);
      Settings loaded = manager.load();
      assertEquals(preset, loaded.uiPreset(), "preset round-trip failed for " + preset);
    }
  }

  @Test
  void load_returnsDefault_forCorruptedJson() throws IOException {
    Files.writeString(settingsFile.toPath(), "{ this is not valid json }}}");
    Settings result = manager.load();
    assertEquals(Settings.DEFAULT, result);
  }

  @Test
  void load_returnsDefault_forEmptyFile() throws IOException {
    Files.writeString(settingsFile.toPath(), "");
    Settings result = manager.load();
    assertEquals(Settings.DEFAULT, result);
  }

  @Test
  void load_returnsDefault_forPartialJson() throws IOException {
    Files.writeString(settingsFile.toPath(), "{\"bgmVolume\": 0.5, \"se");
    Settings result = manager.load();
    assertEquals(Settings.DEFAULT, result);
  }

  @Test
  void save_overwritesPreviousSettings() {
    manager.save(Settings.DEFAULT);
    Settings updated = Settings.DEFAULT.withBgmVolume(0.2f).withUiPreset(UiPreset.MINIMAL);
    manager.save(updated);

    Settings loaded = manager.load();
    assertEquals(0.2f, loaded.bgmVolume(), 0.001f);
    assertEquals(UiPreset.MINIMAL, loaded.uiPreset());
  }
}
