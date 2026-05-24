package core.domain.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** {@link StatsBonus} の単体テスト (§15-9 / ADR-25)。 */
class StatsBonusTest {

  @Test
  void zeroReturnsAllZeroBonus() {
    StatsBonus z = StatsBonus.zero();
    assertEquals(0, z.maxHp());
    assertEquals(0, z.speed());
    assertEquals(0, z.physicalAttack());
    assertEquals(0, z.magicalAttack());
    assertEquals(0, z.physicalDefense());
    assertEquals(0, z.magicalDefense());
  }

  @Test
  void zeroReturnsSharedInstance() {
    // zero() は cached instance を返す (record 構造的等価で確認、参照同一性も保証)
    assertSame(StatsBonus.zero(), StatsBonus.zero());
  }

  @Test
  void plusCombinesAllSixFields() {
    StatsBonus a = new StatsBonus(5, 1, 2, 3, 4, 5);
    StatsBonus b = new StatsBonus(10, 2, 1, 0, -1, 2);

    StatsBonus sum = a.plus(b);

    assertEquals(15, sum.maxHp());
    assertEquals(3, sum.speed());
    assertEquals(3, sum.physicalAttack());
    assertEquals(3, sum.magicalAttack());
    assertEquals(3, sum.physicalDefense());
    assertEquals(7, sum.magicalDefense());
  }

  @Test
  void plusWithZeroIsIdentity() {
    StatsBonus a = new StatsBonus(5, 1, 2, 3, 4, 5);
    assertEquals(a, a.plus(StatsBonus.zero()));
    assertEquals(a, StatsBonus.zero().plus(a));
  }

  @Test
  void plusReturnsNewInstanceLeavingOperandsIntact() {
    StatsBonus a = new StatsBonus(5, 1, 2, 3, 4, 5);
    StatsBonus b = new StatsBonus(1, 1, 1, 1, 1, 1);

    StatsBonus sum = a.plus(b);

    assertEquals(5, a.maxHp(), "左辺は変化しない");
    assertEquals(1, b.maxHp(), "右辺は変化しない");
    assertEquals(6, sum.maxHp());
  }

  @Test
  void negativeFieldsAreAllowedForCurseOrDebuffSemantics() {
    // 「呪い装備」「デバフ」を同型で表現する余地 (StatsBonus 自体はコンストラクタ制約なし)
    StatsBonus curse = new StatsBonus(-5, -1, -1, 0, 0, 0);
    assertEquals(-5, curse.maxHp());
    assertEquals(-1, curse.speed());
  }

  // Phase 1 回帰: plus(null) で NPE (メッセージ "other" 付き) を投げる
  @Test
  void plusNullThrowsNullPointerExceptionWithMessage() {
    StatsBonus a = new StatsBonus(1, 1, 1, 1, 1, 1);
    NullPointerException ex = assertThrows(NullPointerException.class, () -> a.plus(null));
    assertEquals("other", ex.getMessage(), "Objects.requireNonNull の引数名がメッセージに現れる");
  }
}
