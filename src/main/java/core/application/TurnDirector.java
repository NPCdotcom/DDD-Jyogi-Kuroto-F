package core.application;

import core.domain.battle.BattleAction;
import core.domain.battle.BattleEvent;
import core.domain.battle.EnemyAi;
import core.domain.battle.TurnEngine;
import core.domain.battle.TurnEngine.StepResult;
import core.domain.battle.TurnPhase;
import core.domain.card.Card;
import core.domain.card.CardTag;
import core.domain.common.Direction;
import core.domain.common.Position;
import core.domain.dungeon.DungeonState;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.Player;
import java.util.ArrayList;
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
    autoEndPlayerTurnIfStuck();
  }

  /**
   * プレイヤーターン中で「意味のあるアクションが残っていない」場合に自動でターンを終了する (§15-5 詰み回避)。
   *
   * <p>判定:
   *
   * <ul>
   *   <li>AP 枯渇 (current = 0) かつ pendingMoveCount = 0 → 通常の AP 切れ自動終了 (イベント無発火、従来挙動)
   *   <li>AP は残るが {@link #hasMeaningfulAction} = false → 詰み自動終了 + {@link BattleEvent.AutoTurnEnded
   *       AutoTurnEnded(STUCK)} 発火
   *   <li>pendingMoveCount > 0 (移動権残存) → 自動終了しない (移動権を使い切るまで継続)
   * </ul>
   */
  private void autoEndPlayerTurnIfStuck() {
    DungeonState s = context.state();
    if (s.phase() != TurnPhase.PLAYER_TURN) {
      return;
    }
    Player player = s.player();
    if (player.pendingMoveCount() > 0) {
      return; // 移動権残あり、終了しない
    }
    boolean apEmpty = player.actionPoints().isEmpty();
    if (apEmpty) {
      // 従来挙動: AP 枯渇による自動終了 (AutoTurnEnded イベント無発火、通常のターン進行)
      StepResult ended = TurnEngine.resolvePlayerAction(s, new BattleAction.EndTurn());
      context.applyResult(ended);
      return;
    }
    if (!hasMeaningfulAction(s)) {
      // §15-5 詰み回避: 攻撃/防御カードも歩行先も無い → 自動終了 + AutoTurnEnded(STUCK)
      StepResult ended = TurnEngine.resolvePlayerAction(s, new BattleAction.EndTurn());
      List<BattleEvent> events = new ArrayList<>(ended.events());
      events.add(new BattleEvent.AutoTurnEnded(BattleEvent.AutoTurnEnded.Reason.STUCK));
      context.applyResult(new StepResult(ended.state(), List.copyOf(events)));
    }
  }

  /**
   * プレイヤーが「意味のあるアクション」を残しているかを判定する純関数 (§15-5 詰み回避ロジック)。
   *
   * <p>継続条件:
   *
   * <ul>
   *   <li>手札に AP コスト範囲内の {@link CardTag#ATTACK} or {@link CardTag#BUFF} カードあり → true (戦闘 / 防御の意思決定)
   *   <li>AP ≥ 1 かつ 4 方向 ({@link Direction#values}) のいずれかが歩行可能 + 敵/プレイヤー他不在 → true (通常移動で逃げ)
   * </ul>
   *
   * <p>非継続: {@link CardTag#TRAP} や {@link CardTag#MOVEMENT} カードしか無く、かつ歩行先も無い場合 (= 罠を置くしかなく
   * 周囲全部敵で塞がれている等)。
   *
   * @param s 現在のダンジョン状態
   * @return 意味のあるアクションが残っていれば true、詰みなら false
   */
  static boolean hasMeaningfulAction(DungeonState s) {
    Player p = s.player();
    int ap = p.actionPoints().current();
    if (ap <= 0) {
      return false;
    }
    for (Card c : p.cardPileState().hand().cards()) {
      if (c.apCost() > ap) {
        continue;
      }
      CardTag tag = c.tag();
      if (tag == CardTag.ATTACK || tag == CardTag.BUFF) {
        return true;
      }
    }
    Position pos = p.position();
    for (Direction d : Direction.values()) {
      Position next = pos.move(d);
      if (!s.map().isWalkable(next)) {
        continue;
      }
      if (s.isPositionOccupied(next)) {
        continue;
      }
      return true;
    }
    return false;
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
      if (me.isEmpty()) {
        continue;
      }
      Enemy current = me.get();
      if (current.actionPoints().isEmpty()) {
        continue;
      }
      BattleAction decision = EnemyAi.decide(current, context.state());
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
