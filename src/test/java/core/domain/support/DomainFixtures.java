package core.domain.support;

import core.domain.battle.ActionPoints;
import core.domain.battle.TurnPhase;
import core.domain.common.Position;
import core.domain.dungeon.DungeonMap;
import core.domain.dungeon.DungeonState;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.EnemyKind;
import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.domain.meta.Soul;
import core.domain.skill.Skill;
import core.domain.skill.SkillEffect;
import core.domain.skill.SkillId;
import core.domain.skill.SkillSlot;
import java.util.List;

/** テスト用のドメインオブジェクトファクトリ。プロダクションコードから参照してはならない。 */
public final class DomainFixtures {

  private DomainFixtures() {}

  /** 5x5 の四角部屋。外周壁、内部すべて床。 */
  public static final List<String> SQUARE_ROOM_5X5 =
      List.of("#####", "#...#", "#...#", "#...#", "#####");

  /** 5x5 の四角部屋。中央 (2, 2) に階段。 */
  public static final List<String> ROOM_WITH_STAIRS_5X5 =
      List.of("#####", "#...#", "#.>.#", "#...#", "#####");

  public static DungeonMap squareRoom() {
    return DungeonMap.of(SQUARE_ROOM_5X5);
  }

  public static DungeonMap roomWithStairs() {
    return DungeonMap.of(ROOM_WITH_STAIRS_5X5);
  }

  public static Skill lightAttack() {
    return new Skill(SkillId.of("light"), "軽攻撃", 1, new SkillEffect.Damage(5));
  }

  public static Skill heavyAttack() {
    return new Skill(SkillId.of("heavy"), "強攻撃", 3, new SkillEffect.Damage(15));
  }

  public static Player playerAt(Position position) {
    return new Player(
        ActorId.of("p1"),
        position,
        new Stats(30, 30, 3),
        ActionPoints.full(5),
        new SkillSlot(List.of(lightAttack(), heavyAttack()), 4),
        Soul.zero());
  }

  public static Enemy slimeAt(Position position) {
    return slimeAt("slime#" + position.x() + "_" + position.y(), position, ActionPoints.full(3));
  }

  /** id / AP を明示的に渡せるスライム生成。複数体を区別したり AP 不足状態を作りたいときに使う。 */
  public static Enemy slimeAt(String id, Position position, ActionPoints actionPoints) {
    return new Enemy(
        ActorId.of(id),
        position,
        new Stats(10, 10, 2),
        actionPoints,
        new SkillSlot(List.of(lightAttack()), 4),
        EnemyKind.SLIME);
  }

  public static DungeonState newStateWith(
      DungeonMap map, Player player, List<Enemy> enemies, TurnPhase phase) {
    return new DungeonState(map, player, enemies, phase);
  }
}
