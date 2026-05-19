package core.infrastructure.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.card.CardId;
import core.domain.common.Position;
import core.domain.entity.Player;
import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentSlot;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * §15-9 / ADR-26 完全実装の証跡テスト: ぼろ靴 + ぼろい短剣を初期装備として装着し、{@code grantedCards} から
 * 初期デッキを動的生成する事を確認する。
 */
class InitialStateFactoryEquipmentTest {

  // ---------------- 装備マスタ ----------------

  @Test
  void tatteredBootsHasCorrectSpec() {
    // docs/templates/equipments.json の tattered_boots エントリ準拠
    Equipment boots = InitialStateFactory.tatteredBoots();
    assertEquals("tattered_boots", boots.id().value());
    assertEquals("ぼろ靴", boots.displayName());
    assertEquals(EquipmentSlot.FEET, boots.slot());
    assertEquals(1, boots.statsBonus().speed(), "speed +1");
    assertEquals(0, boots.statsBonus().physicalAttack());
    assertEquals(1, boots.grantedCards().size(), "grantedCards に dash 1 枚");
    assertEquals(CardId.of("dash"), boots.grantedCards().get(0));
  }

  @Test
  void tatteredDaggerHasCorrectSpec() {
    // docs/templates/equipments.json の tattered_dagger エントリ準拠
    Equipment dagger = InitialStateFactory.tatteredDagger();
    assertEquals("tattered_dagger", dagger.id().value());
    assertEquals("ぼろい短剣", dagger.displayName());
    assertEquals(EquipmentSlot.HAND, dagger.slot());
    assertEquals(1, dagger.statsBonus().physicalAttack(), "物攻 +1");
    assertEquals(0, dagger.statsBonus().speed());
    assertEquals(1, dagger.grantedCards().size(), "grantedCards に zangeki 1 枚");
    assertEquals(CardId.of("zangeki"), dagger.grantedCards().get(0));
  }

  // ---------------- newPlayer の装備 + デッキ ----------------

  @Test
  void newPlayerEquipsDaggerOnly() {
    // §15-9 / ADR-30: 1 部位スタート (HAND のみ)。ぼろ靴は Shop/Event で獲得。
    Player p = InitialStateFactory.newPlayer(new Position(1, 1), new Random(42));
    var equipment = p.statuses().equipment();
    assertEquals(1, equipment.size(), "1 部位装備 (§15-9 仕様)");
    assertTrue(equipment.containsKey(EquipmentSlot.HAND), "HAND にぼろい短剣");
    assertFalse(equipment.containsKey(EquipmentSlot.FEET), "FEET は未装備 (Shop で獲得)");
    assertEquals("tattered_dagger", equipment.get(EquipmentSlot.HAND).id().value());
  }

  @Test
  void newPlayerInitialDeckMatchesGrantedCards() {
    // §15-9 / ADR-30: 初期デッキ = ぼろい短剣.grantedCards のみ = [zangeki]
    Player p = InitialStateFactory.newPlayer(new Position(1, 1), new Random(42));
    int totalDeckSize =
        p.cardPileState().drawPile().size()
            + p.cardPileState().hand().size()
            + p.cardPileState().discardPile().size();
    assertEquals(1, totalDeckSize, "デッキ全体は 1 枚 (zangeki のみ)");

    java.util.Set<String> ids = new java.util.HashSet<>();
    p.cardPileState().drawPile().cards().forEach(c -> ids.add(c.id().value()));
    p.cardPileState().hand().cards().forEach(c -> ids.add(c.id().value()));
    p.cardPileState().discardPile().cards().forEach(c -> ids.add(c.id().value()));
    assertTrue(ids.contains("zangeki"), "デッキに zangeki カードが含まれる");
    assertFalse(ids.contains("dash"), "dash は初期デッキに含まれない (Shop 獲得)");
  }

  @Test
  void newPlayerEffectiveStatsReflectEquipmentBonus() {
    // §15-4 / ADR-25 / ADR-30: effectiveStats() で素ステ + 装備補正が合算される。
    // 1 部位スタート (HAND ぼろい短剣のみ) のため speed 補正なし、物攻 +1 のみ。
    Player p = InitialStateFactory.newPlayer(new Position(1, 1), new Random(42));
    var base = p.stats();
    var eff = p.effectiveStats();

    assertEquals(base.speed(), eff.speed(), "ぼろ靴未装備のため speed は素ステ通り");
    assertEquals(base.physicalAttack() + 1, eff.physicalAttack(), "ぼろい短剣 物攻 +1 が反映される");
    assertEquals(base.maxHp(), eff.maxHp(), "maxHp は装備補正なし");
    assertEquals(base.magicalAttack(), eff.magicalAttack(), "魔攻は装備補正なし");
  }

  // ---------------- resolveCard ----------------

  @Test
  void resolveCardSucceedsForAllKnownIds() {
    // dash / zangeki / 既存全カードが解決できる
    assertNotNull(InitialStateFactory.resolveCard(CardId.of("dash")));
    assertNotNull(InitialStateFactory.resolveCard(CardId.of("zangeki")));
    assertNotNull(InitialStateFactory.resolveCard(CardId.of("magic_bolt")));
    assertNotNull(InitialStateFactory.resolveCard(CardId.of("strong_strike")));
    assertNotNull(InitialStateFactory.resolveCard(CardId.of("fireball")));
    assertNotNull(InitialStateFactory.resolveCard(CardId.of("ember_shot")));
    assertNotNull(InitialStateFactory.resolveCard(CardId.of("blaze_nova")));
    assertNotNull(InitialStateFactory.resolveCard(CardId.of("blink_step")));
    assertNotNull(InitialStateFactory.resolveCard(CardId.of("flame_circle")));
  }

  @Test
  void resolveCardRejectsUnknownId() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InitialStateFactory.resolveCard(CardId.of("nonexistent_card_id")));
  }

  @Test
  void resolveCardRejectsNullId() {
    assertThrows(NullPointerException.class, () -> InitialStateFactory.resolveCard(null));
  }

  @Test
  void resolveCardReturnedCardMatchesId() {
    // 解決された Card の id は引数と一致する
    assertEquals("dash", InitialStateFactory.resolveCard(CardId.of("dash")).id().value());
    assertEquals("zangeki", InitialStateFactory.resolveCard(CardId.of("zangeki")).id().value());
  }

  // ---------------- 装備固有カード経由のデッキ整合性 ----------------

  @Test
  void initialDeckIsSubsetOfEquipmentCards() {
    // §15-9: 初期デッキは装備固有カードのみで構成される (ハードコード 4 枚の負債を解消)
    Player p = InitialStateFactory.newPlayer(new Position(1, 1), new Random(42));
    java.util.List<CardId> equipmentCards = p.statuses().equipmentCards();
    java.util.Set<String> deckIds = new java.util.HashSet<>();
    p.cardPileState().drawPile().cards().forEach(c -> deckIds.add(c.id().value()));
    p.cardPileState().hand().cards().forEach(c -> deckIds.add(c.id().value()));
    p.cardPileState().discardPile().cards().forEach(c -> deckIds.add(c.id().value()));

    for (String deckId : deckIds) {
      boolean inEquipmentCards =
          equipmentCards.stream().anyMatch(eq -> eq.value().equals(deckId));
      assertTrue(
          inEquipmentCards, "デッキの全カードは装備固有カードに含まれる: " + deckId);
    }
  }
}
