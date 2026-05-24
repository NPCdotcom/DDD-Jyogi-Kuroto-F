package core.domain.battle;

import core.domain.battle.TurnEngine.StepResult;
import core.domain.card.CardElement;
import core.domain.common.Position;
import core.domain.dungeon.DungeonState;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.domain.skill.Skill;
import core.domain.skill.SkillEffect;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * スキル使用アクション解決 (§15-4、Wave 5 W5-α-3)。
 *
 * <p>プレイヤー / 敵の {@link Skill} 発動を扱う。隣接対象探索、AP 消費、{@link SkillEffect} の sealed 分岐による効果適用。 共通ヘルパ
 * ({@link TurnEngine#reject} / {@link TurnEngine#resolveDamageToEnemy}) は同パッケージから直接呼ぶ。
 *
 * <p>純関数: state を不変として扱い、結果は新 {@link StepResult} で返す。
 *
 * <p>罠 ({@link TurnEngine#checkAndTriggerTrap}) は Movement と Skill 両方から使う共通ヘルパとして TurnEngine
 * 本体に残してある (Plan の「checkAndTriggerTrap も移動」からの妥当な逸脱、罠は移動経由でしか発火しないため Skill
 * 側に置くより共通位置のほうが依存方向がクリーン)。
 */
final class TurnEngineSkillResolver {

  private TurnEngineSkillResolver() {}

  /** プレイヤーのスキル使用。隣接敵が無ければ reject、あれば AP 消費して {@link SkillEffect} を適用。 */
  static StepResult applyPlayerSkill(DungeonState state, int slotIndex) {
    Player player = state.player();
    Optional<Skill> skillOpt = player.skillSlot().at(slotIndex);
    if (skillOpt.isEmpty()) {
      return TurnEngineHelpers.reject(state, player.id(), "スキル枠が空");
    }
    Skill skill = skillOpt.get();
    if (!player.actionPoints().canSpend(skill.apCost())) {
      return TurnEngineHelpers.reject(state, player.id(), "AP 不足");
    }
    Optional<Enemy> targetOpt = findAdjacentEnemy(state, player.position());
    if (targetOpt.isEmpty()) {
      return TurnEngineHelpers.reject(state, player.id(), "対象がいない");
    }
    Player afterCost = player.withActionPoints(player.actionPoints().spend(skill.apCost()));
    DungeonState afterCostState = state.withPlayer(afterCost);
    return applyEffectByPlayer(afterCostState, skill, targetOpt.get());
  }

  /** 敵のスキル使用。プレイヤー隣接判定の上で AP 消費 + 効果適用。 */
  static StepResult applyEnemySkill(DungeonState state, Enemy enemy, int slotIndex) {
    Optional<Skill> skillOpt = enemy.skillSlot().at(slotIndex);
    if (skillOpt.isEmpty()) {
      return TurnEngineHelpers.reject(state, enemy.id(), "スキル枠が空");
    }
    Skill skill = skillOpt.get();
    if (!enemy.actionPoints().canSpend(skill.apCost())) {
      return TurnEngineHelpers.reject(state, enemy.id(), "AP 不足");
    }
    if (!enemy.position().isAdjacentTo(state.player().position())) {
      return TurnEngineHelpers.reject(state, enemy.id(), "対象がいない");
    }
    Enemy afterCost = enemy.withActionPoints(enemy.actionPoints().spend(skill.apCost()));
    DungeonState afterCostState = state.withEnemyReplaced(afterCost);
    return applyEffectByEnemy(afterCostState, skill, enemy.id());
  }

  private static StepResult applyEffectByPlayer(DungeonState state, Skill skill, Enemy target) {
    List<BattleEvent> events = new ArrayList<>();
    events.add(new BattleEvent.SkillUsed(state.player().id(), skill.displayName()));
    return switch (skill.effect()) {
      // ADR-17 改訂: スキルダメージも被弾側 (敵) の防御を通す。
      case SkillEffect.Damage dmg ->
          TurnEngineHelpers.resolveDamageToEnemy(
              state,
              resolveSkillDamage(dmg.amount(), target.stats(), dmg.element()),
              target,
              events);
    };
  }

  private static StepResult applyEffectByEnemy(
      DungeonState state, Skill skill, ActorId attackerId) {
    List<BattleEvent> events = new ArrayList<>();
    events.add(new BattleEvent.SkillUsed(attackerId, skill.displayName()));
    return switch (skill.effect()) {
      // ADR-17 改訂: 敵スキルもプレイヤーの実効防御 (装備/Buff 込み) を通す。
      case SkillEffect.Damage dmg ->
          resolveDamageToPlayer(
              state,
              resolveSkillDamage(dmg.amount(), state.player().effectiveStats(), dmg.element()),
              attackerId,
              events);
    };
  }

  /**
   * スキルダメージに被弾側の防御を適用する (ADR-17 改訂)。
   *
   * <p>計算は {@link DamageFormula#resolveWithoutAttacker} に委譲。スキルは攻撃側ステを加算せず 固定 {@code amount}
   * を基準値とするため、カード経路の {@link DamageFormula#resolve} ではなく {@code resolveWithoutAttacker} を使う。
   */
  private static int resolveSkillDamage(int amount, Stats victim, CardElement element) {
    return DamageFormula.resolveWithoutAttacker(amount, victim, element);
  }

  /** プレイヤーにダメージを適用する共通ヘルパ (int finalDamage 受け取り、Skill 経路専用)。 */
  private static StepResult resolveDamageToPlayer(
      DungeonState state, int finalDamage, ActorId attackerId, List<BattleEvent> events) {
    Player player = state.player();
    Stats damagedStats = player.stats().damaged(finalDamage);
    events.add(
        new BattleEvent.DamageDealt(
            attackerId, player.id(), finalDamage, damagedStats.currentHp()));
    Player hit = player.withStats(damagedStats);
    if (!damagedStats.isAlive()) {
      events.add(new BattleEvent.ActorDied(player.id()));
      DungeonState ns = state.withPlayer(hit).withPhase(TurnPhase.GAME_OVER);
      events.add(new BattleEvent.TurnPhaseChanged(TurnPhase.GAME_OVER));
      return new StepResult(ns, events);
    }
    return new StepResult(state.withPlayer(hit), events);
  }

  /** プレイヤー隣接敵を探す (Skill 専用、スキルは隣接にしか効かない§15-4)。 */
  private static Optional<Enemy> findAdjacentEnemy(DungeonState state, Position from) {
    for (Enemy e : state.enemies()) {
      if (from.isAdjacentTo(e.position())) {
        return Optional.of(e);
      }
    }
    return Optional.empty();
  }
}
