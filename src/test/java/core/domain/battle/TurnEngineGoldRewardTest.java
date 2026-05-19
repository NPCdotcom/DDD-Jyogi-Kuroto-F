package core.domain.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.card.Card;
import core.domain.card.CardEffect;
import core.domain.card.CardElement;
import core.domain.card.CardId;
import core.domain.card.CardPileState;
import core.domain.card.CardTag;
import core.domain.card.DiscardPile;
import core.domain.card.DrawPile;
import core.domain.card.Hand;
import core.domain.common.Direction;
import core.domain.common.Position;
import core.domain.dungeon.DungeonState;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.EnemyKind;
import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.domain.support.DomainFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * §15-2 / §15-9: 敵撃破時に Player.gold が EnemyKind.goldReward() ぶん加算され、
 * BattleEvent.GoldGained が発火することの検証。
 */
class TurnEngineGoldRewardTest {

  private static Card overkillDamageCard() {
    return new Card(
        CardId.of("overkill"),
        "ワンパン",
        1,
        CardTag.ATTACK,
        CardElement.PHYSICAL,
        new CardEffect.Damage(100));
  }

  /** プレイヤー (HP 30) + 隣接スライム (HP 10) を作り、Damage 100 のカードで一撃必殺するシナリオ。 */
  private static DungeonState stateWithAdjacentSlime() {
    Player player =
        DomainFixtures.playerAt(new Position(1, 1))
            .withCardPileState(
                new CardPileState(
                    DrawPile.empty(),
                    Hand.empty().add(overkillDamageCard()),
                    DiscardPile.empty()));
    Enemy slime =
        new Enemy(
            ActorId.of("slime#test"),
            new Position(1, 2), // Direction.UP = (0, +1) で当たる位置
            new Stats(10, 10, 1, 0, 0, 0, 0),
            core.domain.battle.ActionPoints.full(1),
            new core.domain.skill.SkillSlot(List.of(DomainFixtures.lightAttack()), 4),
            EnemyKind.SLIME);
    return DomainFixtures.newStateWith(
        DomainFixtures.squareRoom(), player, List.of(slime), TurnPhase.PLAYER_TURN);
  }

  @Test
  void defeatingSlimeAddsBothSoulAndGoldRewards() {
    DungeonState state = stateWithAdjacentSlime();
    int soulBefore = state.player().soul().amount();
    int goldBefore = state.player().gold().amount();

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(state, new BattleAction.UseCard(0, Direction.UP));

    Player after = result.state().player();
    // EnemyKind.SLIME: soulReward=1, goldReward=5 (§15-2 雑魚レート、ADR-30 で Soul を 5→1 に修正)
    assertEquals(soulBefore + 1, after.soul().amount(), "ソウル +1");
    assertEquals(goldBefore + 5, after.gold().amount(), "金貨 +5");
  }

  @Test
  void defeatingSlimeEmitsGoldGainedEvent() {
    DungeonState state = stateWithAdjacentSlime();

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(state, new BattleAction.UseCard(0, Direction.UP));

    boolean hasGoldGained =
        result.events().stream()
            .anyMatch(
                e ->
                    e instanceof BattleEvent.GoldGained g
                        && g.amount() == 5
                        && g.who().equals(state.player().id()));
    assertTrue(hasGoldGained, "GoldGained(player, 5) が発火される");
  }

  @Test
  void slimeKindReportsCorrectGoldReward() {
    // §15-2 雑魚レート: SLIME = goldReward 5
    assertEquals(5, EnemyKind.SLIME.goldReward());
  }

  @Test
  void hitWithoutKillDoesNotEmitGoldGained() {
    // 普通の斬撃 (Damage 5) では 1 撃で敵を殺せない (HP 10)、Gold も入らない
    Card weakCard =
        new Card(
            CardId.of("weak"),
            "弱撃",
            1,
            CardTag.ATTACK,
            CardElement.PHYSICAL,
            new CardEffect.Damage(3));
    Player player =
        DomainFixtures.playerAt(new Position(1, 1))
            .withCardPileState(
                new CardPileState(
                    DrawPile.empty(),
                    Hand.empty().add(weakCard),
                    DiscardPile.empty()));
    Enemy slime =
        new Enemy(
            ActorId.of("slime#tough"),
            new Position(1, 2),
            new Stats(10, 10, 1, 0, 0, 0, 0),
            core.domain.battle.ActionPoints.full(1),
            new core.domain.skill.SkillSlot(List.of(DomainFixtures.lightAttack()), 4),
            EnemyKind.SLIME);
    DungeonState state =
        DomainFixtures.newStateWith(
            DomainFixtures.squareRoom(), player, List.of(slime), TurnPhase.PLAYER_TURN);
    int goldBefore = state.player().gold().amount();

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(state, new BattleAction.UseCard(0, Direction.UP));

    Player after = result.state().player();
    assertEquals(goldBefore, after.gold().amount(), "撃破していなければ Gold は加算されない");
    boolean hasGoldGained =
        result.events().stream().anyMatch(e -> e instanceof BattleEvent.GoldGained);
    assertFalse(hasGoldGained, "GoldGained は発火されない");
  }
}
