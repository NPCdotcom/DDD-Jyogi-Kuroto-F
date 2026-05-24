package core.domain.battle;

import core.domain.battle.TurnEngine.StepResult;
import core.domain.card.TrapLifetime;
import core.domain.common.Position;
import core.domain.dungeon.DungeonState;
import core.domain.dungeon.PlacedTrap;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.domain.meta.Gold;
import core.domain.meta.Soul;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 戦闘・移動・スキル解決に共通する純関数ヘルパ群 (Wave 7 W7-α で TurnEngine から切り出し)。
 *
 * <p>本クラスは TurnEngine 本体 + TurnEngineMovement / TurnEngineCardResolver / TurnEngineSkillResolver の
 * 4 箇所から呼ばれる「共通インフラ」。Wave 5 W5-α 系で 3 リゾルバを分離した際、 共通ヘルパは TurnEngine 本体に package-private で残置していた
 * (チェーン依存方向回避)。 Wave 7 では独立クラス化し、TurnEngine 本体を「ターン進行フロー制御」だけに絞る。
 *
 * <p>純関数: state を不変として扱い、結果は新 {@link StepResult} or 新 {@link DungeonState} で返す。 副作用なし、I/O なし、乱数なし。
 */
final class TurnEngineHelpers {

  private TurnEngineHelpers() {}

  /**
   * 失敗アクションを {@link BattleEvent.ActionRejected} で表現する共通ヘルパ。
   *
   * <p>同パッケージの TurnEngine / Movement / CardResolver / SkillResolver から呼ばれる package-private。
   */
  static StepResult reject(DungeonState state, ActorId who, String reason) {
    return new StepResult(state, List.of(new BattleEvent.ActionRejected(who, reason)));
  }

  /**
   * 敵にダメージを適用する共通ヘルパ (ADR-18 で int finalDamage 受け取りに統一)。
   *
   * <p>Skill 経路 ({@code TurnEngineSkillResolver.resolveSkillDamage} 計算結果) と Card 経路 ({@code
   * CardEffect.Damage.resolve} 計算結果) のどちらからも、防御適用済みの確定 int を受け取る。本メソッドは二重減算しない。
   *
   * <p>撃破処理: HP <= 0 で {@link BattleEvent.ActorDied} 発火 + Soul / Gold 加算 + Elite なら {@link
   * BattleEvent.EliteDefeated} + Boss なら {@link TurnPhase#RUN_CLEARED} 遷移。
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
}
