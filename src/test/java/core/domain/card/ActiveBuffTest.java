package core.domain.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** {@link ActiveBuff} の単体テスト (§15-3 / ADR-25 / ADR-27)。 */
class ActiveBuffTest {

  @Test
  void compactConstructorRejectsNullKind() {
    assertThrows(NullPointerException.class, () -> new ActiveBuff(null, 1, 3));
  }

  @Test
  void compactConstructorRejectsZeroAmount() {
    // amount=0 は無意味な Buff (デバフは負値で表現するため 0 だけ弾く)
    assertThrows(
        IllegalArgumentException.class,
        () -> new ActiveBuff(CardEffect.BuffKind.PHYSICAL_ATTACK_UP, 0, 3));
  }

  @Test
  void compactConstructorRejectsNegativeRemainingTurns() {
    assertThrows(
        IllegalArgumentException.class, () -> new ActiveBuff(CardEffect.BuffKind.SPEED_UP, 1, -1));
  }

  @Test
  void compactConstructorAcceptsZeroRemainingTurnsAsExpiredMarker() {
    // remainingTurns=0 は「次の startPlayerTurn で除去」される中間状態として許容 (PlacedTrap.Turns(0) と同型)
    ActiveBuff expired = new ActiveBuff(CardEffect.BuffKind.SPEED_UP, 1, 0);
    assertEquals(0, expired.remainingTurns());
    assertTrue(expired.isExpired());
  }

  @Test
  void decrementedTurnReducesRemainingByOne() {
    ActiveBuff b = new ActiveBuff(CardEffect.BuffKind.PHYSICAL_ATTACK_UP, 2, 3);
    ActiveBuff next = b.decrementedTurn();
    assertEquals(2, next.remainingTurns());
    assertEquals(CardEffect.BuffKind.PHYSICAL_ATTACK_UP, next.kind(), "kind は不変");
    assertEquals(2, next.amount(), "amount は不変");
  }

  @Test
  void decrementedTurnClampsAtZero() {
    ActiveBuff b = new ActiveBuff(CardEffect.BuffKind.SPEED_UP, 1, 0);
    ActiveBuff next = b.decrementedTurn();
    assertEquals(0, next.remainingTurns(), "0 で止まる (負にならない)");
  }

  @Test
  void decrementedTurnReturnsNewInstanceLeavingOriginalIntact() {
    ActiveBuff b = new ActiveBuff(CardEffect.BuffKind.MAGICAL_DEFENSE_UP, 1, 3);
    ActiveBuff next = b.decrementedTurn();
    assertNotSame(b, next);
    assertEquals(3, b.remainingTurns(), "元 ActiveBuff は変化しない");
  }

  @Test
  void isExpiredFalseAboveZero() {
    assertFalse(new ActiveBuff(CardEffect.BuffKind.SPEED_UP, 1, 1).isExpired());
    assertFalse(new ActiveBuff(CardEffect.BuffKind.SPEED_UP, 1, 99).isExpired());
  }

  @Test
  void negativeAmountAllowedForDebuffSemantics() {
    // 負値は「デバフ」を同枠で表現するため許容 (CardEffect.Buff と同設計)
    ActiveBuff debuff = new ActiveBuff(CardEffect.BuffKind.PHYSICAL_ATTACK_UP, -2, 3);
    assertEquals(-2, debuff.amount());
  }
}
