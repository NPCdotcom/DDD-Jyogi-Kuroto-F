package core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.battle.BattleEvent;
import core.domain.battle.TurnEngine;
import core.domain.battle.TurnPhase;
import core.domain.common.Position;
import core.domain.dungeon.DungeonState;
import core.domain.support.DomainFixtures;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link GameContext} の単体テスト。
 *
 * <p>§15-5 / E-8 で追加した {@link GameContext#totalEventsEmitted()} カーソルの挙動を確認する。 DungeonScreen
 * がこの値で「未処理 DamageDealt」を検知して popup / shake を発火するため、 イベント発火のたびに monotonic
 * に増加し、循環バッファで古いイベントが破棄されても減らないことが要件。
 */
class GameContextTest {

  private static DungeonState baseState() {
    return DomainFixtures.newStateWith(
        DomainFixtures.squareRoom(),
        DomainFixtures.playerAt(new Position(1, 1)),
        List.of(),
        TurnPhase.PLAYER_TURN);
  }

  // ---------------- startNewRun / state ----------------

  @Test
  void startNewRunInitializesStateAndZeroEventCount() {
    DungeonState initial = baseState();
    GameContext ctx = GameContext.startNewRun(initial);

    assertEquals(initial, ctx.state());
    assertEquals(0L, ctx.totalEventsEmitted(), "新規 GameContext は累積イベント 0");
    assertEquals(0, ctx.latestEvents(10).size(), "イベント未発火なら latestEvents も空");
  }

  @Test
  void startNewRunRejectsNullInitial() {
    assertThrows(NullPointerException.class, () -> GameContext.startNewRun(null));
  }

  // ---------------- applyResult / totalEventsEmitted ----------------

  @Test
  void applyResultIncrementsTotalEventsByEventListSize() {
    GameContext ctx = GameContext.startNewRun(baseState());
    long before = ctx.totalEventsEmitted();

    List<BattleEvent> events =
        List.of(
            new BattleEvent.TurnPhaseChanged(TurnPhase.ENEMY_TURN),
            new BattleEvent.TurnPhaseChanged(TurnPhase.PLAYER_TURN));
    ctx.applyResult(new TurnEngine.StepResult(ctx.state(), events));

    assertEquals(before + 2, ctx.totalEventsEmitted(), "2 件発火で +2");
  }

  @Test
  void applyResultWithEmptyEventListDoesNotIncrement() {
    GameContext ctx = GameContext.startNewRun(baseState());
    ctx.applyResult(new TurnEngine.StepResult(ctx.state(), List.of()));
    assertEquals(0L, ctx.totalEventsEmitted(), "イベントなしならカウンタ据置");
  }

  @Test
  void totalEventsEmittedDoesNotDecreaseEvenAfterBufferOverflow() {
    // MAX_LOG_LINES = 64 を超えるイベントを発火しても totalEventsEmitted は monotonic
    GameContext ctx = GameContext.startNewRun(baseState());

    List<BattleEvent> manyEvents = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      manyEvents.add(new BattleEvent.TurnPhaseChanged(TurnPhase.PLAYER_TURN));
    }
    ctx.applyResult(new TurnEngine.StepResult(ctx.state(), manyEvents));

    assertEquals(100L, ctx.totalEventsEmitted(), "発火件数すべてカウントされる");
    assertTrue(ctx.latestEvents(200).size() <= 64, "latestEvents は MAX_LOG_LINES (64) で打ち切り");
  }

  @Test
  void multipleApplyResultsAccumulateTotalEvents() {
    GameContext ctx = GameContext.startNewRun(baseState());

    ctx.applyResult(
        new TurnEngine.StepResult(
            ctx.state(), List.of(new BattleEvent.TurnPhaseChanged(TurnPhase.ENEMY_TURN))));
    ctx.applyResult(
        new TurnEngine.StepResult(
            ctx.state(),
            List.of(
                new BattleEvent.TurnPhaseChanged(TurnPhase.PLAYER_TURN),
                new BattleEvent.TurnPhaseChanged(TurnPhase.ENEMY_TURN))));

    assertEquals(3L, ctx.totalEventsEmitted(), "1 + 2 = 3 件累積");
  }

  // ---------------- latestEvents ----------------

  @Test
  void latestEventsReturnsRequestedCountInOrder() {
    GameContext ctx = GameContext.startNewRun(baseState());
    BattleEvent e1 = new BattleEvent.TurnPhaseChanged(TurnPhase.ENEMY_TURN);
    BattleEvent e2 = new BattleEvent.TurnPhaseChanged(TurnPhase.PLAYER_TURN);
    BattleEvent e3 = new BattleEvent.TurnPhaseChanged(TurnPhase.ENEMY_TURN);
    ctx.applyResult(new TurnEngine.StepResult(ctx.state(), List.of(e1, e2, e3)));

    List<BattleEvent> latest = ctx.latestEvents(2);
    assertEquals(2, latest.size());
    // 新しい順 (e2 → e3 の最後 2 件) で返る (e2, e3)
    assertEquals(e2, latest.get(0));
    assertEquals(e3, latest.get(1));
  }

  @Test
  void applyResultRejectsNullResult() {
    GameContext ctx = GameContext.startNewRun(baseState());
    assertThrows(NullPointerException.class, () -> ctx.applyResult(null));
  }
}
