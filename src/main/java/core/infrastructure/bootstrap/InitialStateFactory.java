package core.infrastructure.bootstrap;

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

/**
 * ダンジョン初期状態 (1 ラン分の DungeonState) を組み立てるファクトリ。
 *
 * <p>MVP では JSON ロードを行わず固定値で構築する (YAGNI)。マップ・敵・スキル定義を 1 か所に集めて 「驚き最小」: 何がどこに置かれるかが 1
 * ファイルで読める。動的化が必要になった段階で、ここを Repository 経由のロードに差し替える。
 */
public final class InitialStateFactory {

  private InitialStateFactory() {}

  // ----------------------------- スキルマスタ -----------------------------

  public static Skill lightSlash() {
    return new Skill(SkillId.of("light_slash"), "Light Slash", 1, new SkillEffect.Damage(5));
  }

  public static Skill heavySlash() {
    return new Skill(SkillId.of("heavy_slash"), "Heavy Slash", 3, new SkillEffect.Damage(15));
  }

  public static Skill slimeBite() {
    return new Skill(SkillId.of("slime_bite"), "Bite", 1, new SkillEffect.Damage(4));
  }

  // ----------------------------- マップ -----------------------------

  /** 10x10 の固定マップ。外周は壁、内部は床。右下に階段。 */
  public static final List<String> FLOOR_01 =
      List.of(
          "##########",
          "#........#",
          "#........#",
          "#........#",
          "#........#",
          "#........#",
          "#........#",
          "#........#",
          "#.......>#",
          "##########");

  // ----------------------------- ファクトリ -----------------------------

  public static DungeonState firstFloor() {
    DungeonMap map = DungeonMap.of(FLOOR_01);
    Player player = newPlayer(new Position(1, 1));
    // 階段 (8, 1) の前に 1 体、フロア中央付近にもう 1 体配置することで、
    // 戦闘を経ずに踏破するルートにも敵を絡みやすくする。
    Enemy slimeNearStairs = newSlime("slime#stairs", new Position(6, 1));
    Enemy slimeMidRoom = newSlime("slime#mid", new Position(4, 4));
    return new DungeonState(
        map, player, List.of(slimeNearStairs, slimeMidRoom), TurnPhase.PLAYER_TURN);
  }

  public static Player newPlayer(Position spawn) {
    return new Player(
        ActorId.of("player"),
        spawn,
        new Stats(30, 30, 3),
        ActionPoints.full(5),
        new SkillSlot(List.of(lightSlash(), heavySlash()), 4),
        Soul.zero());
  }

  public static Enemy newSlime(String id, Position spawn) {
    return new Enemy(
        ActorId.of(id),
        spawn,
        new Stats(10, 10, 2),
        ActionPoints.full(3),
        new SkillSlot(List.of(slimeBite()), 4),
        EnemyKind.SLIME);
  }
}
