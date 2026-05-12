package core.domain.battle;

import core.domain.common.Direction;
import core.domain.common.Position;
import core.domain.dungeon.DungeonState;
import core.domain.dungeon.Tile;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.domain.meta.Soul;
import core.domain.skill.Skill;
import core.domain.skill.SkillEffect;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
      case BattleAction.Move move -> applyPlayerMove(state, move.direction());
      case BattleAction.UseSkill use -> applyPlayerSkill(state, use.slotIndex());
      case BattleAction.Wait ignored -> applyPlayerWait(state);
      case BattleAction.EndTurn ignored -> endPlayerTurn(state);
    };
  }

  /** 敵ターン → プレイヤーターン遷移時に、プレイヤーの AP を回復させる。 */
  public static StepResult startPlayerTurn(DungeonState state) {
    Objects.requireNonNull(state, "state");
    Player p = state.player();
    Player refreshed = p.withActionPoints(p.actionPoints().regenerate(p.stats().speed()));
    DungeonState ns = state.withPlayer(refreshed).withPhase(TurnPhase.PLAYER_TURN);
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
      case BattleAction.Move move -> applyEnemyMove(state, enemy, move.direction());
      case BattleAction.UseSkill use -> applyEnemySkill(state, enemy, use.slotIndex());
      case BattleAction.Wait ignored -> applyEnemyWait(state, enemy);
      case BattleAction.EndTurn ignored -> new StepResult(state, List.of());
    };
  }

  /** プレイヤーターン終了時に、全敵の AP を回復させて ENEMY_TURN に遷移させる。 */
  private static StepResult endPlayerTurn(DungeonState state) {
    List<Enemy> regenerated = new ArrayList<>(state.enemies().size());
    for (Enemy e : state.enemies()) {
      regenerated.add(e.withActionPoints(e.actionPoints().regenerate(e.stats().speed())));
    }
    DungeonState ns = state.withEnemies(regenerated).withPhase(TurnPhase.ENEMY_TURN);
    return new StepResult(ns, List.of(new BattleEvent.TurnPhaseChanged(TurnPhase.ENEMY_TURN)));
  }

  // ===================================================================================
  //  プレイヤー個別アクション
  // ===================================================================================

  private static StepResult applyPlayerMove(DungeonState state, Direction direction) {
    Player player = state.player();
    if (!player.actionPoints().canSpend(1)) {
      return reject(state, player.id(), "AP 不足");
    }
    Position next = player.position().move(direction);
    if (!state.map().isWalkable(next) || state.findEnemyAt(next).isPresent()) {
      return reject(state, player.id(), "そこへは移動できない");
    }
    Player moved = player.withPosition(next).withActionPoints(player.actionPoints().spend(1));
    DungeonState afterMove = state.withPlayer(moved);
    List<BattleEvent> events = new ArrayList<>();
    events.add(new BattleEvent.Moved(player.id(), player.position(), next));
    // 階段に到達したらフロア踏破 (CLEARED)。敵全滅では遷移しない。
    if (state.map().tileAt(next) == Tile.STAIRS_DOWN) {
      afterMove = afterMove.withPhase(TurnPhase.CLEARED);
      events.add(new BattleEvent.TurnPhaseChanged(TurnPhase.CLEARED));
    }
    return new StepResult(afterMove, events);
  }

  private static StepResult applyPlayerSkill(DungeonState state, int slotIndex) {
    Player player = state.player();
    Optional<Skill> skillOpt = player.skillSlot().at(slotIndex);
    if (skillOpt.isEmpty()) {
      return reject(state, player.id(), "スキル枠が空");
    }
    Skill skill = skillOpt.get();
    if (!player.actionPoints().canSpend(skill.apCost())) {
      return reject(state, player.id(), "AP 不足");
    }
    Optional<Enemy> targetOpt = findAdjacentEnemy(state, player.position());
    if (targetOpt.isEmpty()) {
      return reject(state, player.id(), "対象がいない");
    }
    Player afterCost = player.withActionPoints(player.actionPoints().spend(skill.apCost()));
    DungeonState afterCostState = state.withPlayer(afterCost);
    return applyEffectByPlayer(afterCostState, skill, targetOpt.get());
  }

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

  private static StepResult applyEnemyMove(DungeonState state, Enemy enemy, Direction direction) {
    if (!enemy.actionPoints().canSpend(1)) {
      return reject(state, enemy.id(), "AP 不足");
    }
    Position next = enemy.position().move(direction);
    if (!state.map().isWalkable(next) || state.isPositionOccupied(next)) {
      return reject(state, enemy.id(), "そこへは移動できない");
    }
    Enemy moved = enemy.withPosition(next).withActionPoints(enemy.actionPoints().spend(1));
    return new StepResult(
        state.withEnemyReplaced(moved),
        List.of(new BattleEvent.Moved(enemy.id(), enemy.position(), next)));
  }

  private static StepResult applyEnemySkill(DungeonState state, Enemy enemy, int slotIndex) {
    Optional<Skill> skillOpt = enemy.skillSlot().at(slotIndex);
    if (skillOpt.isEmpty()) {
      return reject(state, enemy.id(), "スキル枠が空");
    }
    Skill skill = skillOpt.get();
    if (!enemy.actionPoints().canSpend(skill.apCost())) {
      return reject(state, enemy.id(), "AP 不足");
    }
    if (!enemy.position().isAdjacentTo(state.player().position())) {
      return reject(state, enemy.id(), "対象がいない");
    }
    Enemy afterCost = enemy.withActionPoints(enemy.actionPoints().spend(skill.apCost()));
    DungeonState afterCostState = state.withEnemyReplaced(afterCost);
    return applyEffectByEnemy(afterCostState, skill, enemy.id());
  }

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

  private static StepResult applyEffectByPlayer(DungeonState state, Skill skill, Enemy target) {
    List<BattleEvent> events = new ArrayList<>();
    events.add(new BattleEvent.SkillUsed(state.player().id(), skill.displayName()));
    return switch (skill.effect()) {
      case SkillEffect.Damage dmg -> resolveDamageToEnemy(state, dmg, target, events);
    };
  }

  private static StepResult applyEffectByEnemy(
      DungeonState state, Skill skill, ActorId attackerId) {
    List<BattleEvent> events = new ArrayList<>();
    events.add(new BattleEvent.SkillUsed(attackerId, skill.displayName()));
    return switch (skill.effect()) {
      case SkillEffect.Damage dmg -> resolveDamageToPlayer(state, dmg, attackerId, events);
    };
  }

  private static StepResult resolveDamageToEnemy(
      DungeonState state, SkillEffect.Damage dmg, Enemy target, List<BattleEvent> events) {
    Stats damagedStats = target.stats().damaged(dmg.amount());
    events.add(
        new BattleEvent.DamageDealt(
            state.player().id(), target.id(), dmg.amount(), damagedStats.currentHp()));
    if (!damagedStats.isAlive()) {
      events.add(new BattleEvent.ActorDied(target.id()));
      int reward = target.kind().soulReward();
      Player rewardedPlayer = state.player().addSoul(new Soul(reward));
      events.add(new BattleEvent.SoulGained(rewardedPlayer.id(), reward));
      DungeonState ns = state.withEnemyRemoved(target.id()).withPlayer(rewardedPlayer);
      // CLEARED への遷移は階段踏破で行う (applyPlayerMove)。
      // 敵全滅では CLEARED にしない: 敵 1 体撃破で即クリアになるのを避けるため。
      return new StepResult(ns, events);
    }
    Enemy hit = target.withStats(damagedStats);
    return new StepResult(state.withEnemyReplaced(hit), events);
  }

  private static StepResult resolveDamageToPlayer(
      DungeonState state, SkillEffect.Damage dmg, ActorId attackerId, List<BattleEvent> events) {
    Player player = state.player();
    Stats damagedStats = player.stats().damaged(dmg.amount());
    events.add(
        new BattleEvent.DamageDealt(
            attackerId, player.id(), dmg.amount(), damagedStats.currentHp()));
    Player hit = player.withStats(damagedStats);
    if (!damagedStats.isAlive()) {
      events.add(new BattleEvent.ActorDied(player.id()));
      DungeonState ns = state.withPlayer(hit).withPhase(TurnPhase.GAME_OVER);
      events.add(new BattleEvent.TurnPhaseChanged(TurnPhase.GAME_OVER));
      return new StepResult(ns, events);
    }
    return new StepResult(state.withPlayer(hit), events);
  }

  // ===================================================================================
  //  Helpers
  // ===================================================================================

  private static StepResult reject(DungeonState state, ActorId who, String reason) {
    return new StepResult(state, List.of(new BattleEvent.ActionRejected(who, reason)));
  }

  private static Optional<Enemy> findAdjacentEnemy(DungeonState state, Position from) {
    for (Enemy e : state.enemies()) {
      if (from.isAdjacentTo(e.position())) {
        return Optional.of(e);
      }
    }
    return Optional.empty();
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
