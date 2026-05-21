package core.domain.battle;

import core.domain.common.Direction;
import core.domain.common.Position;
import core.domain.dungeon.DungeonMap;
import core.domain.dungeon.DungeonState;
import core.domain.entity.Enemy;
import core.domain.skill.Skill;
import java.util.Objects;
import java.util.Optional;

/**
 * 敵 AI の意思決定。純関数で副作用を持たない。
 *
 * <p>MVP の戦略:
 *
 * <ul>
 *   <li>スキル枠 0 番のスキルを保持していて、AP が足り、プレイヤーに隣接していれば、それで攻撃
 *   <li>そうでなければ {@link DungeonMap#firstStepToward} の BFS でプレイヤーへの最短経路の 1 歩目へ移動 (壁を迂回)
 *   <li>経路が無い / 次マスが他アクターで塞がる / AP 不足 なら Wait
 * </ul>
 *
 * <p>このクラスは「次に取る行動」を返すだけで、行動の解決は {@link TurnEngine} の責務。 TurnEngine 側が再度バリデーションするので、ここでの判定は
 * best-effort で良い (TurnEngine.reject が拾うため失敗してもサイレント) が、無効アクションを返さない方が呼び出し側の loop が安全。
 */
public final class EnemyAi {

  private EnemyAi() {}

  public static BattleAction decide(Enemy enemy, DungeonState state) {
    Objects.requireNonNull(enemy, "enemy");
    Objects.requireNonNull(state, "state");

    Position me = enemy.position();
    Position target = state.player().position();

    if (me.isAdjacentTo(target) && canUseSkill(enemy, 0)) {
      return new BattleAction.UseSkill(0);
    }

    // BSP 廊下でも壁を迂回できるよう、BFS でプレイヤーへの最短経路の 1 歩目を求める。
    Optional<Direction> step = state.map().firstStepToward(me, target);
    if (step.isEmpty() || !enemy.actionPoints().canSpend(1)) {
      return new BattleAction.Wait();
    }
    // 経路上の次マスが他アクター (敵同士) で塞がっていれば、今ターンは待機する。
    if (state.isPositionOccupied(me.move(step.get()))) {
      return new BattleAction.Wait();
    }
    return new BattleAction.Move(step.get());
  }

  private static boolean canUseSkill(Enemy enemy, int slotIndex) {
    Optional<Skill> skill = enemy.skillSlot().at(slotIndex);
    return skill.isPresent() && enemy.actionPoints().canSpend(skill.get().apCost());
  }
}
