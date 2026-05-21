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
