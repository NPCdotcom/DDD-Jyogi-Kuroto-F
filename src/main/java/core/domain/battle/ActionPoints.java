package core.domain.battle;

/**
 * 行動ポイント (AP)。current は常に [0, max] の範囲に保たれる。
 *
 * <p>GAME_DESIGN §5-2 の AP 回復方式 (案 A-2) を表現する:
 *
 * <ul>
 *   <li>max は固定
 *   <li>{@link #regenerate(int)} で速度ステ分の AP を加算（上限 max）
 *   <li>蓄積可能 (current < max のときに溜まる)
 * </ul>
 */
public record ActionPoints(int current, int max) {

  public ActionPoints {
    if (max <= 0) {
      throw new IllegalArgumentException("max must be positive: " + max);
    }
    if (current < 0 || current > max) {
      throw new IllegalArgumentException("current must be in [0, %d]: %d".formatted(max, current));
    }
  }

  public static ActionPoints full(int max) {
    return new ActionPoints(max, max);
  }

  public static ActionPoints empty(int max) {
    return new ActionPoints(0, max);
  }

  public ActionPoints regenerate(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("regenerate amount must be non-negative: " + amount);
    }
    return new ActionPoints(Math.min(max, current + amount), max);
  }

  public boolean canSpend(int cost) {
    return cost >= 0 && current >= cost;
  }

  public ActionPoints spend(int cost) {
    if (!canSpend(cost)) {
      throw new IllegalStateException("cannot spend %d AP (current %d)".formatted(cost, current));
    }
    return new ActionPoints(current - cost, max);
  }

  public boolean isEmpty() {
    return current == 0;
  }
}
