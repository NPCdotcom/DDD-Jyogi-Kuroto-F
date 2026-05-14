package core.domain.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 捨て札 (使用済みカードの溜まり場)。
 *
 * <p>不変。 追加 ({@link #add}) と山札への再シャッフル ({@link #reshuffleInto}) は新インスタンスを返す。
 *
 * <p>山札切れ時に {@link CardPileState#drawN} から再シャッフル要求が走り、 自身を {@link DrawPile} に変換しつつ自分は空になる (これも純関数として表現)。
 */
public record DiscardPile(List<Card> cards) {

  public DiscardPile {
    Objects.requireNonNull(cards, "cards");
    cards = List.copyOf(cards);
  }

  public static DiscardPile empty() {
    return new DiscardPile(List.of());
  }

  public boolean isEmpty() {
    return cards.isEmpty();
  }

  public int size() {
    return cards.size();
  }

  /** カードを 1 枚追加した新しい捨て札を返す。 */
  public DiscardPile add(Card card) {
    Objects.requireNonNull(card, "card");
    List<Card> next = new ArrayList<>(cards.size() + 1);
    next.addAll(cards);
    next.add(card);
    return new DiscardPile(next);
  }

  /**
   * 自身をシャッフルして {@link DrawPile} に変換し、 自分は空の {@link DiscardPile} になる。 結果はタプル ({@link
   * ReshuffleResult}) で返す。
   *
   * <p>Random は引数注入。 ドメイン副作用ゼロを維持。
   */
  public ReshuffleResult reshuffleInto(java.util.Random rng) {
    Objects.requireNonNull(rng, "rng");
    DrawPile drawPile = DrawPile.shuffledFrom(cards, rng);
    return new ReshuffleResult(drawPile, DiscardPile.empty());
  }

  /** 再シャッフル結果 (新山札 + 空になった捨て札) のタプル。 */
  public record ReshuffleResult(DrawPile drawPile, DiscardPile discardPile) {
    public ReshuffleResult {
      Objects.requireNonNull(drawPile, "drawPile");
      Objects.requireNonNull(discardPile, "discardPile");
    }
  }
}
