package core.presentation.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.Test;

/** {@link DamagePopup} の単体テスト (§15-5 / E-8)。 */
class DamagePopupTest {

  private static DamagePopup fresh() {
    return new DamagePopup(100f, 200f, 5, 0f, Color.WHITE);
  }

  // ---------------- compact constructor ----------------

  @Test
  void compactConstructorRejectsNegativeAge() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new DamagePopup(0f, 0f, 1, -0.01f, Color.WHITE));
  }

  @Test
  void compactConstructorRejectsNullColor() {
    assertThrows(NullPointerException.class, () -> new DamagePopup(0f, 0f, 1, 0f, null));
  }

  @Test
  void compactConstructorAcceptsZeroAge() {
    DamagePopup p = new DamagePopup(0f, 0f, 1, 0f, Color.WHITE);
    assertEquals(0f, p.ageSeconds());
  }

  @Test
  void compactConstructorAcceptsZeroAmount() {
    // amount=0 は「ガード成功 = 0 ダメージ」表示として将来使える可能性 (本 PR では発火しない)
    DamagePopup p = new DamagePopup(0f, 0f, 0, 0f, Color.WHITE);
    assertEquals(0, p.amount());
  }

  // ---------------- advanced ----------------

  @Test
  void advancedAccumulatesAge() {
    DamagePopup p = fresh().advanced(0.1f).advanced(0.2f);
    assertEquals(0.3f, p.ageSeconds(), 0.0001f);
  }

  @Test
  void advancedReturnsNewInstanceLeavingOriginalIntact() {
    DamagePopup original = fresh();
    DamagePopup next = original.advanced(0.1f);
    assertNotSame(original, next);
    assertEquals(0f, original.ageSeconds(), "元 popup は変化しない");
  }

  @Test
  void advancedRejectsNegativeDelta() {
    assertThrows(IllegalArgumentException.class, () -> fresh().advanced(-0.01f));
  }

  @Test
  void advancedZeroDeltaIsIdentity() {
    DamagePopup p = fresh().advanced(0f);
    assertEquals(0f, p.ageSeconds());
  }

  // ---------------- isExpired ----------------

  @Test
  void isExpiredFalseBelowDuration() {
    assertFalse(fresh().isExpired());
    assertFalse(fresh().advanced(DamagePopup.DURATION - 0.01f).isExpired());
  }

  @Test
  void isExpiredTrueAtAndPastDuration() {
    assertTrue(fresh().advanced(DamagePopup.DURATION).isExpired());
    assertTrue(fresh().advanced(DamagePopup.DURATION + 1f).isExpired());
  }

  // ---------------- alpha ----------------

  @Test
  void alphaIsOneAtAgeZero() {
    assertEquals(1f, fresh().alpha(), 0.0001f);
  }

  @Test
  void alphaIsHalfAtHalfDuration() {
    assertEquals(0.5f, fresh().advanced(DamagePopup.DURATION / 2f).alpha(), 0.0001f);
  }

  @Test
  void alphaIsZeroAtAndPastDuration() {
    assertEquals(0f, fresh().advanced(DamagePopup.DURATION).alpha(), 0.0001f);
    assertEquals(0f, fresh().advanced(DamagePopup.DURATION + 5f).alpha(), 0.0001f);
  }

  // ---------------- currentY ----------------

  @Test
  void currentYStartsAtWorldY() {
    DamagePopup p = new DamagePopup(0f, 200f, 1, 0f, Color.WHITE);
    assertEquals(200f, p.currentY(), 0.0001f);
  }

  @Test
  void currentYRisesProportionalToAge() {
    DamagePopup p = new DamagePopup(0f, 200f, 1, 0f, Color.WHITE);
    DamagePopup later = p.advanced(0.5f);
    // 0.5 秒 × 60 px/秒 = 30 px 上昇
    assertEquals(200f + 30f, later.currentY(), 0.0001f);
  }

  @Test
  void currentYAtFullDurationEqualsFullRise() {
    DamagePopup p = new DamagePopup(0f, 200f, 1, 0f, Color.WHITE);
    DamagePopup atEnd = p.advanced(DamagePopup.DURATION);
    assertEquals(200f + DamagePopup.RISE_VELOCITY * DamagePopup.DURATION, atEnd.currentY(), 0.0001f);
  }
}
