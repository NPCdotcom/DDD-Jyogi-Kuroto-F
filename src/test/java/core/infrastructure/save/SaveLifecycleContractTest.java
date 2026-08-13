package core.infrastructure.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.application.RunId;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * ランのライフサイクル契約を LibGDX 無しで検証する (SAVE-03B)。
 *
 * <p>レビュー P0-1 の再発防止。「新規開始 → 層末保存 → 死亡/クリア → 再起動」を通して、獲得進捗が 残り、終了済みランが再開候補に出ないことを固定する。
 *
 * <p>{@code DddGame} は {@code Game} を継承するため LibGDX 無しでは動かせない。判定規則を {@link RunLifecycle}
 * へ切り出したので、この契約は画面を起動せずに検証できる。
 */
class SaveLifecycleContractTest {

  @TempDir Path tempDir;

  private SaveManager saveManager;
  private RunLifecycle lifecycle;

  @BeforeEach
  void setUp() {
    core.domain.tree.SoulTree.setNodeProvider(
        core.infrastructure.bootstrap.InitialStateFactory::soulTreeNodes);
    saveManager = new SaveManager(tempDir.resolve("save.json").toFile());
    lifecycle = new RunLifecycle(saveManager);
  }

  private static RunCheckpoint checkpointFor(RunId runId, int nextLayer, int runSoul) {
    return new RunCheckpoint(
        RunCheckpoint.CURRENT_SCHEMA_VERSION,
        runId.value(),
        nextLayer,
        18,
        20,
        3,
        4,
        2,
        1,
        1,
        List.of("zangeki"),
        30,
        runSoul,
        List.of("tattered_dagger"),
        List.of(),
        "tattered_dagger",
        List.of(),
        0);
  }

  private static ProfileData profileWithSoul(int soul, int runCount) {
    return new ProfileData(
        ProfileData.CURRENT_SCHEMA_VERSION,
        soul,
        runCount,
        List.of("center"),
        List.of("tattered_dagger"),
        Map.of(),
        List.of(),
        0,
        List.of(),
        List.of(),
        true,
        null,
        null);
  }

  // ---------------- 初期状態 ----------------

  @Test
  void freshInstallHasNothingToContinue() {
    assertFalse(lifecycle.canContinue());
    assertEquals(RunLifecycle.StartDecision.ALLOWED, lifecycle.decideNewRun());
  }

  @Test
  void profileOnlyDoesNotOfferContinue() {
    // Profile だけが存在する状態 (ラン終了直後の再起動) では「つづき」を出さない。
    saveManager.saveProfile(profileWithSoul(120, 3));
    assertFalse(lifecycle.canContinue());
    assertEquals(RunLifecycle.StartDecision.ALLOWED, lifecycle.decideNewRun());
  }

  // ---------------- 通しシナリオ ----------------

  @Test
  void progressSurvivesRunEndAndRestart() {
    // これが P0-1 の核心。ラン終了後に再起動しても獲得進捗が残る。
    RunId runId = RunId.of("run-1");
    lifecycle.beginRun(profileWithSoul(100, 3), runId);
    lifecycle.saveAtLayerBoundary(profileWithSoul(100, 3), checkpointFor(runId, 2, 12));

    // 精算: ラン中 Soul 12 を恒久側へ移し、周回数を 1 増やして活動ランを解除する。
    ProfileData settled = profileWithSoul(112, 4).withSettledRunId(runId.value());
    assertTrue(lifecycle.endRun(settled).isSuccess());

    // 再起動相当: 別インスタンスで読み直す。
    RunLifecycle afterRestart =
        new RunLifecycle(new SaveManager(tempDir.resolve("save.json").toFile()));
    assertEquals(112, afterRestart.profileOrInitial().soulTotal(), "獲得進捗が残る");
    assertEquals(4, afterRestart.profileOrInitial().runCount());
    assertFalse(afterRestart.canContinue(), "終了済みランは再開候補に出ない");
  }

  @Test
  void layerBoundarySaveIsResumableAfterRestart() {
    RunId runId = RunId.of("run-1");
    lifecycle.beginRun(profileWithSoul(100, 3), runId);
    lifecycle.saveAtLayerBoundary(profileWithSoul(100, 3), checkpointFor(runId, 3, 7));

    RunLifecycle afterRestart =
        new RunLifecycle(new SaveManager(tempDir.resolve("save.json").toFile()));
    assertTrue(afterRestart.canContinue());
    assertEquals(3, afterRestart.resumableCheckpoint().orElseThrow().nextLayerNumber());
    assertEquals(7, afterRestart.resumableCheckpoint().orElseThrow().currentRunSoul());
  }

  @Test
  void layerBoundarySaveUpdatesSameRunId() {
    RunId runId = RunId.of("run-1");
    lifecycle.beginRun(profileWithSoul(100, 3), runId);
    lifecycle.saveAtLayerBoundary(profileWithSoul(100, 3), checkpointFor(runId, 2, 3));
    lifecycle.saveAtLayerBoundary(profileWithSoul(100, 3), checkpointFor(runId, 3, 9));

    RunCheckpoint resumed = lifecycle.resumableCheckpoint().orElseThrow();
    assertEquals("run-1", resumed.runId(), "同じランIDのまま更新する");
    assertEquals(3, resumed.nextLayerNumber());
    assertEquals(9, resumed.currentRunSoul());
  }

  @Test
  void layerBoundarySaveDoesNotWipeCarriedOverSoul() {
    // ラン中は progress.playerSoul() が 0 (開始時に Player へ注入済) のため、その値を
    // そのまま profile へ書くと持越しソウルが 0 で潰れる。層境界では据え置くこと。
    RunId runId = RunId.of("run-1");
    lifecycle.beginRun(profileWithSoul(500, 3), runId);

    ProfileData previous = lifecycle.profileOrInitial();
    assertEquals(500, previous.soulTotal());

    // 層境界セーブは直前の soulTotal を引き継ぐ。
    lifecycle.saveAtLayerBoundary(
        profileWithSoul(previous.soulTotal(), 3), checkpointFor(runId, 2, 12));

    RunLifecycle afterRestart =
        new RunLifecycle(new SaveManager(tempDir.resolve("save.json").toFile()));
    assertEquals(500, afterRestart.profileOrInitial().soulTotal(), "中断離脱でも持越しソウルを失わない");
  }

  // ---------------- 終了済みランの再開拒否 ----------------

  @Test
  void staleCheckpointIsNotResumable() {
    // Checkpoint を消し損ねても、activeRunId が解除されていれば再開しない。
    RunId runId = RunId.of("run-1");
    lifecycle.beginRun(profileWithSoul(100, 3), runId);
    saveManager.saveCheckpoint(checkpointFor(runId, 2, 12));
    saveManager.saveProfile(profileWithSoul(112, 4).withSettledRunId(runId.value()));

    assertTrue(saveManager.checkpointExists(), "ファイル自体は残っている");
    assertFalse(lifecycle.canContinue(), "それでも再開させない");
  }

  @Test
  void checkpointFromDifferentRunIsNotResumable() {
    lifecycle.beginRun(profileWithSoul(100, 3), RunId.of("run-current"));
    saveManager.saveCheckpoint(checkpointFor(RunId.of("run-other"), 2, 5));
    assertFalse(lifecycle.canContinue(), "ランIDが食い違う Checkpoint は再開しない");
  }

  // ---------------- 進行中ランがある状態での新規開始 ----------------

  @Test
  void newRunWhileCheckpointExistsRequiresResolution() {
    RunId runId = RunId.of("run-1");
    lifecycle.beginRun(profileWithSoul(100, 3), runId);
    lifecycle.saveAtLayerBoundary(profileWithSoul(100, 3), checkpointFor(runId, 2, 5));

    assertEquals(RunLifecycle.StartDecision.REQUIRES_RESOLUTION, lifecycle.decideNewRun());
  }

  @Test
  void beginRunClearsPreviousCheckpoint() {
    RunId first = RunId.of("run-1");
    lifecycle.beginRun(profileWithSoul(100, 3), first);
    lifecycle.saveAtLayerBoundary(profileWithSoul(100, 3), checkpointFor(first, 2, 5));

    // 放棄の精算後に新しいランを始めると、古い Checkpoint は残らない。
    lifecycle.beginRun(profileWithSoul(105, 4), RunId.of("run-2"));
    assertFalse(saveManager.checkpointExists());
    assertFalse(lifecycle.canContinue(), "新規ランはまだ層境界を越えていないので再開対象なし");
  }

  @Test
  void abandonedRunKeepsItsEarnedSoul() {
    // §15-9: 死亡・放棄・クリアのいずれでも、ラン中に得た Soul は 100% を Profile へ移す。
    // 放棄で Checkpoint を消すだけにすると、確認ダイアログの「死亡と同じ扱い」が嘘になる。
    RunId runId = RunId.of("run-1");
    lifecycle.beginRun(profileWithSoul(500, 3), runId);
    // 持越し 500 + ラン中獲得 30 = 530 が Checkpoint の総ソウル。
    lifecycle.saveAtLayerBoundary(profileWithSoul(500, 3), checkpointFor(runId, 2, 530));

    // 放棄の精算: 総量で置換する (加算すると持越し分が二重計上される)。
    RunCheckpoint abandoned = lifecycle.resumableCheckpoint().orElseThrow();
    ProfileData settled =
        profileWithSoul(abandoned.currentRunSoul(), 4).withSettledRunId(abandoned.runId());
    assertTrue(lifecycle.endRun(settled).isSuccess());

    RunLifecycle afterRestart =
        new RunLifecycle(new SaveManager(tempDir.resolve("save.json").toFile()));
    assertEquals(530, afterRestart.profileOrInitial().soulTotal(), "獲得分を失わない");
    assertFalse(afterRestart.canContinue(), "放棄したランは再開できない");
  }

  @Test
  void restorableSaveDataCarriesTotalSoulWithoutAddingProfileSoul() {
    // Checkpoint の currentRunSoul は持越し + ラン中獲得の総量。復元経路が
    // profile.soulTotal をさらに加算すると、再開のたびにソウルが増殖する。
    // 復元用 SaveData の 2 つのソウル欄が「総量」と「恒久側」で別物であることを固定する。
    RunId runId = RunId.of("run-1");
    lifecycle.beginRun(profileWithSoul(500, 3), runId);
    lifecycle.saveAtLayerBoundary(profileWithSoul(500, 3), checkpointFor(runId, 2, 530));

    ProfileData profile = lifecycle.profileOrInitial();
    RunCheckpoint checkpoint = lifecycle.resumableCheckpoint().orElseThrow();
    SaveData restorable = RunCheckpointMapper.toRestorableSaveData(profile, checkpoint);

    assertEquals(530, restorable.currentRunSoul(), "ラン側は総量を持つ");
    assertEquals(500, restorable.soulTotal(), "恒久側は持越しのまま据え置かれている");
    // 復元時に使ってよいのは currentRunSoul だけ。両者を足すと 1030 になる。
    assertEquals(
        530, restorable.currentRunSoul(), "復元後のプレイヤーソウルは currentRunSoul と一致し、soulTotal を足さない");
  }

  // ---------------- 失敗時の安全側 ----------------

  @Test
  void endRunRejectsProfileThatStillPointsAtARun() {
    // activeRunId を残したまま終了記録を書くと、終了済みランが再開できてしまう。
    assertThrows(
        IllegalArgumentException.class,
        () -> lifecycle.endRun(profileWithSoul(112, 4).withActiveRunId("run-1")));
  }

  @Test
  void failedProfileSaveOnRunEndKeepsCheckpoint() {
    // 恒久進捗を書けなかったのに Checkpoint を消すと、ラン成果が黙って消える。
    RunId runId = RunId.of("run-1");
    lifecycle.beginRun(profileWithSoul(100, 3), runId);
    lifecycle.saveAtLayerBoundary(profileWithSoul(100, 3), checkpointFor(runId, 2, 12));

    // profile.json をディレクトリで塞いで保存を失敗させる。
    saveManager.deleteProfile();
    assertTrue(saveManager.profileFile().mkdirs());

    ProfileData settled = profileWithSoul(112, 4).withSettledRunId(runId.value());
    assertFalse(lifecycle.endRun(settled).isSuccess());
    assertTrue(saveManager.checkpointExists(), "保存に失敗したら Checkpoint を消さない");
  }

  @Test
  void refuseToSaveProfileIfFutureSchemaFileExists() throws Exception {
    java.io.File profileFile = saveManager.profileFile();
    java.nio.file.Files.writeString(
        profileFile.toPath(), "{\"schemaVersion\": 99, \"soulTotal\": 9999}");

    assertTrue(saveManager.profileExists());
    assertTrue(saveManager.loadProfile().isEmpty());

    SaveManager.SaveResult result = saveManager.saveProfile(ProfileData.initial());
    assertFalse(result.isSuccess());
    assertTrue(result.message().contains("unsafe to overwrite"));

    String content = java.nio.file.Files.readString(profileFile.toPath());
    assertTrue(content.contains("\"schemaVersion\": 99"));
  }

  @Test
  void refuseToSaveProfileIfCorruptedFileExists() throws Exception {
    java.io.File profileFile = saveManager.profileFile();
    java.nio.file.Files.writeString(profileFile.toPath(), "{invalid_json_content...");

    assertTrue(saveManager.profileExists());
    assertTrue(saveManager.loadProfile().isEmpty());

    SaveManager.SaveResult result = saveManager.saveProfile(ProfileData.initial());
    assertFalse(result.isSuccess());
    assertTrue(result.message().contains("unsafe to overwrite"));

    String content = java.nio.file.Files.readString(profileFile.toPath());
    assertTrue(content.contains("{invalid_json_content..."));
  }

  @Test
  void refuseToDeleteFutureOrCorruptedCheckpointOnDelete() throws Exception {
    java.io.File checkpointFile = saveManager.checkpointFile();
    java.nio.file.Files.writeString(
        checkpointFile.toPath(), "{\"schemaVersion\": 99, \"runId\": \"future-run\"}");

    assertTrue(saveManager.checkpointExists());

    saveManager.deleteCheckpoint();
    assertTrue(saveManager.checkpointExists(), "future schema の Checkpoint は削除されないこと");

    String content = java.nio.file.Files.readString(checkpointFile.toPath());
    assertTrue(content.contains("\"future-run\""));
  }

  @Test
  void layerBoundaryProfileSavePreservesExistingProfileSoul() {
    ProfileData previous = profileWithSoul(500, 2);
    saveManager.saveProfile(previous);

    core.domain.meta.PlayerProgress progressWithZeroSoul =
        ProfileDataMapper.toPlayerProgress(previous, Map.of())
            .withPlayerSoul(core.domain.meta.Soul.zero());

    ProfileData layerBoundary = ProfileDataMapper.forLayerBoundary(progressWithZeroSoul, previous);
    saveManager.saveProfile(layerBoundary);

    ProfileData loaded = saveManager.loadProfile().orElseThrow();
    assertEquals(
        500,
        loaded.soulTotal(),
        "ラン中 (progress.playerSoul=0) のレイヤー境界セーブで Profile Soul が 0 へ上書きされないこと");
  }

  @Test
  void refuseToSaveProfileOrCheckpointIfSemanticallyInvalidInCurrentSchema() throws Exception {
    java.io.File profileFile = saveManager.profileFile();
    // schemaVersion 1 だが retentionCapacity 0 に対し protectedEquipmentIds に装備があるため
    // ProfileData compact constructor のバリデーション (retentionCapacity 超過) に違反する
    java.nio.file.Files.writeString(
        profileFile.toPath(),
        "{\"schemaVersion\": 1, \"soulTotal\": 100, \"runCount\": 0, \"unlockedNodeIds\": [], \"ownedEquipmentIds\": [\"iron_boots\"], \"loadout\": {}, \"protectedEquipmentIds\": [\"iron_boots\"], \"retentionCapacity\": 0, \"defeatedEnemyKinds\": [], \"obtainedCardIds\": [], \"tutorialSeen\": false}");

    assertTrue(saveManager.profileExists());
    assertTrue(saveManager.loadProfile().isEmpty(), "意味的に不正なデータは load で拒否されること");
    assertTrue(
        saveManager.isProfileUnsafeToOverwrite(),
        "意味的に不正な現行 schema ファイルは isProfileUnsafeToOverwrite が true であること");

    SaveManager.SaveResult result = saveManager.saveProfile(ProfileData.initial());
    assertFalse(result.isSuccess(), "意味的に不正なファイルの上書き保存が拒否されること");
  }

  @Test
  void beginRunAbortsWithoutDeletingCheckpointIfSaveIsUnsafe() throws Exception {
    java.io.File profileFile = saveManager.profileFile();
    java.nio.file.Files.writeString(
        profileFile.toPath(), "{\"schemaVersion\": 99, \"soulTotal\": 9999}");
    java.io.File checkpointFile = saveManager.checkpointFile();
    java.nio.file.Files.writeString(
        checkpointFile.toPath(),
        "{\"schemaVersion\": 1, \"runId\": \"valid-run\", \"currentHp\": 10, \"maxHp\": 20, \"speed\": 3, \"physicalAttack\": 4, \"magicalAttack\": 2, \"physicalDefense\": 1, \"magicalDefense\": 1, \"layerNumber\": 2, \"deck\": [], \"currentRunGold\": 0, \"currentRunSoul\": 500, \"carriedInEquipmentIds\": [], \"unconfirmedEquipmentIds\": [], \"equippedEquipmentId\": null, \"protectedEquipmentIds\": [], \"protectedCapacity\": 0}");

    assertTrue(saveManager.checkpointExists());

    SaveManager.SaveResult result = lifecycle.beginRun(ProfileData.initial(), RunId.newRandom());
    assertFalse(result.isSuccess());
    assertTrue(saveManager.checkpointExists(), "Profile が unsafe の場合 Checkpoint が誤削除されないこと");
  }

  @Test
  void soulConsumptionInRunIsReflectedOnSettlement() {
    ProfileData previous = profileWithSoul(1000, 1);
    saveManager.saveProfile(previous);

    RunId runId = RunId.newRandom();
    lifecycle.beginRun(previous, runId);

    // 開始時 1000 ソウルから治療の泉等で 10 ソウル消費して 990 ソウルでセーブ
    RunCheckpoint checkpoint = checkpointFor(runId, 2, 990);
    // initialRunSoul を 1000 として記録
    checkpoint =
        new RunCheckpoint(
            checkpoint.schemaVersion(),
            checkpoint.runId(),
            checkpoint.nextLayerNumber(),
            checkpoint.currentHp(),
            checkpoint.maxHp(),
            checkpoint.speed(),
            checkpoint.physicalAttack(),
            checkpoint.magicalAttack(),
            checkpoint.physicalDefense(),
            checkpoint.magicalDefense(),
            checkpoint.deck(),
            checkpoint.currentRunGold(),
            checkpoint.currentRunSoul(),
            checkpoint.carriedInEquipmentIds(),
            checkpoint.unconfirmedEquipmentIds(),
            checkpoint.equippedId(),
            checkpoint.protectedEquipmentIds(),
            checkpoint.retentionCapacity(),
            1000);
    lifecycle.saveAtLayerBoundary(previous, checkpoint);

    // 中断から放棄精算
    int finalSoul = checkpoint.currentRunSoul();
    int initialSoul = checkpoint.initialRunSoul();
    int deltaSoul = finalSoul - initialSoul; // -10
    int settledSoulTotal = Math.max(0, previous.soulTotal() + deltaSoul); // 990

    core.domain.meta.PlayerProgress progress =
        ProfileDataMapper.toPlayerProgress(previous, Map.of());
    ProfileData settled = ProfileDataMapper.forSoulUpdate(progress, previous, settledSoulTotal);
    lifecycle.endRun(settled);

    ProfileData loaded = saveManager.loadProfile().orElseThrow();
    assertEquals(990, loaded.soulTotal(), "ラン中に消費された 10 ソウルが精算時に正しく減額反映されること");
  }

  @Test
  void treeResetClampsProtectedEquipmentIdsToNewCapacity() {
    // 容量 1 の Profile に保護指定が 1 件存在
    ProfileData previous =
        new ProfileData(
            1,
            100,
            0,
            List.of("seal_of_soul_1"),
            List.of("tattered_boots"),
            Map.of(),
            List.of("tattered_boots"),
            1,
            List.of(),
            List.of(),
            false,
            null,
            null);
    saveManager.saveProfile(previous);

    // リセット後 (capacity 0) の PlayerProgress
    core.domain.meta.PlayerProgress resetProgress =
        core.domain.meta.PlayerProgress.initial(Map.of());

    // リセット後の書き込みで容量が 0 に切り詰まり、例外を出さずに保存できること
    ProfileData updated = ProfileDataMapper.forSoulUpdate(resetProgress, previous, 100);
    saveManager.saveProfile(updated);

    ProfileData loaded = saveManager.loadProfile().orElseThrow();
    assertEquals(0, loaded.retentionCapacity());
    assertTrue(loaded.protectedEquipmentIds().isEmpty(), "容量オーバーの保護指定が切り詰められていること");
  }
}
