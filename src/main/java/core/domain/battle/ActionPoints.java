package core.domain.battle;

/**
 * 行動ポイント (AP)。current は常に [0, max] の範囲に保たれる。
 *
 * <p>§15-3 / ADR-01 の使い切り型 AP モデル:
 *
 * <ul>
 *   <li>毎ターン頭で {@link #refilledTo(int)} を呼ぶことで速度ステ分まで全リセット (前ターンの残 AP は蓄積しない)
 *   <li>AP 切れ ({@link #isEmpty()}) で相手ターンへ自動遷移する (判定は TurnDirector / TurnEngine 側)
 *   <li>{@code max == 0} は速度ステ 0 のキャラ (現状想定なし、{@link #empty(int)} で表現可)
 * </ul>
 *
 * <p>ターン継続/終了の「AP 切れ or 手札切れ」OR 判定のうち、手札切れは本 record の責務ではない (依存事項 D で Hand 連動する想定)。
 */
public record ActionPoints(int current, int max) {

  public ActionPoints {
    if (max < 0) {
      throw new IllegalArgumentException("max must be non-negative: " + max);
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

  /**
   * 新しい max を指定し、current も新 max まで全充填する (§15-3 使い切り型のターン頭再充填)。
   *
   * <p>使い切り型では「前ターンの残 AP を蓄積しない」「max が変化したら即追従する」必要があり、本メソッドが両方を 1 呼び出しで扱う。通常は {@code
   * Stats#speed()} を引数に渡す。
   *
   * @param newMax 新しい max ({@code >= 0})、通常はターン頭の速度ステ値
   * @return current = newMax, max = newMax の新 ActionPoints
   * @throws IllegalArgumentException {@code newMax < 0} のとき
   */
  public ActionPoints refilledTo(int newMax) {
    if (newMax < 0) {
      throw new IllegalArgumentException("newMax must be non-negative: " + newMax);
    }
    return new ActionPoints(newMax, newMax);
  }

  /**
   * {@code max} を変えずに {@code current} を {@code max} まで再充填する。
   *
   * <p>{@link #refilledTo(int)} と違い max を引数で上書きしない。AP 上限が固定のキャラ (敵: spawn 時に層番号で 設定済み、ADR-06)
   * のターン頭リフィルに使う。{@code refilledTo(stats.speed())} を敵に使うと AP 上限が速度ステ値で 上書きされ「敵 AP =
   * 層番号」が崩れるため、敵にはこちらを使うこと。
   *
   * @return current = max, max = 据置 の新 ActionPoints
   */
  public ActionPoints refilled() {
    return new ActionPoints(max, max);
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
