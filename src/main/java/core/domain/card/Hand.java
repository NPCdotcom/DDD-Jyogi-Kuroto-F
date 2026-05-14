package core.domain.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 手札 (戦闘中、プレイヤーが使用可能なカードのリスト)。
 *
 * <p>仕様: §15-3。最大手札サイズは {@link #MAX_SIZE} 枚。これを超える追加は例外で弾く (上位レイヤがドロー停止判定を実施する責務)。 不変。 追加・削除は新インスタンスを返す。
 */
public record Hand(List<Card> cards) {

  /** 手札の最大枚数 (§15-3)。 */
  public static final int MAX_SIZE = 9;

  public Hand {
    Objects.requireNonNull(cards, "cards");
    if (cards.size() > MAX_SIZE) {
      throw new IllegalArgumentException(
          "Hand size must be <= %d (got %d)".formatted(MAX_SIZE, cards.size()));
    }
    cards = List.copyOf(cards);
  }

  public static Hand empty() {
    return new Hand(List.of());
  }

  public int size() {
    return cards.size();
  }

  public boolean isFull() {
    return cards.size() >= MAX_SIZE;
  }

  /**
   * カードを 1 枚加えた新しい手札を返す。
   *
   * <p>すでに {@link #MAX_SIZE} 枚到達済みなら {@link IllegalStateException}。
   */
  public Hand add(Card card) {
    Objects.requireNonNull(card, "card");
    if (isFull()) {
      throw new IllegalStateException("Hand is full (size=" + cards.size() + ")");
    }
    List<Card> next = new ArrayList<>(cards.size() + 1);
    next.addAll(cards);
    next.add(card);
    return new Hand(next);
  }

  /**
   * 指定 index のカードを取り除いた新しい手札を返す。
   *
   * <p>範囲外なら {@link IndexOutOfBoundsException}。
   */
  public Hand remove(int handIndex) {
    if (handIndex < 0 || handIndex >= cards.size()) {
      throw new IndexOutOfBoundsException(
          "handIndex out of range [0, %d): %d".formatted(cards.size(), handIndex));
    }
    List<Card> next = new ArrayList<>(cards.size() - 1);
    for (int i = 0; i < cards.size(); i++) {
      if (i != handIndex) {
        next.add(cards.get(i));
      }
    }
    return new Hand(next);
  }

  /** 指定 index のカードを取得する (削除はしない)。範囲外なら {@link IndexOutOfBoundsException}。 */
  public Card get(int handIndex) {
    if (handIndex < 0 || handIndex >= cards.size()) {
      throw new IndexOutOfBoundsException(
          "handIndex out of range [0, %d): %d".formatted(cards.size(), handIndex));
    }
    return cards.get(handIndex);
  }
}
