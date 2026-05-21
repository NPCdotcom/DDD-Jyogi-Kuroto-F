package core.domain.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.card.ActiveBuff;
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
import core.domain.entity.Player;
import core.domain.support.DomainFixtures;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * §15-3 / ADR-27 完全実装の証跡テスト: Buff カードが TurnEngine で resolveCardBuff 経由で適用され、 effectiveStats
 * への合算、startPlayerTurn での remainingTurns 減算、重複 BuffKind 上書きが動作する。
 */
class TurnEngineBuffTest {

  // ----- fixture: Buff カード ----------

  private static Card physicalAttackUpCard(int amount, int duration, int apCost) {
    return new Card(
        CardId.of("phys_atk_up_buff"),
        "物攻アップ",
        apCost,
        CardTag.BUFF,
        CardElement.PHYSICAL,
        new CardEffect.Buff(CardEffect.BuffKind.PHYSICAL_ATTACK_UP, amount, duration));
  }

  private static Card speedUpCard() {
    return new Card(
        CardId.of("spd_up_buff"),
        "速度アップ",
        1,
        CardTag.BUFF,
        CardElement.PHYSICAL,
        new CardEffect.Buff(CardEffect.BuffKind.SPEED_UP, 1, 2));
  }

  private static DungeonState stateWithBuffInHand(Card buffCard) {
    Player player =
        DomainFixtures.playerAt(new Position(1, 1))
            .withCardPileState(
                new CardPileState(
                    DrawPile.empty(), Hand.empty().add(buffCard), DiscardPile.empty()));
    return DomainFixtures.newStateWith(
        DomainFixtures.squareRoom(), player, List.of(), TurnPhase.PLAYER_TURN);
  }

  // ---------------- Buff カード使用 ----------------

  @Test
  void useBuffCardAddsActiveBuffAndConsumesApAndCard() {
    DungeonState state = stateWithBuffInHand(physicalAttackUpCard(2, 3, 1));
    int apBefore = state.player().actionPoints().current();

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(state, new BattleAction.UseCard(0, Direction.UP));

    Player after = result.state().player();
    assertEquals(1, after.statuses().activeBuffs().size(), "ActiveBuff 1 件追加");
    ActiveBuff applied = after.statuses().activeBuffs().get(0);
    assertEquals(CardEffect.BuffKind.PHYSICAL_ATTACK_UP, applied.kind());
    assertEquals(2, applied.amount());
    assertEquals(3, applied.remainingTurns(), "適用直後の残ターン = durationTurns");
    assertEquals(apBefore - 1, after.actionPoints().current(), "AP コスト 1 消費");
    assertEquals(0, after.cardPileState().hand().size(), "手札から消費");
    assertEquals(1, after.cardPileState().discardPile().size(), "捨て札に移動");
  }

  @Test
  void useBuffCardEmitsSkillUsedAndBuffAppliedEvents() {
    DungeonState state = stateWithBuffInHand(physicalAttackUpCard(2, 3, 1));

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(state, new BattleAction.UseCard(0, Direction.UP));

    boolean hasSkillUsed =
        result.events().stream().anyMatch(e -> e instanceof BattleEvent.SkillUsed);
    boolean hasBuffApplied =
        result.events().stream()
            .anyMatch(
                e ->
                    e instanceof BattleEvent.BuffApplied ba
                        && ba.kind() == CardEffect.BuffKind.PHYSICAL_ATTACK_UP
                        && ba.amount() == 2
                        && ba.remainingTurns() == 3);
    assertTrue(hasSkillUsed, "SkillUsed イベントが発火");
    assertTrue(hasBuffApplied, "BuffApplied(物攻+2, 残3) が発火");
  }

  @Test
  void effectiveStatsReflectActiveBuff() {
    DungeonState state = stateWithBuffInHand(physicalAttackUpCard(2, 3, 1));
    int basePhyAtk = state.player().stats().physicalAttack();

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(state, new BattleAction.UseCard(0, Direction.UP));

    Player after = result.state().player();
    assertEquals(
        basePhyAtk + 2, after.effectiveStats().physicalAttack(), "effectiveStats に物攻 +2 が反映される");
  }

  // ---------------- 重複 BuffKind の上書き (ADR-27) ----------------

  @Test
  void duplicateBuffKindOverridesPreviousInstance() {
    DungeonState state = stateWithBuffInHand(physicalAttackUpCard(2, 3, 1));

    // 1 回目: 物攻 +2 / 残 3
    TurnEngine.StepResult first =
        TurnEngine.resolvePlayerAction(state, new BattleAction.UseCard(0, Direction.UP));
    // 2 回目: 物攻 +5 / 残 1 (異なる amount / 残ターンで上書き)
    Player playerAfterFirst =
        first
            .state()
            .player()
            .withCardPileState(
                new CardPileState(
                    DrawPile.empty(),
                    Hand.empty().add(physicalAttackUpCard(5, 1, 1)),
                    DiscardPile.empty()));
    DungeonState stateAfterFirst = first.state().withPlayer(playerAfterFirst);
    TurnEngine.StepResult second =
        TurnEngine.resolvePlayerAction(stateAfterFirst, new BattleAction.UseCard(0, Direction.UP));

    Player finalPlayer = second.state().player();
    assertEquals(1, finalPlayer.statuses().activeBuffs().size(), "重複 BuffKind は上書き (1 件のまま)");
    ActiveBuff overridden = finalPlayer.statuses().activeBuffs().get(0);
    assertEquals(5, overridden.amount(), "新しい amount で上書き");
    assertEquals(1, overridden.remainingTurns(), "新しい残ターンで上書き");
  }

  @Test
  void differentBuffKindAccumulates() {
    // 異なる BuffKind は並列保持される (物攻 +2 と 速度 +1 が共存)
    DungeonState state = stateWithBuffInHand(physicalAttackUpCard(2, 3, 1));

    TurnEngine.StepResult first =
        TurnEngine.resolvePlayerAction(state, new BattleAction.UseCard(0, Direction.UP));
    Player playerAfterFirst =
        first
            .state()
            .player()
            .withCardPileState(
                new CardPileState(
                    DrawPile.empty(), Hand.empty().add(speedUpCard()), DiscardPile.empty()));
    DungeonState stateAfterFirst = first.state().withPlayer(playerAfterFirst);
    TurnEngine.StepResult second =
        TurnEngine.resolvePlayerAction(stateAfterFirst, new BattleAction.UseCard(0, Direction.UP));

    Player finalPlayer = second.state().player();
    assertEquals(2, finalPlayer.statuses().activeBuffs().size(), "異種 BuffKind 2 件");
  }

  // ---------------- startPlayerTurn で remainingTurns-- ----------------

  @Test
  void startPlayerTurnDecrementsActiveBuffRemainingTurns() {
    DungeonState state = stateWithBuffInHand(physicalAttackUpCard(2, 3, 1));
    DungeonState afterUse =
        TurnEngine.resolvePlayerAction(state, new BattleAction.UseCard(0, Direction.UP)).state();
    // ENEMY_TURN 経由をスキップして、直接 startPlayerTurn を呼ぶ (テストの簡略化)
    DungeonState enemyPhase = afterUse.withPhase(TurnPhase.ENEMY_TURN);

    TurnEngine.StepResult result = TurnEngine.startPlayerTurn(enemyPhase, new Random(42));

    Player after = result.state().player();
    assertEquals(1, after.statuses().activeBuffs().size());
    assertEquals(2, after.statuses().activeBuffs().get(0).remainingTurns(), "残ターン 3 → 2");
  }

  @Test
  void startPlayerTurnRemovesExpiredBuffs() {
    // remainingTurns=1 の Buff が startPlayerTurn 後に 0 → 除去される
    DungeonState state = stateWithBuffInHand(physicalAttackUpCard(2, 1, 1));
    DungeonState afterUse =
        TurnEngine.resolvePlayerAction(state, new BattleAction.UseCard(0, Direction.UP)).state();
    DungeonState enemyPhase = afterUse.withPhase(TurnPhase.ENEMY_TURN);

    TurnEngine.StepResult result = TurnEngine.startPlayerTurn(enemyPhase, new Random(42));

    Player after = result.state().player();
    assertEquals(0, after.statuses().activeBuffs().size(), "残ターン 0 で除去される");
  }

  @Test
  void startPlayerTurnSpeedBuffAffectsApRefillAmount() {
    // 速度 +2 Buff 適用後、startPlayerTurn で AP リフィル量が speed 5 → 7 になる
    Card spdUpStrong =
        new Card(
            CardId.of("spd_up_strong"),
            "速度+2",
            1,
            CardTag.BUFF,
            CardElement.PHYSICAL,
            new CardEffect.Buff(CardEffect.BuffKind.SPEED_UP, 2, 3));
    DungeonState state = stateWithBuffInHand(spdUpStrong);
    DungeonState afterUse =
        TurnEngine.resolvePlayerAction(state, new BattleAction.UseCard(0, Direction.UP)).state();
    DungeonState enemyPhase = afterUse.withPhase(TurnPhase.ENEMY_TURN);

    TurnEngine.StepResult result = TurnEngine.startPlayerTurn(enemyPhase, new Random(42));

    Player after = result.state().player();
    // playerAt の素ステ speed = 3、Buff +2 で effective speed = 5、AP リフィルは effective speed まで
    assertEquals(5, after.actionPoints().current(), "AP リフィル量 = effectiveStats().speed() = 3 + 2");
  }

  // ---------------- Damage カードで Buff の物攻が反映される ----------------

  @Test
  void damageCardUsesEffectiveStatsForAttackerSoBuffAppliesToDamage() {
    // 物攻 +2 Buff が適用された状態で Damage カードを使うと、Buff 込みでダメージ計算される
    Card buff = physicalAttackUpCard(2, 3, 1);
    Card dmgCard =
        new Card(
            CardId.of("test_dmg"),
            "テスト斬撃",
            1,
            CardTag.ATTACK,
            CardElement.PHYSICAL,
            new CardEffect.Damage(3));

    Player player =
        DomainFixtures.playerAt(new Position(1, 1))
            .withCardPileState(
                new CardPileState(
                    DrawPile.empty(), Hand.empty().add(buff).add(dmgCard), DiscardPile.empty()));
    // 隣接 (1, 2) に敵を配置 (Direction.UP = y+1 で当たる)
    DungeonState state =
        DomainFixtures.newStateWith(
            DomainFixtures.squareRoom(),
            player,
            List.of(DomainFixtures.slimeAt(new Position(1, 2))),
            TurnPhase.PLAYER_TURN);

    // 1) Buff を使う
    DungeonState afterBuff =
        TurnEngine.resolvePlayerAction(state, new BattleAction.UseCard(0, Direction.UP)).state();
    // 2) Damage カード (handIndex=0、Buff が消費されたので残り 1 枚) を SOUTH 方向に
    TurnEngine.StepResult dmgResult =
        TurnEngine.resolvePlayerAction(afterBuff, new BattleAction.UseCard(0, Direction.UP));

    // baseValue 3 + (素ステ物攻 0 + Buff +2) - (敵 物防 0) = 5 のはず
    boolean hit5Damage =
        dmgResult.events().stream()
            .anyMatch(e -> e instanceof BattleEvent.DamageDealt d && d.damage() == 5);
    assertTrue(hit5Damage, "Buff +2 が物攻に乗ったダメージ 5 が発火される");
  }
}
