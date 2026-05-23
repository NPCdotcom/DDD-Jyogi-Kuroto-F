package core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.battle.ActionPoints;
import core.domain.battle.BattleAction;
import core.domain.battle.BattleEvent;
import core.domain.battle.TurnPhase;
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
import core.domain.dungeon.DungeonMap;
import core.domain.dungeon.DungeonState;
import core.domain.entity.Enemy;
import core.domain.entity.Player;
import core.domain.support.DomainFixtures;
import core.infrastructure.bootstrap.InitialStateFactory;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * {@link TurnDirector#advanceFloor(DungeonState)} の単体テスト (§15-6 / ADR-23)。
 *
 * <p>本クラスは application 層初の自動テスト (Plan Part 1 #4 で指摘されたカバレッジ欠如への着手)。 fixture は infrastructure 層の
 * {@link InitialStateFactory} に依存する (テスト便宜上の依存、production code の依存方向ルールには違反しない)。
 */
class TurnDirectorTest {

  /** 固定シード (再現性確保、startPlayerTurn 内のドロー結果が決定的になる)。 */
  private static final long SEED = 42L;

  // ---------------- ハッピーパス ----------------

  @Test
  void advanceFloorOnClearedTransitionsToNextLayer() {
    // §15-6: 階段踏破 (CLEARED) → ENTER 相当の入力 → 次層へ進む
    DungeonState cleared =
        InitialStateFactory.firstFloor(new Random(SEED)).withPhase(TurnPhase.CLEARED);
    GameContext ctx = GameContext.startNewRun(cleared);
    TurnDirector director = new TurnDirector(ctx, new Random(SEED));

    DungeonState nextLayer = InitialStateFactory.advanceLayer(cleared, new Random(SEED));
    director.advanceFloor(nextLayer);

    assertEquals(2, ctx.state().layer().number(), "層番号が +1 されている");
    assertEquals(TurnPhase.PLAYER_TURN, ctx.state().phase(), "次層はプレイヤーターンから始まる");
  }

  @Test
  void moveCardWithLastApDoesNotAutoEndTurn() {
    // §15-3 / ADR-21: 移動カードで AP 0 になっても、移動権 (pendingMoveCount) が残る間はターンを終えない。
    // 旧バグ: AP 0 即 autoEnd で瞬歩が発動せず敵ターンに移っていた。
    Player player =
        DomainFixtures.playerAt(new Position(1, 1))
            .withActionPoints(new ActionPoints(1, 5))
            .withCardPileState(
                new CardPileState(
                    DrawPile.empty(),
                    Hand.empty().add(DomainFixtures.moveCard("dash")),
                    DiscardPile.empty()));
    DungeonState state =
        DomainFixtures.newStateWith(
            DomainFixtures.squareRoom(), player, List.of(), TurnPhase.PLAYER_TURN);
    GameContext ctx = GameContext.startNewRun(state);
    TurnDirector director = new TurnDirector(ctx, new Random(SEED));

    director.applyPlayerAction(new BattleAction.UseCard(0, Direction.RIGHT));

    assertEquals(TurnPhase.PLAYER_TURN, ctx.state().phase(), "移動権が残るうちは敵ターンに移らない");
    assertEquals(0, ctx.state().player().actionPoints().current(), "AP は 0");
    assertEquals(2, ctx.state().player().pendingMoveCount(), "移動権 2 が保持される");
  }

  @Test
  void endTurnEscapesMovementTokenSoftLock() {
    // 移動権ソフトロック回避: 移動権を持ったまま (pendingMoveCount > 0) でも EndTurn でターンを
    // 終えられる。四方を壁・敵で塞がれ移動権を消費できない場合の唯一の脱出路 (PlayerInputs が ENTER に割当)。
    Player player =
        DomainFixtures.playerAt(new Position(1, 1))
            .withActionPoints(new ActionPoints(1, 5))
            .withCardPileState(
                new CardPileState(
                    DrawPile.empty(),
                    Hand.empty().add(DomainFixtures.moveCard("dash")),
                    DiscardPile.empty()));
    DungeonState state =
        DomainFixtures.newStateWith(
            DomainFixtures.squareRoom(), player, List.of(), TurnPhase.PLAYER_TURN);
    GameContext ctx = GameContext.startNewRun(state);
    TurnDirector director = new TurnDirector(ctx, new Random(SEED));

    director.applyPlayerAction(new BattleAction.UseCard(0, Direction.RIGHT));
    // この時点で pendingMoveCount > 0 / AP 0 / PLAYER_TURN。EndTurn で脱出できることを検証。
    director.applyPlayerAction(new BattleAction.EndTurn());

    assertEquals(TurnPhase.ENEMY_TURN, ctx.state().phase(), "移動権が残っていても EndTurn でターンを終えられる");
  }

  @Test
  void enemyWithTwoApActsTwiceInOneTurn() {
    // ADR-06: 敵 AP = 層番号。AP 2 の敵は 1 ターンに 2 回行動する (ここではプレイヤーへ 2 マス前進)。
    DungeonMap room = DungeonMap.of(List.of("######", "#....#", "######"));
    Player player = DomainFixtures.playerAt(new Position(1, 1));
    Enemy slime = InitialStateFactory.newSlimeForLayer("e", new Position(4, 1), 2); // AP 2
    DungeonState state =
        DomainFixtures.newStateWith(room, player, List.of(slime), TurnPhase.ENEMY_TURN);
    GameContext ctx = GameContext.startNewRun(state);
    TurnDirector director = new TurnDirector(ctx, new Random(SEED));

    director.runEnemyTurn();

    assertEquals(
        new Position(2, 1),
        ctx.state().findEnemy(slime.id()).orElseThrow().position(),
        "AP 2 の敵はプレイヤーへ 2 マス前進する (1 マスで止まらない)");
  }

  @Test
  void allEnemiesActWithFullApInOneTurn() {
    // バグ 2 回帰: 敵が複数いても各自 AP ぶん行動する (1 体目で敵ターンが止まらない)。
    DungeonMap room = DungeonMap.of(List.of("#########", "#.......#", "#########"));
    Player player = DomainFixtures.playerAt(new Position(4, 1));
    Enemy left = InitialStateFactory.newSlimeForLayer("left", new Position(1, 1), 2); // AP 2
    Enemy right = InitialStateFactory.newSlimeForLayer("right", new Position(7, 1), 2); // AP 2
    DungeonState state =
        DomainFixtures.newStateWith(room, player, List.of(left, right), TurnPhase.ENEMY_TURN);
    GameContext ctx = GameContext.startNewRun(state);
    TurnDirector director = new TurnDirector(ctx, new Random(SEED));

    director.runEnemyTurn();

    assertEquals(
        new Position(3, 1),
        ctx.state().findEnemy(left.id()).orElseThrow().position(),
        "左の敵は AP 2 ぶん 2 マス前進");
    assertEquals(
        new Position(5, 1),
        ctx.state().findEnemy(right.id()).orElseThrow().position(),
        "右の敵も AP 2 ぶん 2 マス前進 (1 体目で止まらない)");
  }

  @Test
  void advanceFloorRefillsApAndDrawsCardOnNewLayer() {
    // §15-6 / ADR-19: 新層開始時に AP リフィル + 1 枚ドロー (startPlayerTurn 流用)
    // 初期デッキ (装備固有カード dash + zangeki) は 2 枚で全部初期ドローされて山札 0 になるため、
    // 1 枚ドローを検証するには捨て札に 1 枚予め積む (山札空 → 捨て札シャッフル → 山札 1 → 1 ドロー)。
    DungeonState first = InitialStateFactory.firstFloor(new Random(SEED));
    core.domain.card.CardPileState pileWithDiscard =
        new core.domain.card.CardPileState(
            first.player().cardPileState().drawPile(),
            first.player().cardPileState().hand(),
            first
                .player()
                .cardPileState()
                .discardPile()
                .add(DomainFixtures.magicCard("magic_bolt")));
    var playerArmed = first.player().withCardPileState(pileWithDiscard);
    // AP を 0 まで使い切った状態を再現してから CLEARED にする (リフィルが効いているか確認するため)
    DungeonState exhausted =
        first.withPlayer(
            playerArmed.withActionPoints(
                playerArmed.actionPoints().spend(playerArmed.actionPoints().current())));
    DungeonState cleared = exhausted.withPhase(TurnPhase.CLEARED);
    GameContext ctx = GameContext.startNewRun(cleared);
    TurnDirector director = new TurnDirector(ctx, new Random(SEED));

    int handBefore = ctx.state().player().cardPileState().hand().size();
    director.advanceFloor(InitialStateFactory.advanceLayer(cleared, new Random(SEED)));

    assertTrue(ctx.state().player().actionPoints().current() > 0, "AP がリフィルされている (current > 0)");
    int handAfter = ctx.state().player().cardPileState().hand().size();
    assertEquals(handBefore + 1, handAfter, "1 枚ドローされて手札が +1");
  }

  @Test
  void advanceFloorEmitsFloorAdvancedAndPhaseChangedEvents() {
    // FloorAdvanced イベントが新層番号付きで発火する (HUD ログ「2 層に到達」用)
    DungeonState cleared =
        InitialStateFactory.firstFloor(new Random(SEED)).withPhase(TurnPhase.CLEARED);
    GameContext ctx = GameContext.startNewRun(cleared);
    TurnDirector director = new TurnDirector(ctx, new Random(SEED));

    director.advanceFloor(InitialStateFactory.advanceLayer(cleared, new Random(SEED)));

    // 最後 4 件以内に FloorAdvanced と TurnPhaseChanged(PLAYER_TURN) が含まれる
    java.util.List<BattleEvent> recent = ctx.latestEvents(8);
    boolean hasFloorAdvanced =
        recent.stream()
            .anyMatch(e -> e instanceof BattleEvent.FloorAdvanced fa && fa.newLayer() == 2);
    boolean hasPhaseChanged =
        recent.stream()
            .anyMatch(
                e ->
                    e instanceof BattleEvent.TurnPhaseChanged tp
                        && tp.newPhase() == TurnPhase.PLAYER_TURN);
    assertTrue(hasFloorAdvanced, "FloorAdvanced(2) が発火されている");
    assertTrue(hasPhaseChanged, "TurnPhaseChanged(PLAYER_TURN) が発火されている");
  }

  @Test
  void advanceFloorChainsAcrossMultipleLayers() {
    // 2 層 → 3 層 への遷移も成立する (連鎖確認)
    DungeonState cleared =
        InitialStateFactory.firstFloor(new Random(SEED)).withPhase(TurnPhase.CLEARED);
    GameContext ctx = GameContext.startNewRun(cleared);
    TurnDirector director = new TurnDirector(ctx, new Random(SEED));

    director.advanceFloor(InitialStateFactory.advanceLayer(cleared, new Random(SEED)));
    // 2 層到達後、context を 2 層 CLEARED 状態に揃えてから 3 層へ進む
    DungeonState secondCleared = ctx.state().withPhase(TurnPhase.CLEARED);
    ctx.applyResult(
        new core.domain.battle.TurnEngine.StepResult(secondCleared, java.util.List.of()));
    director.advanceFloor(InitialStateFactory.advanceLayer(secondCleared, new Random(SEED)));

    assertEquals(3, ctx.state().layer().number(), "2 連続で advanceFloor すると 3 層に到達");
  }

  // ---------------- 異常系 / no-op ----------------

  @Test
  void advanceFloorIsNoOpDuringPlayerTurn() {
    // PLAYER_TURN 中に誤って advanceFloor が呼ばれても state を変えない (二重ガード)
    DungeonState playing = InitialStateFactory.firstFloor(new Random(SEED)); // phase = PLAYER_TURN
    GameContext ctx = GameContext.startNewRun(playing);
    TurnDirector director = new TurnDirector(ctx, new Random(SEED));
    DungeonState before = ctx.state();

    // 仮にも advanceLayer の結果を渡しても、CLEARED でないため反映されないこと
    director.advanceFloor(
        InitialStateFactory.advanceLayer(playing.withPhase(TurnPhase.CLEARED), new Random(SEED)));

    assertSame(before, ctx.state(), "state が変化していない");
    assertEquals(1, ctx.state().layer().number(), "層番号は 1 のまま");
    assertEquals(TurnPhase.PLAYER_TURN, ctx.state().phase(), "phase は PLAYER_TURN のまま");
  }

  @Test
  void advanceFloorIsNoOpDuringGameOver() {
    // GAME_OVER 中に誤って advanceFloor が呼ばれても state を変えない
    DungeonState gameOver =
        InitialStateFactory.firstFloor(new Random(SEED)).withPhase(TurnPhase.GAME_OVER);
    GameContext ctx = GameContext.startNewRun(gameOver);
    TurnDirector director = new TurnDirector(ctx, new Random(SEED));
    DungeonState before = ctx.state();

    director.advanceFloor(
        InitialStateFactory.advanceLayer(gameOver.withPhase(TurnPhase.CLEARED), new Random(SEED)));

    assertSame(before, ctx.state(), "state が変化していない");
    assertEquals(TurnPhase.GAME_OVER, ctx.state().phase(), "phase は GAME_OVER のまま");
  }

  @Test
  void advanceFloorRejectsNullNextLayerState() {
    // null 引数は NPE で拒絶 (Objects.requireNonNull)
    DungeonState cleared =
        InitialStateFactory.firstFloor(new Random(SEED)).withPhase(TurnPhase.CLEARED);
    GameContext ctx = GameContext.startNewRun(cleared);
    TurnDirector director = new TurnDirector(ctx, new Random(SEED));

    assertThrows(NullPointerException.class, () -> director.advanceFloor(null));
  }

  // ---------------- 副作用境界 ----------------

  @Test
  void advanceFloorDoesNotMutatePassedDungeonState() {
    // 渡した nextLayerState は不変 (record + 純関数フロー)
    DungeonState cleared =
        InitialStateFactory.firstFloor(new Random(SEED)).withPhase(TurnPhase.CLEARED);
    GameContext ctx = GameContext.startNewRun(cleared);
    TurnDirector director = new TurnDirector(ctx, new Random(SEED));

    DungeonState nextLayer = InitialStateFactory.advanceLayer(cleared, new Random(SEED));
    int originalLayerNumber = nextLayer.layer().number();
    director.advanceFloor(nextLayer);

    assertEquals(originalLayerNumber, nextLayer.layer().number(), "渡した nextLayer は呼出後も変化していない");
    assertFalse(
        nextLayer == ctx.state(), "context.state() は startPlayerTurn 適用後の新インスタンス (渡したものとは別)");
  }

  // ---------------- 詰み判定 (§15-5 hasMeaningfulAction) ----------------

  @Test
  void hasMeaningfulActionReturnsFalseWhenAllNeighborsBlocked() {
    // 5x5 部屋の角 (1,1)、隣接 4 マスは 2 壁 + 2 敵で全塞ぎ、手札は移動カードのみ → 詰み判定 false
    Player player =
        DomainFixtures.playerAt(new Position(1, 1))
            .withActionPoints(ActionPoints.full(3))
            .withCardPileState(
                new CardPileState(
                    DrawPile.empty(),
                    Hand.empty().add(DomainFixtures.moveCard("dash")),
                    DiscardPile.empty()));
    Enemy enemyRight = DomainFixtures.slimeAt(new Position(2, 1));
    Enemy enemyDown = DomainFixtures.slimeAt(new Position(1, 2));
    DungeonState state =
        DomainFixtures.newStateWith(
            DomainFixtures.squareRoom(),
            player,
            List.of(enemyRight, enemyDown),
            TurnPhase.PLAYER_TURN);

    assertFalse(TurnDirector.hasMeaningfulAction(state));
  }

  @Test
  void hasMeaningfulActionReturnsTrueWithAttackCard() {
    // 同じ詰み配置でも ATTACK カードを持っていれば true (戦闘の意思決定が残っている)
    Player player =
        DomainFixtures.playerAt(new Position(1, 1))
            .withActionPoints(ActionPoints.full(3))
            .withCardPileState(
                new CardPileState(
                    DrawPile.empty(),
                    Hand.empty().add(DomainFixtures.attackCard("zangeki")),
                    DiscardPile.empty()));
    Enemy enemyRight = DomainFixtures.slimeAt(new Position(2, 1));
    Enemy enemyDown = DomainFixtures.slimeAt(new Position(1, 2));
    DungeonState state =
        DomainFixtures.newStateWith(
            DomainFixtures.squareRoom(),
            player,
            List.of(enemyRight, enemyDown),
            TurnPhase.PLAYER_TURN);

    assertTrue(TurnDirector.hasMeaningfulAction(state));
  }

  @Test
  void hasMeaningfulActionReturnsTrueWithBuffCard() {
    // 同じ詰み配置でも BUFF カード (鉄の皮膚) を持っていれば true (防御の意思決定が残っている)
    Card ironSkin =
        new Card(
            CardId.of("iron_skin"),
            "鉄の皮膚",
            1,
            CardTag.BUFF,
            CardElement.PHYSICAL,
            new CardEffect.Buff(CardEffect.BuffKind.PHYSICAL_DEFENSE_UP, 2, 3));
    Player player =
        DomainFixtures.playerAt(new Position(1, 1))
            .withActionPoints(ActionPoints.full(3))
            .withCardPileState(
                new CardPileState(
                    DrawPile.empty(), Hand.empty().add(ironSkin), DiscardPile.empty()));
    Enemy enemyRight = DomainFixtures.slimeAt(new Position(2, 1));
    Enemy enemyDown = DomainFixtures.slimeAt(new Position(1, 2));
    DungeonState state =
        DomainFixtures.newStateWith(
            DomainFixtures.squareRoom(),
            player,
            List.of(enemyRight, enemyDown),
            TurnPhase.PLAYER_TURN);

    assertTrue(TurnDirector.hasMeaningfulAction(state));
  }

  @Test
  void hasMeaningfulActionReturnsTrueWithWalkableNeighbor() {
    // 中央 (2,2) で敵なし、手札は移動カードのみ → 隣接 4 マス全て歩行可 → 通常移動で逃げられるため true
    Player player =
        DomainFixtures.playerAt(new Position(2, 2))
            .withActionPoints(ActionPoints.full(3))
            .withCardPileState(
                new CardPileState(
                    DrawPile.empty(),
                    Hand.empty().add(DomainFixtures.moveCard("dash")),
                    DiscardPile.empty()));
    DungeonState state =
        DomainFixtures.newStateWith(
            DomainFixtures.squareRoom(), player, List.of(), TurnPhase.PLAYER_TURN);

    assertTrue(TurnDirector.hasMeaningfulAction(state));
  }
}
