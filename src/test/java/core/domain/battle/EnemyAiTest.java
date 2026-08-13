package core.domain.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.common.Direction;
import core.domain.common.Position;
import core.domain.dungeon.DungeonMap;
import core.domain.dungeon.DungeonState;
import core.domain.entity.Enemy;
import core.domain.entity.EnemyAiState;
import core.domain.entity.EnemyKind;
import core.domain.entity.Player;
import core.domain.support.DomainFixtures;
import java.util.List;
import java.util.Optional;
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
    Enemy e =
        DomainFixtures.slimeAt(
            "low", new Position(2, 1), new core.domain.battle.ActionPoints(0, 3));
    DungeonState s =
        new DungeonState(DomainFixtures.squareRoom(), p, List.of(e), TurnPhase.ENEMY_TURN);

    BattleAction action = EnemyAi.decide(e, s);
    assertTrue(action instanceof BattleAction.Wait, "AP 0 ではスキル発動できないため Wait");
  }

  @Test
  void enemyDetoursAroundWall() {
    // Wave 13 W13-β: 壁で直線が塞がれていても BFS で迂回経路の 1 歩目へ動く (BSP 廊下対応)。
    // 中央列 x=2 は y=2,3 が壁。敵 (3,3) → プレイヤー (1,3) の直線 LEFT は壁、迂回は DOWN から。
    // 新仕様では「視界外 + IDLE = 待機」なので、SEARCHING 状態 + lastKnownPlayerPos でテスト
    // (= 一度プレイヤーを見つけて壁の向こうに見失った後の追跡挙動の検証)。
    DungeonMap map = DungeonMap.of(List.of("#####", "#.#.#", "#.#.#", "#...#", "#####"));
    Player p = DomainFixtures.playerAt(new Position(1, 3));
    Enemy e =
        DomainFixtures.slimeAt(new Position(3, 3))
            .withAiState(EnemyAiState.SEARCHING)
            .withLastKnownPlayerPos(Optional.of(new Position(1, 3)));
    DungeonState s = new DungeonState(map, p, List.of(e), TurnPhase.ENEMY_TURN);

    BattleAction action = EnemyAi.decide(e, s);
    assertTrue(action instanceof BattleAction.Move, "壁に阻まれても Wait せず迂回移動する");
    assertEquals(Direction.DOWN, ((BattleAction.Move) action).direction(), "迂回経路の 1 歩目は DOWN");
  }

  @Test
  void swiftSlimeAttacksWhenAdjacent() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Enemy base = DomainFixtures.slimeAt(new Position(2, 1));
    Enemy e =
        new Enemy(
            base.id(),
            base.position(),
            base.stats(),
            base.actionPoints(),
            base.skillSlot(),
            EnemyKind.SWIFT_SLIME,
            EnemyAiState.ALERT,
            Optional.of(new Position(1, 1)));
    DungeonState s =
        new DungeonState(DomainFixtures.squareRoom(), p, List.of(e), TurnPhase.ENEMY_TURN);

    BattleAction action = EnemyAi.decide(e, s);
    assertTrue(action instanceof BattleAction.UseSkill, "SWIFT_SLIME も隣接時は逃げずに攻撃スキルを使用する");
  }
}
