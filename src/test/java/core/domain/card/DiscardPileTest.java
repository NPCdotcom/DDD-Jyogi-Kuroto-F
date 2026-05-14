package core.domain.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.support.DomainFixtures;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * DiscardPile の不変性と reshuffleInto テスト。
 *
 * <p>ADR-16 検証ポイント 11: add が新インスタンスを返し元インスタンスに変化なし。
 * GAME_DESIGN §15-3: 捨て札は再シャッフルで DrawPile に変換され、自身は空になる。
 */
class DiscardPileTest {

  // ハッピーパス

  @Test
  void addReturnsNewInstanceWithOneMoreCard() {
    DiscardPile original = DiscardPile.empty();
    Card card = DomainFixtures.attackCard("d1");
    DiscardPile grown = original.add(card);

    assertEquals(1, grown.size());
    assertEquals(0, original.size(), "元の DiscardPile は変化してはならない");
  }

  @Test
  void addLeavesOriginalInstanceIntact() {
    // 検証ポイント 11: 不変性確認 — 元インスタンスは変化しない
    DiscardPile original = DomainFixtures.discardPileOfSize(3);
    DiscardPile grown = original.add(DomainFixtures.attackCard("extra"));

    assertEquals(3, original.size(), "add 後も元インスタンスのサイズは不変");
    assertEquals(4, grown.size());
    assertNotSame(original, grown);
  }

  // reshuffleInto

  @Test
  void reshuffleIntoClearsDiscardAndCreatesDrawPile() {
    // GAME_DESIGN §15-3: reshuffleInto で捨て札が DrawPile になり、自身は空になる
    DiscardPile discard = DomainFixtures.discardPileOfSize(5);
    DiscardPile.ReshuffleResult result = discard.reshuffleInto(new Random(0));

    assertEquals(5, result.drawPile().size(), "全カードが DrawPile に移る");
    assertTrue(result.discardPile().isEmpty(), "自身は空の DiscardPile になる");
  }

  @Test
  void reshuffleIntoWithFixedSeedIsDeterministic() {
    // 固定シードで reshuffleInto を 2 回呼んで同じ DrawPile 順序が得られること
    DiscardPile discard = DomainFixtures.discardPileOfSize(4);

    DiscardPile.ReshuffleResult r1 = discard.reshuffleInto(new Random(99));
    DiscardPile.ReshuffleResult r2 = discard.reshuffleInto(new Random(99));

    assertEquals(r1.drawPile().cards(), r2.drawPile().cards(), "同一シードなら同じ順序");
  }

  @Test
  void reshuffleIntoDoesNotMutateOriginal() {
    // 不変性: reshuffleInto を呼んでも元 DiscardPile は変化しない
    DiscardPile original = DomainFixtures.discardPileOfSize(3);
    original.reshuffleInto(new Random(0));

    assertEquals(3, original.size(), "元の DiscardPile は変化してはならない");
  }
}
