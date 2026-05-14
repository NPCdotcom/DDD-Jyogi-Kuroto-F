package core.application;

import core.domain.battle.BattleAction;
import core.domain.battle.EnemyAi;
import core.domain.battle.TurnEngine;
import core.domain.battle.TurnEngine.StepResult;
import core.domain.battle.TurnPhase;
import core.domain.dungeon.DungeonState;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * ターン進行を取り仕切るアプリケーションサービス。
 *
 * <p>ドメインの純関数 ({@link TurnEngine}, {@link EnemyAi}) を呼び出し、その結果を {@link GameContext} に反映する。
 *
 * <p>役割:
 *
 * <ul>
 *   <li>プレイヤー行動を 1 つ受け取って解決する
 *   <li>AP 切れになったら自動でターン終了 → 敵ターンへ
 *   <li>敵ターンを 1 体ずつ AI で進行させ、終わったらプレイヤーターンに戻す
 * </ul>
 */
public final class TurnDirector {

  /**
   * 万一のループ暴走を防ぐ最大ステップ数 (敵 1 体あたり)。
   *
   * <p>算出根拠: 敵 1 体の AP_MAX (現状 3) + 速度 (現状 2) の組合せでも、AP 切れまで進めば 最大数手で必ず終わる。安全マージンを大幅に取って 16
   * を上限とする。EnemyAi 側で AP 不足時に Wait を返す保険と、TurnEngine が reject 時に状態を変えない保険で、通常はこの上限に達しない。
   */
  private static final int ENEMY_STEP_HARD_LIMIT = 16;

  private final GameContext context;
  private final Random rng;

  public TurnDirector(GameContext context, Random rng) {
    this.context = Objects.requireNonNull(context, "context");
    this.rng = Objects.requireNonNull(rng, "rng");
  }

  public void applyPlayerAction(BattleAction action) {
    Objects.requireNonNull(action, "action");
    if (context.state().phase() != TurnPhase.PLAYER_TURN) {
      return;
    }
    StepResult result = TurnEngine.resolvePlayerAction(context.state(), action);
    context.applyResult(result);
    autoEndPlayerTurnIfApDepleted();
  }

  private void autoEndPlayerTurnIfApDepleted() {
    DungeonState s = context.state();
    if (s.phase() == TurnPhase.PLAYER_TURN && s.player().actionPoints().isEmpty()) {
      StepResult ended =
          TurnEngine.resolvePlayerAction(context.state(), new BattleAction.EndTurn());
      context.applyResult(ended);
    }
  }

  /** ENEMY_TURN 中に呼び、全敵 AI を進行させて PLAYER_TURN まで戻す。 */
  public void runEnemyTurn() {
    if (context.state().phase() != TurnPhase.ENEMY_TURN) {
      return;
    }
    for (Enemy enemy : context.state().enemies()) {
      driveEnemy(enemy.id());
      if (context.state().phase() == TurnPhase.GAME_OVER) {
        return; // プレイヤー死亡で打ち切り
      }
    }
    if (context.state().phase() == TurnPhase.ENEMY_TURN) {
      // ADR-19: AP リフィル + 1 枚ドローを 1 呼出で実行 (Random は引数注入)
      StepResult r = TurnEngine.startPlayerTurn(context.state(), rng);
      context.applyResult(r);
    }
  }

  private void driveEnemy(ActorId id) {
    for (int step = 0; step < ENEMY_STEP_HARD_LIMIT; step++) {
      DungeonState current = context.state();
      if (current.phase() != TurnPhase.ENEMY_TURN) {
        return;
      }
      Optional<Enemy> me = current.findEnemy(id);
      if (me.isEmpty() || me.get().actionPoints().isEmpty()) {
        return;
      }
      BattleAction decision = EnemyAi.decide(me.get(), current);
      StepResult r = TurnEngine.resolveEnemyAction(current, id, decision);
      context.applyResult(r);
      if (r.wasRejected()) {
        return; // 同じ選択で無限ループするのを防ぐ
      }
    }
  }
}
