package core.domain.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import core.domain.support.DomainFixtures;
import org.junit.jupiter.api.Test;

/**
 * Card の不変条件テスト。
 *
 * <p>ADR-16 検証ポイント 1: apCost の境界検証。 GAME_DESIGN §15-3: AP コストは 1 以上 (0 コストカードはターン終了条件と相性が悪いため禁止)。
 */
class CardTest {

  // ハッピーパス

  @Test
  void apCostOneIsMinimumValid() {
    // 検証ポイント 1: apCost >= 1 で成功
    Card card = DomainFixtures.attackCard("test-001");
    assertEquals(1, card.apCost());
  }

  @Test
  void apCostAboveOneIsAccepted() {
    Card card =
        new Card(
            CardId.of("heavy-001"),
            "強斬撃",
            3,
            CardTag.ATTACK,
            CardElement.PHYSICAL,
            new CardEffect.Damage(10));
    assertEquals(3, card.apCost());
  }

  // 境界値

  @Test
  void apCostZeroThrowsIllegalArgumentException() {
    // 検証ポイント 1: apCost = 0 で例外
    // GAME_DESIGN §15-3: AP コスト 0 は禁止
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Card(
                CardId.of("zero-cost"),
                "ゼロコスト",
                0,
                CardTag.ATTACK,
                CardElement.PHYSICAL,
                new CardEffect.Damage(1)));
  }

  @Test
  void apCostNegativeThrowsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Card(
                CardId.of("negative-cost"),
                "マイナスコスト",
                -1,
                CardTag.ATTACK,
                CardElement.PHYSICAL,
                new CardEffect.Damage(1)));
  }

  // null / blank 検証

  @Test
  void nullIdThrowsNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () ->
            new Card(
                null, "有効名", 1, CardTag.ATTACK, CardElement.PHYSICAL, new CardEffect.Damage(1)));
  }

  @Test
  void blankDisplayNameThrowsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Card(
                CardId.of("blank-name"),
                "   ",
                1,
                CardTag.ATTACK,
                CardElement.PHYSICAL,
                new CardEffect.Damage(1)));
  }

  @Test
  void nullEffectThrowsNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () ->
            new Card(
                CardId.of("null-effect"), "有効名", 1, CardTag.ATTACK, CardElement.PHYSICAL, null));
  }
}
