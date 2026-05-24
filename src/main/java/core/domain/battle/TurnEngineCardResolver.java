package core.domain.battle;

import core.domain.battle.TurnEngine.StepResult;
import core.domain.card.ActiveBuff;
import core.domain.card.Card;
import core.domain.card.CardEffect;
import core.domain.card.CardPileState;
import core.domain.common.Direction;
import core.domain.common.Position;
import core.domain.dungeon.DungeonState;
import core.domain.dungeon.PlacedTrap;
import core.domain.dungeon.Tile;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.Player;
import core.domain.entity.PlayerStatuses;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * カード使用アクション解決 (§15-3 / ADR-18、Wave 5 W5-α-2)。
 *
 * <p>{@link CardEffect} 4 種 (Damage / Move / Buff / Trap) を独立クラスで処理する。 共通ヘルパ ({@link
 * TurnEngine#reject} / {@link TurnEngine#resolveDamageToEnemy}) は同パッケージから直接呼ぶ。
 *
 * <p>純関数: state を不変として扱い、結果は新 {@link StepResult} で返す。
 */
final class TurnEngineCardResolver {

  private TurnEngineCardResolver() {}

  /**
   * カード使用ディスパッチャ。{@link CardEffect} の sealed 網羅で 4 種を分岐する。
   *
   * <p>処理シーケンス: 手札範囲 → AP コスト → CardEffect で switch → 各効果。
   */
  static StepResult applyPlayerUseCard(DungeonState state, BattleAction.UseCard action) {
    Player player = state.player();
    CardPileState piles = player.cardPileState();
    int handIndex = action.handIndex();
    if (handIndex >= piles.hand().size()) {
      return TurnEngineHelpers.reject(state, player.id(), "手札範囲外");
    }
    Card card = piles.hand().get(handIndex);
    if (!player.actionPoints().canSpend(card.apCost())) {
      return TurnEngineHelpers.reject(state, player.id(), "AP 不足");
    }
    return switch (card.effect()) {
      case CardEffect.Damage dmg ->
          resolveCardDamage(state, card, dmg, action.direction(), handIndex);
      case CardEffect.Move move -> resolveCardMove(state, card, move, handIndex);
      case CardEffect.Buff buff -> resolveCardBuff(state, card, buff, handIndex);
      case CardEffect.Trap trap ->
          resolveCardTrap(state, card, trap, action.direction(), handIndex);
    };
  }

  /**
   * Damage カードの解決 (Wave 12 W12-β: 射程 (range) + 爆風半径 (areaRadius) 対応)。
   *
   * <ol>
   *   <li>direction 方向に step=1 から range マスまで line scan で「中心ターゲット」(最初の敵) を探す (CTO #1:
   *       自分マス除外厳守、step=0 不開始)。壁 / 壊れる壁で line of sight 遮断 (CTO #2: BREAKABLE_WALL
   *       は移動カード専用ギミック、Damage カードでは「ただの壁」)
   *   <li>中心ターゲット不在なら reject (爆風があっても中心不在では発動しない、CTO #2)
   *   <li>AP 消費 + Hand→Discard + SkillUsed イベント発火
   *   <li>areaRadius == 0: 中心 1 体に {@link TurnEngineHelpers#resolveDamageToEnemy} でダメージ反映
   *   <li>areaRadius > 0: 中心位置を基準にチェビシェフ距離 ≤ areaRadius の全敵に順次ダメージ (壁越し OK、KISS)
   * </ol>
   */
  private static StepResult resolveCardDamage(
      DungeonState state, Card card, CardEffect.Damage dmg, Direction direction, int handIndex) {
    Player player = state.player();
    Optional<Enemy> centerOpt = findLineTarget(state, player.position(), direction, dmg.range());
    if (centerOpt.isEmpty()) {
      return TurnEngineHelpers.reject(state, player.id(), "対象がいない");
    }
    Enemy centerEnemy = centerOpt.get();
    Player afterAction =
        player
            .withActionPoints(player.actionPoints().spend(card.apCost()))
            .withCardPileState(player.cardPileState().playFromHand(handIndex));
    DungeonState afterCostState = state.withPlayer(afterAction);
    List<BattleEvent> events = new ArrayList<>();
    events.add(new BattleEvent.SkillUsed(player.id(), card.displayName()));

    if (dmg.areaRadius() == 0) {
      int finalDamage =
          dmg.resolve(afterAction.effectiveStats(), centerEnemy.stats(), card.element());
      return TurnEngineHelpers.resolveDamageToEnemy(
          afterCostState, finalDamage, centerEnemy, events);
    }

    // AOE: 中心敵を基準にチェビシェフ距離 <= areaRadius の全敵を巻き込む。
    // 巻き込み対象は発動瞬間の enemies リストでスナップショット (連鎖死亡で参照失効しないよう ActorId で保持)。
    Position centerPos = centerEnemy.position();
    int radius = dmg.areaRadius();
    List<ActorId> targetIds = new ArrayList<>();
    for (Enemy e : afterCostState.enemies()) {
      int dx = Math.abs(e.position().x() - centerPos.x());
      int dy = Math.abs(e.position().y() - centerPos.y());
      if (Math.max(dx, dy) <= radius) {
        targetIds.add(e.id());
      }
    }
    DungeonState curState = afterCostState;
    for (ActorId id : targetIds) {
      Optional<Enemy> aliveOpt = curState.findEnemy(id);
      if (aliveOpt.isEmpty()) {
        continue; // 既に撃破済 (理論上は同じ AOE 内で連鎖死亡が起きないが防衛)
      }
      Enemy alive = aliveOpt.get();
      int finalDamage = dmg.resolve(afterAction.effectiveStats(), alive.stats(), card.element());
      StepResult sub = TurnEngineHelpers.resolveDamageToEnemy(curState, finalDamage, alive, events);
      curState = sub.state();
    }
    return new StepResult(curState, events);
  }

  /**
   * direction 方向に step=1 から range マス先まで line scan で最初の敵を探す (Wave 12 W12-β)。
   *
   * <p>CTO チェックポイント #1: step=1 から開始 (自分マスを探索しない、自爆バグ回避)。 CTO チェックポイント #2: WALL / BREAKABLE_WALL で
   * line of sight 遮断 (Wave 11 一貫性死守)。
   *
   * @return 最初に発見された敵、または Optional.empty() (壁遮断 / マップ外 / 射程内に敵不在)
   */
  private static Optional<Enemy> findLineTarget(
      DungeonState state, Position origin, Direction direction, int range) {
    for (int step = 1; step <= range; step++) {
      Position scanPos =
          new Position(origin.x() + direction.dx() * step, origin.y() + direction.dy() * step);
      if (!state.map().inBounds(scanPos)) {
        return Optional.empty();
      }
      Tile tile = state.map().tileAt(scanPos);
      if (tile == Tile.WALL || tile == Tile.BREAKABLE_WALL) {
        return Optional.empty();
      }
      Optional<Enemy> enemy = state.findEnemyAt(scanPos);
      if (enemy.isPresent()) {
        return enemy;
      }
    }
    return Optional.empty();
  }

  /**
   * Move カード解決 (§15-5 / ADR-21)。AP コスト消費 + Hand→Discard + pendingMoveCount = distance 設定。
   *
   * <p>カード使用時点で移動は行わない。UseCard 後の AWSD 押下で 1 マスずつ進む (操作分離、ADR-20)。
   */
  private static StepResult resolveCardMove(
      DungeonState state, Card card, CardEffect.Move move, int handIndex) {
    Player player = state.player();
    Player afterUse =
        player
            .withActionPoints(player.actionPoints().spend(card.apCost()))
            .withCardPileState(player.cardPileState().playFromHand(handIndex))
            .withPendingMoveCount(move.distance());
    DungeonState ns = state.withPlayer(afterUse);
    List<BattleEvent> events = new ArrayList<>();
    events.add(new BattleEvent.SkillUsed(player.id(), card.displayName()));
    events.add(new BattleEvent.MovementGranted(player.id(), move.distance()));
    return new StepResult(ns, events);
  }

  /**
   * Buff カード解決 (§15-3 / ADR-27)。同 BuffKind は上書き、新 ActiveBuff を追加。 以降のカード使用 / 受けるダメージに {@link
   * Player#effectiveStats()} 経由で反映される。
   */
  private static StepResult resolveCardBuff(
      DungeonState state, Card card, CardEffect.Buff buff, int handIndex) {
    Player player = state.player();
    PlayerStatuses statuses = player.statuses();

    List<ActiveBuff> newBuffs = new ArrayList<>(statuses.activeBuffs().size() + 1);
    for (ActiveBuff b : statuses.activeBuffs()) {
      if (b.kind() != buff.kind()) {
        newBuffs.add(b);
      }
    }
    ActiveBuff applied = new ActiveBuff(buff.kind(), buff.amount(), buff.durationTurns());
    newBuffs.add(applied);

    PlayerStatuses newStatuses = statuses.withActiveBuffs(newBuffs);
    Player afterUse =
        player
            .withStatuses(newStatuses)
            .withActionPoints(player.actionPoints().spend(card.apCost()))
            .withCardPileState(player.cardPileState().playFromHand(handIndex));

    DungeonState ns = state.withPlayer(afterUse);
    List<BattleEvent> events = new ArrayList<>();
    events.add(new BattleEvent.SkillUsed(player.id(), card.displayName()));
    events.add(
        new BattleEvent.BuffApplied(
            player.id(), applied.kind(), applied.amount(), applied.remainingTurns()));
    return new StepResult(ns, events);
  }

  /** Trap カード解決 (§15-3 / ADR-22)。指定方向の隣接マスに罠を設置 (壁不可、同座標既存罠は上書き)。 */
  private static StepResult resolveCardTrap(
      DungeonState state, Card card, CardEffect.Trap trap, Direction direction, int handIndex) {
    Player player = state.player();
    Position trapPos = player.position().move(direction);
    if (!state.map().isWalkable(trapPos)) {
      return TurnEngineHelpers.reject(state, player.id(), "そこには罠を設置できない");
    }
    List<PlacedTrap> newTraps = new ArrayList<>(state.placedTraps().size() + 1);
    for (PlacedTrap t : state.placedTraps()) {
      if (!t.position().equals(trapPos)) {
        newTraps.add(t);
      }
    }
    newTraps.add(new PlacedTrap(trapPos, trap.baseValue(), trap.lifetime(), card.element()));

    Player afterUse =
        player
            .withActionPoints(player.actionPoints().spend(card.apCost()))
            .withCardPileState(player.cardPileState().playFromHand(handIndex));
    DungeonState ns = state.withPlayer(afterUse).withPlacedTraps(newTraps);
    List<BattleEvent> events = new ArrayList<>();
    events.add(new BattleEvent.SkillUsed(player.id(), card.displayName()));
    events.add(new BattleEvent.TrapPlaced(player.id(), trapPos, trap.baseValue()));
    return new StepResult(ns, events);
  }
}
