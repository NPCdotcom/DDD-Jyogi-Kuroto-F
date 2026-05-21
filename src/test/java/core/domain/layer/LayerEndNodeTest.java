package core.domain.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.infrastructure.bootstrap.InitialStateFactory;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * {@link LayerEndNode} の単体テスト (§15-8 / E-6)。
 *
 * <p>fixture は infrastructure 層の {@link InitialStateFactory#firstFloor} の player を流用
 * (テスト便宜上の依存、production 依存方向ルールには違反しない)。
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
    // §15-8 / E-6: sealed permits の全 5 種が pattern switch で網羅的に処理できることを確認
    LayerEndNode[] allKinds =
        new LayerEndNode[] {
          new LayerEndNode.HpMaxUp(1),
          new LayerEndNode.SpeedUp(1),
          new LayerEndNode.Rest(),
          new LayerEndNode.Shop(
              5, core.infrastructure.bootstrap.InitialStateFactory.strongStrikeCard()),
          new LayerEndNode.Event(10, -3, 0, "テスト")
        };
    for (LayerEndNode node : allKinds) {
      String tag =
          switch (node) {
            case LayerEndNode.HpMaxUp ignored -> "hp";
            case LayerEndNode.SpeedUp ignored -> "speed";
            case LayerEndNode.Rest ignored -> "rest";
            case LayerEndNode.Shop ignored -> "shop";
            case LayerEndNode.Event ignored -> "event";
          };
      assertFalse(tag.isEmpty(), "switch arm が空文字を返してはいけない: " + node);
    }
  }

  // ---------------- Shop ----------------

  @Test
  void shopRejectsNegativeGoldCost() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new LayerEndNode.Shop(
                -1, core.infrastructure.bootstrap.InitialStateFactory.strongStrikeCard()));
  }

  @Test
  void shopRejectsNullCard() {
    assertThrows(NullPointerException.class, () -> new LayerEndNode.Shop(5, null));
  }

  @Test
  void shopConsumesGoldAndAddsCardWhenSufficient() {
    Player p = initialPlayer().addGold(new core.domain.meta.Gold(10));
    int deckSizeBefore =
        p.cardPileState().drawPile().size()
            + p.cardPileState().hand().size()
            + p.cardPileState().discardPile().size();

    Player after =
        new LayerEndNode.Shop(
                5, core.infrastructure.bootstrap.InitialStateFactory.strongStrikeCard())
            .apply(p);

    assertEquals(5, after.gold().amount(), "10 - 5 = 5");
    int deckSizeAfter =
        after.cardPileState().drawPile().size()
            + after.cardPileState().hand().size()
            + after.cardPileState().discardPile().size();
    assertEquals(deckSizeBefore + 1, deckSizeAfter, "DrawPile に Card 1 枚追加");
  }

  @Test
  void shopSilentFailWhenInsufficientGold() {
    Player p = initialPlayer(); // gold 0
    Player after =
        new LayerEndNode.Shop(
                5, core.infrastructure.bootstrap.InitialStateFactory.strongStrikeCard())
            .apply(p);
    // Gold 不足: apply は引数の Player をそのまま返す (silent fail)
    assertEquals(p.gold().amount(), after.gold().amount(), "Gold 不変");
    assertEquals(p.cardPileState(), after.cardPileState(), "デッキ不変");
  }

  @Test
  void shopDisplayNameIncludesCardAndCost() {
    String label =
        new LayerEndNode.Shop(
                5, core.infrastructure.bootstrap.InitialStateFactory.strongStrikeCard())
            .displayName();
    assertTrue(label.contains("ショップ"));
    assertTrue(label.contains("5"));
  }

  // ---------------- Event ----------------

  @Test
  void eventAppliesSoulAndHpAndGoldDeltas() {
    Player p = initialPlayer();
    int hpBefore = p.stats().currentHp();
    int soulBefore = p.soul().amount();

    Player after = new LayerEndNode.Event(30, -5, 10, "ソウルの祠").apply(p);

    assertEquals(soulBefore + 30, after.soul().amount());
    assertEquals(hpBefore - 5, after.stats().currentHp());
    assertEquals(10, after.gold().amount());
  }

  @Test
  void eventWithPositiveHpHeals() {
    Player p = initialPlayer().withStats(initialPlayer().stats().damaged(10));
    int hpBeforeHeal = p.stats().currentHp();

    Player after = new LayerEndNode.Event(0, 7, 0, "回復イベント").apply(p);

    assertEquals(hpBeforeHeal + 7, after.stats().currentHp());
  }

  @Test
  void eventRejectsNegativeSoulOrGoldDelta() {
    assertThrows(IllegalArgumentException.class, () -> new LayerEndNode.Event(-1, 0, 0, "x"));
    assertThrows(IllegalArgumentException.class, () -> new LayerEndNode.Event(0, 0, -1, "x"));
  }

  @Test
  void eventRejectsBlankDisplayLabel() {
    assertThrows(IllegalArgumentException.class, () -> new LayerEndNode.Event(0, 0, 0, ""));
  }
}
