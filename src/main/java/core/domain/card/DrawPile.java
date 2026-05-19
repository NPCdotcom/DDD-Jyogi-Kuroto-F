package core.domain.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 山札 (戦闘中、まだ引かれていないカードの順序付きリスト)。
 *
 * <p>先頭 (index 0) が「次に引かれるカード」。 不変。 ドロー操作は新インスタンスと引いたカードのペア ({@link DrawResult}) を返す。
 *
 * <p>シャッフルは {@link #shuffledFrom(List, java.util.Random)} で実施。 ドメイン層では Random を引数で受け取り、副作用ゼロを維持する
 * (テスト時に固定シードで決定的に検証可能)。
 */
public record DrawPile(List<Card> cards) {

  public DrawPile {
    Objects.requireNonNull(cards, "cards");
    cards = List.copyOf(cards);
  }

  /** カードリストを与えられた Random でシャッフルして山札を作る。元のリストは変更しない。 */
  public static DrawPile shuffledFrom(List<Card> source, java.util.Random rng) {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(rng, "rng");
    List<Card> shuffled = new ArrayList<>(source);
    Collections.shuffle(shuffled, rng);
    return new DrawPile(shuffled);
  }

  /** 空の山札を作る。 */
  public static DrawPile empty() {
    return new DrawPile(List.of());
  }

  public boolean isEmpty() {
    return cards.isEmpty();
  }

  public int size() {
    return cards.size();
  }

  /**
   * 先頭から 1 枚引く。
   *
   * <p>山札が空の場合 {@link DrawResult#drawn()} は {@code Optional.empty()}、{@link DrawResult#remaining()}
   * は空の山札となる。 呼び出し側は空時に捨て札からの再シャッフル ({@link CardPileState#drawN}) で対処する。
   */
  public DrawResult drawOne() {
    if (cards.isEmpty()) {
      return new DrawResult(Optional.empty(), this);
    }
    Card head = cards.get(0);
    return new DrawResult(Optional.of(head), new DrawPile(cards.subList(1, cards.size())));
  }

  /**
   * ドロー結果 (引いたカード + 残り山札) のタプル。
   *
   * <p>{@code Optional<Card>} と {@code DrawPile} を 2 値返却するため専用 record で表現 (驚き最小、Pair 系を導入しない)。
   */
  public record DrawResult(Optional<Card> drawn, DrawPile remaining) {
    public DrawResult {
      Objects.requireNonNull(drawn, "drawn");
      Objects.requireNonNull(remaining, "remaining");
    }
  }
}
