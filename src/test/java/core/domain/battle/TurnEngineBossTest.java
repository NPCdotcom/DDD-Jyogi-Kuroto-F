package core.domain.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import core.domain.card.TrapLifetime;
import core.domain.common.Direction;
import core.domain.common.Position;
import core.domain.dungeon.DungeonState;
import core.domain.dungeon.PlacedTrap;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.EnemyKind;
import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.domain.skill.SkillSlot;
import core.domain.support.DomainFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * §15-6: ボス撃破 → RUN_CLEARED 遷移、および階段踏破 → CLEARED の検証。
 *
 * <p>ボス撃破は通常攻撃経路 ({@code resolveDamageToEnemy}) と罠経路 ({@code checkAndTriggerTrap}) の
 * 両方で勝利になる必要があるため、双方を検証する (Plan D-7)。
 */
class TurnEngineBossTest {

  /** 一撃必殺用の高ダメージカード (Damage 100)。 */
  private static Card overkillCard() {
    return new Card(
        CardId.of("overkill"),
        "ワンパン",
        1,
        CardTag.ATTACK,
        CardElement.PHYSICAL,
        new CardEffect.Damage(100));
  }

  /** {@link EnemyKind#BOSS} の敵を生成する。HP は撃破テスト / 罠テストで使い分けるため引数化。 */
  private static Enemy bossAt(Position position, int hp) {
    return new Enemy(
        ActorId.of("boss#test"),
        position,
        new Stats(hp, hp, 3, 5, 0, 0, 0),
        ActionPoints.full(1),
        new SkillSlot(List.of(DomainFixtures.lightAttack()), 4),
        EnemyKind.BOSS);
  }

  private static Player playerWithOverkill(Position position) {
    return DomainFixtures.playerAt(position)
        .withCardPileState(
            new CardPileState(
                DrawPile.empty(), Hand.empty().add(overkillCard()), DiscardPile.empty()));
  }

  @Test
  void defeatingBossTransitionsToRunCleared() {
    // §15-6: 最終層ボス撃破 = ラン勝利。プレイヤー (1,1) が隣接ボス (1,2) を一撃必殺。
    Player player = playerWithOverkill(new Position(1, 1));
    Enemy boss = bossAt(new Position(1, 2), 60);
    DungeonState state =
        DomainFixtures.newStateWith(
            DomainFixtures.squareRoom(), player, List.of(boss), TurnPhase.PLAYER_TURN);

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(state, new BattleAction.UseCard(0, Direction.UP));

    assertEquals(TurnPhase.RUN_CLEARED, result.state().phase(), "ボス撃破でラン勝利");
  }

  @Test
  void defeatingBossEmitsRunClearedPhaseEvent() {
    Player player = playerWithOverkill(new Position(1, 1));
    Enemy boss = bossAt(new Position(1, 2), 60);
    DungeonState state =
        DomainFixtures.newStateWith(
            DomainFixtures.squareRoom(), player, List.of(boss), TurnPhase.PLAYER_TURN);

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(state, new BattleAction.UseCard(0, Direction.UP));

    boolean hasRunCleared =
        result.events().stream()
            .anyMatch(
                e ->
                    e instanceof BattleEvent.TurnPhaseChanged tp
                        && tp.newPhase() == TurnPhase.RUN_CLEARED);
    assertTrue(hasRunCleared, "TurnPhaseChanged(RUN_CLEARED) が発火される");
  }

  @Test
  void defeatingSlimeDoesNotTransitionToRunCleared() {
    // 回帰防止: 雑魚撃破では RUN_CLEARED にならない (ボス以外は勝利にしない)。
    Player player = playerWithOverkill(new Position(1, 1));
    Enemy slime = DomainFixtures.slimeAt("slime#1", new Position(1, 2), ActionPoints.full(1));
    DungeonState state =
        DomainFixtures.newStateWith(
            DomainFixtures.squareRoom(), player, List.of(slime), TurnPhase.PLAYER_TURN);

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(state, new BattleAction.UseCard(0, Direction.UP));

    assertNotEquals(TurnPhase.RUN_CLEARED, result.state().phase(), "雑魚撃破は勝利にしない");
  }

  @Test
  void defeatingBossByTrapTransitionsToRunCleared() {
    // §15-6 / Plan D-7: 罠でボスを倒した場合もラン勝利になる (checkAndTriggerTrap 経路)。
    Player player = DomainFixtures.playerAt(new Position(1, 1));
    Enemy boss = bossAt(new Position(2, 1), 3);
    PlacedTrap trap =
        new PlacedTrap(
            new Position(2, 2), 50, TrapLifetime.UntilStepped.INSTANCE, CardElement.PHYSICAL);
    DungeonState state =
        DomainFixtures.newStateWith(
                DomainFixtures.squareRoom(), player, List.of(boss), TurnPhase.ENEMY_TURN)
            .withPlacedTraps(List.of(trap));

    TurnEngine.StepResult result =
        TurnEngine.resolveEnemyAction(state, boss.id(), new BattleAction.Move(Direction.UP));

    assertEquals(TurnPhase.RUN_CLEARED, result.state().phase(), "罠でのボス撃破もラン勝利");
  }

  @Test
  void steppingStairsEmitsCleared() {
    // §15-6: 階段踏破は常に CLEARED (層末ノード選択へ)。シームレス層化で ROOM_CLEARED は廃止。
    Player player = DomainFixtures.playerAt(new Position(2, 1));
    DungeonState state =
        DomainFixtures.newStateWith(
            DomainFixtures.roomWithStairs(), player, List.of(), TurnPhase.PLAYER_TURN);

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(state, new BattleAction.Move(Direction.UP));

    assertEquals(TurnPhase.CLEARED, result.state().phase());
  }
}
