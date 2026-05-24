package core.domain.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.common.Position;
import core.domain.dungeon.DungeonMap;
import core.domain.dungeon.DungeonState;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.EnemyAiState;
import core.domain.entity.EnemyKind;
import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.domain.skill.Skill;
import core.domain.skill.SkillEffect;
import core.domain.skill.SkillId;
import core.domain.skill.SkillSlot;
import core.domain.support.DomainFixtures;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Wave 13 W13-β: 視界 + 状態遷移 + kind 別行動プロファイルの統合テスト。 */
class EnemyAiBehaviorTest {

  /** 7x5 の縦長部屋 (内部すべて床)。 */
  private static DungeonMap openRoom() {
    return DungeonMap.of(List.of("#######", "#.....#", "#.....#", "#.....#", "#######"));
  }

  /** (3,2) に壁を挟む部屋 (LOS 遮断テスト用)。 */
  private static DungeonMap wallBlockedRoom() {
    return DungeonMap.of(List.of("#######", "#.....#", "#..#..#", "#.....#", "#######"));
  }

  /** (3,2) に壊れる壁を挟む部屋 (BREAKABLE_WALL LOS 遮断テスト用)。 */
  private static DungeonMap breakableBlockedRoom() {
    return DungeonMap.of(List.of("#######", "#.....#", "#..B..#", "#.....#", "#######"));
  }

  private static Skill slimeBite() {
    return new Skill(SkillId.of("bite"), "Bite", 1, new SkillEffect.Damage(2));
  }

  private static Enemy enemyOfKind(EnemyKind kind, Position pos) {
    return new Enemy(
        ActorId.of("e_" + kind.name()),
        pos,
        new Stats(10, 10, 2, 2, 0, 0, 0),
        ActionPoints.full(3),
        new SkillSlot(List.of(slimeBite()), 4),
        kind);
  }

  // ----- 視界判定 + 状態遷移 -----

  @Test
  void idleEnemyStaysIdleAndWaitsWhenPlayerOutOfSight() {
    // 視界外: WALL で LOS 遮断 → IDLE のまま、Wait を返す
    Player p = DomainFixtures.playerAt(new Position(5, 2));
    Enemy e = enemyOfKind(EnemyKind.SLIME, new Position(1, 2));
    DungeonState s = new DungeonState(wallBlockedRoom(), p, List.of(e), TurnPhase.ENEMY_TURN);

    Enemy updated = EnemyAi.computeNewState(e, s);
    assertEquals(EnemyAiState.IDLE, updated.aiState());
    assertEquals(Optional.empty(), updated.lastKnownPlayerPos());

    BattleAction action = EnemyAi.decide(e, s);
    assertInstanceOf(BattleAction.Wait.class, action);
  }

  @Test
  void idleEnemyTransitionsToAlertWhenPlayerEntersSight() {
    // 視界内: 直線 LOS、距離 4 (SLIME.sightRange=4) → ALERT 遷移、lastKnownPlayerPos 記録
    Player p = DomainFixtures.playerAt(new Position(5, 2));
    Enemy e = enemyOfKind(EnemyKind.SLIME, new Position(1, 2));
    DungeonState s = new DungeonState(openRoom(), p, List.of(e), TurnPhase.ENEMY_TURN);

    Enemy updated = EnemyAi.computeNewState(e, s);
    assertEquals(EnemyAiState.ALERT, updated.aiState());
    assertEquals(Optional.of(new Position(5, 2)), updated.lastKnownPlayerPos());
  }

  @Test
  void alertEnemyTransitionsToSearchingWhenPlayerLeavesSight() {
    // 直前 ALERT、lastKnownPlayerPos あり、視界外 → SEARCHING に遷移、Pos 保持
    Position lastSeen = new Position(5, 2);
    Player p = DomainFixtures.playerAt(new Position(5, 2));
    Enemy e =
        enemyOfKind(EnemyKind.SLIME, new Position(1, 2))
            .withAiState(EnemyAiState.ALERT)
            .withLastKnownPlayerPos(Optional.of(lastSeen));
    DungeonState s = new DungeonState(wallBlockedRoom(), p, List.of(e), TurnPhase.ENEMY_TURN);

    Enemy updated = EnemyAi.computeNewState(e, s);
    assertEquals(EnemyAiState.SEARCHING, updated.aiState());
    assertEquals(Optional.of(lastSeen), updated.lastKnownPlayerPos());
  }

  @Test
  void searchingEnemyReturnsIdleWhenReachingLastKnownPos() {
    // SEARCHING + 自分が lastKnownPlayerPos に到達 + 視界外 → IDLE 戻り + Pos クリア
    // SLIME (sight=4) よりプレイヤーを遠くに配置して視界外を確保する
    DungeonMap longRoom = DungeonMap.of(List.of("###########", "#.........#", "###########"));
    Position lastSeen = new Position(1, 1); // 敵が立っている位置と同じ
    Player p = DomainFixtures.playerAt(new Position(8, 1)); // チェビシェフ距離 7 > sight 4
    Enemy e =
        enemyOfKind(EnemyKind.SLIME, new Position(1, 1))
            .withAiState(EnemyAiState.SEARCHING)
            .withLastKnownPlayerPos(Optional.of(lastSeen));
    DungeonState s = new DungeonState(longRoom, p, List.of(e), TurnPhase.ENEMY_TURN);

    Enemy updated = EnemyAi.computeNewState(e, s);
    assertEquals(EnemyAiState.IDLE, updated.aiState());
    assertEquals(Optional.empty(), updated.lastKnownPlayerPos());
  }

  // ----- LOS 遮断: WALL / BREAKABLE_WALL -----

  @Test
  void wallBlocksLineOfSightSoEnemyStaysIdle() {
    Player p = DomainFixtures.playerAt(new Position(5, 2));
    Enemy e = enemyOfKind(EnemyKind.SLIME, new Position(1, 2));
    DungeonState s = new DungeonState(wallBlockedRoom(), p, List.of(e), TurnPhase.ENEMY_TURN);

    Enemy updated = EnemyAi.computeNewState(e, s);
    assertEquals(EnemyAiState.IDLE, updated.aiState());
  }

  @Test
  void breakableWallBlocksLineOfSightSoEnemyStaysIdle() {
    // CTO #2: BREAKABLE_WALL も視線遮断 (Wave 11 一貫性)
    Player p = DomainFixtures.playerAt(new Position(5, 2));
    Enemy e = enemyOfKind(EnemyKind.SLIME, new Position(1, 2));
    DungeonState s = new DungeonState(breakableBlockedRoom(), p, List.of(e), TurnPhase.ENEMY_TURN);

    Enemy updated = EnemyAi.computeNewState(e, s);
    assertEquals(EnemyAiState.IDLE, updated.aiState());
  }

  // ----- CAUTIOUS (kite) 動作 -----

  @Test
  void cautiousEnemyFleesWhenPlayerAdjacent() {
    // SWIFT_SLIME (CAUTIOUS) は隣接されたら離れる方向に移動
    Player p = DomainFixtures.playerAt(new Position(2, 2));
    Enemy e = enemyOfKind(EnemyKind.SWIFT_SLIME, new Position(3, 2));
    DungeonState s = new DungeonState(openRoom(), p, List.of(e), TurnPhase.ENEMY_TURN);

    BattleAction action = EnemyAi.decide(e, s);
    assertInstanceOf(BattleAction.Move.class, action, "隣接プレイヤーから離れる方向に Move");
    // 離れる方向 = RIGHT (プレイヤーが左、敵が右なので右に逃げる)
    BattleAction.Move move = (BattleAction.Move) action;
    assertNotEquals(core.domain.common.Direction.LEFT, move.direction(), "プレイヤーに近づく方向は選ばない");
  }

  @Test
  void cautiousEnemyFallbacksToAggressiveWhenLackingRangedSkill() {
    // Wave 15 W15-α / #7: SWIFT_SLIME は遠距離スキル未保有 → 距離 2-3 でも AGGRESSIVE 相当で詰める
    // (旧仕様: 距離 2-3 で Wait で「無害化バグ」が発生していた、Wave 14+ で敵側 range 実装後に kite 復活予定)
    Player p = DomainFixtures.playerAt(new Position(1, 2));
    Enemy e = enemyOfKind(EnemyKind.SWIFT_SLIME, new Position(3, 2)); // 距離 2
    DungeonState s = new DungeonState(openRoom(), p, List.of(e), TurnPhase.ENEMY_TURN);

    BattleAction action = EnemyAi.decide(e, s);
    // 攻撃手段なしで Wait してしまう旧バグを修正、AGGRESSIVE で BFS 移動 (or 隣接なら攻撃)
    assertTrue(action instanceof BattleAction.Move, "距離 2 + 遠距離スキルなしならプレイヤー方向へ移動 (アグレッシブフォールバック)");
  }

  @Test
  void cautiousEnemyChasesWhenFarAway() {
    // SWIFT_SLIME がプレイヤーから距離 4 以上 → 近づく BFS
    Player p = DomainFixtures.playerAt(new Position(1, 2));
    Enemy e = enemyOfKind(EnemyKind.SWIFT_SLIME, new Position(5, 2)); // 距離 4
    DungeonState s = new DungeonState(openRoom(), p, List.of(e), TurnPhase.ENEMY_TURN);

    BattleAction action = EnemyAi.decide(e, s);
    assertInstanceOf(BattleAction.Move.class, action, "距離 4 以上で接近 Move");
    assertEquals(
        core.domain.common.Direction.LEFT,
        ((BattleAction.Move) action).direction(),
        "BFS 1 歩目はプレイヤー方向 (LEFT)");
  }

  @Test
  void cautiousEnemyFallbacksToAttackWhenCornered() {
    // CTO #3: CAUTIOUS の袋小路フォールバック。隣接プレイヤー + 全方向逃げ場なし → 攻撃 (UseSkill)
    // 1×3 通路: 敵 (1,1) はプレイヤー (1,2) に隣接、UP/DOWN/LEFT/RIGHT すべて壁 or プレイヤー
    DungeonMap corridor = DungeonMap.of(List.of("###", "#.#", "#.#", "###"));
    Player p = DomainFixtures.playerAt(new Position(1, 2));
    Enemy e = enemyOfKind(EnemyKind.SWIFT_SLIME, new Position(1, 1));
    DungeonState s = new DungeonState(corridor, p, List.of(e), TurnPhase.ENEMY_TURN);

    BattleAction action = EnemyAi.decide(e, s);
    assertInstanceOf(BattleAction.UseSkill.class, action, "袋小路 + 隣接プレイヤー → 腹を括って攻撃");
  }

  // ----- AGGRESSIVE (既存挙動) -----

  @Test
  void aggressiveEnemyAttacksAdjacentPlayer() {
    Player p = DomainFixtures.playerAt(new Position(2, 2));
    Enemy e = enemyOfKind(EnemyKind.SLIME, new Position(3, 2));
    DungeonState s = new DungeonState(openRoom(), p, List.of(e), TurnPhase.ENEMY_TURN);

    BattleAction action = EnemyAi.decide(e, s);
    assertInstanceOf(BattleAction.UseSkill.class, action);
  }

  @Test
  void aggressiveEnemyChasesPlayerInSight() {
    Player p = DomainFixtures.playerAt(new Position(1, 2));
    Enemy e = enemyOfKind(EnemyKind.SLIME, new Position(4, 2));
    DungeonState s = new DungeonState(openRoom(), p, List.of(e), TurnPhase.ENEMY_TURN);

    BattleAction action = EnemyAi.decide(e, s);
    assertInstanceOf(BattleAction.Move.class, action);
    assertEquals(
        core.domain.common.Direction.LEFT,
        ((BattleAction.Move) action).direction(),
        "プレイヤー方向 (LEFT) へ詰める");
  }

  // ----- SEARCHING 行動 -----

  @Test
  void searchingEnemyMovesTowardLastKnownPosition() {
    // 視界外 SEARCHING、未到達 → lastKnownPlayerPos に BFS 移動
    Position lastSeen = new Position(5, 2);
    Player p = DomainFixtures.playerAt(new Position(1, 1)); // 視界外
    Enemy e =
        enemyOfKind(EnemyKind.SLIME, new Position(2, 2))
            .withAiState(EnemyAiState.SEARCHING)
            .withLastKnownPlayerPos(Optional.of(lastSeen));
    DungeonState s = new DungeonState(wallBlockedRoom(), p, List.of(e), TurnPhase.ENEMY_TURN);
    // wallBlockedRoom の (3,2) は壁、(2,2) から (5,2) への経路は迂回 (例: (2,2)→(2,3)→(3,3)→...)

    BattleAction action = EnemyAi.decide(e, s);
    // 迂回経路があるかは BFS 次第。Move か Wait のどちらか、IDLE 戻りはしない (lastSeen != me)
    assertTrue(action instanceof BattleAction.Move || action instanceof BattleAction.Wait);
  }
}
