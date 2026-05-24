package core.domain.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
   * は本 test では未使用な variant 向けにスタブ ({@link IllegalStateException} を投げる) を渡す。
   *
   * <p>こうすることで「equipments resolver は今回の variant ロジックで使われない」ことを構造的に保証する (誤って equipments を呼んだ瞬間に
   * テストが失敗する safety net)。
   *
   * <p>Wave 3 Task B: {@link LayerEndNode.ShopEquipment} は equipments resolver を実際に使うため、 別途 {@link
   * #equipmentContext()} を用意する。
   */
  private static NodeResolveContext testContext() {
    return new NodeResolveContext(
        InitialStateFactory::resolveCard, TestContextHelpers::unusedEquipment);
  }

  /**
   * Wave 3 Task B: {@link LayerEndNode.ShopEquipment} 用の {@link NodeResolveContext}。equipments
   * resolver は {@link InitialStateFactory#equipmentCatalog()} の {@code get} に委譲する (本物のマスタを使うことで
   * displayName が実装現実と一致することを検証できる)。
   */
  private static NodeResolveContext equipmentContext() {
    return new NodeResolveContext(
        InitialStateFactory::resolveCard, id -> InitialStateFactory.equipmentCatalog().get(id));
  }

  /** equipment.json の先頭装備 ID (ShopEquipment テストでは displayName 検証に使う)。 */
  private static EquipmentId sampleEquipmentId() {
    return InitialStateFactory.equipmentCatalog().all().get(0).id();
  }

  /** 先頭装備の本物 displayName (resolver 経由解決の検証用)。 */
  private static String sampleEquipmentDisplayName() {
    return InitialStateFactory.equipmentCatalog().all().get(0).displayName();
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

  // Wave 8 W8-α: displayName は domain 層から撤去。ラベル検証は LayerEndNodeLabelsTest に移動。

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

  // ---------------- Rest ----------------

  @Test
  void restHealsCurrentHpByThirty() {
    // Wave 15 W15-γ / #2: 休憩ノードは maxHp の 30% (= maxHp / 3 切り上げ) 回復に制限
    // (旧仕様: 全回復 → ノード選択の意思決定崩壊、新仕様: 割合回復で 4 ノード択が成立)
    Player p = initialPlayer();
    int maxHp = p.stats().maxHp();
    // current を 1 まで削った状態 (回復量の最大効果を観測しやすく)
    Stats damagedStats = p.stats().damaged(p.stats().currentHp() - 1);
    Player damaged = p.withStats(damagedStats);
    int expectedHeal = Math.max(1, maxHp / 3);

    Player rested = new LayerEndNode.Rest().apply(damaged, testContext());

    assertEquals(
        1 + expectedHeal, rested.stats().currentHp(), "current が maxHp / 3 増える (上限は maxHp)");
    assertEquals(maxHp, rested.stats().maxHp(), "maxHp は変化しない");
  }

  @Test
  void restAtFullHpIsIdempotent() {
    // 既に max の場合、Rest を適用しても max を超えない (healed の上限保証)
    Player p = initialPlayer();

    Player rested = new LayerEndNode.Rest().apply(p, testContext());

    assertEquals(p.stats().maxHp(), rested.stats().currentHp(), "current は maxHp のまま");
  }

  // ---------------- sealed 網羅性 ----------------

  @Test
  void allPermitsAreReachableViaPatternSwitch() {
    // §15-8 / E-6: sealed permits の全 6 種が pattern switch で網羅的に処理できることを確認
    // Wave 3 Task A: Shop は CardId 保持に変更。網羅性チェックは ID で構築する。
    // Wave 3 Task B: ShopEquipment を permits に追加、case 追加で網羅性検証。
    LayerEndNode[] allKinds =
        new LayerEndNode[] {
          new LayerEndNode.HpMaxUp(1),
          new LayerEndNode.SpeedUp(1),
          new LayerEndNode.Rest(),
          new LayerEndNode.Shop(5, sampleCardId()),
          // Wave 8 W8-α: Event は EventKind 必須化、of() factory で構築
          LayerEndNode.Event.of(core.domain.layer.EventKind.SOUL_SHRINE),
          new LayerEndNode.ShopEquipment(15, sampleEquipmentId())
        };
    for (LayerEndNode node : allKinds) {
      String tag =
          switch (node) {
            case LayerEndNode.HpMaxUp ignored -> "hp";
            case LayerEndNode.SpeedUp ignored -> "speed";
            case LayerEndNode.Rest ignored -> "rest";
            case LayerEndNode.Shop ignored -> "shop";
            case LayerEndNode.Event ignored -> "event";
            case LayerEndNode.ShopEquipment ignored -> "shopEquipment";
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

  // Wave 8 W8-α: displayName は domain 層から撤去。Shop ラベル検証は LayerEndNodeLabelsTest に移動。

  @Test
  void shopApplyRequiresNonNullContext() {
    Player p = initialPlayer().addGold(new core.domain.meta.Gold(10));
    assertThrows(
        NullPointerException.class, () -> new LayerEndNode.Shop(5, sampleCardId()).apply(p, null));
  }

  // ---------------- Event (Wave 8 W8-α: EventKind 必須化) ----------------

  @Test
  void eventOfHealingSpringConstructsWithMatchingDeltas() {
    LayerEndNode.Event healing = LayerEndNode.Event.of(core.domain.layer.EventKind.HEALING_SPRING);
    assertEquals(-10, healing.soulDelta());
    assertEquals(20, healing.hpDelta());
    assertEquals(0, healing.goldDelta());
    assertEquals(core.domain.layer.EventKind.HEALING_SPRING, healing.kind());
  }

  @Test
  void eventOfGoldenChestAppliesGoldDelta() {
    Player p = initialPlayer();
    LayerEndNode.Event chest = LayerEndNode.Event.of(core.domain.layer.EventKind.GOLDEN_CHEST);
    Player after = chest.apply(p, testContext());
    assertEquals(50, after.gold().amount(), "黄金の宝箱で Gold +50");
  }

  @Test
  void eventOfSoulShrineAppliesSoulAndHpDeltas() {
    Player p = initialPlayer();
    int hpBefore = p.stats().currentHp();
    int soulBefore = p.soul().amount();

    Player after =
        LayerEndNode.Event.of(core.domain.layer.EventKind.SOUL_SHRINE).apply(p, testContext());

    assertEquals(soulBefore + 30, after.soul().amount(), "ソウル +30");
    assertEquals(hpBefore - 5, after.stats().currentHp(), "HP -5");
  }

  @Test
  void eventOfHealingSpringHealsHp() {
    // 治療の泉は HP +20 (HEALING_SPRING)、ただしソウル -10 のため player の Soul に 10 以上が必要
    Player p = initialPlayer().addSoul(new core.domain.meta.Soul(20));
    Player damaged = p.withStats(p.stats().damaged(15));
    int hpBeforeHeal = damaged.stats().currentHp();
    int maxHp = damaged.stats().maxHp();

    Player after =
        LayerEndNode.Event.of(core.domain.layer.EventKind.HEALING_SPRING)
            .apply(damaged, testContext());

    // Stats.healed は maxHp で上限キャップされるため min を取る
    assertEquals(
        Math.min(maxHp, hpBeforeHeal + 20), after.stats().currentHp(), "HP +20 (max cap 適用)");
    assertEquals(10, after.soul().amount(), "ソウル 20 → 10 (-10 消費)");
  }

  @Test
  void eventRejectsMismatchedDeltas() {
    // kind と delta が一致しない不整合呼出は IAE で早期検出
    assertThrows(
        IllegalArgumentException.class,
        () -> new LayerEndNode.Event(99, 99, 99, core.domain.layer.EventKind.HEALING_SPRING));
  }

  @Test
  void eventRejectsNullKind() {
    assertThrows(NullPointerException.class, () -> new LayerEndNode.Event(0, 0, 50, null));
  }

  @Test
  void eventOfRejectsNullKind() {
    assertThrows(NullPointerException.class, () -> LayerEndNode.Event.of(null));
  }

  @Test
  void eventSilentFailWhenInsufficientSoul() {
    // HEALING_SPRING (soulDelta=-10) で player のソウル不足時は silent fail (player そのまま返す)
    Player p = initialPlayer();
    assertEquals(0, p.soul().amount(), "前提: 初期 player の soul は 0");
    LayerEndNode.Event healing = LayerEndNode.Event.of(core.domain.layer.EventKind.HEALING_SPRING);
    Player after = healing.apply(p, testContext());
    assertSame(p, after, "soul 不足時は player をそのまま返す");
  }

  // ---------------- ShopEquipment (Wave 3 Task B) ----------------

  @Test
  void shopEquipmentRejectsNegativeGoldCost() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new LayerEndNode.ShopEquipment(-1, sampleEquipmentId()));
  }

  @Test
  void shopEquipmentRejectsNullEquipmentId() {
    assertThrows(NullPointerException.class, () -> new LayerEndNode.ShopEquipment(15, null));
  }

  @Test
  void shopEquipmentConsumesGoldWhenSufficient() {
    // Wave 3 Task B: apply は Gold 消費のみ行う純関数 (loadout 装着は presentation 層責務)。
    Player p = initialPlayer().addGold(new core.domain.meta.Gold(20));
    Player after =
        new LayerEndNode.ShopEquipment(15, sampleEquipmentId()).apply(p, equipmentContext());
    assertEquals(5, after.gold().amount(), "20 - 15 = 5");
    // Player 自体は装備を保持しないため、デッキ / Stats は不変であることを確認
    assertEquals(p.cardPileState(), after.cardPileState(), "デッキは ShopEquipment.apply で変化しない");
    assertEquals(p.stats(), after.stats(), "Stats は ShopEquipment.apply で変化しない");
  }

  @Test
  void shopEquipmentSilentFailWhenInsufficientGold() {
    Player p = initialPlayer(); // gold 0
    Player after =
        new LayerEndNode.ShopEquipment(15, sampleEquipmentId()).apply(p, equipmentContext());
    // Gold 不足: apply は引数の Player をそのまま返す (silent fail、Shop と同型)
    assertEquals(p.gold().amount(), after.gold().amount(), "Gold 不変");
    assertEquals(p, after, "Player 全体が不変");
  }

  // Wave 8 W8-α: displayName 検証は LayerEndNodeLabelsTest に移動。

  @Test
  void shopEquipmentApplyRequiresNonNullContext() {
    Player p = initialPlayer().addGold(new core.domain.meta.Gold(20));
    assertThrows(
        NullPointerException.class,
        () -> new LayerEndNode.ShopEquipment(15, sampleEquipmentId()).apply(p, null));
  }

  @Test
  void shopEquipmentZeroGoldCostIsAllowed() {
    // 無料装備購入も将来の Event 報酬等で使う想定 (goldCost >= 0 は許可、コンストラクタは IAE を投げない)
    Player p = initialPlayer();
    Player after =
        new LayerEndNode.ShopEquipment(0, sampleEquipmentId()).apply(p, equipmentContext());
    assertEquals(p.gold().amount(), after.gold().amount(), "Gold 不変");
    assertEquals(p, after, "Player は変化しない (goldCost 0 ノードは spendGold をスキップ)");
  }
}
