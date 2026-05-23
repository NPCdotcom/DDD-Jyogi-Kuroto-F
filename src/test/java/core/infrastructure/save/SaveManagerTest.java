package core.infrastructure.save;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link SaveManager} の save → exists → load → delete の一連と、破損データ時の graceful 動作を検証する (§15-11)。 */
class SaveManagerTest {

  @TempDir File tempDir;

  private SaveManager saveManager;
  private File saveFile;

  @BeforeEach
  void setUp() {
    saveFile = new File(tempDir, "test_save.json");
    saveManager = new SaveManager(saveFile);
  }

  private SaveData sampleData() {
    return new SaveData(
        SaveData.CURRENT_SCHEMA_VERSION,
        1,
        30,
        30,
        3,
        1,
        2,
        1,
        1,
        List.of("zangeki"),
        5,
        0,
        List.of("root"),
        List.of("zangeki"),
        Map.of("HAND", "tattered_dagger"));
  }

  @Test
  void existsReturnsFalse_whenNoFile() {
    assertFalse(saveManager.exists());
  }

  @Test
  void save_createsFile() {
    saveManager.save(sampleData());
    assertTrue(saveFile.exists());
    assertTrue(saveManager.exists());
  }

  @Test
  void load_returnsEmpty_whenNoFile() {
    Optional<SaveData> result = saveManager.load();
    assertTrue(result.isEmpty());
  }

  @Test
  void saveAndLoad_roundTrip() {
    SaveData original = sampleData();
    saveManager.save(original);
    Optional<SaveData> loaded = saveManager.load();

    assertTrue(loaded.isPresent());
    SaveData data = loaded.get();
    assertEquals(original.schemaVersion(), data.schemaVersion());
    assertEquals(original.nextLayerNumber(), data.nextLayerNumber());
    assertEquals(original.currentHp(), data.currentHp());
    assertEquals(original.maxHp(), data.maxHp());
    assertEquals(original.deck(), data.deck());
    assertEquals(original.soulTotal(), data.soulTotal());
    assertEquals(original.runCount(), data.runCount());
    assertEquals(original.unlockedNodeIds(), data.unlockedNodeIds());
    assertEquals(original.obtainedCardIds(), data.obtainedCardIds());
    assertEquals(original.loadout(), data.loadout());
  }

  @Test
  void delete_removesFile() {
    saveManager.save(sampleData());
    assertTrue(saveManager.exists());
    saveManager.delete();
    assertFalse(saveManager.exists());
    assertFalse(saveFile.exists());
  }

  @Test
  void delete_graceful_whenNoFile() {
    // 存在しない状態で delete しても例外を投げない
    assertDoesNotThrow(() -> saveManager.delete());
  }

  @Test
  void load_returnsEmpty_forCorruptedJson() throws IOException {
    // 破損 JSON を書き込む
    Files.writeString(saveFile.toPath(), "{ this is not valid json }}}");
    Optional<SaveData> result = saveManager.load();
    // 破損時は graceful に empty を返す
    assertTrue(result.isEmpty());
  }

  @Test
  void load_returnsEmpty_forEmptyFile() throws IOException {
    // 空ファイル
    Files.writeString(saveFile.toPath(), "");
    Optional<SaveData> result = saveManager.load();
    assertTrue(result.isEmpty());
  }

  @Test
  void load_returnsEmpty_forPartialJson() throws IOException {
    // 不完全な JSON (途中で切れている)
    Files.writeString(saveFile.toPath(), "{\"schemaVersion\": 1, \"nextLayerNum");
    Optional<SaveData> result = saveManager.load();
    assertTrue(result.isEmpty());
  }

  @Test
  void save_overwritesPreviousSave() {
    SaveData first = sampleData();
    saveManager.save(first);

    // 2 層目に更新したデータで上書き
    SaveData second =
        new SaveData(
            SaveData.CURRENT_SCHEMA_VERSION,
            2,
            20,
            30,
            3,
            1,
            2,
            1,
            1,
            List.of("zangeki", "strong_strike"),
            10,
            0,
            List.of("root", "hp_up_1"),
            List.of("zangeki", "strong_strike"),
            Map.of("HAND", "tattered_dagger"));
    saveManager.save(second);

    Optional<SaveData> loaded = saveManager.load();
    assertTrue(loaded.isPresent());
    assertEquals(2, loaded.get().nextLayerNumber());
    assertEquals(20, loaded.get().currentHp());
  }
}
