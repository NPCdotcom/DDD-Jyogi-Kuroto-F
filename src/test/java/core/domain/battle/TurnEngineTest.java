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
import core.domain.card.TrapLifetime;
import core.domain.common.Direction;
import core.domain.common.Position;
import core.domain.dungeon.DungeonMap;
import core.domain.dungeon.DungeonState;
import core.domain.dungeon.PlacedTrap;
import core.domain.entity.Enemy;
import core.domain.entity.EnemyKind;
import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.domain.meta.Soul;
import core.domain.support.DomainFixtures;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TurnEngineTest {

  private DungeonState newState(Player player, List<Enemy> enemies) {
    DungeonMap map = DomainFixtures.squareRoom();
    return new DungeonState(map, player, enemies, TurnPhase.PLAYER_TURN);
  }

  @Test
  void moveIntoFloorSucceeds() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    DungeonState s = newState(p, List.of());

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.Move(Direction.RIGHT));

    assertFalse(result.wasRejected());
    assertEquals(new Position(2, 1), result.state().player().position());
    assertEquals(4, result.state().player().actionPoints().current());
    assertTrue(result.events().get(0) instanceof BattleEvent.Moved);
  }

  @Test
  void moveIntoWallIsRejected() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    DungeonState s = newState(p, List.of());

    // (1,1) から DOWN は (1,0) で壁
    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.Move(Direction.DOWN));

    assertTrue(result.wasRejected());
    assertEquals(p.position(), result.state().player().position());
    assertEquals(p.actionPoints(), result.state().player().actionPoints());
  }

  @Test
  void moveIntoEnemyIsRejected() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Enemy e = DomainFixtures.slimeAt(new Position(2, 1));
    DungeonState s = newState(p, List.of(e));

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.Move(Direction.RIGHT));

    assertTrue(result.wasRejected());
  }

  @Test
  void skillKillsAdjacentEnemyAndAwardsSoulButPhaseStaysPlayerTurn() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Enemy e = DomainFixtures.slimeAt(new Position(2, 1));
    DungeonState s = newState(p, List.of(e));

    // heavy attack (slot 1) は 15 ダメージ → 1 撃でスライム (HP 10) 撃破
    TurnEngine.StepResult result = TurnEngine.resolvePlayerAction(s, new BattleAction.UseSkill(1));

    assertFalse(result.wasRejected());
    assertTrue(result.state().enemies().isEmpty());
    assertEquals(EnemyKind.SLIME.soulReward(), result.state().player().soul().amount());
    // 敵全滅ではフェーズを変えない (CLEARED は階段踏破のみ)
    assertEquals(TurnPhase.PLAYER_TURN, result.state().phase());
    assertTrue(result.events().stream().anyMatch(ev -> ev instanceof BattleEvent.ActorDied));
    assertTrue(result.events().stream().anyMatch(ev -> ev instanceof BattleEvent.SoulGained));
  }

  @Test
  void killingOneEnemyOfTwoDoesNotChangePhase() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Enemy adjacent = DomainFixtures.slimeAt("e1", new Position(2, 1), ActionPoints.full(3));
    Enemy distant = DomainFixtures.slimeAt("e2", new Position(3, 3), ActionPoints.full(3));
    DungeonState s = newState(p, List.of(adjacent, distant));

    // heavy attack で adjacent を 1 撃撃破。distant は残存する。
    TurnEngine.StepResult result = TurnEngine.resolvePlayerAction(s, new BattleAction.UseSkill(1));

    assertFalse(result.wasRejected());
    assertEquals(1, result.state().enemies().size(), "1 体撃破後も 1 体残存");
    assertEquals(TurnPhase.PLAYER_TURN, result.state().phase(), "敵全滅以外では CLEARED にならない");
  }

  @Test
  void playerWithZeroApRejectsActions() {
    Player p = DomainFixtures.playerAt(new Position(1, 1)).withActionPoints(ActionPoints.empty(5));
    DungeonState s = newState(p, List.of());

    for (BattleAction action :
        List.of(new BattleAction.Move(Direction.RIGHT), new BattleAction.Wait())) {
      TurnEngine.StepResult result = TurnEngine.resolvePlayerAction(s, action);
      assertTrue(result.wasRejected(), action + " は AP 0 で reject されるべき");
      assertEquals(p.actionPoints(), result.state().player().actionPoints(), "AP は変化しないままのはず");
    }
  }

  @Test
  void steppingOntoStairsTriggersCleared() {
    Player p = DomainFixtures.playerAt(new Position(1, 2));
    DungeonState s =
        new DungeonState(DomainFixtures.roomWithStairs(), p, List.of(), TurnPhase.PLAYER_TURN);

    // (1,2) から RIGHT で (2,2) = STAIRS_DOWN に乗る
    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.Move(Direction.RIGHT));

    assertFalse(result.wasRejected());
    assertEquals(TurnPhase.CLEARED, result.state().phase());
    assertTrue(
        result.events().stream()
            .anyMatch(
                ev ->
                    ev instanceof BattleEvent.TurnPhaseChanged tpc
                        && tpc.newPhase() == TurnPhase.CLEARED));
  }

  @Test
  void skillWithoutAdjacentTargetIsRejected() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Enemy e = DomainFixtures.slimeAt(new Position(3, 1));
    DungeonState s = newState(p, List.of(e));

    TurnEngine.StepResult result = TurnEngine.resolvePlayerAction(s, new BattleAction.UseSkill(0));

    assertTrue(result.wasRejected());
  }

  @Test
  void skillWithInsufficientApIsRejected() {
    Player p = DomainFixtures.playerAt(new Position(1, 1)).withActionPoints(new ActionPoints(1, 5));
    Enemy e = DomainFixtures.slimeAt(new Position(2, 1));
    DungeonState s = newState(p, List.of(e));

    // heavy attack は 3 AP 必要、手持ち 1 なので拒否
    TurnEngine.StepResult result = TurnEngine.resolvePlayerAction(s, new BattleAction.UseSkill(1));

    assertTrue(result.wasRejected());
  }

  @Test
  void endTurnTransitionsToEnemyTurnAndRegeneratesEnemies() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Enemy e = DomainFixtures.slimeAt(new Position(3, 3)).withActionPoints(new ActionPoints(0, 3));
    DungeonState s = newState(p, List.of(e));

    TurnEngine.StepResult result = TurnEngine.resolvePlayerAction(s, new BattleAction.EndTurn());

    assertEquals(TurnPhase.ENEMY_TURN, result.state().phase());
    Enemy refreshed = result.state().enemies().get(0);
    assertEquals(2, refreshed.actionPoints().current()); // speed=2 ぶん回復
  }

  @Test
  void startPlayerTurnRefillsPlayerApAndSetsPhase() {
    Player p = DomainFixtures.playerAt(new Position(1, 1)).withActionPoints(new ActionPoints(0, 5));
    DungeonState s =
        new DungeonState(DomainFixtures.squareRoom(), p, List.of(), TurnPhase.ENEMY_TURN);

    // ADR-19: Random 引数注入で startPlayerTurn (AP リフィル + 1 枚ドロー)
    TurnEngine.StepResult result = TurnEngine.startPlayerTurn(s, new Random(42));

    assertEquals(TurnPhase.PLAYER_TURN, result.state().phase());
    assertEquals(3, result.state().player().actionPoints().current()); // speed=3 ぶん回復
    // playerAt の CardPileState は empty なのでドロー 0 枚 (drawN は静かに停止)
    assertEquals(0, result.state().player().cardPileState().hand().size());
  }

  @Test
  void startPlayerTurnDrawsOneCardFromDrawPile() {
    // ADR-19: ターン頭で 1 枚ドロー
    Player p =
        DomainFixtures.playerAt(new Position(1, 1))
            .withActionPoints(new ActionPoints(0, 5))
            .withCardPileState(
                new CardPileState(
                    DomainFixtures.drawPileOfSize(3), Hand.empty(), DiscardPile.empty()));
    DungeonState s =
        new DungeonState(DomainFixtures.squareRoom(), p, List.of(), TurnPhase.ENEMY_TURN);

    TurnEngine.StepResult result = TurnEngine.startPlayerTurn(s, new Random(42));

    // 山札 3 → 2、手札 0 → 1
    assertEquals(2, result.state().player().cardPileState().drawPile().size());
    assertEquals(1, result.state().player().cardPileState().hand().size());
  }

  @Test
  void startPlayerTurnReshufflesDiscardWhenDrawPileEmpty() {
    // ADR-19: 山札 0 + 捨て札 3 のとき、drawN(1) で捨て札を再シャッフル → 1 枚引く
    Player p =
        DomainFixtures.playerAt(new Position(1, 1))
            .withActionPoints(new ActionPoints(0, 5))
            .withCardPileState(
                new CardPileState(
                    DrawPile.empty(), Hand.empty(), DomainFixtures.discardPileOfSize(3)));
    DungeonState s =
        new DungeonState(DomainFixtures.squareRoom(), p, List.of(), TurnPhase.ENEMY_TURN);

    TurnEngine.StepResult result = TurnEngine.startPlayerTurn(s, new Random(42));

    // 捨て札 3 → 山札にシャッフル → 1 枚引く → 山札 2、捨て札 0、手札 1
    assertEquals(2, result.state().player().cardPileState().drawPile().size());
    assertEquals(0, result.state().player().cardPileState().discardPile().size());
    assertEquals(1, result.state().player().cardPileState().hand().size());
  }

  @Test
  void enemyAttackKillsPlayerTriggersGameOverAndKeepsSoul() {
    // プレイヤー HP 5 を 5 ダメージで倒す。事前にソウル 7 を持たせ、死亡後も保持されることを検証する
    // (GAME_DESIGN §5-3 「死亡時にソウルは保持」)
    Player weakPlayer =
        DomainFixtures.playerAt(new Position(1, 1))
            // ADR-17: 物攻/魔攻/物防/魔防 は暫定 0 埋め (DomainFixtures 既定値と同じ)
            .withStats(new core.domain.entity.Stats(5, 30, 3, 0, 0, 0, 0))
            .addSoul(new Soul(7));
    Enemy slime = DomainFixtures.slimeAt(new Position(2, 1));
    DungeonState s =
        new DungeonState(
            DomainFixtures.squareRoom(), weakPlayer, List.of(slime), TurnPhase.ENEMY_TURN);

    TurnEngine.StepResult result =
        TurnEngine.resolveEnemyAction(s, slime.id(), new BattleAction.UseSkill(0));

    assertFalse(result.wasRejected());
    assertEquals(TurnPhase.GAME_OVER, result.state().phase());
    assertEquals(0, result.state().player().stats().currentHp());
    assertEquals(7, result.state().player().soul().amount(), "死亡時もソウルは持ち越し");
    assertTrue(result.events().stream().anyMatch(ev -> ev instanceof BattleEvent.ActorDied));
  }

  @Test
  void wrongPhaseRejectsPlayerAction() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    DungeonState s =
        new DungeonState(DomainFixtures.squareRoom(), p, List.of(), TurnPhase.ENEMY_TURN);

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.Move(Direction.RIGHT));

    assertTrue(result.wasRejected());
  }

  // =========================================================
  // §15-3 / ADR-18: BattleAction.UseCard 系
  // =========================================================

  @Test
  void useCardDealsDamageToAdjacentEnemy() {
    // 手札 1 枚 (attackCard: PHYSICAL Damage=5, AP コスト=1) のプレイヤー
    Player p = DomainFixtures.playerAtWithHand(new Position(1, 1), 1);
    Enemy slime = DomainFixtures.slimeAt(new Position(2, 1)); // 右隣 1 マス
    DungeonState s = newState(p, List.of(slime));

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.UseCard(0, Direction.RIGHT));

    assertFalse(result.wasRejected());
    // ダメージ: max(1, 5 + 0 - 0) = 5、スライム HP 10 → 5
    assertEquals(5, result.state().enemies().get(0).stats().currentHp());
    // AP 消費 1 (5 → 4)
    assertEquals(4, result.state().player().actionPoints().current());
    // Hand 1 → 0、Discard 0 → 1
    assertEquals(0, result.state().player().cardPileState().hand().size());
    assertEquals(1, result.state().player().cardPileState().discardPile().size());
  }

  @Test
  void useCardWithInsufficientApIsRejected() {
    Player p =
        DomainFixtures.playerAtWithHand(new Position(1, 1), 1)
            .withActionPoints(new ActionPoints(0, 5));
    Enemy slime = DomainFixtures.slimeAt(new Position(2, 1));
    DungeonState s = newState(p, List.of(slime));

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.UseCard(0, Direction.RIGHT));

    assertTrue(result.wasRejected());
    // 状態は不変
    assertEquals(1, result.state().player().cardPileState().hand().size());
    assertEquals(10, result.state().enemies().get(0).stats().currentHp());
  }

  @Test
  void useCardWithoutTargetInDirectionIsRejected() {
    // 上方向に敵いない、右隣にスライム
    Player p = DomainFixtures.playerAtWithHand(new Position(2, 2), 1);
    Enemy slime = DomainFixtures.slimeAt(new Position(3, 2));
    DungeonState s = newState(p, List.of(slime));

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.UseCard(0, Direction.UP));

    assertTrue(result.wasRejected());
    // AP 消費なし、Hand 不変
    assertEquals(5, result.state().player().actionPoints().current());
    assertEquals(1, result.state().player().cardPileState().hand().size());
  }

  @Test
  void useCardWithInvalidHandIndexIsRejected() {
    Player p = DomainFixtures.playerAtWithHand(new Position(1, 1), 1);
    Enemy slime = DomainFixtures.slimeAt(new Position(2, 1));
    DungeonState s = newState(p, List.of(slime));

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.UseCard(99, Direction.RIGHT));

    assertTrue(result.wasRejected());
    // AP 消費なし、Hand 不変
    assertEquals(5, result.state().player().actionPoints().current());
    assertEquals(1, result.state().player().cardPileState().hand().size());
  }

  @Test
  void useMagicCardUsesMagicalStats() {
    // 魔法カード (MAGICAL, baseValue=3, AP コスト=1)、Player の魔攻=5、スライムの魔防=2
    Card magicBolt =
        new Card(
            CardId.of("magic-1"),
            "魔法弾",
            1,
            CardTag.ATTACK,
            CardElement.MAGICAL,
            new CardEffect.Damage(3));
    CardPileState piles =
        new CardPileState(DrawPile.empty(), Hand.empty().add(magicBolt), DiscardPile.empty());
    Player p =
        DomainFixtures.playerAt(new Position(1, 1))
            .withStats(new Stats(30, 30, 3, 0, 5, 0, 0)) // 魔攻 5
            .withCardPileState(piles);
    Enemy slime =
        DomainFixtures.slimeAt(new Position(2, 1))
            .withStats(new Stats(10, 10, 2, 0, 0, 0, 2)); // 魔防 2
    DungeonState s = newState(p, List.of(slime));

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.UseCard(0, Direction.RIGHT));

    assertFalse(result.wasRejected());
    // ダメージ: max(1, 3 + 5 - 2) = 6、スライム HP 10 → 4
    assertEquals(4, result.state().enemies().get(0).stats().currentHp());
  }

  @Test
  void enemyUseCardInEnemyTurnIsRejected() {
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Enemy slime = DomainFixtures.slimeAt(new Position(2, 1));
    DungeonState s =
        new DungeonState(DomainFixtures.squareRoom(), p, List.of(slime), TurnPhase.ENEMY_TURN);

    TurnEngine.StepResult result =
        TurnEngine.resolveEnemyAction(s, slime.id(), new BattleAction.UseCard(0, Direction.RIGHT));

    assertTrue(result.wasRejected());
  }

  // =========================================================
  // §15-5 / ADR-21: Move カード解決テスト
  // =========================================================

  @Test
  void useMoveCardGrantsPendingMoveCountAndConsumesApWithoutMoving() {
    // §15-5 / ADR-21 「移動カード使用 → pendingMoveCount=distance 設定 + AP 消費 + Hand→Discard、移動しない」
    Card dash = DomainFixtures.moveCard("dash"); // distance=2, apCost=1
    CardPileState piles =
        new CardPileState(DrawPile.empty(), Hand.empty().add(dash), DiscardPile.empty());
    Player p = DomainFixtures.playerAt(new Position(2, 2)).withCardPileState(piles); // AP=5 (full)
    DungeonState s = newState(p, List.of());

    // Move カードは direction 不要だが UseCard 型は direction 必須 → RIGHT を渡す
    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.UseCard(0, Direction.RIGHT));

    assertFalse(result.wasRejected());
    Player after = result.state().player();
    // 移動権 distance=2 が付与されていること
    assertEquals(2, after.pendingMoveCount());
    // AP 1 消費 (5 → 4)
    assertEquals(4, after.actionPoints().current());
    // Hand 1 → 0、Discard 0 → 1
    assertEquals(0, after.cardPileState().hand().size());
    assertEquals(1, after.cardPileState().discardPile().size());
    // 位置は変化しない
    assertEquals(new Position(2, 2), after.position());
    // SkillUsed + MovementGranted(remainingSteps=2) が発火
    assertTrue(result.events().get(0) instanceof BattleEvent.SkillUsed);
    BattleEvent.MovementGranted granted = (BattleEvent.MovementGranted) result.events().get(1);
    assertEquals(2, granted.remainingSteps());
  }

  @Test
  void movingWhilePendingCountPositiveConsumesNoApAndDecrementsCount() {
    // §15-5 / ADR-21 「pendingMoveCount > 0 のとき AWSD 移動は AP 0 消費、pendingMoveCount--」
    Player p = DomainFixtures.playerAt(new Position(2, 2)).withPendingMoveCount(2); // 移動権 2 残
    int apBefore = p.actionPoints().current();
    DungeonState s = newState(p, List.of());

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.Move(Direction.RIGHT));

    assertFalse(result.wasRejected());
    Player after = result.state().player();
    // 1 マス移動
    assertEquals(new Position(3, 2), after.position());
    // pendingMoveCount 2 → 1
    assertEquals(1, after.pendingMoveCount());
    // AP 消費なし
    assertEquals(apBefore, after.actionPoints().current());
    // Moved イベント発火
    assertTrue(result.events().get(0) instanceof BattleEvent.Moved);
  }

  @Test
  void useMoveCardWithZeroApIsRejected() {
    // §15-5 / ADR-21 「AP 0 で Move カード使用 → reject、状態変化なし」
    Card dash = DomainFixtures.moveCard("dash");
    CardPileState piles =
        new CardPileState(DrawPile.empty(), Hand.empty().add(dash), DiscardPile.empty());
    Player p =
        DomainFixtures.playerAt(new Position(2, 2))
            .withCardPileState(piles)
            .withActionPoints(ActionPoints.empty(5)); // AP=0
    DungeonState s = newState(p, List.of());

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.UseCard(0, Direction.RIGHT));

    assertTrue(result.wasRejected());
    // 手札は不変
    assertEquals(1, result.state().player().cardPileState().hand().size());
    // pendingMoveCount は変化しない
    assertEquals(0, result.state().player().pendingMoveCount());
  }

  @Test
  void movingIntoWallWhilePendingCountPositiveIsRejectedWithoutDecrementingCount() {
    // §15-5 / ADR-21 「移動権中の壁ブロックは reject、pendingMoveCount は減らない」
    // (2,2) から DOWN は (2,1)=床、UP は (2,3)=床、LEFT は (1,2)=床、DOWN は (2,1)
    // 外壁に接する (1,1) から DOWN → (1,0) は壁
    Player p = DomainFixtures.playerAt(new Position(1, 1)).withPendingMoveCount(2);
    DungeonState s = newState(p, List.of());

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.Move(Direction.DOWN));

    assertTrue(result.wasRejected());
    // pendingMoveCount は減少しない
    assertEquals(2, result.state().player().pendingMoveCount());
    // 位置も変化しない
    assertEquals(new Position(1, 1), result.state().player().position());
  }

  @Test
  void startPlayerTurnResetsPendingMoveCountToZero() {
    // §15-5 / ADR-21 「ターン頭で pendingMoveCount をリセット (移動権はターンをまたがない)」
    Player p =
        DomainFixtures.playerAt(new Position(2, 2))
            .withActionPoints(new ActionPoints(0, 5))
            .withPendingMoveCount(3); // 前ターン残りの移動権
    DungeonState s =
        new DungeonState(DomainFixtures.squareRoom(), p, List.of(), TurnPhase.ENEMY_TURN);

    TurnEngine.StepResult result = TurnEngine.startPlayerTurn(s, new Random(0));

    // pendingMoveCount が 0 にリセットされる
    assertEquals(0, result.state().player().pendingMoveCount());
    // フェーズは PLAYER_TURN、AP はリフィル済
    assertEquals(TurnPhase.PLAYER_TURN, result.state().phase());
    assertEquals(
        p.stats().speed(), result.state().player().actionPoints().current(), "AP は速度分まで回復");
  }

  // =========================================================
  // §15-3 / ADR-22: Trap カード解決テスト
  // =========================================================

  @Test
  void trapCardUsePlacesTrapAtAdjacentTileAndConsumesApAndHand() {
    // §15-3 / ADR-22 「Trap カード使用 → 隣接マスに PlacedTrap 追加、AP 消費、Hand→Discard、SkillUsed + TrapPlaced
    // 発火」
    Card spikeTrap =
        new Card(
            CardId.of("spike_trap"),
            "スパイクトラップ",
            2,
            CardTag.TRAP,
            CardElement.PHYSICAL,
            new CardEffect.Trap(4, TrapLifetime.UntilStepped.INSTANCE));
    CardPileState piles =
        new CardPileState(DrawPile.empty(), Hand.empty().add(spikeTrap), DiscardPile.empty());
    Player p = DomainFixtures.playerAt(new Position(1, 1)).withCardPileState(piles); // AP=5 (full)
    DungeonState s = newState(p, List.of());

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.UseCard(0, Direction.RIGHT));

    assertFalse(result.wasRejected());
    // 設置先は (player.x+1, player.y) = (2,1)
    Position expected = new Position(2, 1);
    assertEquals(1, result.state().placedTraps().size(), "罠が 1 件設置されること");
    PlacedTrap placed = result.state().placedTraps().get(0);
    assertEquals(expected, placed.position(), "設置先座標");
    assertEquals(4, placed.baseValue(), "baseValue=4 で設置");
    // AP 2 消費 (5 → 3)
    assertEquals(3, result.state().player().actionPoints().current());
    // Hand 1 → 0、Discard 0 → 1
    assertEquals(0, result.state().player().cardPileState().hand().size());
    assertEquals(1, result.state().player().cardPileState().discardPile().size());
    // SkillUsed が index=0、TrapPlaced が index=1
    assertTrue(result.events().get(0) instanceof BattleEvent.SkillUsed);
    BattleEvent.TrapPlaced trapPlaced = (BattleEvent.TrapPlaced) result.events().get(1);
    assertEquals(expected, trapPlaced.position());
    assertEquals(4, trapPlaced.baseValue());
  }

  @Test
  void trapCardPlacedOnWallDirectionIsRejected() {
    // §15-3 / ADR-22 「壁タイル方向への罠設置は reject、placedTraps 据置」
    Card trapCard = DomainFixtures.trapCard("pit");
    CardPileState piles =
        new CardPileState(DrawPile.empty(), Hand.empty().add(trapCard), DiscardPile.empty());
    // (1,1) から DOWN → (1,0) は壁
    Player p = DomainFixtures.playerAt(new Position(1, 1)).withCardPileState(piles);
    DungeonState s = newState(p, List.of());

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.UseCard(0, Direction.DOWN));

    assertTrue(result.wasRejected());
    assertEquals(0, result.state().placedTraps().size(), "壁方向への設置で placedTraps が変化しない");
    // AP・手札も不変
    assertEquals(5, result.state().player().actionPoints().current());
    assertEquals(1, result.state().player().cardPileState().hand().size());
  }

  @Test
  void trapCardOverwritesExistingTrapAtSamePosition() {
    // §15-3 / ADR-22 「同座標既存罠は上書き (新規優先)、size は維持」
    // 先に (2,1) に古い罠 (baseValue=1) を配置しておく
    PlacedTrap existingTrap =
        new PlacedTrap(
            new Position(2, 1), 1, TrapLifetime.UntilStepped.INSTANCE, CardElement.PHYSICAL);
    Card newTrap = DomainFixtures.trapCard("pit2"); // baseValue=3
    CardPileState piles =
        new CardPileState(DrawPile.empty(), Hand.empty().add(newTrap), DiscardPile.empty());
    Player p = DomainFixtures.playerAt(new Position(1, 1)).withCardPileState(piles);
    DungeonState s =
        new DungeonState(
            DomainFixtures.squareRoom(),
            p,
            List.of(),
            TurnPhase.PLAYER_TURN,
            List.of(existingTrap));

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.UseCard(0, Direction.RIGHT));

    assertFalse(result.wasRejected());
    // size は 1 を維持 (上書きなので増えない)
    assertEquals(1, result.state().placedTraps().size(), "上書き後も size=1");
    // 新しい罠の baseValue=3 が残っていること (古い 1 が消えていること)
    PlacedTrap overwritten = result.state().placedTraps().get(0);
    assertEquals(3, overwritten.baseValue(), "新罠の baseValue=3 に上書き");
    assertEquals(new Position(2, 1), overwritten.position());
  }

  @Test
  void playerMovingOntoUntilSteppedTrapTakesDamageAndTrapIsRemoved() {
    // §15-3 / ADR-22 「移動で UntilStepped 罠踏み → ダメージ + 罠除去、Moved + TrapTriggered 発火」
    // Player Stats=(30,30,3,0,0,0,0)、物防=0。罠 baseValue=3 → ダメージ=max(1,3-0)=3
    PlacedTrap trap =
        new PlacedTrap(
            new Position(3, 1), 3, TrapLifetime.UntilStepped.INSTANCE, CardElement.PHYSICAL);
    Player p = DomainFixtures.playerAt(new Position(2, 1));
    DungeonState s =
        new DungeonState(
            DomainFixtures.squareRoom(), p, List.of(), TurnPhase.PLAYER_TURN, List.of(trap));

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.Move(Direction.RIGHT));

    assertFalse(result.wasRejected());
    // HP 30 - 3 = 27
    assertEquals(27, result.state().player().stats().currentHp());
    // UntilStepped なので罠除去
    assertEquals(0, result.state().placedTraps().size(), "踏んだら UntilStepped 罠は除去");
    // Moved が最初、TrapTriggered がその後
    assertTrue(result.events().get(0) instanceof BattleEvent.Moved);
    boolean hasTrapTriggered =
        result.events().stream().anyMatch(ev -> ev instanceof BattleEvent.TrapTriggered);
    assertTrue(hasTrapTriggered, "TrapTriggered イベント発火");
    BattleEvent.TrapTriggered triggered =
        (BattleEvent.TrapTriggered)
            result.events().stream()
                .filter(ev -> ev instanceof BattleEvent.TrapTriggered)
                .findFirst()
                .orElseThrow();
    assertEquals(3, triggered.damage());
    assertEquals(27, triggered.remainingHp());
  }

  @Test
  void playerMovingOntoTurnsTrapTakesDamageButTrapRemains() {
    // §15-3 / ADR-22 「移動で Turns 罠踏み → ダメージは入るが罠は残る、TrapTriggered 発火」
    // Turns(3): 残 3 ターン。踏まれても除去されない。
    PlacedTrap turnsTrap =
        new PlacedTrap(new Position(3, 1), 3, new TrapLifetime.Turns(3), CardElement.PHYSICAL);
    Player p = DomainFixtures.playerAt(new Position(2, 1));
    DungeonState s =
        new DungeonState(
            DomainFixtures.squareRoom(), p, List.of(), TurnPhase.PLAYER_TURN, List.of(turnsTrap));

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.Move(Direction.RIGHT));

    assertFalse(result.wasRejected());
    // ダメージ: max(1, 3-0)=3 → HP 30-3=27
    assertEquals(27, result.state().player().stats().currentHp());
    // Turns 罠は踏まれても残る
    assertEquals(1, result.state().placedTraps().size(), "Turns 罠は踏まれても除去されない");
    assertTrue(
        result.events().stream().anyMatch(ev -> ev instanceof BattleEvent.TrapTriggered),
        "TrapTriggered イベント発火");
  }

  @Test
  void startPlayerTurnDecrementsOneTurnsTrapAndRemovesExpiredTrap() {
    // §15-3 / ADR-22 「startPlayerTurn で Turns(1) 罠 → remaining-- → 0 で除去、Turns(2) は Turns(1)
    // に減って残る」
    PlacedTrap expiringTrap =
        new PlacedTrap(new Position(3, 1), 3, new TrapLifetime.Turns(1), CardElement.PHYSICAL);
    PlacedTrap survivingTrap =
        new PlacedTrap(new Position(3, 3), 3, new TrapLifetime.Turns(2), CardElement.PHYSICAL);
    Player p = DomainFixtures.playerAt(new Position(1, 1)).withActionPoints(new ActionPoints(0, 5));
    DungeonState s =
        new DungeonState(
            DomainFixtures.squareRoom(),
            p,
            List.of(),
            TurnPhase.ENEMY_TURN,
            List.of(expiringTrap, survivingTrap));

    TurnEngine.StepResult result = TurnEngine.startPlayerTurn(s, new Random(0));

    // Turns(1) の罠は remaining 0 → 除去
    // Turns(2) の罠は remaining 1 → 残存
    assertEquals(1, result.state().placedTraps().size(), "Turns(1) は除去、Turns(2) は残存");
    PlacedTrap remaining = result.state().placedTraps().get(0);
    assertEquals(new Position(3, 3), remaining.position(), "残存罠は (3,3) の罠");
    // 残存罠の remaining が 2→1 にデクリメントされていること
    TrapLifetime.Turns decremented = (TrapLifetime.Turns) remaining.lifetime();
    assertEquals(1, decremented.remaining(), "Turns(2) が Turns(1) にデクリメント");
  }

  @Test
  void enemyMovingOntoUntilSteppedTrapTakesDamageAndTrapIsRemoved() {
    // §15-3 / ADR-22 「敵が UntilStepped 罠を踏む → ダメージ + 罠除去、TrapTriggered 発火」
    // 敵 Stats=(10,10,2,0,0,0,0)、物防=0。罠 baseValue=3 → ダメージ=max(1,3-0)=3
    // マップ有効床: x=1〜3, y=1〜3。(3,1) に罠、(2,1) から RIGHT に進む
    PlacedTrap trap =
        new PlacedTrap(
            new Position(3, 1), 3, TrapLifetime.UntilStepped.INSTANCE, CardElement.PHYSICAL);
    Player p = DomainFixtures.playerAt(new Position(1, 1));
    Enemy slime = DomainFixtures.slimeAt("slime1", new Position(2, 1), ActionPoints.full(3));
    DungeonState s =
        new DungeonState(
            DomainFixtures.squareRoom(), p, List.of(slime), TurnPhase.ENEMY_TURN, List.of(trap));

    TurnEngine.StepResult result =
        TurnEngine.resolveEnemyAction(s, slime.id(), new BattleAction.Move(Direction.RIGHT));

    assertFalse(result.wasRejected());
    // 敵 HP 10 - 3 = 7
    assertEquals(7, result.state().enemies().get(0).stats().currentHp());
    // UntilStepped 罠は踏まれたら除去
    assertEquals(0, result.state().placedTraps().size(), "敵が踏んだら UntilStepped 罠は除去");
    assertTrue(
        result.events().stream().anyMatch(ev -> ev instanceof BattleEvent.TrapTriggered),
        "TrapTriggered イベント発火");
  }
}
