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
import core.domain.dungeon.DungeonMap;
import core.domain.dungeon.DungeonState;
import core.domain.entity.Enemy;
import core.domain.entity.Stats;
import core.domain.support.DomainFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Wave 12 W12-β: 遠距離攻撃 (line scan) + AOE (爆風) の統合テスト。 */
class TurnEngineRangedAttackTest {

  /** プレイヤーが (1,2) で AP 5、手札に rangedCard 1 枚。マップは 7×5 で内部すべて床。 */
  private static DungeonState makeState(Card rangedCard, List<Enemy> enemies, DungeonMap map) {
    CardPileState piles =
        new CardPileState(DrawPile.empty(), Hand.empty().add(rangedCard), DiscardPile.empty());
    var player =
        DomainFixtures.playerAt(new Position(1, 2))
            .withCardPileState(piles)
            .withStats(new Stats(30, 30, 5, 0, 0, 0, 0)); // 攻防 0 で baseValue がそのまま反映
    return new DungeonState(map, player, enemies, TurnPhase.PLAYER_TURN);
  }

  /** 7×5 の縦長部屋 (内部すべて床)。 */
  private static DungeonMap openRoom() {
    return DungeonMap.of(List.of("#######", "#.....#", "#.....#", "#.....#", "#######"));
  }

  /** 7×5 の部屋、(3,2) に壁を挟む (line of sight 遮断テスト用)。 */
  private static DungeonMap wallBlockedRoom() {
    return DungeonMap.of(List.of("#######", "#.....#", "#..#..#", "#.....#", "#######"));
  }

  /** 7×5 の部屋、(3,2) に壊れる壁を挟む (BREAKABLE_WALL 遮断テスト用)。 */
  private static DungeonMap breakableBlockedRoom() {
    return DungeonMap.of(List.of("#######", "#.....#", "#..B..#", "#.....#", "#######"));
  }

  private static Card rangedCard(String id, int baseValue, int range, int areaRadius) {
    return new Card(
        CardId.of(id),
        "テスト",
        1,
        CardTag.ATTACK,
        CardElement.PHYSICAL,
        new CardEffect.Damage(baseValue, range, areaRadius));
  }

  // ----- 単体射程 -----

  @Test
  void rangedCardHitsEnemyTwoTilesAway() {
    // プレイヤー (1,2) → RIGHT 方向 (3,2) の敵に当たる (1 マス先=空、2 マス先=敵)
    Enemy slime = DomainFixtures.slimeAt(new Position(3, 2));
    DungeonState s = makeState(rangedCard("ranged", 3, 3, 0), List.of(slime), openRoom());

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.UseCard(0, Direction.RIGHT));

    assertFalse(result.wasRejected());
    // 攻防 0、baseValue 3 → 3 ダメージ、HP 10 → 7
    assertEquals(7, result.state().enemies().get(0).stats().currentHp());
  }

  @Test
  void meleeRangeOneStillWorksOnAdjacentEnemy() {
    // 後方互換: range=1 (近接) で隣接敵 (2,2) に当たる、step=1 から開始する確認
    Enemy slime = DomainFixtures.slimeAt(new Position(2, 2));
    DungeonState s = makeState(rangedCard("melee", 4, 1, 0), List.of(slime), openRoom());

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.UseCard(0, Direction.RIGHT));

    assertFalse(result.wasRejected());
    assertEquals(6, result.state().enemies().get(0).stats().currentHp());
  }

  @Test
  void wallBlocksLineOfSightAndRejects() {
    // プレイヤー (1,2) → RIGHT 方向、(3,2)=WALL でスキャン中断、(4,2) の敵には届かない
    Enemy slime = DomainFixtures.slimeAt(new Position(4, 2));
    DungeonState s =
        makeState(rangedCard("wall_block", 5, 5, 0), List.of(slime), wallBlockedRoom());

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.UseCard(0, Direction.RIGHT));

    assertTrue(result.wasRejected());
    // 敵 HP 変化なし
    assertEquals(10, result.state().enemies().get(0).stats().currentHp());
  }

  @Test
  void breakableWallBlocksRangedDamageAndRemainsIntact() {
    // CTO #2: BREAKABLE_WALL は移動カード専用ギミック。Damage カードでは「ただの壁」として遮断 + 壊れる壁は無傷
    Enemy slime = DomainFixtures.slimeAt(new Position(4, 2));
    DungeonState s =
        makeState(rangedCard("breakable_block", 5, 5, 0), List.of(slime), breakableBlockedRoom());

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.UseCard(0, Direction.RIGHT));

    assertTrue(result.wasRejected());
    assertEquals(10, result.state().enemies().get(0).stats().currentHp());
    // 壊れる壁は破壊されない
    assertEquals(
        core.domain.dungeon.Tile.BREAKABLE_WALL, result.state().map().tileAt(new Position(3, 2)));
  }

  @Test
  void enemyBeyondRangeIsNotHit() {
    // range=2 で 3 マス先 (4,2) の敵には届かない
    Enemy slime = DomainFixtures.slimeAt(new Position(4, 2));
    DungeonState s = makeState(rangedCard("short_range", 5, 2, 0), List.of(slime), openRoom());

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.UseCard(0, Direction.RIGHT));

    assertTrue(result.wasRejected());
    assertEquals(10, result.state().enemies().get(0).stats().currentHp());
  }

  // ----- AOE -----

  @Test
  void aoeHitsCenterAndAdjacentEnemies() {
    // 中心 (3,2)、areaRadius=1 → チェビシェフ距離 ≤ 1 = 中心 + 周囲 8 マス
    // (3,2) と (4,2) の 2 体を巻き込み (両者距離 1 / 0)
    Enemy center = DomainFixtures.slimeAt("slime_c", new Position(3, 2), ActionPoints.full(3));
    Enemy neighbor = DomainFixtures.slimeAt("slime_n", new Position(4, 2), ActionPoints.full(3));
    DungeonState s = makeState(rangedCard("aoe", 3, 3, 1), List.of(center, neighbor), openRoom());

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.UseCard(0, Direction.RIGHT));

    assertFalse(result.wasRejected());
    // 両敵に 3 ダメージ
    assertEquals(7, findEnemyHp(result.state(), "slime_c"));
    assertEquals(7, findEnemyHp(result.state(), "slime_n"));
    // DamageDealt が 2 件
    long damageEvents =
        result.events().stream().filter(e -> e instanceof BattleEvent.DamageDealt).count();
    assertEquals(2, damageEvents);
  }

  @Test
  void aoeReachesEnemyAcrossWall() {
    // AOE は壁越し対応 (KISS、爆風の物理的解釈)
    // 中心 (2,2)、壁 (3,2) を挟んで (4,2) の敵にもダメージ
    Enemy center = DomainFixtures.slimeAt("slime_c", new Position(2, 2), ActionPoints.full(3));
    Enemy across = DomainFixtures.slimeAt("slime_a", new Position(4, 2), ActionPoints.full(3));
    DungeonState s =
        makeState(rangedCard("aoe_wall", 3, 2, 2), List.of(center, across), wallBlockedRoom());

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.UseCard(0, Direction.RIGHT));

    assertFalse(result.wasRejected());
    // 両敵に 3 ダメージ
    assertEquals(7, findEnemyHp(result.state(), "slime_c"));
    assertEquals(7, findEnemyHp(result.state(), "slime_a"));
  }

  @Test
  void aoeRejectsWhenCenterTargetMissing() {
    // CTO #2: 中心不在 = 爆発しない。射程内に敵がいなければ reject
    DungeonState s = makeState(rangedCard("aoe_empty", 5, 3, 1), List.of(), openRoom());

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.UseCard(0, Direction.RIGHT));

    assertTrue(result.wasRejected());
  }

  // ----- ヘルパ -----

  private static int findEnemyHp(DungeonState state, String id) {
    return state.enemies().stream()
        .filter(e -> e.id().value().equals(id))
        .findFirst()
        .orElseThrow()
        .stats()
        .currentHp();
  }
}
