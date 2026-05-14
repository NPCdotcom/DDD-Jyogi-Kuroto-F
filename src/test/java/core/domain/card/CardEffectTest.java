package core.domain.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * CardEffect sealed 型の各バリアント境界検証と switch 網羅性テスト。
 *
 * <p>ADR-16 検証ポイント 5: sealed switch の全分岐網羅。
 * ADR-16 検証ポイント 9: Damage / Move / Buff / Trap の各 0 値境界。
 * GAME_DESIGN §15-3: CardEffect は sealed で網羅性保証。
 */
class CardEffectTest {

  // =========================================================
  // 検証ポイント 5: sealed switch 網羅性
  // =========================================================

  /**
   * Damage / Move / Buff / Trap の 4 パターンを switch pattern matching で受け、
   * default なしでコンパイルが通ること (= sealed が網羅性を保証していること) の確認。
   *
   * <p>各 case が到達可能であることをアサーションで確認し、トートロジーを回避する。
   */
  @Test
  void sealedSwitchCoversAllFourVariants() {
    // 検証ポイント 5: 4 種類すべての case を実際に通す
    CardEffect damage = new CardEffect.Damage(5);
    CardEffect move   = new CardEffect.Move(2);
    CardEffect buff   = new CardEffect.Buff(CardEffect.BuffKind.SPEED_UP, 2, 1);
    CardEffect trap   = new CardEffect.Trap(3, TrapLifetime.UntilStepped.INSTANCE);

    assertEquals("damage:5",  describe(damage));
    assertEquals("move:2",    describe(move));
    assertEquals("buff:SPEED_UP:2:1", describe(buff));
    assertEquals("trap:3",    describe(trap));
  }

  /**
   * default なしの switch で全分岐を記述 (コンパイル時に sealed 網羅チェックが走る)。
   * 新バリアントが追加されれば、ここがコンパイルエラーになってすぐ検知できる。
   */
  private String describe(CardEffect effect) {
    return switch (effect) {
      case CardEffect.Damage d -> "damage:" + d.baseValue();
      case CardEffect.Move   m -> "move:"   + m.distance();
      case CardEffect.Buff   b -> "buff:"   + b.kind() + ":" + b.amount() + ":" + b.durationTurns();
      case CardEffect.Trap   t -> "trap:"   + t.baseValue();
    };
  }

  // =========================================================
  // 検証ポイント 9: 各バリアントの境界値
  // =========================================================

  // --- Damage ---

  @Test
  void damageBaseValueOneIsMinimumValid() {
    // baseValue 最小値 1 は成功
    CardEffect.Damage d = new CardEffect.Damage(1);
    assertEquals(1, d.baseValue());
  }

  @Test
  void damageBaseValueZeroThrowsIllegalArgumentException() {
    // GAME_DESIGN §15-4: 最低 1 ダメ保証のため baseValue=0 は設計時点で禁止
    assertThrows(IllegalArgumentException.class, () -> new CardEffect.Damage(0));
  }

  @Test
  void damageBaseValueNegativeThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new CardEffect.Damage(-1));
  }

  // --- Move ---

  @Test
  void moveDistanceOneIsMinimumValid() {
    CardEffect.Move m = new CardEffect.Move(1);
    assertEquals(1, m.distance());
  }

  @Test
  void moveDistanceZeroThrowsIllegalArgumentException() {
    // distance=0 は「動かない移動」で無意味 → 設計時点で禁止
    assertThrows(IllegalArgumentException.class, () -> new CardEffect.Move(0));
  }

  @Test
  void moveDistanceNegativeThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new CardEffect.Move(-1));
  }

  // --- Buff ---

  @Test
  void buffWithPositiveAmountIsValid() {
    // amount > 0 (バフ) は成功
    CardEffect.Buff b = new CardEffect.Buff(CardEffect.BuffKind.PHYSICAL_ATTACK_UP, 3, 2);
    assertEquals(3, b.amount());
    assertEquals(2, b.durationTurns());
  }

  @Test
  void buffWithNegativeAmountIsValid() {
    // amount < 0 (デバフとして利用) も仕様上許容 (設計余地として残す)
    CardEffect.Buff debuff = new CardEffect.Buff(CardEffect.BuffKind.PHYSICAL_DEFENSE_UP, -2, 1);
    assertEquals(-2, debuff.amount());
  }

  @Test
  void buffAmountZeroThrowsIllegalArgumentException() {
    // amount=0 は「変化なし」で無意味 → 設計時点で禁止
    assertThrows(
        IllegalArgumentException.class,
        () -> new CardEffect.Buff(CardEffect.BuffKind.SPEED_UP, 0, 1));
  }

  @Test
  void buffDurationTurnsZeroThrowsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new CardEffect.Buff(CardEffect.BuffKind.SPEED_UP, 1, 0));
  }

  @Test
  void buffNullKindThrowsNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () -> new CardEffect.Buff(null, 1, 1));
  }

  // --- Trap ---

  @Test
  void trapBaseValueOneIsMinimumValid() {
    CardEffect.Trap t = new CardEffect.Trap(1, TrapLifetime.UntilStepped.INSTANCE);
    assertEquals(1, t.baseValue());
  }

  @Test
  void trapBaseValueZeroThrowsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new CardEffect.Trap(0, TrapLifetime.UntilStepped.INSTANCE));
  }

  @Test
  void trapNullLifetimeThrowsNullPointerException() {
    assertThrows(NullPointerException.class, () -> new CardEffect.Trap(1, null));
  }
}
