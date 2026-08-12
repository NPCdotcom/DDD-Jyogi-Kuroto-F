package core.infrastructure.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link ProfileData} と {@link RunCheckpoint} の分離保存を検証する (SAVE-01)。
 *
 * <p>レビュー P0-1 の再発防止。恒久進捗とラン途中のチェックポイントが単一 {@code save.json} に
 * 混在していたため、ラン終了後も古い層から再開でき、逆にラン終了時の進捗が保存されなかった。
 */
class ProfileCheckpointRoundTripTest {

  @TempDir Path tempDir;

  private SaveManager saveManager;

  @BeforeEach
  void setUp() {
    saveManager = new SaveManager(tempDir.resolve("save.json").toFile());
  }

  private static ProfileData sampleProfile() {
    return new ProfileData(
        ProfileData.CURRENT_SCHEMA_VERSION,
        120, // soulTotal
        3, // runCount
        List.of("center", "hp_1"),
        List.of("tattered_dagger", "dash_boots"),
        Map.of("HAND", "tattered_dagger"),
        List.of("dash_boots"),
        1, // retentionCapacity
        List.of("SLIME"),
        List.of("zangeki"),
        true,
        "run-0001",
        "run-0000");
  }

  private static RunCheckpoint sampleCheckpoint() {
    return new RunCheckpoint(
        RunCheckpoint.CURRENT_SCHEMA_VERSION,
        "run-0001",
        2, // nextLayerNumber
        18,
        20,
        3,
        4,
        2,
        1,
        1,
        List.of("zangeki", "strong_strike"),
        45, // gold
        7, // run soul
        List.of("tattered_dagger", "dash_boots"),
        List.of("dark_blade"),
        "dash_boots",
        List.of("dash_boots"),
        1);
  }

  // ---------------- 往復 ----------------

  @Test
  void profileRoundTripsThroughJson() {
    ProfileData original = sampleProfile();
    assertTrue(saveManager.saveProfile(original).isSuccess());
    assertEquals(Optional.of(original), saveManager.loadProfile());
  }

  @Test
  void checkpointRoundTripsThroughJson() {
    RunCheckpoint original = sampleCheckpoint();
    assertTrue(saveManager.saveCheckpoint(original).isSuccess());
    assertEquals(Optional.of(original), saveManager.loadCheckpoint());
  }

  // ---------------- 分離 ----------------

  @Test
  void profileAndCheckpointUseSeparateFiles() {
    saveManager.saveProfile(sampleProfile());
    saveManager.saveCheckpoint(sampleCheckpoint());
    assertTrue(tempDir.resolve("profile.json").toFile().exists());
    assertTrue(tempDir.resolve("run-checkpoint.json").toFile().exists());
  }

  @Test
  void deletingCheckpointKeepsProfile() {
    // 死亡・クリア時は Checkpoint だけを消し、恒久進捗は残す (P0-1 の核心)。
    saveManager.saveProfile(sampleProfile());
    saveManager.saveCheckpoint(sampleCheckpoint());
    saveManager.deleteCheckpoint();
    assertFalse(saveManager.checkpointExists());
    assertTrue(saveManager.profileExists());
    assertEquals(Optional.of(sampleProfile()), saveManager.loadProfile());
  }

  @Test
  void existsReportsAbsenceBeforeAnySave() {
    assertFalse(saveManager.profileExists());
    assertFalse(saveManager.checkpointExists());
  }

  // ---------------- 保護枠の検証 ----------------

  @Test
  void profileRejectsRetentionCapacityAboveTwo() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ProfileData(
                ProfileData.CURRENT_SCHEMA_VERSION,
                0,
                0,
                List.of(),
                List.of(),
                Map.of(),
                List.of(),
                3,
                List.of(),
                List.of(),
                false,
                null,
                null));
  }

  @Test
  void profileRejectsMoreProtectedIdsThanCapacity() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ProfileData(
                ProfileData.CURRENT_SCHEMA_VERSION,
                0,
                0,
                List.of(),
                List.of("a", "b"),
                Map.of(),
                List.of("a", "b"),
                1,
                List.of(),
                List.of(),
                false,
                null,
                null));
  }

  @Test
  void invalidProfileOnDiskIsTreatedAsAbsent() throws IOException {
    // 保護枠違反のデータを読み込まない (手編集や移行途中の壊れたファイル)。
    Files.writeString(
        tempDir.resolve("profile.json"),
        """
        {"schemaVersion":1,"soulTotal":0,"runCount":0,"unlockedNodeIds":[],\
        "ownedEquipmentIds":[],"loadout":{},"protectedEquipmentIds":["a","b","c"],\
        "retentionCapacity":1,"defeatedEnemyKinds":[],"obtainedCardIds":[],\
        "tutorialSeen":false,"activeRunId":null,"lastSettledRunId":null}
        """,
        StandardCharsets.UTF_8);
    assertTrue(saveManager.loadProfile().isEmpty());
  }

  // ---------------- 未来スキーマの拒否 ----------------

  @Test
  void futureProfileSchemaIsRejectedAndFileIsPreserved() throws IOException {
    Path profile = tempDir.resolve("profile.json");
    String future =
        """
        {"schemaVersion":999,"soulTotal":5,"runCount":1,"unlockedNodeIds":[],\
        "ownedEquipmentIds":[],"loadout":{},"protectedEquipmentIds":[],\
        "retentionCapacity":0,"defeatedEnemyKinds":[],"obtainedCardIds":[],\
        "tutorialSeen":false,"activeRunId":null,"lastSettledRunId":null}
        """;
    Files.writeString(profile, future, StandardCharsets.UTF_8);

    assertTrue(saveManager.loadProfile().isEmpty(), "未来 schema は読み込まない");
    assertEquals(future, Files.readString(profile, StandardCharsets.UTF_8), "既存ファイルを壊さない");
  }

  // ---------------- 失敗の通知 ----------------

  @Test
  void saveReportsFailureWhenDestinationIsNotWritable() {
    // 保存先をディレクトリで塞ぎ、書込不能な状況を作る。
    File blocked = tempDir.resolve("blocked").toFile();
    assertTrue(blocked.mkdirs());
    File asFile = new File(blocked, "profile.json");
    assertTrue(asFile.mkdirs()); // profile.json という名前のディレクトリ

    SaveManager blockedManager = new SaveManager(new File(blocked, "save.json"));
    SaveManager.SaveResult result = blockedManager.saveProfile(sampleProfile());

    assertFalse(result.isSuccess(), "I/O 失敗を成功として返さない");
  }

  @Test
  void overwritingProfileReplacesContentEntirely() {
    // 原子的置換で旧世代が混ざらないことを、異なる内容で確認する。
    saveManager.saveProfile(sampleProfile());
    ProfileData updated = sampleProfile().withSettledRunId("run-0001");
    assertTrue(saveManager.saveProfile(updated).isSuccess());

    Optional<ProfileData> loaded = saveManager.loadProfile();
    assertEquals(Optional.of(updated), loaded);
    assertEquals("run-0001", loaded.orElseThrow().lastSettledRunId());
    assertEquals(null, loaded.orElseThrow().activeRunId(), "精算済みなら進行中ランは解除される");
  }

  @Test
  void failedSaveLeavesNoTempResidue() {
    // 一時ファイルを置いたまま失敗すると、次回起動時に中途半端な JSON が残る。
    File blocked = tempDir.resolve("blocked2").toFile();
    assertTrue(blocked.mkdirs());
    assertTrue(new File(blocked, "profile.json").mkdirs());

    SaveManager blockedManager = new SaveManager(new File(blocked, "save.json"));
    assertFalse(blockedManager.saveProfile(sampleProfile()).isSuccess());

    File[] residue = blocked.listFiles((dir, name) -> name.endsWith(".tmp"));
    assertEquals(0, residue == null ? 0 : residue.length, "失敗後に .tmp を残さない");
  }

  @Test
  void failedCheckpointSaveKeepsExistingProfileIntact() {
    // Checkpoint の保存失敗が恒久進捗を巻き添えにしない。
    saveManager.saveProfile(sampleProfile());
    assertTrue(new File(tempDir.toFile(), "run-checkpoint.json").mkdirs());

    assertFalse(saveManager.saveCheckpoint(sampleCheckpoint()).isSuccess());
    assertTrue(saveManager.profileExists());
    assertEquals(Optional.of(sampleProfile()), saveManager.loadProfile());
  }

  @Test
  void deleteOnAbsentFilesIsSilent() {
    saveManager.deleteCheckpoint();
    saveManager.deleteProfile();
    assertFalse(saveManager.profileExists());
    assertFalse(saveManager.checkpointExists());
  }
}
