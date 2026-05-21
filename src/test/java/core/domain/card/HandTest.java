package core.domain.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.support.DomainFixtures;
import org.junit.jupiter.api.Test;

/**
 * Hand の不変条件とサイズ境界テスト。
 *
 * <p>ADR-16 検証ポイント 2: MAX_SIZE=9 の境界。 GAME_DESIGN §15-3: 手札の最大枚数は 9。
 */
class HandTest {

  // ハッピーパス

  @Test
  void emptyHandHasSizeZero() {
    assertEquals(0, Hand.empty().size());
  }

  @Test
  void addingCardUpToMaxSizeSucceeds() {
    // 検証ポイント 2: 9 枚目 (MAX_SIZE) まで add は成功する
    Hand h = DomainFixtures.handOfSize(Hand.MAX_SIZE);
    assertEquals(Hand.MAX_SIZE, h.size());
    assertTrue(h.isFull());
  }

  @Test
  void addReturnsNewInstanceLeavingOriginalIntact() {
    // 不変性確認: add は元インスタンスを変更しない
    Hand original = DomainFixtures.handOfSize(1);
    Hand grown = original.add(DomainFixtures.attackCard("extra-001"));
    assertEquals(1, original.size(), "元の Hand は変化してはならない");
    assertEquals(2, grown.size());
    assertNotSame(original, grown);
  }

  // 境界値: MAX_SIZE 到達時の例外

  @Test
  void addToFullHandThrowsIllegalStateException() {
    // 検証ポイント 2: 10 枚目 add で例外 (MAX_SIZE=9 境界)
    // GAME_DESIGN §15-3: 手札上限超過は上位レイヤが制御するが、Hand 自体が例外で守る
    Hand full = DomainFixtures.handOfSize(Hand.MAX_SIZE);
    assertThrows(
        IllegalStateException.class, () -> full.add(DomainFixtures.attackCard("overflow-001")));
  }

  @Test
  void constructorWithMoreThanMaxSizeThrowsIllegalArgumentException() {
    // コンストラクタ経由でも MAX_SIZE 超は拒否
    java.util.List<Card> tooMany = new java.util.ArrayList<>();
    for (int i = 0; i <= Hand.MAX_SIZE; i++) {
      tooMany.add(DomainFixtures.attackCard("c-" + i));
    }
    assertThrows(IllegalArgumentException.class, () -> new Hand(tooMany));
  }

  // remove の境界

  @Test
  void removeAtValidIndexReturnsHandWithOneFewerCard() {
    Hand h = DomainFixtures.handOfSize(3);
    Hand removed = h.remove(1);
    assertEquals(2, removed.size());
    assertEquals(3, h.size(), "元の Hand は変化してはならない");
  }

  @Test
  void removeAtNegativeIndexThrowsIndexOutOfBoundsException() {
    // 検証ポイント 12: 範囲外 index で IndexOutOfBoundsException
    Hand h = DomainFixtures.handOfSize(2);
    assertThrows(IndexOutOfBoundsException.class, () -> h.remove(-1));
  }

  @Test
  void removeAtSizeIndexThrowsIndexOutOfBoundsException() {
    // 検証ポイント 12: size() と等しい index (範囲外) は例外
    Hand h = DomainFixtures.handOfSize(2);
    assertThrows(IndexOutOfBoundsException.class, () -> h.remove(h.size()));
  }

  @Test
  void isFullFalseWhenBelowMaxSize() {
    Hand h = DomainFixtures.handOfSize(Hand.MAX_SIZE - 1);
    assertFalse(h.isFull());
  }
}
