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

  // Wave 11 W11-β: rarity Optional 正規化 + rarityOrDefault()

  @Test
  void legacyConstructorDefaultsRarityToEmpty() {
    // 6 引数の後方互換コンストラクタは rarity = Optional.empty() で初期化
    Card card = DomainFixtures.attackCard("legacy-001");
    assertEquals(java.util.Optional.empty(), card.rarity());
    assertEquals(CardRarity.COMMON, card.rarityOrDefault());
  }

  @Test
  void explicitRarityIsPreserved() {
    Card card =
        new Card(
            CardId.of("rare-001"),
            "レア",
            2,
            CardTag.ATTACK,
            CardElement.MAGICAL,
            new CardEffect.Damage(8),
            java.util.Optional.of(CardRarity.RARE));
    assertEquals(java.util.Optional.of(CardRarity.RARE), card.rarity());
    assertEquals(CardRarity.RARE, card.rarityOrDefault());
  }

  @Test
  void nullRarityIsNormalizedToEmpty() {
    // 7 引数コンストラクタに null を渡しても Optional.empty() に正規化される
    Card card =
        new Card(
            CardId.of("null-rarity"),
            "名無し",
            1,
            CardTag.ATTACK,
            CardElement.PHYSICAL,
            new CardEffect.Damage(2),
            null);
    assertEquals(java.util.Optional.empty(), card.rarity());
    assertEquals(CardRarity.COMMON, card.rarityOrDefault());
  }
}
