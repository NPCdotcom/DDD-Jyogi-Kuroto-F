package core.infrastructure.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 旧セーブ移行の再入可能性を検証する (SAVE-02B)。
 *
 * <p>Profile / RunCheckpoint / 完了マーカーの各書込地点で停止しても、再実行で不足側だけが 揃うこと、返金が累積しないこと、完了まで旧ファイルを消さないことを固定する。
 */
class LegacySaveMigratorTest {

  @TempDir Path tempDir;

  private SaveManager saveManager;
  private LegacySaveMigrator migrator;

  @BeforeEach
  void setUp() {
    saveManager = new SaveManager(tempDir.resolve("save.json").toFile());
    migrator = new LegacySaveMigrator(saveManager);
  }

  /** slot_expand を 2 件持つ v3 の旧セーブ (返金 80 ソウル)。 */
  private static SaveData legacyWithTwoSlotExpands() {
    return new SaveData(
        3,
        2,
        18,
        20,
        3,
        4,
        2,
        1,
        1,
        List.of("zangeki"),
        100,
        3,
        List.of("center", "slot_expand_1", "slot_expand_2"),
        List.of("zangeki"),
        Map.of("HAND", "dark_blade"),
        List.of("SLIME"),
        true,
        45,
        7);
  }

  private void writeLegacy() {
    saveManager.save(legacyWithTwoSlotExpands());
    assertTrue(saveManager.exists());
  }

  // ---------------- 通常の移行 ----------------

  @Test
  void freshMigrationProducesBothFilesAndRemovesLegacy() {
    writeLegacy();
    LegacySaveMigrator.MigrationResult result = migrator.migrateIfNeeded();

    assertTrue(result.migrated());
    assertTrue(saveManager.profileExists());
    assertTrue(saveManager.checkpointExists());
    assertFalse(saveManager.exists(), "完了後に旧ファイルを削除する");
    assertTrue(saveManager.loadMigrationState().orElseThrow().completed());
  }

  @Test
  void migrationIsSkippedWhenNoLegacyFileExists() {
    assertFalse(migrator.migrateIfNeeded().migrated());
    assertFalse(saveManager.profileExists());
  }

  @Test
  void refundIsAppliedExactlyOnce() {
    writeLegacy();
    migrator.migrateIfNeeded();
    // 100 + 40 * 2 = 180
    assertEquals(180, saveManager.loadProfile().orElseThrow().soulTotal());
  }

  // ---------------- 障害注入と再開 ----------------

  @Test
  void resumesAfterFailureBetweenProfileAndCheckpoint() {
    writeLegacy();
    // Profile まで書けた状態を再現する。
    String id = LegacySaveMigrator.sha256(saveManager.legacySaveFile()).orElseThrow();
    saveManager.saveProfile(LegacySaveMapper.map(legacyWithTwoSlotExpands()).profile());
    saveManager.saveMigrationState(LegacyMigrationState.started(id));
    assertFalse(saveManager.checkpointExists());

    assertTrue(migrator.migrateIfNeeded().migrated());

    assertTrue(saveManager.checkpointExists(), "不足していた Checkpoint を補う");
    assertEquals(180, saveManager.loadProfile().orElseThrow().soulTotal(), "返金は累積しない");
    assertFalse(saveManager.exists());
  }

  @Test
  void resumesAfterFailureBeforeCompletionMarker() {
    writeLegacy();
    String id = LegacySaveMigrator.sha256(saveManager.legacySaveFile()).orElseThrow();
    LegacySaveMapper.Result mapped = LegacySaveMapper.map(legacyWithTwoSlotExpands());
    saveManager.saveProfile(mapped.profile());
    saveManager.saveCheckpoint(mapped.checkpoint().orElseThrow());
    saveManager.saveMigrationState(LegacyMigrationState.started(id));

    assertTrue(migrator.migrateIfNeeded().migrated());

    assertTrue(saveManager.loadMigrationState().orElseThrow().completed());
    assertEquals(180, saveManager.loadProfile().orElseThrow().soulTotal(), "返金は累積しない");
    assertFalse(saveManager.exists());
  }

  @Test
  void legacyFileSurvivesWhenMigrationAborts() {
    // 完了マーカーを書けない状況を作り、abort 後も旧ファイルが残ることを確かめる。
    // 以前の版はこの試験が migrateIfNeeded() を呼んでおらず、delete() を無条件化しても緑のままだった。
    writeLegacy();
    assertTrue(new File(tempDir.toFile(), "migration-state.json").mkdirs());

    assertFalse(migrator.migrateIfNeeded().migrated());
    assertTrue(saveManager.exists(), "移行が完了しなければ旧ファイルを消さない");
  }

  // ---------------- 既存 Profile の保護 ----------------

  @Test
  void migrationDoesNotClobberExistingProfile() {
    // 旧 save.json はクラウド同期の復元や旧ビルドの起動で再出現しうる。
    // そのとき既存の恒久進捗を旧セーブで上書きすると全損する。
    migrator.migrateIfNeeded(); // まず通常移行はしない (旧ファイル無し)
    ProfileData existing = ProfileData.initial().withActiveRunId("run-current");
    saveManager.saveProfile(
        new ProfileData(
            ProfileData.CURRENT_SCHEMA_VERSION,
            5000,
            42,
            List.of("center"),
            existing.ownedEquipmentIds(),
            Map.of(),
            List.of(),
            0,
            List.of(),
            List.of(),
            true,
            "run-current",
            null));

    writeLegacy();
    assertFalse(migrator.migrateIfNeeded().migrated(), "既存 Profile があるなら移行しない");

    ProfileData after = saveManager.loadProfile().orElseThrow();
    assertEquals(5000, after.soulTotal(), "既存の恒久進捗を壊さない");
    assertEquals(42, after.runCount());
    assertTrue(saveManager.exists(), "判断材料として旧ファイルも残す");
  }

  // ---------------- 冪等性 ----------------

  @Test
  void completedMigrationIsNotReprocessed() {
    writeLegacy();
    migrator.migrateIfNeeded();
    int soulAfterFirst = saveManager.loadProfile().orElseThrow().soulTotal();

    // 旧ファイルを書き戻しても再処理しない。既存 Profile 保護と完了 journal の
    // 両方が効くため、どちらの経路でも進捗は変わらない。
    writeLegacy();
    assertFalse(migrator.migrateIfNeeded().migrated());
    assertEquals(soulAfterFirst, saveManager.loadProfile().orElseThrow().soulTotal());
  }

  @Test
  void repeatedMigrationCallsDoNotAccumulateRefund() {
    writeLegacy();
    migrator.migrateIfNeeded();
    // 2 回目以降は旧ファイルが消えているため即 return する。返金が二重に乗らないことの確認。
    migrator.migrateIfNeeded();
    migrator.migrateIfNeeded();
    assertEquals(180, saveManager.loadProfile().orElseThrow().soulTotal());
  }

  // ---------------- 移行 ID ----------------

  @Test
  void migrationIdIsSha256OfLegacyFile() {
    writeLegacy();
    String id = LegacySaveMigrator.sha256(saveManager.legacySaveFile()).orElseThrow();
    assertEquals(64, id.length(), "SHA-256 は 16 進 64 文字");
    assertTrue(id.matches("[0-9a-f]{64}"));

    migrator.migrateIfNeeded();
    assertEquals(id, saveManager.loadMigrationState().orElseThrow().migrationId());
  }

  @Test
  void differentLegacyContentDoesNotOverwriteMigratedProfile() {
    writeLegacy();
    migrator.migrateIfNeeded();
    int soulAfterFirst = saveManager.loadProfile().orElseThrow().soulTotal();

    // 内容の異なる旧ファイルが現れても、既に Profile がある以上は上書きしない。
    // 以前の版はここで soulTotal が 7 へ置き換わることを正解として固定しており、
    // 恒久進捗の全損を仕様にしてしまっていた。
    SaveData other =
        new SaveData(
            3,
            5,
            10,
            10,
            2,
            2,
            2,
            1,
            1,
            List.of("zangeki"),
            7,
            1,
            List.of("center"),
            List.of(),
            Map.of(),
            List.of(),
            false,
            0,
            0);
    saveManager.save(other);

    assertFalse(migrator.migrateIfNeeded().migrated(), "既存 Profile があるなら移行しない");
    assertEquals(
        soulAfterFirst,
        saveManager.loadProfile().orElseThrow().soulTotal(),
        "移行済みの恒久進捗を旧セーブで上書きしない");
    assertTrue(saveManager.exists(), "判断材料として旧ファイルを残す");
  }
}
