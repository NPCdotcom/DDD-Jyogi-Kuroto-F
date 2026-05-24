package core.domain.battle;

import core.domain.card.ActiveBuff;
import core.domain.dungeon.DungeonState;
import core.domain.dungeon.PlacedTrap;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.Player;
import core.domain.entity.PlayerStatuses;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * 戦闘・移動・ターン進行を司る純関数ユーティリティ。
 *
 * <p>I/O・乱数・時間に依存しない。入力 (DungeonState + BattleAction) から、新しい DungeonState と 発生イベントのリストを {@link
 * StepResult} として返す。
 *
 * <p>原則:
 *
 * <ul>
 *   <li>状態を渡された時点のスナップショットとして扱い、副作用を起こさない
 *   <li>失敗した行動 (AP 不足・行き先ブロック・フェーズ不一致など) も状態を変えず {@link BattleEvent.ActionRejected} のみを返す
 *   <li>switch 式の網羅性で全アクションを処理する (sealed の意義)
 * </ul>
 *
 * <p>§15-3 / ADR-18 で `BattleAction.UseCard` を追加、プレイヤーがカードを使って敵を殴れる。カードダメージ計算は {@link
 * TurnEngineCardResolver} (Wave 5 W5-α-2 切り出し) に委譲する。スキルダメージは ADR-17 改訂で固定値をやめ、 {@link
 * TurnEngineSkillResolver} (Wave 5 W5-α-3 切り出し) で被弾側の防御 (物防/魔防) を通す。 移動 / 罠踏みは {@link
 * TurnEngineMovement} (Wave 5 W5-α-1 切り出し) に分離。
 */
public final class TurnEngine {

  private TurnEngine() {}

  // ===================================================================================
  //  プレイヤーターン処理
  // ===================================================================================

  /** プレイヤーのアクションを 1 つ解決する。 */
  public static StepResult resolvePlayerAction(DungeonState state, BattleAction action) {
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(action, "action");
    if (state.phase() != TurnPhase.PLAYER_TURN) {
      return TurnEngineHelpers.reject(state, state.player().id(), "プレイヤーのターンではない");
    }
    return switch (action) {
      case BattleAction.Move move -> TurnEngineMovement.applyPlayerMove(state, move.direction());
      case BattleAction.UseSkill use ->
          TurnEngineSkillResolver.applyPlayerSkill(state, use.slotIndex());
      case BattleAction.UseCard use -> TurnEngineCardResolver.applyPlayerUseCard(state, use);
      case BattleAction.Wait ignored -> applyPlayerWait(state);
      case BattleAction.EndTurn ignored -> endPlayerTurn(state);
    };
  }

  /**
   * 敵ターン → プレイヤーターン遷移時に、プレイヤーの AP リフィル + 手札補充ドロー (§15-3 / ADR-19)。
   *
   * <p>処理順:
   *
   * <ol>
   *   <li>AP を速度ステ分まで全リセット (使い切り型、ADR-01)
   *   <li>山札から 1 枚ドロー (手札上限 9 枚で停止、山札切れ時は捨て札を {@code rng} で再シャッフル)
   * </ol>
   *
   * <p>Random は引数注入 (ドメイン副作用ゼロ、ADR-16 §4 と整合)。テスト時は固定シードで決定的に検証可能。
   *
   * @param state 遷移前の DungeonState (ENEMY_TURN 状態であること前提、検証は呼出側 TurnDirector に委ねる)
   * @param rng カード再シャッフル / 将来の確率処理に使う乱数源
   * @return AP リフィル + 1 枚ドロー済の新 DungeonState + TurnPhaseChanged イベント
   */
  public static StepResult startPlayerTurn(DungeonState state, Random rng) {
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(rng, "rng");
    Player p = state.player();
    // Wave 15 W15-α / #10: AP リフィル → Buff decrement の順に変更 (旧 SPEED_UP 持続1の論理消滅バグ修正)。
    // 持続1の SPEED_UP は「使用ターン中 effectiveStats() 経由で効力 + 次ターンリフィル時にも反映 → 消滅」の
    // 2 ターン挙動になる (§15-3 持続 N ターンの自然な解釈)。他バフ (ATTACK_UP / DEFENSE_UP) も
    // effectiveStats() 経由で使用ターン中既に効いているため、順序入れ替えは TurnEngineBuffTest 全件緑で検証済。
    // ADR-21: pendingMoveCount はターンまたぎ持ち越さない (移動権は当該ターンに使い切る)。
    // ADR-25: effectiveStats().speed() を使い、装備 + Buff 込みの速度で AP をリフィルする。
    Player refilled =
        p.withActionPoints(p.actionPoints().refilledTo(p.effectiveStats().speed()))
            .withPendingMoveCount(0);
    // ADR-25 / ADR-27: AP リフィル後にアクティブ Buff の残ターンを 1 減らし、0 (expired) は除去する。
    // PlacedTrap.Turns と同型パターン (ADR-22)。
    List<ActiveBuff> aliveBuffs = new ArrayList<>(refilled.statuses().activeBuffs().size());
    for (ActiveBuff b : refilled.statuses().activeBuffs()) {
      ActiveBuff decremented = b.decrementedTurn();
      if (!decremented.isExpired()) {
        aliveBuffs.add(decremented);
      }
    }
    PlayerStatuses buffsApplied = refilled.statuses().withActiveBuffs(aliveBuffs);
    // Wave 15 W15-α / #17: 毎ターンドローは「手札 5 枚補充」に変更 (drawN(1) → drawToHandSize(5))。
    // 山札 + 捨て札 < 5 のときは引けるだけ引く graceful、手札 >= 5 のときは no-op (3 段防衛線、CardPileState.drawToHandSize)。
    Player refreshed =
        refilled
            .withStatuses(buffsApplied)
            .withCardPileState(refilled.cardPileState().drawToHandSize(5, rng));
    // ADR-22: Turns 罠の remaining-- + expired (=0) を除去。UntilStepped 罠は据置。
    List<PlacedTrap> aliveTraps = new ArrayList<>(state.placedTraps().size());
    for (PlacedTrap t : state.placedTraps()) {
      PlacedTrap decremented = t.decrementedLifetime();
      if (decremented.isAlive()) {
        aliveTraps.add(decremented);
      }
    }
    DungeonState ns =
        state.withPlayer(refreshed).withPlacedTraps(aliveTraps).withPhase(TurnPhase.PLAYER_TURN);
    return new StepResult(ns, List.of(new BattleEvent.TurnPhaseChanged(TurnPhase.PLAYER_TURN)));
  }

  // ===================================================================================
  //  敵ターン処理
  // ===================================================================================

  /**
   * 敵 1 体のアクションを解決する (Wave 13 W13-β: 冒頭で {@link EnemyAi#computeNewState} を呼び、 視界判定 + AI 状態遷移 (IDLE
   * / ALERT / SEARCHING) を反映してから行動解決する)。
   */
  public static StepResult resolveEnemyAction(
      DungeonState state, ActorId enemyId, BattleAction action) {
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(enemyId, "enemyId");
    Objects.requireNonNull(action, "action");
    if (state.phase() != TurnPhase.ENEMY_TURN) {
      return TurnEngineHelpers.reject(state, enemyId, "敵のターンではない");
    }
    Optional<Enemy> enemyOpt = state.findEnemy(enemyId);
    if (enemyOpt.isEmpty()) {
      return TurnEngineHelpers.reject(state, enemyId, "敵が存在しない");
    }
    // Wave 13 W13-β: AI 状態 (aiState / lastKnownPlayerPos) を更新してから行動解決。
    // computeNewState は純関数 (視界判定 + 遷移計算)、副作用は state.withEnemyReplaced で反映。
    Enemy updatedEnemy = EnemyAi.computeNewState(enemyOpt.get(), state);
    DungeonState updatedState = state.withEnemyReplaced(updatedEnemy);
    return switch (action) {
      case BattleAction.Move move ->
          TurnEngineMovement.applyEnemyMove(updatedState, updatedEnemy, move.direction());
      case BattleAction.UseSkill use ->
          TurnEngineSkillResolver.applyEnemySkill(updatedState, updatedEnemy, use.slotIndex());
      // §15-3 / ADR-18: 敵はカードを使わない (Skill ベース)。誤って UseCard が渡されたら reject。
      case BattleAction.UseCard ignored ->
          TurnEngineHelpers.reject(updatedState, updatedEnemy.id(), "敵はカードを使えない");
      case BattleAction.Wait ignored -> applyEnemyWait(updatedState, updatedEnemy);
      case BattleAction.EndTurn ignored -> new StepResult(updatedState, List.of());
    };
  }

  /**
   * プレイヤーターン終了時に、全敵の AP を回復させて ENEMY_TURN に遷移させる。
   *
   * <p>敵 AP は {@link ActionPoints#refilled()} で「自分の max まで」回復する。max は spawn 時 ({@code
   * InitialStateFactory}) に層番号 (強化個体 +2 / ボス +α、ADR-06「敵 AP = 層番号」) で設定済みのため、 これを保ったまま満タンに戻る。{@code
   * refilledTo(stats().speed())} を使うと max が速度ステ値で上書きされ、 層が深くても敵 AP が固定化される (旧バグ)。プレイヤー側は速度が装備/Buff
   * で動くため {@code startPlayerTurn} では {@code refilledTo(effectiveStats().speed())} を使う (意図的な非対称)。
   */
  private static StepResult endPlayerTurn(DungeonState state) {
    List<Enemy> regenerated = new ArrayList<>(state.enemies().size());
    for (Enemy e : state.enemies()) {
      regenerated.add(e.withActionPoints(e.actionPoints().refilled()));
    }
    DungeonState ns = state.withEnemies(regenerated).withPhase(TurnPhase.ENEMY_TURN);
    return new StepResult(ns, List.of(new BattleEvent.TurnPhaseChanged(TurnPhase.ENEMY_TURN)));
  }

  // ===================================================================================
  //  プレイヤー個別アクション
  // ===================================================================================

  // Wave 5 W5-α-1: applyPlayerMove は {@link TurnEngineMovement#applyPlayerMove} に切り出し済。
  // Wave 5 W5-α-2: applyPlayerUseCard 系 5 メソッドは {@link TurnEngineCardResolver} に切り出し済。
  // Wave 5 W5-α-3: applyPlayerSkill は {@link TurnEngineSkillResolver#applyPlayerSkill} に切り出し済。

  private static StepResult applyPlayerWait(DungeonState state) {
    Player player = state.player();
    if (!player.actionPoints().canSpend(1)) {
      return TurnEngineHelpers.reject(state, player.id(), "AP 不足");
    }
    Player after = player.withActionPoints(player.actionPoints().spend(1));
    return new StepResult(state.withPlayer(after), List.of());
  }

  // ===================================================================================
  //  敵個別アクション
  // ===================================================================================

  // Wave 5 W5-α-1: applyEnemyMove は {@link TurnEngineMovement#applyEnemyMove} に切り出し済。
  // Wave 5 W5-α-3: applyEnemySkill は {@link TurnEngineSkillResolver#applyEnemySkill} に切り出し済。

  private static StepResult applyEnemyWait(DungeonState state, Enemy enemy) {
    if (!enemy.actionPoints().canSpend(1)) {
      return TurnEngineHelpers.reject(state, enemy.id(), "AP 不足");
    }
    Enemy after = enemy.withActionPoints(enemy.actionPoints().spend(1));
    return new StepResult(state.withEnemyReplaced(after), List.of());
  }

  // ===================================================================================
  //  効果適用 (Damage etc.)
  // ===================================================================================

  // Wave 5 W5-α-3: applyEffectByPlayer / applyEffectByEnemy / resolveSkillDamage
  // / resolveDamageToPlayer は {@link TurnEngineSkillResolver} に切り出し済。
  // Wave 7 W7-α: resolveDamageToEnemy / reject / checkAndTriggerTrap は {@link TurnEngineHelpers}
  // に切り出し済 (3 リゾルバ + TurnEngine 本体の共通ヘルパとして集約)。

  /** 1 ステップの解決結果。state は常に有効で、accepted 判定は events 側の有無で行う。 */
  public record StepResult(DungeonState state, List<BattleEvent> events) {
    public StepResult {
      Objects.requireNonNull(state, "state");
      Objects.requireNonNull(events, "events");
      events = List.copyOf(events);
    }

    public boolean wasRejected() {
      return events.stream().anyMatch(e -> e instanceof BattleEvent.ActionRejected);
    }
  }
}
