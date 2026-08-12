package core.infrastructure.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.equipment.EquipmentOwnership;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 旧 {@code save.json} から Profile / RunCheckpoint への写像を検証する (SAVE-02A)。
 *
 * <p>v1 と v2 はラン中 Soul / Gold を持たないため復元できない。v3 は持つ。いずれの版でも Soul 総量を二重計上しないことと、廃止した SlotExpand
 * ノードを返金して削除することを固定する。
 */
class LegacySaveMapperTest {

  private static final String STARTER = EquipmentOwnership.STARTER_EQUIPMENT_VALUE;

  /** 指定した schemaVersion の旧セーブを組み立てる。 */
  private static SaveData legacySave(
      int schemaVersion, int soulTotal, int runSoul, int runGold, List<String> unlockedNodeIds) {
    return new SaveData(
        schemaVersion,
        2, // nextLayerNumber
        18,
        20,
        3,
        4,
        2,
        1,
        1,
        List.of("zangeki"),
        soulTotal,
        3, // runCount
        unlockedNodeIds,
        List.of("zangeki"),
        Map.of("HAND", "dark_blade"),
        List.of("SLIME"),
        true,
        runGold,
        runSoul);
  }

  // ---------------- 装備の写像 ----------------

  @Test
  void loadoutIdsBecomeOwnedEquipment() {
    LegacySaveMapper.Result result =
        LegacySaveMapper.map(legacySave(3, 100, 7, 45, List.of("center")));
    assertTrue(result.profile().ownedEquipmentIds().contains("dark_blade"));
  }

  @Test
  void starterIsAlwaysOwnedEvenIfAbsentFromLegacyLoadout() {
    LegacySaveMapper.Result result =
        LegacySaveMapper.map(legacySave(3, 100, 7, 45, List.of("center")));
    assertTrue(result.profile().ownedEquipmentIds().contains(STARTER));
  }

  @Test
  void legacyLoadoutBecomesCarriedInAndEquipped() {
    LegacySaveMapper.Result result =
        LegacySaveMapper.map(legacySave(3, 100, 7, 45, List.of("center")));
    RunCheckpoint checkpoint = result.checkpoint().orElseThrow();
    assertTrue(checkpoint.carriedInEquipmentIds().contains("dark_blade"));
    assertEquals("dark_blade", checkpoint.equippedId());
  }

  @Test
  void unconfirmedAndProtectedStartEmpty() {
    LegacySaveMapper.Result result =
        LegacySaveMapper.map(legacySave(3, 100, 7, 45, List.of("center")));
    RunCheckpoint checkpoint = result.checkpoint().orElseThrow();
    assertTrue(checkpoint.unconfirmedEquipmentIds().isEmpty());
    assertTrue(checkpoint.protectedEquipmentIds().isEmpty());
    assertEquals(0, checkpoint.retentionCapacity());
    assertTrue(result.profile().protectedEquipmentIds().isEmpty());
  }

  // ---------------- SlotExpand の返金と削除 ----------------

  @Test
  void slotExpandNodesAreRemovedAndRefundedOnce() {
    LegacySaveMapper.Result result =
        LegacySaveMapper.map(
            legacySave(3, 100, 0, 0, List.of("center", "slot_expand_1", "slot_expand_2")));
    assertFalse(result.profile().unlockedNodeIds().contains("slot_expand_1"));
    assertFalse(result.profile().unlockedNodeIds().contains("slot_expand_2"));
    assertTrue(result.profile().unlockedNodeIds().contains("center"));
    // 100 + 40 * 2 = 180
    assertEquals(180, result.profile().soulTotal());
  }

  @Test
  void duplicatedSlotExpandIdsAreRefundedOnlyOnce() {
    LegacySaveMapper.Result result =
        LegacySaveMapper.map(
            legacySave(3, 0, 0, 0, List.of("slot_expand_1", "slot_expand_1", "slot_expand_1")));
    assertEquals(40, result.profile().soulTotal());
  }

  @Test
  void saveWithoutSlotExpandGetsNoRefund() {
    LegacySaveMapper.Result result =
        LegacySaveMapper.map(legacySave(3, 100, 0, 0, List.of("center", "hp_1")));
    assertEquals(100, result.profile().soulTotal());
  }

  // ---------------- 版ごとの通貨の扱い ----------------

  @Test
  void v3MovesRunCurrencyToCheckpointWithoutDoubleCounting() {
    LegacySaveMapper.Result result =
        LegacySaveMapper.map(legacySave(3, 100, 7, 45, List.of("center")));
    assertEquals(100, result.profile().soulTotal(), "ラン中 Soul を Profile へ足さない");
    RunCheckpoint checkpoint = result.checkpoint().orElseThrow();
    assertEquals(7, checkpoint.currentRunSoul());
    assertEquals(45, checkpoint.currentRunGold());
  }

  @Test
  void v1AndV2CannotRestoreRunCurrencyAndSaySo() {
    for (int version : new int[] {1, 2}) {
      LegacySaveMapper.Result result =
          LegacySaveMapper.map(legacySave(version, 100, 0, 0, List.of("center")));
      RunCheckpoint checkpoint = result.checkpoint().orElseThrow();
      assertEquals(0, checkpoint.currentRunSoul(), "v" + version + " はラン中 Soul を持たない");
      assertEquals(0, checkpoint.currentRunGold(), "v" + version + " はラン中 Gold を持たない");
      assertTrue(
          result.warnings().stream().anyMatch(w -> w.contains("復元")),
          "v" + version + " は復元不能を通知する: " + result.warnings());
    }
  }

  // ---------------- ラン ID の整合 ----------------

  @Test
  void profileActiveRunIdMatchesCheckpointRunId() {
    // 一致しない Checkpoint は再開も精算もできない (P0-1 の防止条件)。
    LegacySaveMapper.Result result =
        LegacySaveMapper.map(legacySave(3, 100, 7, 45, List.of("center")));
    assertEquals(result.profile().activeRunId(), result.checkpoint().orElseThrow().runId());
  }

  @Test
  void settledRunIdStartsEmptySoMigratedRunCanStillSettle() {
    LegacySaveMapper.Result result =
        LegacySaveMapper.map(legacySave(3, 100, 7, 45, List.of("center")));
    assertEquals(null, result.profile().lastSettledRunId());
    assertFalse(result.profile().isAlreadySettled(result.profile().activeRunId()));
  }

  // ---------------- 引き継ぐ進捗 ----------------

  @Test
  void metaProgressIsCarriedOver() {
    LegacySaveMapper.Result result =
        LegacySaveMapper.map(legacySave(3, 100, 7, 45, List.of("center")));
    ProfileData profile = result.profile();
    assertEquals(3, profile.runCount());
    assertTrue(profile.defeatedEnemyKinds().contains("SLIME"));
    assertTrue(profile.obtainedCardIds().contains("zangeki"));
    assertTrue(profile.tutorialSeen());
    assertEquals(Map.of("HAND", "dark_blade"), profile.loadout());
  }

  @Test
  void checkpointKeepsStatsAndDeck() {
    LegacySaveMapper.Result result =
        LegacySaveMapper.map(legacySave(3, 100, 7, 45, List.of("center")));
    RunCheckpoint checkpoint = result.checkpoint().orElseThrow();
    assertEquals(2, checkpoint.nextLayerNumber());
    assertEquals(18, checkpoint.currentHp());
    assertEquals(20, checkpoint.maxHp());
    assertEquals(List.of("zangeki"), checkpoint.deck());
  }
}
