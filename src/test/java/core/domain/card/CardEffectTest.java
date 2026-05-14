package core.domain.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import core.domain.entity.Stats;
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

  // =========================================================
  // Damage.resolve (ADR-17、§15-4 ダメージ計算)
  // =========================================================

  /** 攻防 0 のとき baseValue がそのまま返る (既存 TurnEngineTest と等価)。 */
  @Test
  void damageResolveWithZeroStatsReturnsBaseValue() {
    CardEffect.Damage d = new CardEffect.Damage(5);
    Stats zero = new Stats(10, 10, 1, 0, 0, 0, 0);
    assertEquals(5, d.resolve(zero, zero, CardElement.PHYSICAL));
    assertEquals(5, d.resolve(zero, zero, CardElement.MAGICAL));
  }

  /** PHYSICAL: max(1, baseValue + 物攻 - 物防)。物攻が高ければ増、物防が高ければ減。 */
  @Test
  void damageResolvePhysicalAddsPhysicalAttackAndSubtractsPhysicalDefense() {
    CardEffect.Damage d = new CardEffect.Damage(5);
    Stats attacker = new Stats(10, 10, 1, 3, 0, 0, 0);
    Stats defender = new Stats(10, 10, 1, 0, 0, 2, 0);
    // 5 + 3 - 2 = 6
    assertEquals(6, d.resolve(attacker, defender, CardElement.PHYSICAL));
  }

  /** MAGICAL: max(1, baseValue + 魔攻 - 魔防)。物攻/物防は影響しない。 */
  @Test
  void damageResolveMagicalAddsMagicalAttackAndSubtractsMagicalDefense() {
    CardEffect.Damage d = new CardEffect.Damage(5);
    Stats attacker = new Stats(10, 10, 1, 99, 4, 0, 0);
    Stats defender = new Stats(10, 10, 1, 0, 0, 99, 1);
    // 物理側 99 は無視、5 + 4 - 1 = 8
    assertEquals(8, d.resolve(attacker, defender, CardElement.MAGICAL));
  }

  /** 防御 > 攻撃でも最低 1 ダメージ保証。 */
  @Test
  void damageResolveGuaranteesMinimumOneEvenWhenDefenseExceedsAttack() {
    CardEffect.Damage d = new CardEffect.Damage(3);
    Stats attacker = new Stats(10, 10, 1, 1, 1, 0, 0);
    Stats defender = new Stats(10, 10, 1, 0, 0, 100, 100);
    // 3 + 1 - 100 = -96 → max(1, -96) = 1
    assertEquals(1, d.resolve(attacker, defender, CardElement.PHYSICAL));
    assertEquals(1, d.resolve(attacker, defender, CardElement.MAGICAL));
  }

  /** PHYSICAL と MAGICAL は互いに影響しない (混線防止)。 */
  @Test
  void damageResolvePhysicalIgnoresMagicalStats() {
    CardEffect.Damage d = new CardEffect.Damage(5);
    // 攻撃側に魔攻 99 を持たせても、PHYSICAL 計算には影響しない
    Stats attacker = new Stats(10, 10, 1, 2, 99, 0, 0);
    Stats defender = new Stats(10, 10, 1, 0, 0, 1, 99);
    // 5 + 2 - 1 = 6 (魔攻/魔防は無視)
    assertEquals(6, d.resolve(attacker, defender, CardElement.PHYSICAL));
  }

  @Test
  void damageResolveNullAttackerThrowsNullPointerException() {
    CardEffect.Damage d = new CardEffect.Damage(5);
    Stats s = new Stats(10, 10, 1, 0, 0, 0, 0);
    assertThrows(NullPointerException.class, () -> d.resolve(null, s, CardElement.PHYSICAL));
  }

  @Test
  void damageResolveNullDefenderThrowsNullPointerException() {
    CardEffect.Damage d = new CardEffect.Damage(5);
    Stats s = new Stats(10, 10, 1, 0, 0, 0, 0);
    assertThrows(NullPointerException.class, () -> d.resolve(s, null, CardElement.PHYSICAL));
  }

  @Test
  void damageResolveNullElementThrowsNullPointerException() {
    CardEffect.Damage d = new CardEffect.Damage(5);
    Stats s = new Stats(10, 10, 1, 0, 0, 0, 0);
    assertThrows(NullPointerException.class, () -> d.resolve(s, s, null));
  }
}
