package core.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import core.domain.common.Position;
import core.domain.meta.Gold;
import core.domain.support.DomainFixtures;
import org.junit.jupiter.api.Test;

/** {@link Player} の Gold 操作 (§15-2 / §15-9) の単体テスト。Soul と同型の動作を確認する。 */
class PlayerGoldTest {

  @Test
  void initialPlayerHasZeroGold() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    assertEquals(0, p.gold().amount());
  }

  @Test
  void addGoldAccumulates() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Player after = p.addGold(new Gold(15));
    assertEquals(15, after.gold().amount());
    assertEquals(0, p.gold().amount(), "元 Player は変化しない (純関数)");
    assertNotSame(p, after);
  }

  @Test
  void addGoldChains() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Player after = p.addGold(new Gold(5)).addGold(new Gold(10));
    assertEquals(15, after.gold().amount(), "5 + 10 = 15");
  }

  @Test
  void spendGoldDeductsAmount() {
    Player rich = DomainFixtures.playerAt(new Position(1, 1)).addGold(new Gold(20));
    Player after = rich.spendGold(new Gold(7));
    assertEquals(13, after.gold().amount(), "20 - 7 = 13");
  }

  @Test
  void spendGoldRejectsInsufficient() {
    Player poor = DomainFixtures.playerAt(new Position(1, 1)).addGold(new Gold(3));
    assertThrows(IllegalStateException.class, () -> poor.spendGold(new Gold(5)));
  }

  @Test
  void addGoldDoesNotAffectSoul() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Player after = p.addGold(new Gold(10));
    assertEquals(p.soul().amount(), after.soul().amount(), "Soul は不変");
    assertEquals(p.position(), after.position(), "Position は不変");
    assertEquals(p.statuses(), after.statuses(), "Statuses は不変");
  }

  // §15-9 Shop: 残額ちょうどの spendGold は成功する (境界値)
  @Test
  void spendGoldExactAmountSucceeds() {
    Player p = DomainFixtures.playerAt(new Position(1, 1)).addGold(new Gold(10));
    Player broke = p.spendGold(new Gold(10));
    assertEquals(0, broke.gold().amount(), "残額ちょうどを消費した後は 0");
  }

  // §15-9 Shop: 残額を 1 超えた spendGold は IllegalStateException (P3-1 追加ケース)
  @Test
  void spendGoldOneOverThrowsIllegalStateException() {
    Player p = DomainFixtures.playerAt(new Position(1, 1)).addGold(new Gold(9));
    // 残 9 に対して 10 を要求 → 1 超過
    assertThrows(IllegalStateException.class, () -> p.spendGold(new Gold(10)));
  }

  // addGold(0) は gold.amount() を変えない (境界値: 加算量 0)
  @Test
  void addZeroGoldDoesNotChangeAmount() {
    Player p = DomainFixtures.playerAt(new Position(1, 1)).addGold(new Gold(5));
    Player same = p.addGold(new Gold(0));
    assertEquals(5, same.gold().amount(), "0 加算後も gold は変化しない");
    assertNotSame(p, same, "add は常に新インスタンスを返す");
  }
}
