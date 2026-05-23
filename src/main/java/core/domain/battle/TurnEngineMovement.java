package core.domain.battle;

import core.domain.battle.TurnEngine.StepResult;
import core.domain.common.Direction;
import core.domain.common.Position;
import core.domain.dungeon.DungeonState;
import core.domain.dungeon.Tile;
import core.domain.entity.Enemy;
import core.domain.entity.Player;
import java.util.ArrayList;
import java.util.List;

/**
 * 移動アクション解決 (§15-5 / ADR-21、Wave 5 W5-α-1)。
 *
 * <p>{@link TurnEngine} から独立した移動ロジック。プレイヤー / 敵の単純移動と、移動権 ({@code pendingMoveCount}) 消費を扱う。 罠踏みは
 * {@link TurnEngine#checkAndTriggerTrap} に委譲する (同パッケージから呼べる package-private)。
 *
 * <p>純関数ユーティリティ: 入力 state は不変、副作用なし、結果は新 {@link StepResult} で返す。
 */
final class TurnEngineMovement {

  private TurnEngineMovement() {}

  /**
   * プレイヤー移動 (§15-5 / ADR-21)。{@code pendingMoveCount > 0} なら AP 消費なし (移動権使用)、 それ以外は AP 1 消費 (通常移動)。
   *
   * <p>ADR-20 / ADR-21 「移動カード切る → distance ぶん AWSD で連続移動」を本メソッドで実装。 階段踏破時は CLEARED フェーズに遷移する。
   */
  static StepResult applyPlayerMove(DungeonState state, Direction direction) {
    Player player = state.player();
    boolean usingPendingMove = player.pendingMoveCount() > 0;
    if (!usingPendingMove && !player.actionPoints().canSpend(1)) {
      return TurnEngine.reject(state, player.id(), "AP 不足");
    }
    Position next = player.position().move(direction);
    if (!state.map().isWalkable(next) || state.findEnemyAt(next).isPresent()) {
      return TurnEngine.reject(state, player.id(), "そこへは移動できない");
    }
    Player moved;
    if (usingPendingMove) {
      moved = player.withPosition(next).withPendingMoveCount(player.pendingMoveCount() - 1);
    } else {
      moved = player.withPosition(next).withActionPoints(player.actionPoints().spend(1));
    }
    DungeonState afterMove = state.withPlayer(moved);
    List<BattleEvent> events = new ArrayList<>();
    events.add(new BattleEvent.Moved(player.id(), player.position(), next));
    if (state.map().tileAt(next) == Tile.STAIRS_DOWN) {
      afterMove = afterMove.withPhase(TurnPhase.CLEARED);
      events.add(new BattleEvent.TurnPhaseChanged(TurnPhase.CLEARED));
    }
    // ADR-22: 罠踏み判定 (Player が罠タイルに進入したか)。CLEARED 時もダメージは入る (踏破直前の罠で死亡もあり得る)。
    afterMove = TurnEngine.checkAndTriggerTrap(afterMove, moved.id(), next, true, events);
    return new StepResult(afterMove, events);
  }

  /** 敵の単純移動 (1 マス、AP 1 消費)。移動先が壁・他アクタなら reject、罠タイルなら踏破判定を呼ぶ。 */
  static StepResult applyEnemyMove(DungeonState state, Enemy enemy, Direction direction) {
    if (!enemy.actionPoints().canSpend(1)) {
      return TurnEngine.reject(state, enemy.id(), "AP 不足");
    }
    Position next = enemy.position().move(direction);
    if (!state.map().isWalkable(next) || state.isPositionOccupied(next)) {
      return TurnEngine.reject(state, enemy.id(), "そこへは移動できない");
    }
    Enemy moved = enemy.withPosition(next).withActionPoints(enemy.actionPoints().spend(1));
    DungeonState afterMove = state.withEnemyReplaced(moved);
    List<BattleEvent> events = new ArrayList<>();
    events.add(new BattleEvent.Moved(enemy.id(), enemy.position(), next));
    afterMove = TurnEngine.checkAndTriggerTrap(afterMove, moved.id(), next, false, events);
    return new StepResult(afterMove, events);
  }
}
