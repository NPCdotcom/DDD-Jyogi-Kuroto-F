package core.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import core.domain.battle.ActionPoints;
import core.domain.common.Position;
import core.domain.skill.SkillSlot;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Wave 13 W13-α: Enemy record の AI 状態フィールド (aiState / lastKnownPlayerPos) 拡張テスト。 */
class EnemyTest {

  private static Enemy legacyEnemy() {
    return new Enemy(
        ActorId.of("e1"),
        new Position(2, 2),
        new Stats(10, 10, 2, 2, 0, 0, 0),
        ActionPoints.full(2),
        new SkillSlot(List.of(), 4),
        EnemyKind.SLIME);
  }

  @Test
  void legacyConstructorDefaultsToIdleStateAndEmptyMemory() {
    // 後方互換 6 引数コンストラクタは aiState=IDLE / lastKnownPlayerPos=Optional.empty()
    Enemy e = legacyEnemy();
    assertEquals(EnemyAiState.IDLE, e.aiState());
    assertEquals(Optional.empty(), e.lastKnownPlayerPos());
  }

  @Test
  void nullAiStateNormalizedToIdle() {
    Enemy e =
        new Enemy(
            ActorId.of("e1"),
            new Position(2, 2),
            new Stats(10, 10, 2, 2, 0, 0, 0),
            ActionPoints.full(2),
            new SkillSlot(List.of(), 4),
            EnemyKind.SLIME,
            null,
            null);
    assertEquals(EnemyAiState.IDLE, e.aiState());
    assertEquals(Optional.empty(), e.lastKnownPlayerPos());
  }

  @Test
  void withAiStateUpdatesStatePreservingOtherFields() {
    Enemy e = legacyEnemy();
    Enemy alerted = e.withAiState(EnemyAiState.ALERT);
    assertEquals(EnemyAiState.ALERT, alerted.aiState());
    // 他フィールドは保持
    assertEquals(e.position(), alerted.position());
    assertEquals(e.lastKnownPlayerPos(), alerted.lastKnownPlayerPos());
    // 元の Enemy は不変
    assertEquals(EnemyAiState.IDLE, e.aiState());
  }

  @Test
  void withLastKnownPlayerPosUpdatesMemoryPreservingOtherFields() {
    Enemy e = legacyEnemy();
    Position lastSeen = new Position(5, 3);
    Enemy remembered = e.withLastKnownPlayerPos(Optional.of(lastSeen));
    assertEquals(Optional.of(lastSeen), remembered.lastKnownPlayerPos());
    // 他フィールドは保持
    assertEquals(e.position(), remembered.position());
    assertEquals(e.aiState(), remembered.aiState());
  }

  @Test
  void enemyKindExposesSightRangeAndAiProfile() {
    // SLIME = AGGRESSIVE / sight 4
    assertEquals(4, EnemyKind.SLIME.sightRange());
    assertSame(EnemyAiProfile.AGGRESSIVE, EnemyKind.SLIME.aiProfile());
    // SWIFT_SLIME = AGGRESSIVE / sight 6
    assertEquals(6, EnemyKind.SWIFT_SLIME.sightRange());
    assertSame(EnemyAiProfile.AGGRESSIVE, EnemyKind.SWIFT_SLIME.aiProfile());
    // BOSS = AGGRESSIVE / sight 8
    assertEquals(8, EnemyKind.BOSS.sightRange());
    assertSame(EnemyAiProfile.AGGRESSIVE, EnemyKind.BOSS.aiProfile());
  }
}
