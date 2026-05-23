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
      return TurnEngineHelpers.reject(state, player.id(), "AP 不足");
    }
    Position next = player.position().move(direction);
    if (state.findEnemyAt(next).isPresent()) {
      return TurnEngineHelpers.reject(state, player.id(), "そこへは移動できない");
    }
    // Wave 11 W11-α: 移動カード由来の移動権使用中なら、隣接 BREAKABLE_WALL を破壊して通過可能。
    // 通常移動 (AP 1 消費) では BREAKABLE_WALL を通過できない (移動カード専用ギミック)。
    boolean breakingWall =
        usingPendingMove
            && state.map().inBounds(next)
            && state.map().tileAt(next) == Tile.BREAKABLE_WALL;
    if (!breakingWall && !state.map().isWalkable(next)) {
      return TurnEngineHelpers.reject(state, player.id(), "そこへは移動できない");
    }
    List<BattleEvent> events = new ArrayList<>();
    DungeonState afterMove = state;
    // CTO チェックポイント #2 (3 段順序厳守):
    // (1) 世界の書き換え → (2) WallBroken イベント発火 → (3) アクター位置更新。
    // 順序を逆にすると「BREAKABLE_WALL の上にプレイヤーが乗っている」中間状態が生じる。
    if (breakingWall) {
      afterMove = afterMove.withMap(state.map().withTileAt(next, Tile.FLOOR));
      events.add(new BattleEvent.WallBroken(next));
    }
    Player moved;
    if (usingPendingMove) {
      moved = player.withPosition(next).withPendingMoveCount(player.pendingMoveCount() - 1);
    } else {
      moved = player.withPosition(next).withActionPoints(player.actionPoints().spend(1));
    }
    afterMove = afterMove.withPlayer(moved);
    events.add(new BattleEvent.Moved(player.id(), player.position(), next));
    if (afterMove.map().tileAt(next) == Tile.STAIRS_DOWN) {
      afterMove = afterMove.withPhase(TurnPhase.CLEARED);
      events.add(new BattleEvent.TurnPhaseChanged(TurnPhase.CLEARED));
    }
    // ADR-22: 罠踏み判定 (Player が罠タイルに進入したか)。CLEARED 時もダメージは入る (踏破直前の罠で死亡もあり得る)。
    afterMove = TurnEngineHelpers.checkAndTriggerTrap(afterMove, moved.id(), next, true, events);
    return new StepResult(afterMove, events);
  }

  /** 敵の単純移動 (1 マス、AP 1 消費)。移動先が壁・他アクタなら reject、罠タイルなら踏破判定を呼ぶ。 */
  static StepResult applyEnemyMove(DungeonState state, Enemy enemy, Direction direction) {
    if (!enemy.actionPoints().canSpend(1)) {
      return TurnEngineHelpers.reject(state, enemy.id(), "AP 不足");
    }
    Position next = enemy.position().move(direction);
    if (!state.map().isWalkable(next) || state.isPositionOccupied(next)) {
      return TurnEngineHelpers.reject(state, enemy.id(), "そこへは移動できない");
    }
    Enemy moved = enemy.withPosition(next).withActionPoints(enemy.actionPoints().spend(1));
    DungeonState afterMove = state.withEnemyReplaced(moved);
    List<BattleEvent> events = new ArrayList<>();
    events.add(new BattleEvent.Moved(enemy.id(), enemy.position(), next));
    afterMove = TurnEngineHelpers.checkAndTriggerTrap(afterMove, moved.id(), next, false, events);
    return new StepResult(afterMove, events);
  }
}
