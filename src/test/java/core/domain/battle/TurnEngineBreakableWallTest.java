package core.domain.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.common.Direction;
import core.domain.common.Position;
import core.domain.dungeon.DungeonMap;
import core.domain.dungeon.DungeonState;
import core.domain.dungeon.Tile;
import core.domain.entity.Player;
import core.domain.support.DomainFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Wave 11 W11-α 壊れる壁ギミックの統合テスト。 */
class TurnEngineBreakableWallTest {

  /** (3,2) に BREAKABLE_WALL を置いた 5×5 部屋。 */
  private static DungeonMap mapWithBreakableWall() {
    return DungeonMap.of(List.of("#####", "#...#", "#..B#", "#...#", "#####"));
  }

  @Test
  void moveCardLetsPlayerBreakAndPassThroughBreakableWall() {
    // (2,2) → (3,2)=BREAKABLE_WALL に移動権で踏み込む。
    Player p = DomainFixtures.playerAt(new Position(2, 2)).withPendingMoveCount(2);
    DungeonState s = new DungeonState(mapWithBreakableWall(), p, List.of(), TurnPhase.PLAYER_TURN);

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.Move(Direction.RIGHT));

    assertFalse(result.wasRejected());
    // (1) 世界の書き換え: BREAKABLE_WALL は FLOOR に
    assertEquals(Tile.FLOOR, result.state().map().tileAt(new Position(3, 2)));
    // (3) プレイヤー位置更新: 目的地に立っている
    assertEquals(new Position(3, 2), result.state().player().position());
    // 移動権は 1 消費
    assertEquals(1, result.state().player().pendingMoveCount());
  }

  @Test
  void breakingWallFiresWallBrokenEventBeforeMovedEvent() {
    // CTO チェックポイント #2: イベント順序 = WallBroken → Moved
    Player p = DomainFixtures.playerAt(new Position(2, 2)).withPendingMoveCount(1);
    DungeonState s = new DungeonState(mapWithBreakableWall(), p, List.of(), TurnPhase.PLAYER_TURN);

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.Move(Direction.RIGHT));

    assertFalse(result.wasRejected());
    // events[0] = WallBroken (世界の書き換えを通知)
    assertTrue(result.events().get(0) instanceof BattleEvent.WallBroken);
    BattleEvent.WallBroken wb = (BattleEvent.WallBroken) result.events().get(0);
    assertEquals(new Position(3, 2), wb.position());
    // events[1] = Moved (プレイヤー位置更新を通知)
    assertTrue(result.events().get(1) instanceof BattleEvent.Moved);
    BattleEvent.Moved moved = (BattleEvent.Moved) result.events().get(1);
    assertEquals(new Position(2, 2), moved.from());
    assertEquals(new Position(3, 2), moved.to());
  }

  @Test
  void normalMoveCannotPassThroughBreakableWall() {
    // 移動権なし (pendingMoveCount = 0) で通常 AP 移動 → BREAKABLE_WALL は reject される (移動カード専用ギミック)。
    Player p = DomainFixtures.playerAt(new Position(2, 2)); // pendingMoveCount=0
    DungeonState s = new DungeonState(mapWithBreakableWall(), p, List.of(), TurnPhase.PLAYER_TURN);

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.Move(Direction.RIGHT));

    assertTrue(result.wasRejected());
    // BREAKABLE_WALL は壊れず残る
    assertEquals(Tile.BREAKABLE_WALL, result.state().map().tileAt(new Position(3, 2)));
    // プレイヤー位置も変わらない
    assertEquals(new Position(2, 2), result.state().player().position());
  }
}
