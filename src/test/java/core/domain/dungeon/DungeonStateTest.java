package core.domain.dungeon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.battle.TurnPhase;
import core.domain.common.Position;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.Player;
import core.domain.support.DomainFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class DungeonStateTest {

  private DungeonState stateWith(List<Enemy> enemies) {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    return new DungeonState(DomainFixtures.squareRoom(), p, enemies, TurnPhase.PLAYER_TURN);
  }

  @Test
  void withEnemyReplacedThrowsWhenIdNotFound() {
    DungeonState s = stateWith(List.of());
    Enemy stranger = DomainFixtures.slimeAt(new Position(2, 1));
    assertThrows(IllegalStateException.class, () -> s.withEnemyReplaced(stranger));
  }

  @Test
  void withEnemyRemovedThrowsWhenIdNotFound() {
    DungeonState s = stateWith(List.of());
    assertThrows(IllegalStateException.class, () -> s.withEnemyRemoved(ActorId.of("ghost")));
  }

  @Test
  void isPositionOccupiedDetectsPlayerAndEnemies() {
    Enemy e = DomainFixtures.slimeAt(new Position(2, 2));
    DungeonState s = stateWith(List.of(e));

    assertTrue(s.isPositionOccupied(new Position(1, 1)), "プレイヤーの位置");
    assertTrue(s.isPositionOccupied(new Position(2, 2)), "敵の位置");
    assertFalse(s.isPositionOccupied(new Position(3, 3)), "誰もいない位置");
  }
}
