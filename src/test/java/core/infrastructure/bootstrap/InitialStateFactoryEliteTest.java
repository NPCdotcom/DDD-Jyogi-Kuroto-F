package core.infrastructure.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.dungeon.DungeonState;
import core.domain.entity.Enemy;
import core.domain.entity.EnemyKind;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * §15-6 強化個体仕様の検証 (ADR-30 で 5 層ごとに Elite 1 体追加することを決定)。
 * `InitialStateFactory.advanceLayer` が層番号 5 / 10 / 15 ... で Elite を含むことを確認。
 */
class InitialStateFactoryEliteTest {

  private static DungeonState advanceTo(int targetLayer) {
    DungeonState state = InitialStateFactory.firstFloor(new Random(42));
    while (state.layer().number() < targetLayer) {
      state = InitialStateFactory.advanceLayer(state);
    }
    return state;
  }

  @Test
  void firstFloorDoesNotContainElite() {
    DungeonState state = InitialStateFactory.firstFloor(new Random(42));
    long eliteCount =
        state.enemies().stream().filter(e -> e.kind() == EnemyKind.ELITE_SLIME).count();
    assertEquals(0, eliteCount, "1 層目には Elite なし");
  }

  @Test
  void layer2DoesNotContainElite() {
    DungeonState state = advanceTo(2);
    long eliteCount =
        state.enemies().stream().filter(e -> e.kind() == EnemyKind.ELITE_SLIME).count();
    assertEquals(0, eliteCount, "2 層目には Elite なし");
  }

  @Test
  void layer5ContainsExactlyOneElite() {
    // §15-6: layerNumber % 5 == 0 で Elite 1 体追加
    DungeonState state = advanceTo(5);
    long eliteCount =
        state.enemies().stream().filter(e -> e.kind() == EnemyKind.ELITE_SLIME).count();
    assertEquals(1, eliteCount, "5 層目に Elite 1 体出現");
    long slimeCount =
        state.enemies().stream().filter(e -> e.kind() == EnemyKind.SLIME).count();
    assertEquals(2, slimeCount, "雑魚 2 体は維持");
    assertEquals(3, state.enemies().size(), "5 層目の総敵数は 3 (雑魚 2 + Elite 1)");
  }

  @Test
  void layer10ContainsElite() {
    DungeonState state = advanceTo(10);
    long eliteCount =
        state.enemies().stream().filter(e -> e.kind() == EnemyKind.ELITE_SLIME).count();
    assertEquals(1, eliteCount, "10 層目に Elite 1 体出現");
  }

  @Test
  void layer6DoesNotContainElite() {
    // 5 層を通過した後でも、6 層目には Elite は出ない (5 倍数のみ)
    DungeonState state = advanceTo(6);
    long eliteCount =
        state.enemies().stream().filter(e -> e.kind() == EnemyKind.ELITE_SLIME).count();
    assertEquals(0, eliteCount, "6 層目には Elite なし (5 倍数のみ)");
  }

  @Test
  void eliteSlimeHasCorrectRewardRates() {
    // §15-2 / ADR-30: Elite Soul 3 / Gold 15
    assertEquals(3, EnemyKind.ELITE_SLIME.soulReward(), "Elite Soul 報酬 = 3");
    assertEquals(15, EnemyKind.ELITE_SLIME.goldReward(), "Elite Gold 報酬 = 15");
  }

  @Test
  void newEliteSlimeForLayerHasStrongStats() {
    // Elite は雑魚より強い: HP 20 (倍)、物攻 3 (1.5 倍)、物防 1、AP 層番号 +2
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
        InitialStateFactory.newEliteSlimeForLayer(
            "e5", new core.domain.common.Position(5, 5), 5);
    Enemy elite10 =
        InitialStateFactory.newEliteSlimeForLayer(
            "e10", new core.domain.common.Position(5, 5), 10);
    assertEquals(7, elite5.actionPoints().max());
    assertEquals(12, elite10.actionPoints().max());
    assertTrue(elite10.actionPoints().max() > elite5.actionPoints().max(), "層数で AP 増加");
  }
}
