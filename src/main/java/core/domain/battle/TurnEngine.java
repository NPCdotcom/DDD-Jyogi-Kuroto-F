package core.domain.battle;

import core.domain.card.ActiveBuff;
import core.domain.card.TrapLifetime;
import core.domain.common.Position;
import core.domain.dungeon.DungeonState;
import core.domain.dungeon.PlacedTrap;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.Player;
import core.domain.entity.PlayerStatuses;
import core.domain.entity.Stats;
import core.domain.meta.Gold;
import core.domain.meta.Soul;
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
      return reject(state, state.player().id(), "プレイヤーのターンではない");
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
    // ADR-25 / ADR-27: アクティブ Buff の残ターンを 1 減らし、0 (expired) は除去する。
    // PlacedTrap.Turns と同型パターン (ADR-22)。effectiveStats() で AP リフィル量が変化するため、
    // Buff 期限切れ処理を AP リフィルより先に行う。
    List<ActiveBuff> aliveBuffs = new ArrayList<>(p.statuses().activeBuffs().size());
    for (ActiveBuff b : p.statuses().activeBuffs()) {
      ActiveBuff decremented = b.decrementedTurn();
      if (!decremented.isExpired()) {
        aliveBuffs.add(decremented);
      }
    }
    PlayerStatuses buffsApplied = p.statuses().withActiveBuffs(aliveBuffs);
    Player buffsDecremented = p.withStatuses(buffsApplied);
    // ADR-21: pendingMoveCount はターンまたぎ持ち越さない (移動権は当該ターンに使い切る)。
    // ADR-25: effectiveStats().speed() を使い、装備 + Buff 込みの速度で AP をリフィルする。
    Player refreshed =
        buffsDecremented
            .withActionPoints(
                buffsDecremented
                    .actionPoints()
                    .refilledTo(buffsDecremented.effectiveStats().speed()))
            .withCardPileState(buffsDecremented.cardPileState().drawN(1, rng))
            .withPendingMoveCount(0);
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

  /** 敵 1 体のアクションを解決する。 */
  public static StepResult resolveEnemyAction(
      DungeonState state, ActorId enemyId, BattleAction action) {
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(enemyId, "enemyId");
    Objects.requireNonNull(action, "action");
    if (state.phase() != TurnPhase.ENEMY_TURN) {
      return reject(state, enemyId, "敵のターンではない");
    }
    Optional<Enemy> enemyOpt = state.findEnemy(enemyId);
    if (enemyOpt.isEmpty()) {
      return reject(state, enemyId, "敵が存在しない");
    }
    Enemy enemy = enemyOpt.get();
    return switch (action) {
      case BattleAction.Move move ->
          TurnEngineMovement.applyEnemyMove(state, enemy, move.direction());
      case BattleAction.UseSkill use ->
          TurnEngineSkillResolver.applyEnemySkill(state, enemy, use.slotIndex());
      // §15-3 / ADR-18: 敵はカードを使わない (Skill ベース)。誤って UseCard が渡されたら reject。
      case BattleAction.UseCard ignored -> reject(state, enemy.id(), "敵はカードを使えない");
      case BattleAction.Wait ignored -> applyEnemyWait(state, enemy);
      case BattleAction.EndTurn ignored -> new StepResult(state, List.of());
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
      return reject(state, player.id(), "AP 不足");
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
      return reject(state, enemy.id(), "AP 不足");
    }
    Enemy after = enemy.withActionPoints(enemy.actionPoints().spend(1));
    return new StepResult(state.withEnemyReplaced(after), List.of());
  }

  // ===================================================================================
  //  効果適用 (Damage etc.)
  // ===================================================================================

  // Wave 5 W5-α-3: applyEffectByPlayer / applyEffectByEnemy / resolveSkillDamage
  // / resolveDamageToPlayer は {@link TurnEngineSkillResolver} に切り出し済。

  /**
   * 敵にダメージを適用する共通ヘルパ (ADR-18 で int finalDamage 受け取りに統一)。
   *
   * <p>Skill 経路 ({@link #resolveSkillDamage} 計算結果) と Card 経路 ({@code CardEffect.Damage.resolve}
   * 計算結果) のどちらからも、防御適用済みの確定 int を受け取る。本メソッドは二重減算しない。
   */
  static StepResult resolveDamageToEnemy(
      DungeonState state, int finalDamage, Enemy target, List<BattleEvent> events) {
    Stats damagedStats = target.stats().damaged(finalDamage);
    events.add(
        new BattleEvent.DamageDealt(
            state.player().id(), target.id(), finalDamage, damagedStats.currentHp()));
    if (!damagedStats.isAlive()) {
      events.add(new BattleEvent.ActorDied(target.id()));
      int soulReward = target.kind().soulReward();
      int goldReward = target.kind().goldReward();
      // §15-2: 撃破時に Soul + Gold を加算 (敵種ごとのレート)。
      Player rewardedPlayer =
          state.player().addSoul(new Soul(soulReward)).addGold(new Gold(goldReward));
      events.add(new BattleEvent.SoulGained(rewardedPlayer.id(), soulReward));
      events.add(new BattleEvent.GoldGained(rewardedPlayer.id(), goldReward));
      // §15-3 / §15-6: 強化個体撃破時にプレゼン層でカード追加 UI を発火するためのトリガ。
      if (target.kind().isElite()) {
        events.add(new BattleEvent.EliteDefeated(target.id()));
      }
      DungeonState ns = state.withEnemyRemoved(target.id()).withPlayer(rewardedPlayer);
      // 階段踏破での CLEARED 遷移は applyPlayerMove で行う。敵全滅では CLEARED にしない
      // (敵 1 体撃破で即クリアになるのを避けるため)。
      // §15-6 例外: ボス撃破 = ラン勝利 (RUN_CLEARED)。ボスは最終層・最終部屋にのみ配置され、
      // そのフロアに階段は無いため、ボス撃破が唯一の進行手段となる。
      if (target.kind().isBoss()) {
        ns = ns.withPhase(TurnPhase.RUN_CLEARED);
        events.add(new BattleEvent.TurnPhaseChanged(TurnPhase.RUN_CLEARED));
      }
      return new StepResult(ns, events);
    }
    Enemy hit = target.withStats(damagedStats);
    return new StepResult(state.withEnemyReplaced(hit), events);
  }

  // ===================================================================================
  //  Helpers
  // ===================================================================================

  /**
   * 失敗アクションを {@link BattleEvent.ActionRejected} で表現する共通ヘルパ。
   *
   * <p>Wave 5 W5-α-1 以降、同パッケージの {@link TurnEngineMovement} 等から呼べるよう package-private。
   */
  static StepResult reject(DungeonState state, ActorId who, String reason) {
    return new StepResult(state, List.of(new BattleEvent.ActionRejected(who, reason)));
  }

  /**
   * 罠踏み判定 + ダメージ適用の共通ヘルパ (§15-3 / ADR-22)。プレイヤー / 敵どちらの移動でも呼ばれる。
   *
   * <ol>
   *   <li>{@code at} に罠があるか {@link DungeonState#findTrapAt} で確認、無ければ state をそのまま返す
   *   <li>{@link PlacedTrap#resolveDamage(Stats)} で最終ダメージを確定 (element に応じた物防/魔防参照)
   *   <li>victim の Stats を damaged で減算、{@link BattleEvent.TrapTriggered} 発火
   *   <li>{@link TrapLifetime.UntilStepped} なら罠を除去、{@link TrapLifetime.Turns} なら維持
   *   <li>victim 死亡時は ActorDied + プレイヤーなら GAME_OVER、敵なら撃破報酬 + 除去
   * </ol>
   */
  static DungeonState checkAndTriggerTrap(
      DungeonState state,
      ActorId victimId,
      Position at,
      boolean isPlayer,
      List<BattleEvent> events) {
    Optional<PlacedTrap> trapOpt = state.findTrapAt(at);
    if (trapOpt.isEmpty()) {
      return state;
    }
    PlacedTrap trap = trapOpt.get();
    // 防御計算はプレイヤーなら実効ステ (装備/Buff 込み)、敵は素ステを使う。
    Stats defenseStats =
        isPlayer
            ? state.player().effectiveStats()
            : state.findEnemy(victimId).orElseThrow().stats();
    int damage = trap.resolveDamage(defenseStats);
    // HP 減算は素ステに対して行う (effectiveStats を damaged → withStats すると実効値が素ステに焼き付くため)。
    Stats victimBaseStats =
        isPlayer ? state.player().stats() : state.findEnemy(victimId).orElseThrow().stats();
    Stats damagedStats = victimBaseStats.damaged(damage);
    events.add(new BattleEvent.TrapTriggered(victimId, at, damage, damagedStats.currentHp()));

    // UntilStepped なら除去、Turns なら維持 (3 並列レビュー結論、物理/魔法の対比)。
    List<PlacedTrap> updatedTraps = new ArrayList<>(state.placedTraps().size());
    for (PlacedTrap t : state.placedTraps()) {
      if (t.position().equals(at)) {
        if (t.lifetime() instanceof TrapLifetime.Turns) {
          updatedTraps.add(t);
        }
        // UntilStepped は除去 (updatedTraps に追加しない)
      } else {
        updatedTraps.add(t);
      }
    }
    DungeonState ns = state.withPlacedTraps(updatedTraps);

    if (isPlayer) {
      Player victimPlayer = state.player().withStats(damagedStats);
      ns = ns.withPlayer(victimPlayer);
      if (!damagedStats.isAlive()) {
        events.add(new BattleEvent.ActorDied(victimPlayer.id()));
        ns = ns.withPhase(TurnPhase.GAME_OVER);
        events.add(new BattleEvent.TurnPhaseChanged(TurnPhase.GAME_OVER));
      }
    } else {
      Enemy victimEnemy = state.findEnemy(victimId).orElseThrow().withStats(damagedStats);
      if (!damagedStats.isAlive()) {
        events.add(new BattleEvent.ActorDied(victimId));
        int soulReward = victimEnemy.kind().soulReward();
        int goldReward = victimEnemy.kind().goldReward();
        // §15-2: 罠撃破でも通常撃破と同じレートで Soul + Gold を加算。
        Player rewardedPlayer =
            ns.player().addSoul(new Soul(soulReward)).addGold(new Gold(goldReward));
        events.add(new BattleEvent.SoulGained(rewardedPlayer.id(), soulReward));
        events.add(new BattleEvent.GoldGained(rewardedPlayer.id(), goldReward));
        // §15-3 / §15-6: Elite が罠で死亡した場合もカード追加 UI を発火。
        if (victimEnemy.kind().isElite()) {
          events.add(new BattleEvent.EliteDefeated(victimId));
        }
        ns = ns.withEnemyRemoved(victimId).withPlayer(rewardedPlayer);
        // §15-6 例外: 罠でボスを倒した場合もラン勝利 (resolveDamageToEnemy と同じ扱い)。
        if (victimEnemy.kind().isBoss()) {
          ns = ns.withPhase(TurnPhase.RUN_CLEARED);
          events.add(new BattleEvent.TurnPhaseChanged(TurnPhase.RUN_CLEARED));
        }
      } else {
        ns = ns.withEnemyReplaced(victimEnemy);
      }
    }
    return ns;
  }

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
