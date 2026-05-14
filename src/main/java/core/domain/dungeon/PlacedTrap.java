package core.domain.dungeon;

import core.domain.card.CardElement;
import core.domain.card.TrapLifetime;
import core.domain.common.Position;
import core.domain.entity.Stats;
import java.util.Objects;

/**
 * マップ上に設置された罠 (§15-3 / ADR-22)。プレイヤーが {@code CardEffect.Trap} カードを使うと作成され、 敵 or
 * プレイヤーが当該マスに移動すると発動する。
 *
 * <p>同座標に既存の罠がある場合、新規設置は **上書き** (3 並列レビュー結論、驚き最小 = 最新が優先)。
 *
 * <p>ライフタイム:
 *
 * <ul>
 *   <li>{@link TrapLifetime.UntilStepped}: 踏まれるまで永続、踏まれた時点で除去
 *   <li>{@link TrapLifetime.Turns}: 毎プレイヤーターン頭で {@code remaining--}、0 になったら除去 (踏まれても除去)
 * </ul>
 *
 * <p>element はダメージ計算で使う。物理罠 = PHYSICAL なら被害側の物防、魔法罠 = MAGICAL なら魔防を参照。
 */
public record PlacedTrap(
    Position position, int baseValue, TrapLifetime lifetime, CardElement element) {

  public PlacedTrap {
    Objects.requireNonNull(position, "position");
    Objects.requireNonNull(lifetime, "lifetime");
    Objects.requireNonNull(element, "element");
    if (baseValue < 1) {
      throw new IllegalArgumentException("baseValue must be >= 1: " + baseValue);
    }
  }

  /** 残ターン数を 1 減らした新インスタンス。{@link TrapLifetime.UntilStepped} の場合は変化なし。 */
  public PlacedTrap decrementedLifetime() {
    return switch (lifetime) {
      case TrapLifetime.UntilStepped ignored -> this;
      case TrapLifetime.Turns turns -> new PlacedTrap(
          position, baseValue, new TrapLifetime.Turns(turns.remaining() - 1), element);
    };
  }

  /**
   * 罠が有効か (残ターンが 1 以上、または UntilStepped)。{@link #decrementedLifetime()} 後にこれを使って expired
   * を除去する。
   */
  public boolean isAlive() {
    return switch (lifetime) {
      case TrapLifetime.UntilStepped ignored -> true;
      case TrapLifetime.Turns turns -> turns.remaining() >= 1;
    };
  }

  /**
   * 踏まれた被害者へのダメージを計算する。{@code max(1, baseValue - 防御ステ)} で物理/魔法に応じた防御を引く (§15-4 加減算式)。
   *
   * <p>罠は設置者のステに依存しない (= 罠の baseValue のみで決まる)。理由: 設置時にスナップショットを取る複雑性を回避する KISS 判断 (ADR-22)。
   * 将来、設置者ステ依存に拡張する場合は PlacedTrap にスナップショットフィールドを追加。
   */
  public int resolveDamage(Stats victim) {
    Objects.requireNonNull(victim, "victim");
    int defense =
        switch (element) {
          case PHYSICAL -> victim.physicalDefense();
          case MAGICAL -> victim.magicalDefense();
        };
    return Math.max(1, baseValue - defense);
  }
}
