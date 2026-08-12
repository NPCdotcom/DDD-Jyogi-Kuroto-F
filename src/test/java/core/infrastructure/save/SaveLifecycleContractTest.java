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
}
