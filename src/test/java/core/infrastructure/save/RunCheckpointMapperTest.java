package core.infrastructure.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.application.RunId;
import core.domain.card.Card;
import core.domain.card.CardPileState;
import core.domain.card.DiscardPile;
import core.domain.card.DrawPile;
import core.domain.card.Hand;
import core.domain.common.Position;
import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.domain.equipment.EquipmentId;
import core.domain.equipment.EquipmentOwnership;
import core.domain.equipment.RetentionCapacity;
import core.domain.equipment.RunInventory;
import core.domain.meta.Gold;
import core.domain.meta.Soul;
import core.domain.support.DomainFixtures;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link RunCheckpointMapper} の相互変換を検証する (SAVE-03A)。
 *
 * <p>往復してランID・一時所持品・通貨が失われないことと、ラン中 Soul が Profile 側へ 漏れず Checkpoint に留まることを固定する。
 */
class RunCheckpointMapperTest {

  private static final RunId RUN_ID = RunId.of("run-test-1");
  private static final EquipmentId STARTER = EquipmentOwnership.STARTER_EQUIPMENT_ID;
  private static final EquipmentId BOOTS = EquipmentId.of("dash_boots");
  private static final EquipmentId BLADE = EquipmentId.of("dark_blade");

  /** 山札に指定 ID のカードを積んだプレイヤー。Soul / Gold はラン中の獲得量を表す。 */
  private static Player playerWith(int soul, int gold, List<String> deckIds) {
    List<Card> cards = deckIds.stream().map(DomainFixtures::attackCard).toList();
    Player base = DomainFixtures.playerAt(new Position(1, 1));
    return base.withCardPileState(
            new CardPileState(new DrawPile(cards), Hand.empty(), DiscardPile.empty()))
        .addSoul(new Soul(soul))
        .addGold(new Gold(gold));
  }

  private static RunInventory inventory() {
    return new RunInventory(
        Set.of(STARTER, BOOTS), Set.of(BLADE), Set.of(BOOTS), Optional.of(BOOTS));
  }

  // ---------------- ラン ID ----------------

  @Test
  void checkpointKeepsRunId() {
    RunCheckpoint checkpoint =
        RunCheckpointMapper.toCheckpoint(
            RUN_ID, playerWith(7, 45, List.of("zangeki")), 2, inventory(), RetentionCapacity.of(1));
    assertEquals("run-test-1", checkpoint.runId());
    assertEquals(RUN_ID, RunCheckpointMapper.toRunId(checkpoint));
  }

  @Test
  void runIdSurvivesRepeatedSaves() {
    // 層境界セーブを繰り返してもランIDが変わらない (Profile.activeRunId との突合が壊れない)。
    Player player = playerWith(7, 45, List.of("zangeki"));
    RunCheckpoint first =
        RunCheckpointMapper.toCheckpoint(RUN_ID, player, 2, inventory(), RetentionCapacity.of(1));
    RunCheckpoint second =
        RunCheckpointMapper.toCheckpoint(
            RunCheckpointMapper.toRunId(first), player, 3, inventory(), RetentionCapacity.of(1));
    assertEquals(first.runId(), second.runId());
  }

  // ---------------- 一時所持品 ----------------

  @Test
  void inventoryRoundTripsWithoutLoss() {
    RunCheckpoint checkpoint =
        RunCheckpointMapper.toCheckpoint(
            RUN_ID, playerWith(7, 45, List.of("zangeki")), 2, inventory(), RetentionCapacity.of(1));
    RunInventory restored = RunCheckpointMapper.toRunInventory(checkpoint);

    assertEquals(Set.of(STARTER, BOOTS), restored.carriedIn());
    assertEquals(Set.of(BLADE), restored.unconfirmed(), "ラン中購入の未確定品を失わない");
    assertEquals(Set.of(BOOTS), restored.protectedIds());
    assertEquals(Optional.of(BOOTS), restored.equipped());
  }

  @Test
  void emptyEquippedRoundTrips() {
    RunInventory noEquip = new RunInventory(Set.of(STARTER), Set.of(), Set.of(), Optional.empty());
    RunCheckpoint checkpoint =
        RunCheckpointMapper.toCheckpoint(
            RUN_ID, playerWith(0, 0, List.of()), 1, noEquip, RetentionCapacity.none());
    assertTrue(RunCheckpointMapper.toRunInventory(checkpoint).equipped().isEmpty());
  }

  @Test
  void retentionCapacityRoundTrips() {
    RunCheckpoint checkpoint =
        RunCheckpointMapper.toCheckpoint(
            RUN_ID, playerWith(0, 0, List.of()), 1, inventory(), RetentionCapacity.of(2));
    assertEquals(2, RunCheckpointMapper.toRetentionCapacity(checkpoint).value());
  }

  // ---------------- 通貨 ----------------

  @Test
  void runCurrencyStaysInCheckpoint() {
    // ラン中 Soul は精算まで Checkpoint 側に留まる。Profile へ移すのは SETTLE-01/02。
    RunCheckpoint checkpoint =
        RunCheckpointMapper.toCheckpoint(
            RUN_ID, playerWith(7, 45, List.of("zangeki")), 2, inventory(), RetentionCapacity.of(1));
    assertEquals(7, checkpoint.currentRunSoul());
    assertEquals(45, checkpoint.currentRunGold());
  }

  @Test
  void repeatedSavesDoNotAccumulateRunSoul() {
    // 同じ Player から 2 回作っても Soul が足し込まれない (層境界セーブの繰り返し)。
    Player player = playerWith(7, 45, List.of("zangeki"));
    RunCheckpoint first =
        RunCheckpointMapper.toCheckpoint(RUN_ID, player, 2, inventory(), RetentionCapacity.of(1));
    RunCheckpoint second =
        RunCheckpointMapper.toCheckpoint(RUN_ID, player, 3, inventory(), RetentionCapacity.of(1));
    assertEquals(first.currentRunSoul(), second.currentRunSoul());
    assertEquals(7, second.currentRunSoul());
  }

  // ---------------- 能力値とデッキ ----------------

  @Test
  void statsRoundTripInFieldOrder() {
    // Stats の 7 フィールドを取り違えていないか (currentHp/maxHp/speed/物攻/魔攻/物防/魔防)。
    Player player = playerWith(0, 0, List.of()).withStats(new Stats(11, 22, 3, 4, 5, 6, 7));
    RunCheckpoint checkpoint =
        RunCheckpointMapper.toCheckpoint(RUN_ID, player, 2, inventory(), RetentionCapacity.of(1));
    assertEquals(new Stats(11, 22, 3, 4, 5, 6, 7), RunCheckpointMapper.toStats(checkpoint));
  }

  @Test
  void deckKeepsEveryCardAcrossPiles() {
    // 以前の版は山札だけに積んで手札・捨て札を空にしていたため、mapper の手札ループと
    // 捨て札ループを丸ごと削除しても緑のままだった。層境界セーブは 1 枚ドロー直後に走るので
    // 手札は必ず非空。3 つのパイルに別 ID を積み、順序込みで固定する。
    Player player =
        DomainFixtures.playerAt(new Position(1, 1))
            .withCardPileState(
                new CardPileState(
                    new DrawPile(List.of(DomainFixtures.attackCard("draw-1"))),
                    new Hand(List.of(DomainFixtures.attackCard("hand-1"))),
                    new DiscardPile(List.of(DomainFixtures.attackCard("disc-1")))));

    RunCheckpoint checkpoint =
        RunCheckpointMapper.toCheckpoint(RUN_ID, player, 2, inventory(), RetentionCapacity.of(1));

    assertEquals(List.of("draw-1", "hand-1", "disc-1"), checkpoint.deck());
  }

  // ---------------- 異常系 ----------------

  @Test
  void toCheckpointRejectsProtectionBeyondCapacity() {
    // RunInventory は容量を持たないため型では防げない。層境界セーブ中に落ちる前に弾く。
    RunInventory twoProtected =
        new RunInventory(
            Set.of(STARTER, BOOTS, BLADE), Set.of(), Set.of(BOOTS, BLADE), Optional.of(BOOTS));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            RunCheckpointMapper.toCheckpoint(
                RUN_ID, playerWith(0, 0, List.of()), 2, twoProtected, RetentionCapacity.of(1)));
  }

  @Test
  void toCheckpointAcceptsProtectionAtExactCapacity() {
    RunInventory twoProtected =
        new RunInventory(
            Set.of(STARTER, BOOTS, BLADE), Set.of(), Set.of(BOOTS, BLADE), Optional.of(BOOTS));
    RunCheckpoint checkpoint =
        RunCheckpointMapper.toCheckpoint(
            RUN_ID, playerWith(0, 0, List.of()), 2, twoProtected, RetentionCapacity.of(2));
    assertEquals(2, checkpoint.protectedEquipmentIds().size());
  }

  @Test
  void nextLayerNumberIsPreserved() {
    RunCheckpoint checkpoint =
        RunCheckpointMapper.toCheckpoint(
            RUN_ID, playerWith(0, 0, List.of()), 3, inventory(), RetentionCapacity.of(1));
    assertEquals(3, checkpoint.nextLayerNumber());
  }
}
