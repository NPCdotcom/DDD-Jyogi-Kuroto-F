package core.domain.meta;

import java.util.Objects;

/** メタ通貨「ソウル」。死亡時にも持ち帰れる (GAME_DESIGN §5-3, §5-4)。 */
public record Soul(int amount) {

  public Soul {
    if (amount < 0) {
      throw new IllegalArgumentException("amount must be non-negative: " + amount);
    }
  }

  public static Soul zero() {
    return new Soul(0);
  }

  public Soul add(Soul other) {
    Objects.requireNonNull(other, "other");
    return new Soul(amount + other.amount);
  }

  public Soul subtract(Soul other) {
    Objects.requireNonNull(other, "other");
    if (amount < other.amount) {
      throw new IllegalStateException(
          "insufficient soul: have %d, need %d".formatted(amount, other.amount));
    }
    return new Soul(amount - other.amount);
  }
}
