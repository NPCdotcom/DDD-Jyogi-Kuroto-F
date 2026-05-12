package core.domain.entity;

import core.domain.battle.ActionPoints;
import core.domain.common.Position;
import core.domain.meta.Soul;
import core.domain.skill.SkillSlot;
import java.util.Objects;

/**
 * プレイヤーキャラクター。ソウルの保持・加算ができる点が Enemy と異なる。
 *
 * <p>共通インターフェース (Actor) は MVP で実利用が無いため設けない (YAGNI)。共通処理が必要に なった段階で sealed interface として復活させる。
 */
public record Player(
    ActorId id,
    Position position,
    Stats stats,
    ActionPoints actionPoints,
    SkillSlot skillSlot,
    Soul soul) {

  public Player {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(position, "position");
    Objects.requireNonNull(stats, "stats");
    Objects.requireNonNull(actionPoints, "actionPoints");
    Objects.requireNonNull(skillSlot, "skillSlot");
    Objects.requireNonNull(soul, "soul");
  }

  public Player withPosition(Position newPosition) {
    return new Player(id, newPosition, stats, actionPoints, skillSlot, soul);
  }

  public Player withStats(Stats newStats) {
    return new Player(id, position, newStats, actionPoints, skillSlot, soul);
  }

  public Player withActionPoints(ActionPoints newActionPoints) {
    return new Player(id, position, stats, newActionPoints, skillSlot, soul);
  }

  public Player addSoul(Soul delta) {
    return new Player(id, position, stats, actionPoints, skillSlot, soul.add(delta));
  }
}
