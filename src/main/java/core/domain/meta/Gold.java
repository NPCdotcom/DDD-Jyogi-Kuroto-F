package core.domain.meta;

import java.util.Objects;

/**
 * ラン内通貨「金貨」 (§15-2)。ショップ購入や層末ノードのコストに使う。
 *
 * <p>{@link Soul} (永続) と異なり、ラン終了時にリセットされる (新ラン開始で 0)。獲得レート (§15-2): 雑魚撃破 = 5、強化個体撃破 = 15、ボス撃破 = 50。
 * 死亡時には持ち帰れない (Soul のみ生存する、§5-3 と整合)。
 *
 * <p>本 record は値オブジェクトとして単独で実装し、{@link core.domain.entity.Player} や {@link
 * core.application.GameContext} への統合は別 PR (Player 9 引数化問題を避けるため、`PlayerStatuses` 集約 record の検討と合わせて
 * ADR-22 で決定予定)。
 */
public record Gold(int amount) {

  public Gold {
    if (amount < 0) {
      throw new IllegalArgumentException("amount must be non-negative: " + amount);
    }
  }

  public static Gold zero() {
    return new Gold(0);
  }

  public Gold add(Gold other) {
    Objects.requireNonNull(other, "other");
    return new Gold(amount + other.amount);
  }

  public Gold subtract(Gold other) {
    Objects.requireNonNull(other, "other");
    if (amount < other.amount) {
      throw new IllegalStateException(
          "insufficient gold: have %d, need %d".formatted(amount, other.amount));
    }
    return new Gold(amount - other.amount);
  }
}
