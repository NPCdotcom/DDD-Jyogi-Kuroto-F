package core.domain.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.common.Direction;
import core.domain.common.Position;
import core.domain.dungeon.DungeonState;
import core.domain.entity.Enemy;
import core.domain.entity.Player;
import core.domain.support.DomainFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class EnemyAiTest {

  @Test
  void adjacentEnemyUsesSkill() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Enemy e = DomainFixtures.slimeAt(new Position(2, 1));
    DungeonState s =
        new DungeonState(DomainFixtures.squareRoom(), p, List.of(e), TurnPhase.ENEMY_TURN);

    BattleAction action = EnemyAi.decide(e, s);
    assertTrue(action instanceof BattleAction.UseSkill);
    assertEquals(0, ((BattleAction.UseSkill) action).slotIndex());
  }

  @Test
  void distantEnemyStepsCloser() {
    Player p = DomainFixtures.playerAt(new Position(3, 1));
    Enemy e = DomainFixtures.slimeAt(new Position(1, 1));
    DungeonState s =
        new DungeonState(DomainFixtures.squareRoom(), p, List.of(e), TurnPhase.ENEMY_TURN);

    BattleAction action = EnemyAi.decide(e, s);
    assertTrue(action instanceof BattleAction.Move);
    assertEquals(Direction.RIGHT, ((BattleAction.Move) action).direction());
  }

  @Test
  void blockedEnemyWaits() {
    // decider (3,1) からプレイヤー (1,1) へ向かう LEFT 方向 (2,1) に blocker を置いて塞ぐ
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Enemy blocker = DomainFixtures.slimeAt("blocker", new Position(2, 1), new ActionPoints(0, 3));
    Enemy decider = DomainFixtures.slimeAt("decider", new Position(3, 1), ActionPoints.full(3));

    DungeonState s =
        new DungeonState(
            DomainFixtures.squareRoom(), p, List.of(blocker, decider), TurnPhase.ENEMY_TURN);

    BattleAction action = EnemyAi.decide(decider, s);
    assertTrue(action instanceof BattleAction.Wait);
  }

  @Test
  void adjacentEnemyWithoutApWaits() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Enemy e = DomainFixtures.slimeAt("low", new Position(2, 1), new ActionPoints(0, 3));
    DungeonState s =
        new DungeonState(DomainFixtures.squareRoom(), p, List.of(e), TurnPhase.ENEMY_TURN);

    BattleAction action = EnemyAi.decide(e, s);
    assertTrue(action instanceof BattleAction.Wait, "AP 0 ではスキル発動できないため Wait");
  }
}
