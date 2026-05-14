package core.domain.card;

import java.util.Objects;

/**
 * カードを一意に識別する不変 ID。
 *
 * <p>同一の文字列値を持つ {@code CardId} は equals で等価。空文字列・blank は不正値として拒否する。
 */
public record CardId(String value) {

  public CardId {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("CardId must not be blank");
    }
  }

  public static CardId of(String value) {
    return new CardId(value);
  }
}
