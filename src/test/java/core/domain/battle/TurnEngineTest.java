package core.domain.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.common.Direction;
import core.domain.common.Position;
import core.domain.dungeon.DungeonMap;
import core.domain.dungeon.DungeonState;
import core.domain.entity.Enemy;
import core.domain.entity.EnemyKind;
import core.domain.entity.Player;
import core.domain.meta.Soul;
import core.domain.support.DomainFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class TurnEngineTest {

  private DungeonState newState(Player player, List<Enemy> enemies) {
    DungeonMap map = DomainFixtures.squareRoom();
    return new DungeonState(map, player, enemies, TurnPhase.PLAYER_TURN);
  }

  @Test
  void moveIntoFloorSucceeds() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    DungeonState s = newState(p, List.of());

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.Move(Direction.RIGHT));

    assertFalse(result.wasRejected());
    assertEquals(new Position(2, 1), result.state().player().position());
    assertEquals(4, result.state().player().actionPoints().current());
    assertTrue(result.events().get(0) instanceof BattleEvent.Moved);
  }

  @Test
  void moveIntoWallIsRejected() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    DungeonState s = newState(p, List.of());

    // (1,1) から DOWN は (1,0) で壁
    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.Move(Direction.DOWN));

    assertTrue(result.wasRejected());
    assertEquals(p.position(), result.state().player().position());
    assertEquals(p.actionPoints(), result.state().player().actionPoints());
  }

  @Test
  void moveIntoEnemyIsRejected() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Enemy e = DomainFixtures.slimeAt(new Position(2, 1));
    DungeonState s = newState(p, List.of(e));

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.Move(Direction.RIGHT));

    assertTrue(result.wasRejected());
  }

  @Test
  void skillKillsAdjacentEnemyAndAwardsSoulButPhaseStaysPlayerTurn() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Enemy e = DomainFixtures.slimeAt(new Position(2, 1));
    DungeonState s = newState(p, List.of(e));

    // heavy attack (slot 1) は 15 ダメージ → 1 撃でスライム (HP 10) 撃破
    TurnEngine.StepResult result = TurnEngine.resolvePlayerAction(s, new BattleAction.UseSkill(1));

    assertFalse(result.wasRejected());
    assertTrue(result.state().enemies().isEmpty());
    assertEquals(EnemyKind.SLIME.soulReward(), result.state().player().soul().amount());
    // 敵全滅ではフェーズを変えない (CLEARED は階段踏破のみ)
    assertEquals(TurnPhase.PLAYER_TURN, result.state().phase());
    assertTrue(result.events().stream().anyMatch(ev -> ev instanceof BattleEvent.ActorDied));
    assertTrue(result.events().stream().anyMatch(ev -> ev instanceof BattleEvent.SoulGained));
  }

  @Test
  void killingOneEnemyOfTwoDoesNotChangePhase() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Enemy adjacent = DomainFixtures.slimeAt("e1", new Position(2, 1), ActionPoints.full(3));
    Enemy distant = DomainFixtures.slimeAt("e2", new Position(3, 3), ActionPoints.full(3));
    DungeonState s = newState(p, List.of(adjacent, distant));

    // heavy attack で adjacent を 1 撃撃破。distant は残存する。
    TurnEngine.StepResult result = TurnEngine.resolvePlayerAction(s, new BattleAction.UseSkill(1));

    assertFalse(result.wasRejected());
    assertEquals(1, result.state().enemies().size(), "1 体撃破後も 1 体残存");
    assertEquals(TurnPhase.PLAYER_TURN, result.state().phase(), "敵全滅以外では CLEARED にならない");
  }

  @Test
  void playerWithZeroApRejectsActions() {
    Player p = DomainFixtures.playerAt(new Position(1, 1)).withActionPoints(ActionPoints.empty(5));
    DungeonState s = newState(p, List.of());

    for (BattleAction action :
        List.of(new BattleAction.Move(Direction.RIGHT), new BattleAction.Wait())) {
      TurnEngine.StepResult result = TurnEngine.resolvePlayerAction(s, action);
      assertTrue(result.wasRejected(), action + " は AP 0 で reject されるべき");
      assertEquals(p.actionPoints(), result.state().player().actionPoints(), "AP は変化しないままのはず");
    }
  }

  @Test
  void steppingOntoStairsTriggersCleared() {
    Player p = DomainFixtures.playerAt(new Position(1, 2));
    DungeonState s =
        new DungeonState(DomainFixtures.roomWithStairs(), p, List.of(), TurnPhase.PLAYER_TURN);

    // (1,2) から RIGHT で (2,2) = STAIRS_DOWN に乗る
    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.Move(Direction.RIGHT));

    assertFalse(result.wasRejected());
    assertEquals(TurnPhase.CLEARED, result.state().phase());
    assertTrue(
        result.events().stream()
            .anyMatch(
                ev ->
                    ev instanceof BattleEvent.TurnPhaseChanged tpc
                        && tpc.newPhase() == TurnPhase.CLEARED));
  }

  @Test
  void skillWithoutAdjacentTargetIsRejected() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Enemy e = DomainFixtures.slimeAt(new Position(3, 1));
    DungeonState s = newState(p, List.of(e));

    TurnEngine.StepResult result = TurnEngine.resolvePlayerAction(s, new BattleAction.UseSkill(0));

    assertTrue(result.wasRejected());
  }

  @Test
  void skillWithInsufficientApIsRejected() {
    Player p = DomainFixtures.playerAt(new Position(1, 1)).withActionPoints(new ActionPoints(1, 5));
    Enemy e = DomainFixtures.slimeAt(new Position(2, 1));
    DungeonState s = newState(p, List.of(e));

    // heavy attack は 3 AP 必要、手持ち 1 なので拒否
    TurnEngine.StepResult result = TurnEngine.resolvePlayerAction(s, new BattleAction.UseSkill(1));

    assertTrue(result.wasRejected());
  }

  @Test
  void endTurnTransitionsToEnemyTurnAndRegeneratesEnemies() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Enemy e = DomainFixtures.slimeAt(new Position(3, 3)).withActionPoints(new ActionPoints(0, 3));
    DungeonState s = newState(p, List.of(e));

    TurnEngine.StepResult result = TurnEngine.resolvePlayerAction(s, new BattleAction.EndTurn());

    assertEquals(TurnPhase.ENEMY_TURN, result.state().phase());
    Enemy refreshed = result.state().enemies().get(0);
    assertEquals(2, refreshed.actionPoints().current()); // speed=2 ぶん回復
  }

  @Test
  void startPlayerTurnRefillsPlayerApAndSetsPhase() {
    Player p = DomainFixtures.playerAt(new Position(1, 1)).withActionPoints(new ActionPoints(0, 5));
    DungeonState s =
        new DungeonState(DomainFixtures.squareRoom(), p, List.of(), TurnPhase.ENEMY_TURN);

    TurnEngine.StepResult result = TurnEngine.startPlayerTurn(s);

    assertEquals(TurnPhase.PLAYER_TURN, result.state().phase());
    assertEquals(3, result.state().player().actionPoints().current()); // speed=3 ぶん回復
  }

  @Test
  void enemyAttackKillsPlayerTriggersGameOverAndKeepsSoul() {
    // プレイヤー HP 5 を 5 ダメージで倒す。事前にソウル 7 を持たせ、死亡後も保持されることを検証する
    // (GAME_DESIGN §5-3 「死亡時にソウルは保持」)
    Player weakPlayer =
        DomainFixtures.playerAt(new Position(1, 1))
            .withStats(new core.domain.entity.Stats(5, 30, 3))
            .addSoul(new Soul(7));
    Enemy slime = DomainFixtures.slimeAt(new Position(2, 1));
    DungeonState s =
        new DungeonState(
            DomainFixtures.squareRoom(), weakPlayer, List.of(slime), TurnPhase.ENEMY_TURN);

    TurnEngine.StepResult result =
        TurnEngine.resolveEnemyAction(s, slime.id(), new BattleAction.UseSkill(0));

    assertFalse(result.wasRejected());
    assertEquals(TurnPhase.GAME_OVER, result.state().phase());
    assertEquals(0, result.state().player().stats().currentHp());
    assertEquals(7, result.state().player().soul().amount(), "死亡時もソウルは持ち越し");
    assertTrue(result.events().stream().anyMatch(ev -> ev instanceof BattleEvent.ActorDied));
  }

  @Test
  void wrongPhaseRejectsPlayerAction() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    DungeonState s =
        new DungeonState(DomainFixtures.squareRoom(), p, List.of(), TurnPhase.ENEMY_TURN);

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.Move(Direction.RIGHT));

    assertTrue(result.wasRejected());
  }
}
