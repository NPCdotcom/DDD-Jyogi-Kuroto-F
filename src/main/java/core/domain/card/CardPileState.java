package core.domain.card;

import java.util.Objects;
import java.util.Random;

/**
 * 戦闘中のカード山札・手札・捨て札の動的状態を 1 つにまとめた集約。
 *
 * <p>仕様: §15-3。 山札切れ時の捨て札再シャッフルや、初期ドロー枚数の決定など、 「カード周りのフロー」をここに局所閉包する (Tell, Don't Ask)。
 *
 * <p>すべての操作は新インスタンスを返す。 Random は引数注入 (テスト時に固定シードで決定的検証)。
 */
public record CardPileState(DrawPile drawPile, Hand hand, DiscardPile discardPile) {

  /** 手札の最大枚数 (Hand.MAX_SIZE と一致させる、§15-3)。 */
  public static final int MAX_HAND_SIZE = 9;

  /** 小規模デッキ時の初期ドロー上限 (デッキ <= 5 枚なら最大 3 枚)。 */
  public static final int INITIAL_DRAW_CAP = 3;

  /** これ以上デッキが大きくなると初期ドロー上限が 5 枚に切り上がる境界 (§15-3)。 */
  public static final int DRAW_CONVERGENCE_DECK_SIZE = 6;

  public CardPileState {
    Objects.requireNonNull(drawPile, "drawPile");
    Objects.requireNonNull(hand, "hand");
    Objects.requireNonNull(discardPile, "discardPile");
  }

  /**
   * デッキサイズに応じた戦闘開始時の初期ドロー枚数を決定する。
   *
   * <p>仕様 (§15-3): deck=1→1, deck=2→2, deck=3→3, deck=4→3, deck=5→3, deck=6→5, deck>=10→5。
   *
   * <p>境界は「デッキ <= 5 なら min(deck, 3)、デッキ >= 6 なら min(deck, 5)」で表現できる。 deck >= 6 で 5 を上回るカードを引かない理由は「初期 5
   * 枚で安定」させるため (§15-3)。
   */
  public static int initialDrawCount(int totalDeckSize) {
    if (totalDeckSize < 1) {
      throw new IllegalArgumentException("totalDeckSize must be >= 1 (got " + totalDeckSize + ")");
    }
    if (totalDeckSize < DRAW_CONVERGENCE_DECK_SIZE) {
      return Math.min(totalDeckSize, INITIAL_DRAW_CAP);
    }
    return Math.min(totalDeckSize, 5);
  }

  /**
   * N 枚ドローする。
   *
   * <p>山札が途中で空になったら、 捨て札を {@code rng} でシャッフルして山札に積み直す (§15-3 「山札切れ → 捨て札再シャッフル」)。 それでも引ききれない場合
   * (山札 + 捨て札の合計 < n) は引ける分だけ引いて停止する。
   *
   * <p>また手札が {@link #MAX_HAND_SIZE} に達した時点でもドロー停止する (上限に従って静かに止まる、例外は投げない)。
   */
  public CardPileState drawN(int n, Random rng) {
    if (n < 0) {
      throw new IllegalArgumentException("n must be non-negative (got " + n + ")");
    }
    Objects.requireNonNull(rng, "rng");

    DrawPile dp = this.drawPile;
    Hand h = this.hand;
    DiscardPile disc = this.discardPile;

    for (int i = 0; i < n; i++) {
      if (h.size() >= MAX_HAND_SIZE) {
        break;
      }
      if (dp.isEmpty()) {
        if (disc.isEmpty()) {
          break; // 山札も捨て札も枯渇 → 引けない
        }
        DiscardPile.ReshuffleResult rr = disc.reshuffleInto(rng);
        dp = rr.drawPile();
        disc = rr.discardPile();
      }
      DrawPile.DrawResult dr = dp.drawOne();
      if (dr.drawn().isEmpty()) {
        break; // 念のためのガード (通常通らない)
      }
      h = h.add(dr.drawn().get());
      dp = dr.remaining();
    }
    return new CardPileState(dp, h, disc);
  }

  /**
   * 手札の {@code handIndex} 番目を使用済みとして捨て札に送り、 手札から取り除いた新状態を返す。
   *
   * <p>範囲外なら {@link IndexOutOfBoundsException}。
   */
  public CardPileState playFromHand(int handIndex) {
    Card played = hand.get(handIndex);
    Hand newHand = hand.remove(handIndex);
    DiscardPile newDiscard = discardPile.add(played);
    return new CardPileState(drawPile, newHand, newDiscard);
  }
}
