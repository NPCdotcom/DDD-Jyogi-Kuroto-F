package core.infrastructure.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.dungeon.DungeonState;
import core.domain.entity.Enemy;
import core.domain.entity.EnemyKind;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * §15-6 強化個体仕様の検証。固定 3 層構成では §15-6「5 層おき」が一度も成立しないため、ハッカソン版は 強化個体を「2 層目の最終部屋」に出現させる
 * (§15-3「強化個体撃破時のカード追加 UI」をデモ可能にする)。
 */
class InitialStateFactoryEliteTest {

  private static long eliteCount(DungeonState state) {
    return state.enemies().stream().filter(e -> e.kind() == EnemyKind.ELITE_SLIME).count();
  }

  private static long slimeCount(DungeonState state) {
    return state.enemies().stream().filter(e -> e.kind() == EnemyKind.SLIME).count();
  }

  /** 層 2 の最終部屋 (部屋 2) の DungeonState。firstFloor → advanceLayer → advanceRoom。 */
  private static DungeonState layer2LastRoom() {
    DungeonState layer1 = InitialStateFactory.firstFloor(new Random(42));
    DungeonState layer2Room1 = InitialStateFactory.advanceLayer(layer1, new Random(42));
    return InitialStateFactory.advanceRoom(layer2Room1, new Random(42));
  }

  @Test
  void firstFloorContainsNoElite() {
    assertEquals(0, eliteCount(InitialStateFactory.firstFloor(new Random(42))), "1 層目には Elite なし");
  }

  @Test
  void layer2FirstRoomContainsNoElite() {
    // 強化個体は層 2 の最終部屋に出現する。1 部屋目にはまだ出ない。
    DungeonState layer2Room1 =
        InitialStateFactory.advanceLayer(
            InitialStateFactory.firstFloor(new Random(42)), new Random(42));
    assertEquals(0, eliteCount(layer2Room1), "層 2 の 1 部屋目には Elite なし");
  }

  @Test
  void layer2LastRoomContainsExactlyOneElite() {
    // §15-3 / §15-6: 層 2 の最終部屋に Elite 1 体 + 雑魚 2 体。撃破でカード追加 UI が発火する。
    DungeonState room = layer2LastRoom();
    assertEquals(1, eliteCount(room), "層 2 最終部屋に Elite 1 体出現");
    assertEquals(2, slimeCount(room), "雑魚 2 体は維持");
    assertEquals(3, room.enemies().size(), "総敵数は 3 (雑魚 2 + Elite 1)");
  }

  @Test
  void layer3FirstRoomContainsNoElite() {
    // 層 3 の通常部屋には Elite なし (層 3 の最終部屋はボス部屋)
    DungeonState layer3 =
        InitialStateFactory.advanceLayer(
            InitialStateFactory.advanceLayer(
                InitialStateFactory.firstFloor(new Random(42)), new Random(42)),
            new Random(42));
    assertEquals(0, eliteCount(layer3), "層 3 の 1 部屋目には Elite なし");
  }

  @Test
  void eliteSlimeHasCorrectRewardRates() {
    // §15-2 / ADR-30: Elite Soul 3 / Gold 15
    assertEquals(3, EnemyKind.ELITE_SLIME.soulReward(), "Elite Soul 報酬 = 3");
    assertEquals(15, EnemyKind.ELITE_SLIME.goldReward(), "Elite Gold 報酬 = 15");
  }

  @Test
  void newEliteSlimeForLayerHasStrongStats() {
    // Elite は雑魚より強い: HP 20 (倍)、物攻 3、物防 1、AP 層番号 +2
    Enemy elite =
        InitialStateFactory.newEliteSlimeForLayer(
            "elite_test", new core.domain.common.Position(5, 5), 3);
    assertEquals(20, elite.stats().maxHp(), "Elite maxHp = 20");
    assertEquals(3, elite.stats().physicalAttack(), "Elite 物攻 = 3");
    assertEquals(1, elite.stats().physicalDefense(), "Elite 物防 = 1");
    assertEquals(5, elite.actionPoints().max(), "Elite AP = 層 3 + 2 = 5");
    assertEquals(EnemyKind.ELITE_SLIME, elite.kind());
  }

  @Test
  void newEliteSlimeRejectsLayerNumberZero() {
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            InitialStateFactory.newEliteSlimeForLayer(
                "elite_zero", new core.domain.common.Position(5, 5), 0));
  }

  @Test
  void apIncreasesWithLayerForElite() {
    // 5 層の Elite AP = 5 + 2 = 7、10 層の Elite AP = 10 + 2 = 12
    Enemy elite5 =
        InitialStateFactory.newEliteSlimeForLayer("e5", new core.domain.common.Position(5, 5), 5);
    Enemy elite10 =
        InitialStateFactory.newEliteSlimeForLayer("e10", new core.domain.common.Position(5, 5), 10);
    assertEquals(7, elite5.actionPoints().max());
    assertEquals(12, elite10.actionPoints().max());
    assertTrue(elite10.actionPoints().max() > elite5.actionPoints().max(), "層数で AP 増加");
  }
}
