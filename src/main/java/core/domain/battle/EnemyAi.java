package core.domain.battle;

import core.domain.common.Direction;
import core.domain.common.Position;
import core.domain.dungeon.DungeonState;
import core.domain.entity.Enemy;
import core.domain.entity.EnemyAiProfile;
import core.domain.entity.EnemyAiState;
import core.domain.skill.Skill;
import java.util.Objects;
import java.util.Optional;

/**
 * 敵 AI の意思決定 (Wave 13 W13-β: 視界 + 状態遷移 + kind 別行動プロファイル)。
 *
 * <p>純関数で副作用を持たない。視界判定 → 状態遷移計算 ({@link #computeNewState}) → 行動決定 ({@link #decide}) の 2 段構造:
 *
 * <ul>
 *   <li>{@link EnemyAiState#IDLE}: その場で待機 (KISS、ランダムウォークは Wave 14+ 送り)。
 *   <li>{@link EnemyAiState#ALERT}: kind の {@link EnemyAiProfile} に応じて行動 (AGGRESSIVE = BFS 追跡 /
 *       CAUTIOUS = 距離保ち)。
 *   <li>{@link EnemyAiState#SEARCHING}: {@code lastKnownPlayerPos} に向けて BFS 移動。到達後 IDLE 戻り
 *       (computeNewState で実施)。
 * </ul>
 *
 * <p>状態遷移は {@link #computeNewState} で純関数として計算し、{@link TurnEngine#resolveEnemyAction} 側で {@code
 * state.withEnemyReplaced(updatedEnemy)} として反映する (EnemyAi 自体はステートレス、ドメインルールの汚染を回避)。
 */
public final class EnemyAi {

  private EnemyAi() {}

  /**
   * 視界判定 + 状態遷移を計算した新 Enemy を返す純関数 (Wave 13 W13-β)。{@link TurnEngine#resolveEnemyAction}
   * が冒頭で呼び、{@code state.withEnemyReplaced(...)} で反映する。
   *
   * <ul>
   *   <li>視界内 (canSeeWithin true) → {@link EnemyAiState#ALERT} に遷移、{@code lastKnownPlayerPos =
   *       Optional.of(player.position())} で記憶を更新
   *   <li>視界外 + ALERT → {@link EnemyAiState#SEARCHING} に遷移 (lastKnownPlayerPos 保持)
   *   <li>視界外 + SEARCHING + 既に lastKnownPlayerPos に到達 → {@link EnemyAiState#IDLE} 戻り + 記憶クリア
   *   <li>視界外 + SEARCHING + 未到達 → SEARCHING 継続
   *   <li>視界外 + IDLE → IDLE 継続
   * </ul>
   */
  public static Enemy computeNewState(Enemy enemy, DungeonState state) {
    Objects.requireNonNull(enemy, "enemy");
    Objects.requireNonNull(state, "state");
    Position me = enemy.position();
    Position playerPos = state.player().position();
    boolean canSee = state.map().canSeeWithin(me, playerPos, enemy.kind().sightRange());

    if (canSee) {
      return enemy.withAiState(EnemyAiState.ALERT).withLastKnownPlayerPos(Optional.of(playerPos));
    }
    // 視界外
    return switch (enemy.aiState()) {
      case ALERT -> enemy.withAiState(EnemyAiState.SEARCHING); // lastKnownPlayerPos 保持
      case SEARCHING -> {
        Optional<Position> last = enemy.lastKnownPlayerPos();
        if (last.isEmpty()) {
          yield enemy.withAiState(EnemyAiState.IDLE); // 安全弁: 記憶喪失なら IDLE
        }
        if (me.equals(last.get())) {
          yield enemy.withAiState(EnemyAiState.IDLE).withLastKnownPlayerPos(Optional.empty());
        }
        yield enemy; // SEARCHING 継続
      }
      case IDLE -> enemy; // IDLE 継続
    };
  }

  /**
   * 行動決定 (Wave 13 W13-β)。冒頭で {@link #computeNewState} を呼んで観測情報を確定し、新状態に応じた行動を返す。 戻り値は {@link
   * BattleAction} のみ (Enemy 状態更新は {@link TurnEngine#resolveEnemyAction} 側で別途実施、純関数)。
   */
  public static BattleAction decide(Enemy enemy, DungeonState state) {
    Objects.requireNonNull(enemy, "enemy");
    Objects.requireNonNull(state, "state");
    Enemy observed = computeNewState(enemy, state);
    return switch (observed.aiState()) {
      case ALERT -> decideAlertAction(observed, state);
      case SEARCHING -> decideSearchingAction(observed, state);
      case IDLE -> new BattleAction.Wait(); // KISS: IDLE はその場待機
    };
  }

  // ALERT 中の行動: AiProfile で分岐。
  // CAUTIOUS は「隣接時に逃げる」が原則なので、AGGRESSIVE と別経路で隣接攻撃判定を行う。
  private static BattleAction decideAlertAction(Enemy enemy, DungeonState state) {
    Position playerPos = state.player().position();
    return switch (enemy.kind().aiProfile()) {
      case AGGRESSIVE -> aggressiveAction(enemy, state, playerPos);
      case CAUTIOUS -> cautiousAction(enemy, state, playerPos);
    };
  }

  // AGGRESSIVE: 隣接攻撃を優先、それ以外は BFS で詰める (既存挙動)。
  private static BattleAction aggressiveAction(
      Enemy enemy, DungeonState state, Position playerPos) {
    Position me = enemy.position();
    if (me.isAdjacentTo(playerPos) && canUseSkill(enemy, 0)) {
      return new BattleAction.UseSkill(0);
    }
    return chaseToward(enemy, state, playerPos);
  }

  // CAUTIOUS (kite 型): 距離 2-3 を保つ、近すぎたら離れる、遠すぎたら近づく。
  // CTO チェックポイント #3: 袋小路フォールバック (逃げ場なし時は隣接攻撃 or 待機)。
  private static BattleAction cautiousAction(Enemy enemy, DungeonState state, Position playerPos) {
    Position me = enemy.position();
    int chebyshev = chebyshevDistance(me, playerPos);
    if (chebyshev < 2) {
      // 近すぎ: 離れる方向を探す
      Optional<Direction> flee = findFleeDirection(me, playerPos, state);
      if (flee.isPresent() && enemy.actionPoints().canSpend(1)) {
        return new BattleAction.Move(flee.get());
      }
      // フォールバック: 隣接プレイヤーなら腹を括って攻撃 (CTO #3)
      if (me.isAdjacentTo(playerPos) && canUseSkill(enemy, 0)) {
        return new BattleAction.UseSkill(0);
      }
      return new BattleAction.Wait();
    }
    if (chebyshev > 3) {
      // 遠すぎ: BFS 追跡
      return chaseToward(enemy, state, playerPos);
    }
    // 理想射程 2-3: 待機 (Wave 14+ で敵側遠距離スキルを発動)
    return new BattleAction.Wait();
  }

  // SEARCHING: lastKnownPlayerPos に向けて BFS 移動。
  private static BattleAction decideSearchingAction(Enemy enemy, DungeonState state) {
    Optional<Position> last = enemy.lastKnownPlayerPos();
    if (last.isEmpty()) {
      return new BattleAction.Wait(); // 安全弁: computeNewState で IDLE 化されているはず
    }
    return chaseToward(enemy, state, last.get());
  }

  // BFS 1 歩目で target に近づく (既存挙動の流用、AGGRESSIVE / SEARCHING 共通)。
  private static BattleAction chaseToward(Enemy enemy, DungeonState state, Position target) {
    Position me = enemy.position();
    if (me.equals(target)) {
      return new BattleAction.Wait();
    }
    Optional<Direction> step = state.map().firstStepToward(me, target);
    if (step.isEmpty() || !enemy.actionPoints().canSpend(1)) {
      return new BattleAction.Wait();
    }
    if (state.isPositionOccupied(me.move(step.get()))) {
      return new BattleAction.Wait();
    }
    return new BattleAction.Move(step.get());
  }

  /**
   * プレイヤーから離れる方向を 4 方向中で探す。マンハッタン距離が増える候補を {@code Direction.values()} の登録順で評価し、最初に見つかった walkable +
   * 他アクター不在のマスへ向かう方向を返す (テスト再現性のため決定的順序)。
   */
  private static Optional<Direction> findFleeDirection(
      Position me, Position playerPos, DungeonState state) {
    Direction[] dirs = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
    int currentDist = chebyshevDistance(me, playerPos);
    Direction best = null;
    int bestDist = currentDist;
    for (Direction d : dirs) {
      Position next = new Position(me.x() + d.dx(), me.y() + d.dy());
      if (!state.map().isWalkable(next) || state.isPositionOccupied(next)) {
        continue;
      }
      int newDist = chebyshevDistance(next, playerPos);
      if (newDist > bestDist) {
        best = d;
        bestDist = newDist;
      }
    }
    return Optional.ofNullable(best);
  }

  private static int chebyshevDistance(Position a, Position b) {
    return Math.max(Math.abs(a.x() - b.x()), Math.abs(a.y() - b.y()));
  }

  private static boolean canUseSkill(Enemy enemy, int slotIndex) {
    Optional<Skill> skill = enemy.skillSlot().at(slotIndex);
    return skill.isPresent() && enemy.actionPoints().canSpend(skill.get().apCost());
  }
}
