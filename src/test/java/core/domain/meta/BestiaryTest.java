package core.domain.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.entity.EnemyKind;
import org.junit.jupiter.api.Test;

/** {@link Bestiary} の不変・撃破記録・冪等性を検証する (§15-5 / E-7 雛形)。 */
class BestiaryTest {

  @Test
  void emptyHasZeroSize() {
    Bestiary b = Bestiary.empty();
    assertEquals(0, b.size());
  }

  @Test
  void withDefeatedRegistersKind() {
    Bestiary b = Bestiary.empty().withDefeated(EnemyKind.SLIME);
    assertTrue(b.isRegistered(EnemyKind.SLIME));
    assertEquals(1, b.size());
  }

  @Test
  void isRegisteredFalseForUnknownKind() {
    Bestiary b = Bestiary.empty().withDefeated(EnemyKind.SLIME);
    assertFalse(b.isRegistered(EnemyKind.BOSS));
  }

  @Test
  void withDefeatedReturnsNewInstance() {
    Bestiary original = Bestiary.empty();
    Bestiary added = original.withDefeated(EnemyKind.ELITE_SLIME);
    assertNotSame(original, added);
    assertEquals(0, original.size(), "元の Bestiary は不変");
  }

  @Test
  void withDefeatedIsIdempotentForSameKind() {
    // 同じ敵種を 2 回撃破しても size は 1 (重複登録しない)。
    Bestiary b = Bestiary.empty().withDefeated(EnemyKind.SLIME).withDefeated(EnemyKind.SLIME);
    assertEquals(1, b.size());
  }

  @Test
  void withDefeatedSameKindReturnsSameInstance() {
    Bestiary b = Bestiary.empty().withDefeated(EnemyKind.SLIME);
    Bestiary again = b.withDefeated(EnemyKind.SLIME);
    assertSame(b, again, "既登録の敵種は同一インスタンスを返す (オブジェクト生成節約)");
  }

  @Test
  void withDefeatedAccumulatesMultipleKinds() {
    Bestiary b =
        Bestiary.empty()
            .withDefeated(EnemyKind.SLIME)
            .withDefeated(EnemyKind.SWIFT_SLIME)
            .withDefeated(EnemyKind.BOSS);
    assertEquals(3, b.size());
    assertTrue(b.isRegistered(EnemyKind.SLIME));
    assertTrue(b.isRegistered(EnemyKind.SWIFT_SLIME));
    assertTrue(b.isRegistered(EnemyKind.BOSS));
  }

  @Test
  void constructorRejectsNullSet() {
    assertThrows(NullPointerException.class, () -> new Bestiary(null));
  }

  @Test
  void withDefeatedRejectsNullKind() {
    Bestiary b = Bestiary.empty();
    assertThrows(NullPointerException.class, () -> b.withDefeated(null));
  }
}
