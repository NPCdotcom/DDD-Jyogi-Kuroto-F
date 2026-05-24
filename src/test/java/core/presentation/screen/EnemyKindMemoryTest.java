package core.presentation.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.entity.ActorId;
import core.domain.entity.EnemyKind;
import org.junit.jupiter.api.Test;

/**
 * {@link EnemyKindMemory} の単体テスト (Wave 4 W4-α)。
 *
 * <p>純粋データクラスなので LibGDX 依存なしでテスト可能。recordEnemy / getEnemyKind / clear / size の各 API を網羅する。
 */
class EnemyKindMemoryTest {

  @Test
  void freshMemoryIsEmpty() {
    EnemyKindMemory m = new EnemyKindMemory();
    assertEquals(0, m.size());
    assertTrue(m.getEnemyKind(ActorId.of("e1")).isEmpty());
  }

  @Test
  void recordEnemyStoresKindRetrievableById() {
    EnemyKindMemory m = new EnemyKindMemory();
    ActorId id = ActorId.of("slime_1");
    m.recordEnemy(id, EnemyKind.SLIME);
    assertEquals(1, m.size());
    assertEquals(EnemyKind.SLIME, m.getEnemyKind(id).orElseThrow());
  }

  @Test
  void recordEnemyOverwritesExistingId() {
    EnemyKindMemory m = new EnemyKindMemory();
    ActorId id = ActorId.of("x");
    m.recordEnemy(id, EnemyKind.SLIME);
    m.recordEnemy(id, EnemyKind.ELITE_SLIME);
    assertEquals(1, m.size(), "同一 ID 上書きはサイズ不変");
    assertEquals(EnemyKind.ELITE_SLIME, m.getEnemyKind(id).orElseThrow());
  }

  @Test
  void getEnemyKindReturnsEmptyForUnknownId() {
    EnemyKindMemory m = new EnemyKindMemory();
    m.recordEnemy(ActorId.of("a"), EnemyKind.SLIME);
    assertTrue(m.getEnemyKind(ActorId.of("b")).isEmpty());
  }

  @Test
  void clearRemovesAllEntries() {
    EnemyKindMemory m = new EnemyKindMemory();
    m.recordEnemy(ActorId.of("a"), EnemyKind.SLIME);
    m.recordEnemy(ActorId.of("b"), EnemyKind.BOSS);
    m.clear();
    assertEquals(0, m.size());
    assertFalse(m.getEnemyKind(ActorId.of("a")).isPresent());
  }

  @Test
  void recordEnemyRejectsNullId() {
    EnemyKindMemory m = new EnemyKindMemory();
    assertThrows(NullPointerException.class, () -> m.recordEnemy(null, EnemyKind.SLIME));
  }

  @Test
  void recordEnemyRejectsNullKind() {
    EnemyKindMemory m = new EnemyKindMemory();
    assertThrows(NullPointerException.class, () -> m.recordEnemy(ActorId.of("x"), null));
  }
}
