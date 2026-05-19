package core.domain.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.infrastructure.bootstrap.InitialStateFactory;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * {@link LayerEndNode} の単体テスト (§15-8 / E-6)。
 *
 * <p>fixture は infrastructure 層の {@link InitialStateFactory#firstFloor} の player を流用 (テスト便宜上の依存、production 依存方向ルールには違反しない)。
 */
class LayerEndNodeTest {

  private static Player initialPlayer() {
    return InitialStateFactory.firstFloor(new Random(42)).player();
  }

  // ---------------- HpMaxUp ----------------

  @Test
  void hpMaxUpRaisesBothCurrentAndMaxHp() {
    // §15-8 「Slay the Spire の永続バフ型」: max を上げると同時に current も同量上がり、上限張り付きが起きない
    Player p = initialPlayer();
    int beforeCurrent = p.stats().currentHp();
    int beforeMax = p.stats().maxHp();

    Player up = new LayerEndNode.HpMaxUp(5).apply(p);

    assertEquals(beforeCurrent + 5, up.stats().currentHp(), "currentHp が +5");
    assertEquals(beforeMax + 5, up.stats().maxHp(), "maxHp が +5");
    assertEquals(p.stats().speed(), up.stats().speed(), "速度は変化しない");
  }

  @Test
  void hpMaxUpReturnsNewInstanceLeavingOriginalIntact() {
    // 純関数: 元 Player は変化しない
    Player p = initialPlayer();
    Stats originalStats = p.stats();

    Player up = new LayerEndNode.HpMaxUp(3).apply(p);

    assertNotSame(p, up);
    assertEquals(originalStats, p.stats(), "元 Player の Stats は変化していない");
  }

  @Test
  void hpMaxUpZeroAmountIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new LayerEndNode.HpMaxUp(0));
  }

  @Test
  void hpMaxUpNegativeAmountIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new LayerEndNode.HpMaxUp(-1));
  }

  @Test
  void hpMaxUpDisplayNameIncludesAmount() {
    assertEquals("HP +5", new LayerEndNode.HpMaxUp(5).displayName());
  }

  // ---------------- SpeedUp ----------------

  @Test
  void speedUpRaisesSpeedOnly() {
    // §15-4: 速度 = 1 ターン AP 量。他ステは不変
    Player p = initialPlayer();
    int beforeSpeed = p.stats().speed();
    int beforeHp = p.stats().currentHp();
    int beforeMax = p.stats().maxHp();

    Player up = new LayerEndNode.SpeedUp(1).apply(p);

    assertEquals(beforeSpeed + 1, up.stats().speed(), "speed が +1");
    assertEquals(beforeHp, up.stats().currentHp(), "currentHp は不変");
    assertEquals(beforeMax, up.stats().maxHp(), "maxHp は不変");
  }

  @Test
  void speedUpZeroAmountIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new LayerEndNode.SpeedUp(0));
  }

  @Test
  void speedUpDisplayNameIncludesAmount() {
    assertEquals("速度 +1", new LayerEndNode.SpeedUp(1).displayName());
  }

  // ---------------- Rest ----------------

  @Test
  void restHealsCurrentHpToMax() {
    // §15-8 休憩ノード: HP 全回復。Stats.healed(maxHp) で min(maxHp, current + maxHp) = maxHp
    Player p = initialPlayer();
    // current を半分まで削った状態を作る (originalCurrent / 2)
    int half = p.stats().currentHp() / 2;
    Stats damagedStats = p.stats().damaged(half);
    Player damaged = p.withStats(damagedStats);

    Player rested = new LayerEndNode.Rest().apply(damaged);

    assertEquals(damaged.stats().maxHp(), rested.stats().currentHp(), "current が max まで回復");
    assertEquals(damaged.stats().maxHp(), rested.stats().maxHp(), "maxHp は変化しない");
  }

  @Test
  void restAtFullHpIsIdempotent() {
    // 既に max の場合、Rest を適用しても max を超えない (healed の上限保証)
    Player p = initialPlayer();

    Player rested = new LayerEndNode.Rest().apply(p);

    assertEquals(p.stats().maxHp(), rested.stats().currentHp(), "current は maxHp のまま");
  }

  @Test
  void restDisplayNameIsFixedString() {
    assertEquals("HP 全回復", new LayerEndNode.Rest().displayName());
  }

  // ---------------- sealed 網羅性 ----------------

  @Test
  void allPermitsAreReachableViaPatternSwitch() {
    // §15-8 / E-6: sealed permits の全 3 種が pattern switch で網羅的に処理できることを確認
    LayerEndNode[] allKinds =
        new LayerEndNode[] {
          new LayerEndNode.HpMaxUp(1), new LayerEndNode.SpeedUp(1), new LayerEndNode.Rest()
        };
    for (LayerEndNode node : allKinds) {
      String tag =
          switch (node) {
            case LayerEndNode.HpMaxUp ignored -> "hp";
            case LayerEndNode.SpeedUp ignored -> "speed";
            case LayerEndNode.Rest ignored -> "rest";
          };
      // 各タグが意味ある (空でない) 文字列であることだけ確認 (網羅性はコンパイル時に保証される)
      assertFalse(tag.isEmpty(), "switch arm が空文字を返してはいけない: " + node);
    }
  }
}
