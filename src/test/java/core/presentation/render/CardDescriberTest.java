package core.presentation.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import core.domain.card.Card;
import core.domain.card.CardEffect;
import core.domain.card.CardElement;
import core.domain.card.CardId;
import core.domain.card.CardTag;
import core.domain.card.TrapLifetime;
import org.junit.jupiter.api.Test;

/** {@link CardDescriber} が 4 効果種別を正しく日本語説明に変換することの検証。 */
class CardDescriberTest {

  private static Card card(CardTag tag, CardElement element, CardEffect effect) {
    return new Card(CardId.of("t"), "テスト", 1, tag, element, effect);
  }

  @Test
  void describesPhysicalDamage() {
    assertEquals(
        "物理ダメージ 5",
        CardDescriber.describe(
            card(CardTag.ATTACK, CardElement.PHYSICAL, new CardEffect.Damage(5))));
  }

  @Test
  void describesMagicalDamage() {
    assertEquals(
        "魔法ダメージ 3",
        CardDescriber.describe(
            card(CardTag.ATTACK, CardElement.MAGICAL, new CardEffect.Damage(3))));
  }

  @Test
  void describesMove() {
    assertEquals(
        "3 マス移動",
        CardDescriber.describe(
            card(CardTag.MOVEMENT, CardElement.PHYSICAL, new CardEffect.Move(3))));
  }

  @Test
  void describesBuff() {
    assertEquals(
        "物攻 +3 (2T)",
        CardDescriber.describe(
            card(
                CardTag.BUFF,
                CardElement.PHYSICAL,
                new CardEffect.Buff(CardEffect.BuffKind.PHYSICAL_ATTACK_UP, 3, 2))));
  }

  @Test
  void describesNegativeBuff() {
    assertEquals(
        "速度 -2 (3T)",
        CardDescriber.describe(
            card(
                CardTag.BUFF,
                CardElement.MAGICAL,
                new CardEffect.Buff(CardEffect.BuffKind.SPEED_UP, -2, 3))));
  }

  @Test
  void describesTrapUntilStepped() {
    assertEquals(
        "罠 物理 4 / 踏むまで",
        CardDescriber.describe(
            card(
                CardTag.TRAP,
                CardElement.PHYSICAL,
                new CardEffect.Trap(4, TrapLifetime.UntilStepped.INSTANCE))));
  }

  @Test
  void describesTrapTurns() {
    assertEquals(
        "罠 魔法 5 / 3T 残",
        CardDescriber.describe(
            card(
                CardTag.TRAP,
                CardElement.MAGICAL,
                new CardEffect.Trap(5, new TrapLifetime.Turns(3)))));
  }
}
