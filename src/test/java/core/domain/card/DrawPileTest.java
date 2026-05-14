package core.domain.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.support.DomainFixtures;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * DrawPile の drawOne / shuffledFrom テスト。
 *
 * <p>ADR-16 検証ポイント 10: 空 DrawPile.drawOne() → Optional.empty()。
 * GAME_DESIGN §15-3: 山札先頭から 1 枚引く。
 */
class DrawPileTest {

  // ハッピーパス

  @Test
  void drawOneFromNonEmptyPileReturnsTopCard() {
    DrawPile pile = DomainFixtures.drawPileOfSize(3);
    DrawPile.DrawResult result = pile.drawOne();

    assertTrue(result.drawn().isPresent(), "非空の山札は Optional に値を持つ");
    // 先頭が引かれ、残りが 2 枚
    assertEquals(2, result.remaining().size());
  }

  @Test
  void drawOneDecreasesSize() {
    DrawPile pile = DomainFixtures.drawPileOfSize(5);
    DrawPile.DrawResult result = pile.drawOne();

    assertEquals(4, result.remaining().size());
    assertEquals(5, pile.size(), "元の DrawPile は変化してはならない (不変性)");
  }

  // 境界値: 空 DrawPile

  @Test
  void drawOneOnEmptyPileReturnsEmptyOptional() {
    // 検証ポイント 10: 空山札 → Optional.empty()
    DrawPile empty = DrawPile.empty();
    DrawPile.DrawResult result = empty.drawOne();

    assertTrue(result.drawn().isEmpty(), "空山札からの drawOne は Optional.empty()");
    assertTrue(result.remaining().isEmpty(), "残り山札も空のまま");
  }

  // shuffledFrom

  @Test
  void shuffledFromProducesShuffledCopyNotSameReference() {
    // 不変性: shuffledFrom は新インスタンスを返し、元リストに影響しない
    java.util.List<Card> source = java.util.List.of(
        DomainFixtures.attackCard("s1"),
        DomainFixtures.attackCard("s2"),
        DomainFixtures.attackCard("s3"));
    DrawPile shuffled = DrawPile.shuffledFrom(source, new Random(0));

    assertNotSame(source, shuffled.cards());
    assertEquals(3, shuffled.size());
  }

  @Test
  void shuffledFromWithFixedSeedIsDeterministic() {
    // 固定シードで同じ順序が再現されること
    java.util.List<Card> source = java.util.List.of(
        DomainFixtures.attackCard("a"),
        DomainFixtures.attackCard("b"),
        DomainFixtures.attackCard("c"));

    DrawPile r1 = DrawPile.shuffledFrom(source, new Random(42));
    DrawPile r2 = DrawPile.shuffledFrom(source, new Random(42));

    assertEquals(r1.cards(), r2.cards(), "同一シードなら同じシャッフル結果");
  }
}
