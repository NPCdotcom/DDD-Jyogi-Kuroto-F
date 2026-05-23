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
   * Damage カードの解決。方向指定で隣接マスを取り、敵不在なら reject、存在すれば
   *
   * <ol>
   *   <li>{@link CardEffect.Damage#resolve} で最終ダメージを確定 (ADR-17)
   *   <li>AP 消費 + Hand→Discard 移動 (純関数操作)
   *   <li>{@link TurnEngine#resolveDamageToEnemy} 経由でダメージ反映 + 死亡判定
   * </ol>
   */
  private static StepResult resolveCardDamage(
      DungeonState state, Card card, CardEffect.Damage dmg, Direction direction, int handIndex) {
    Player player = state.player();
    Position targetPos = player.position().move(direction);
    Optional<Enemy> targetOpt = state.findEnemyAt(targetPos);
    if (targetOpt.isEmpty()) {
      return TurnEngineHelpers.reject(state, player.id(), "対象がいない");
    }
    Enemy target = targetOpt.get();
    int finalDamage = dmg.resolve(player.effectiveStats(), target.stats(), card.element());
    Player afterAction =
        player
            .withActionPoints(player.actionPoints().spend(card.apCost()))
            .withCardPileState(player.cardPileState().playFromHand(handIndex));
    DungeonState afterCostState = state.withPlayer(afterAction);
    List<BattleEvent> events = new ArrayList<>();
    events.add(new BattleEvent.SkillUsed(player.id(), card.displayName()));
    return TurnEngineHelpers.resolveDamageToEnemy(afterCostState, finalDamage, target, events);
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
