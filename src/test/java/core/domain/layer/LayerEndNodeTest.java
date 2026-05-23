package core.domain.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.card.Card;
import core.domain.card.CardId;
import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentId;
import core.infrastructure.bootstrap.InitialStateFactory;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * {@link LayerEndNode} の単体テスト (§15-8 / E-6)。
 *
 * <p>fixture は infrastructure 層の {@link InitialStateFactory#firstFloor} の player を流用
 * (テスト便宜上の依存、production 依存方向ルールには違反しない)。
 *
 * <p>Wave 3 Task A: Shop は CardId 保持に変更、apply / displayName は {@link NodeResolveContext} を受ける。
 * テスト用の context は {@link InitialStateFactory#resolveCard} と Equipment のエラースタブで組み立てる。
 */
class LayerEndNodeTest {

  private static Player initialPlayer() {
    return InitialStateFactory.firstFloor(new Random(42)).player();
  }

  /** テスト用の CardId (cards.json の strong_strike)。 */
  private static CardId sampleCardId() {
    return CardId.of("strong_strike");
  }

  /** テスト用カード (Shop ノードの結果検証に使う)。cards.json の strong_strike を解決。 */
  private static Card sampleCard() {
    return InitialStateFactory.resolveCard(sampleCardId());
  }

  /**
   * テスト用 {@link NodeResolveContext}。cards は {@link InitialStateFactory#resolveCard} に委譲、 equipments
   * は本 test では未使用なのでスタブ ({@link IllegalStateException} を投げる) を渡す。
   *
   * <p>こうすることで「equipments resolver は今回の variant ロジックで使われない」ことを構造的に保証する (誤って equipments を呼んだ瞬間に
   * テストが失敗する safety net)。
   */
  private static NodeResolveContext testContext() {
    return new NodeResolveContext(
        InitialStateFactory::resolveCard, TestContextHelpers::unusedEquipment);
  }

  /** equipments resolver は本テストでは未使用、呼ばれたら fail させる stub。 */
  private static final class TestContextHelpers {
    static Equipment unusedEquipment(EquipmentId id) {
      throw new IllegalStateException(
          "equipment resolver should not be invoked in LayerEndNodeTest; got id=" + id.value());
    }
  }

  // ---------------- HpMaxUp ----------------

  @Test
  void hpMaxUpRaisesBothCurrentAndMaxHp() {
    // §15-8 「Slay the Spire の永続バフ型」: max を上げると同時に current も同量上がり、上限張り付きが起きない
    Player p = initialPlayer();
    int beforeCurrent = p.stats().currentHp();
    int beforeMax = p.stats().maxHp();

    Player up = new LayerEndNode.HpMaxUp(5).apply(p, testContext());

    assertEquals(beforeCurrent + 5, up.stats().currentHp(), "currentHp が +5");
    assertEquals(beforeMax + 5, up.stats().maxHp(), "maxHp が +5");
    assertEquals(p.stats().speed(), up.stats().speed(), "速度は変化しない");
  }

  @Test
  void hpMaxUpReturnsNewInstanceLeavingOriginalIntact() {
    // 純関数: 元 Player は変化しない
    Player p = initialPlayer();
    Stats originalStats = p.stats();

    Player up = new LayerEndNode.HpMaxUp(3).apply(p, testContext());

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
    assertEquals("HP +5", new LayerEndNode.HpMaxUp(5).displayName(testContext()));
  }

  // ---------------- SpeedUp ----------------

  @Test
  void speedUpRaisesSpeedOnly() {
    // §15-4: 速度 = 1 ターン AP 量。他ステは不変
    Player p = initialPlayer();
    int beforeSpeed = p.stats().speed();
    int beforeHp = p.stats().currentHp();
    int beforeMax = p.stats().maxHp();

    Player up = new LayerEndNode.SpeedUp(1).apply(p, testContext());

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
    assertEquals("速度 +1", new LayerEndNode.SpeedUp(1).displayName(testContext()));
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

    Player rested = new LayerEndNode.Rest().apply(damaged, testContext());

    assertEquals(damaged.stats().maxHp(), rested.stats().currentHp(), "current が max まで回復");
    assertEquals(damaged.stats().maxHp(), rested.stats().maxHp(), "maxHp は変化しない");
  }

  @Test
  void restAtFullHpIsIdempotent() {
    // 既に max の場合、Rest を適用しても max を超えない (healed の上限保証)
    Player p = initialPlayer();

    Player rested = new LayerEndNode.Rest().apply(p, testContext());

    assertEquals(p.stats().maxHp(), rested.stats().currentHp(), "current は maxHp のまま");
  }

  @Test
  void restDisplayNameIsFixedString() {
    assertEquals("HP 全回復", new LayerEndNode.Rest().displayName(testContext()));
  }

  // ---------------- sealed 網羅性 ----------------

  @Test
  void allPermitsAreReachableViaPatternSwitch() {
    // §15-8 / E-6: sealed permits の全 5 種が pattern switch で網羅的に処理できることを確認
    // Wave 3 Task A: Shop は CardId 保持に変更。網羅性チェックは ID で構築する。
    LayerEndNode[] allKinds =
        new LayerEndNode[] {
          new LayerEndNode.HpMaxUp(1),
          new LayerEndNode.SpeedUp(1),
          new LayerEndNode.Rest(),
          new LayerEndNode.Shop(5, sampleCardId()),
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
    assertThrows(IllegalArgumentException.class, () -> new LayerEndNode.Shop(-1, sampleCardId()));
  }

  @Test
  void shopRejectsNullCardId() {
    // Wave 3 Task A: 旧 'grantedCard' → 'cardId' に名前変更
    assertThrows(NullPointerException.class, () -> new LayerEndNode.Shop(5, null));
  }

  @Test
  void shopConsumesGoldAndAddsCardWhenSufficient() {
    Player p = initialPlayer().addGold(new core.domain.meta.Gold(10));
    int deckSizeBefore =
        p.cardPileState().drawPile().size()
            + p.cardPileState().hand().size()
            + p.cardPileState().discardPile().size();

    Player after = new LayerEndNode.Shop(5, sampleCardId()).apply(p, testContext());

    assertEquals(5, after.gold().amount(), "10 - 5 = 5");
    int deckSizeAfter =
        after.cardPileState().drawPile().size()
            + after.cardPileState().hand().size()
            + after.cardPileState().discardPile().size();
    assertEquals(deckSizeBefore + 1, deckSizeAfter, "DrawPile に Card 1 枚追加");
    // 追加されたカードが resolver の解決結果と一致 (CardId 経由解決の検証)
    Card lastAdded =
        after.cardPileState().drawPile().cards().get(after.cardPileState().drawPile().size() - 1);
    assertEquals(sampleCard().id(), lastAdded.id(), "追加カードの id が resolver の解決結果と一致");
  }

  @Test
  void shopSilentFailWhenInsufficientGold() {
    Player p = initialPlayer(); // gold 0
    Player after = new LayerEndNode.Shop(5, sampleCardId()).apply(p, testContext());
    // Gold 不足: apply は引数の Player をそのまま返す (silent fail)
    assertEquals(p.gold().amount(), after.gold().amount(), "Gold 不変");
    assertEquals(p.cardPileState(), after.cardPileState(), "デッキ不変");
  }

  @Test
  void shopDisplayNameIncludesCardAndCost() {
    String label = new LayerEndNode.Shop(5, sampleCardId()).displayName(testContext());
    assertTrue(label.contains("ショップ"));
    assertTrue(label.contains("5"));
    // 本物のカード名を resolver 経由で解決していることを確認
    assertTrue(label.contains(sampleCard().displayName()), "Shop displayName にカード名が含まれる");
  }

  @Test
  void shopApplyRequiresNonNullContext() {
    Player p = initialPlayer().addGold(new core.domain.meta.Gold(10));
    assertThrows(
        NullPointerException.class, () -> new LayerEndNode.Shop(5, sampleCardId()).apply(p, null));
  }

  @Test
  void shopDisplayNameRequiresNonNullContext() {
    assertThrows(
        NullPointerException.class,
        () -> new LayerEndNode.Shop(5, sampleCardId()).displayName(null));
  }

  // ---------------- Event ----------------

  @Test
  void eventAppliesSoulAndHpAndGoldDeltas() {
    Player p = initialPlayer();
    int hpBefore = p.stats().currentHp();
    int soulBefore = p.soul().amount();

    Player after = new LayerEndNode.Event(30, -5, 10, "ソウルの祠").apply(p, testContext());

    assertEquals(soulBefore + 30, after.soul().amount());
    assertEquals(hpBefore - 5, after.stats().currentHp());
    assertEquals(10, after.gold().amount());
  }

  @Test
  void eventWithPositiveHpHeals() {
    Player p = initialPlayer().withStats(initialPlayer().stats().damaged(10));
    int hpBeforeHeal = p.stats().currentHp();

    Player after = new LayerEndNode.Event(0, 7, 0, "回復イベント").apply(p, testContext());

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
