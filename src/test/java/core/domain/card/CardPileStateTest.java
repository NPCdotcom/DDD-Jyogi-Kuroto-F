package core.domain.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.support.DomainFixtures;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * CardPileState の初期ドロー枚数テーブルと drawN / playFromHand テスト。
 *
 * <p>ADR-16 検証ポイント 3: initialDrawCount テーブル。
 * ADR-16 検証ポイント 4: drawN で山札切れ時の再シャッフル (固定シード)。
 * ADR-16 検証ポイント 13: playFromHand でカードが Hand→DiscardPile に移動。
 * GAME_DESIGN §15-3: 山札切れ → 捨て札を再シャッフルして山札に積み直す。
 */
class CardPileStateTest {

  // =========================================================
  // 検証ポイント 3: initialDrawCount テーブル
  // =========================================================

  @Test
  void initialDrawCountDeck1Returns1() {
    // GAME_DESIGN §15-3: deck=1 → 1
    assertEquals(1, CardPileState.initialDrawCount(1));
  }

  @Test
  void initialDrawCountDeck2Returns2() {
    // deck=2 → 2 (min(2, INITIAL_DRAW_CAP=3))
    assertEquals(2, CardPileState.initialDrawCount(2));
  }

  @Test
  void initialDrawCountDeck3ReturnsCap3() {
    // deck=3 → 3 (= INITIAL_DRAW_CAP)
    assertEquals(CardPileState.INITIAL_DRAW_CAP, CardPileState.initialDrawCount(3));
  }

  @Test
  void initialDrawCountDeck4StaysAtCap3() {
    // deck=4 → 3 (cap 適用、DRAW_CONVERGENCE_DECK_SIZE=6 未満)
    assertEquals(CardPileState.INITIAL_DRAW_CAP, CardPileState.initialDrawCount(4));
  }

  @Test
  void initialDrawCountDeck5StaysAtCap3() {
    // deck=5 → 3 (cap 適用)
    assertEquals(CardPileState.INITIAL_DRAW_CAP, CardPileState.initialDrawCount(5));
  }

  @Test
  void initialDrawCountDeck6JumpsTo5() {
    // deck=6 → 5 (DRAW_CONVERGENCE_DECK_SIZE 境界を超えたので上限 5)
    // GAME_DESIGN §15-3: deck >= 6 で初期ドロー上限 5 に切り上がる
    assertEquals(5, CardPileState.initialDrawCount(CardPileState.DRAW_CONVERGENCE_DECK_SIZE));
  }

  @Test
  void initialDrawCountDeck10Returns5() {
    // deck=10 → 5 (上限 5 固定)
    assertEquals(5, CardPileState.initialDrawCount(10));
  }

  @Test
  void initialDrawCountZeroDeckThrowsIllegalArgumentException() {
    // 検証ポイント 3 (deck=0): 空デッキは禁止 (§15-3)
    assertThrows(IllegalArgumentException.class, () -> CardPileState.initialDrawCount(0));
  }

  @Test
  void initialDrawCountNegativeDeckThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> CardPileState.initialDrawCount(-1));
  }

  // =========================================================
  // 検証ポイント 4: drawN で山札切れ時の再シャッフル (固定シード)
  // =========================================================

  /**
   * シナリオ: 山札 3 枚 / 捨て札 5 枚の状態で drawN(5, seed=42) を呼ぶ。
   *
   * <p>期待: Hand に 5 枚、DiscardPile は空、DrawPile に残り 3 枚。
   * 同じシードで 2 回呼んで同じ結果が得られることも確認 (再現性テスト)。
   *
   * <p>ADR-16: Random は引数注入でテスト時は決定的シードで検証 (ドメイン副作用ゼロ)。
   */
  @Test
  void drawNReshuffle_draws3FromPile_then2FromReshuffledDiscard_resultIs5HandCards() {
    // 山札 3 枚、捨て札 5 枚
    DrawPile initialDraw = DomainFixtures.drawPileOfSize(3);
    DiscardPile initialDiscard = DomainFixtures.discardPileOfSize(5);
    CardPileState state = new CardPileState(initialDraw, Hand.empty(), initialDiscard);

    CardPileState result = state.drawN(5, new Random(42));

    assertEquals(5, result.hand().size(), "Hand に 5 枚引けなければならない");
    assertTrue(result.discardPile().isEmpty(), "捨て札は再シャッフル後に空になる");
    // 山札残り: 再シャッフル後に 5 枚あった捨て札から 2 枚引いたので残り 3 枚
    assertEquals(3, result.drawPile().size(), "再シャッフル後の山札残りは 3 枚");
  }

  @Test
  void drawNWithSameSeedProducesDeterministicResult() {
    // 再現性テスト: 同じシードなら 2 回呼んで同じ Hand 順序
    DrawPile initialDraw = DomainFixtures.drawPileOfSize(3);
    DiscardPile initialDiscard = DomainFixtures.discardPileOfSize(5);
    CardPileState state = new CardPileState(initialDraw, Hand.empty(), initialDiscard);

    CardPileState result1 = state.drawN(5, new Random(42));
    CardPileState result2 = state.drawN(5, new Random(42));

    // Hand のカード順序が一致すること
    assertEquals(result1.hand().cards(), result2.hand().cards(), "固定シードなら結果は決定的");
    assertEquals(result1.drawPile().cards(), result2.drawPile().cards());
  }

  @Test
  void drawNStopsWhenBothPilesExhausted() {
    // 山札 2 枚 / 捨て札 0 枚: 2 枚しか引けず停止 (例外は投げない)
    DrawPile smallDraw = DomainFixtures.drawPileOfSize(2);
    CardPileState state = new CardPileState(smallDraw, Hand.empty(), DiscardPile.empty());

    CardPileState result = state.drawN(10, new Random(0));

    assertEquals(2, result.hand().size(), "山札 + 捨て札の上限で静かに停止する");
    assertTrue(result.drawPile().isEmpty());
  }

  @Test
  void drawNStopsAtHandMaxSize() {
    // 手札が MAX_HAND_SIZE に達したら停止 (例外を投げない)
    DrawPile bigDraw = DomainFixtures.drawPileOfSize(20);
    CardPileState state = new CardPileState(bigDraw, Hand.empty(), DiscardPile.empty());

    CardPileState result = state.drawN(20, new Random(0));

    assertEquals(CardPileState.MAX_HAND_SIZE, result.hand().size(),
        "手札は MAX_HAND_SIZE を超えない");
  }

  @Test
  void drawNNegativeThrowsIllegalArgumentException() {
    CardPileState state =
        new CardPileState(DrawPile.empty(), Hand.empty(), DiscardPile.empty());
    assertThrows(IllegalArgumentException.class, () -> state.drawN(-1, new Random(0)));
  }

  // =========================================================
  // 検証ポイント 13: playFromHand でカードが Hand→DiscardPile に移動
  // =========================================================

  @Test
  void playFromHandMovesCardFromHandToDiscardPile() {
    // 検証ポイント 13: GAME_DESIGN §15-3 カードを使うと捨て札に送られる
    Hand hand = Hand.empty().add(DomainFixtures.attackCard("strike-001"));
    CardPileState state =
        new CardPileState(DrawPile.empty(), hand, DiscardPile.empty());

    CardPileState after = state.playFromHand(0);

    assertEquals(0, after.hand().size(), "使用後は Hand からカードが消える");
    assertEquals(1, after.discardPile().size(), "使用したカードは DiscardPile に移動する");
    assertEquals("strike-001", after.discardPile().cards().get(0).id().value());
  }

  @Test
  void playFromHandOutOfRangeThrowsIndexOutOfBoundsException() {
    CardPileState state =
        new CardPileState(DrawPile.empty(), Hand.empty(), DiscardPile.empty());
    assertThrows(IndexOutOfBoundsException.class, () -> state.playFromHand(0));
  }

  @Test
  void playFromHandLeavesOriginalStateIntact() {
    // 不変性: 元の CardPileState は変化しない
    Hand hand = Hand.empty().add(DomainFixtures.attackCard("move-001"));
    CardPileState state =
        new CardPileState(DrawPile.empty(), hand, DiscardPile.empty());

    state.playFromHand(0);

    assertEquals(1, state.hand().size(), "元の CardPileState は変化してはならない");
    assertEquals(0, state.discardPile().size());
  }
}
