package core.application;

import core.domain.battle.BattleAction;
import core.domain.battle.BattleEvent;
import core.domain.battle.EnemyAi;
import core.domain.battle.TurnEngine;
import core.domain.battle.TurnEngine.StepResult;
import core.domain.battle.TurnPhase;
import core.domain.dungeon.DungeonState;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import java.util.List;
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
   * 敵ターン全体のループ暴走を防ぐ最大ステップ数。
   *
   * <p>1 ステップは必ず敵の AP を 1 消費するか reject された敵の AP を使い切らせるため、敵ターンは厳密に有限。 本上限は純粋な保険 (敵数 × AP_MAX
   * を大きく上回る値)。
   */
  private static final int ENEMY_TURN_STEP_CAP = 256;

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
    // pendingMoveCount > 0 (移動カードの移動権が残存) の間は自動終了しない。
    // 移動カードは AP を消費しつつ移動権を付与するため、AP 0 になっても移動権を使い切るまでターンを継続する。
    if (s.phase() == TurnPhase.PLAYER_TURN
        && s.player().actionPoints().isEmpty()
        && s.player().pendingMoveCount() == 0) {
      StepResult ended =
          TurnEngine.resolvePlayerAction(context.state(), new BattleAction.EndTurn());
      context.applyResult(ended);
    }
  }

  /** ENEMY_TURN 中に呼び、全敵 AI を即時進行させて PLAYER_TURN まで戻す (テスト・非演出用)。 */
  public void runEnemyTurn() {
    if (context.state().phase() != TurnPhase.ENEMY_TURN) {
      return;
    }
    int guard = 0;
    while (stepEnemyTurnOnce() && guard++ < ENEMY_TURN_STEP_CAP) {
      // 敵ターンが終わる (stepEnemyTurnOnce が false) まで全ステップを即時消化する。
    }
  }

  /**
   * 敵ターンを 1 アクションだけ進める (§15-5 敵行動の可視化)。
   *
   * <p>行動可能な敵 (AP 残あり) を先頭から探し、1 体に 1 アクションさせて {@code true} を返す (敵ターン継続)。 行動可能な敵がいなくなったら {@link
   * TurnEngine#startPlayerTurn} で AP リフィル + 1 ドローし {@code false} を返す。
   *
   * <p>DungeonScreen はこれをタイマー (約 0.1 秒間隔) で呼び、敵が 1 体ずつ動くのを見せる。{@link #runEnemyTurn} は
   * 内部でこれをループし敵ターンを即時消化する。
   */
  public boolean stepEnemyTurnOnce() {
    if (context.state().phase() != TurnPhase.ENEMY_TURN) {
      return false;
    }
    for (Enemy enemy : context.state().enemies()) {
      Optional<Enemy> me = context.state().findEnemy(enemy.id());
      if (me.isEmpty() || me.get().actionPoints().isEmpty()) {
        continue;
      }
      BattleAction decision = EnemyAi.decide(me.get(), context.state());
      StepResult r = TurnEngine.resolveEnemyAction(context.state(), enemy.id(), decision);
      context.applyResult(r);
      if (r.wasRejected()) {
        // reject されたら同じ選択での無限ループを避けるため、この敵の AP を使い切らせる。
        exhaustEnemy(enemy.id());
      }
      // プレイヤー死亡等で ENEMY_TURN を抜けた場合は「敵ターン継続」でないため false。
      return context.state().phase() == TurnPhase.ENEMY_TURN;
    }
    // 行動可能な敵なし → 敵ターン終了、AP リフィル + 1 ドローでプレイヤーターンへ。
    context.applyResult(TurnEngine.startPlayerTurn(context.state(), rng));
    return false;
  }

  /** reject された敵の AP を 0 にし、stepEnemyTurnOnce が同じ敵を再選択しないようにする (無限ループ保険)。 */
  private void exhaustEnemy(ActorId id) {
    context
        .state()
        .findEnemy(id)
        .ifPresent(
            e -> {
              Enemy drained =
                  e.withActionPoints(e.actionPoints().spend(e.actionPoints().current()));
              context.applyResult(
                  new StepResult(context.state().withEnemyReplaced(drained), List.of()));
            });
  }

  /**
   * 階段踏破後 (CLEARED 状態) に次の層へ進む (§15-6 / ADR-23)。
   *
   * <p>処理:
   *
   * <ol>
   *   <li>呼出側 (DddGame) が生成した次層 {@link DungeonState} を反映
   *   <li>{@link BattleEvent.FloorAdvanced} を発火し HUD ログに「N 層に到達」を表示
   *   <li>{@link TurnEngine#startPlayerTurn} 流用で AP リフィル + 1 枚ドロー (新層初手のリソースを整える)
   * </ol>
   *
   * <p>CLEARED 以外の状態で呼ばれた場合は no-op (DungeonScreen 側のキー入力で誤って呼ばれるケースを防ぐ二重ガード)。
   *
   * <p>次層 {@link DungeonState} の生成は呼出側に委ねる (依存逆転)。application 層は infrastructure 層を直接 import しない。
   */
  public void advanceFloor(DungeonState nextLayerState) {
    Objects.requireNonNull(nextLayerState, "nextLayerState");
    if (context.state().phase() != TurnPhase.CLEARED) {
      return;
    }
    context.applyResult(
        new StepResult(
            nextLayerState,
            List.of(new BattleEvent.FloorAdvanced(nextLayerState.layer().number()))));
    // 新層開始時の AP リフィル + 1 枚ドローを startPlayerTurn 流用で適用 (DRY)。
    // advanceLayer は既に PLAYER_TURN 状態を返しているため、startPlayerTurn 内の withPhase は no-op、
    // TurnPhaseChanged(PLAYER_TURN) イベントが「次層スタート = プレイヤーターン頭」として発火する。
    context.applyResult(TurnEngine.startPlayerTurn(context.state(), rng));
  }
}
