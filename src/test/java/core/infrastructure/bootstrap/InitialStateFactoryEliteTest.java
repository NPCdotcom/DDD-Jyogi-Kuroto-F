package core.infrastructure.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.dungeon.DungeonState;
import core.domain.entity.Enemy;
import core.domain.entity.EnemyKind;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * §15-6 強化個体仕様の検証。シームレス層モデルでは強化個体を「層 2」に出現させる (§15-3「強化個体撃破時のカード追加 UI」をデモ可能にする)。層 1 / 層 3 には出ない。
 */
class InitialStateFactoryEliteTest {

  private static long eliteCount(DungeonState state) {
    return state.enemies().stream().filter(e -> e.kind() == EnemyKind.ELITE_SLIME).count();
  }

  /** 層 2 の DungeonState。firstFloor → advanceLayer。 */
  private static DungeonState layer2() {
    DungeonState layer1 = InitialStateFactory.firstFloor(new Random(42));
    return InitialStateFactory.advanceLayer(layer1, new Random(42));
  }

  @Test
  void firstFloorContainsNoElite() {
    assertEquals(0, eliteCount(InitialStateFactory.firstFloor(new Random(42))), "1 層目には Elite なし");
  }

  @Test
  void layer2ContainsExactlyOneElite() {
    // §15-3 / §15-6: 層 2 に Elite 1 体 + 雑魚 5 体 (enemyCountFor(2)=6)。撃破でカード追加 UI が発火する。
    DungeonState layer2 = layer2();
    assertEquals(1, eliteCount(layer2), "層 2 に Elite 1 体出現");
    // §15-5 敵バリエーション以降、雑魚枠は通常/素早い/頑強が混在する。Elite を除いた残り 5 体を雑魚枠として検証。
    assertEquals(5, layer2.enemies().size() - eliteCount(layer2), "Elite を除く雑魚枠は 5 体");
    assertEquals(6, layer2.enemies().size(), "総敵数は 6 (雑魚 5 + Elite 1)");
  }

  @Test
  void layer3ContainsNoElite() {
    // 層 3 は最終層 (ボス層)。Elite は出ず、ボス 1 体のみ。
    DungeonState layer3 = InitialStateFactory.advanceLayer(layer2(), new Random(42));
    assertEquals(0, eliteCount(layer3), "層 3 には Elite なし");
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
