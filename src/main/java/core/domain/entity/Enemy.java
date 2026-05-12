package core.domain.entity;

import core.domain.battle.ActionPoints;
import core.domain.common.Position;
import core.domain.skill.SkillSlot;
import java.util.Objects;

/** 敵キャラクター。種別 (kind) が撃破報酬や見た目を決定する。 */
public record Enemy(
    ActorId id,
    Position position,
    Stats stats,
    ActionPoints actionPoints,
    SkillSlot skillSlot,
    EnemyKind kind) {

  public Enemy {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(position, "position");
    Objects.requireNonNull(stats, "stats");
    Objects.requireNonNull(actionPoints, "actionPoints");
    Objects.requireNonNull(skillSlot, "skillSlot");
    Objects.requireNonNull(kind, "kind");
  }

  public Enemy withPosition(Position newPosition) {
    return new Enemy(id, newPosition, stats, actionPoints, skillSlot, kind);
  }

  public Enemy withStats(Stats newStats) {
    return new Enemy(id, position, newStats, actionPoints, skillSlot, kind);
  }

  public Enemy withActionPoints(ActionPoints newActionPoints) {
    return new Enemy(id, position, stats, newActionPoints, skillSlot, kind);
  }
}
