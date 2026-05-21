package core.domain.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import core.domain.card.CardElement;
import org.junit.jupiter.api.Test;

/** {@link SkillEffect.Damage} の属性 (ADR-17 改訂) とコンストラクタ検証。 */
class SkillEffectTest {

  @Test
  void singleArgConstructorDefaultsToPhysical() {
    // 既存呼出互換: 1 引数コンストラクタは element = PHYSICAL を既定にする。
    SkillEffect.Damage dmg = new SkillEffect.Damage(5);
    assertEquals(5, dmg.amount());
    assertEquals(CardElement.PHYSICAL, dmg.element(), "1 引数コンストラクタは PHYSICAL 既定");
  }

  @Test
  void twoArgConstructorKeepsGivenElement() {
    SkillEffect.Damage dmg = new SkillEffect.Damage(8, CardElement.MAGICAL);
    assertEquals(8, dmg.amount());
    assertEquals(CardElement.MAGICAL, dmg.element());
  }

  @Test
  void negativeAmountRejected() {
    assertThrows(IllegalArgumentException.class, () -> new SkillEffect.Damage(-1));
    assertThrows(
        IllegalArgumentException.class, () -> new SkillEffect.Damage(-1, CardElement.MAGICAL));
  }

  @Test
  void nullElementRejected() {
    assertThrows(NullPointerException.class, () -> new SkillEffect.Damage(5, null));
  }

  @Test
  void zeroAmountAccepted() {
    // amount=0 は許容 (下限 1 ダメージ保証は TurnEngine 側、本 record は値の保持のみ)。
    SkillEffect.Damage dmg = new SkillEffect.Damage(0);
    assertEquals(0, dmg.amount());
  }
}
