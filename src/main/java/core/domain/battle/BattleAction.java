package core.domain.battle;

import core.domain.common.Direction;
import java.util.Objects;

/**
 * 1 単位の戦闘アクション。プレイヤー入力 / EnemyAI 出力の両方が同じ型で表現される。
 *
 * <p>sealed にしているのは TurnEngine の switch 網羅性確保のため。
 */
public sealed interface BattleAction
    permits BattleAction.Move, BattleAction.UseSkill, BattleAction.Wait, BattleAction.EndTurn {

  /** 指定方向に 1 マス移動する (AP コスト 1)。 */
  record Move(Direction direction) implements BattleAction {
    public Move {
      Objects.requireNonNull(direction, "direction");
    }
  }

  /** スキル枠 slotIndex のスキルを発動する。 */
  record UseSkill(int slotIndex) implements BattleAction {
    public UseSkill {
      if (slotIndex < 0) {
        throw new IllegalArgumentException("slotIndex must be non-negative: " + slotIndex);
      }
    }
  }

  /** AP を 1 消費してその場で待機する。 */
  record Wait() implements BattleAction {}

  /** 自分のターンを意図的に終了する (残 AP を持ち越す)。 */
  record EndTurn() implements BattleAction {}
}
