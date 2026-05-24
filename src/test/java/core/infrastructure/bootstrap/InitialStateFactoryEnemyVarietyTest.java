package core.infrastructure.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.common.Position;
import core.domain.dungeon.DungeonState;
import core.domain.entity.Enemy;
import core.domain.entity.EnemyKind;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * §15-5 敵バリエーション (部分実装) の検証。
 *
 * <p>雑魚枠に SWIFT_SLIME / TOUGH_SLIME を追加し、spawn index に応じて複数種が 1 つの層に混在すること、
 * 各ファクトリが暫定能力値どおりの敵を生成することを確認する。
 */
class InitialStateFactoryEnemyVarietyTest {

  @Test
  void newSwiftSlimeForLayerHasFastLowHpStats() {
    Enemy swift = InitialStateFactory.newSwiftSlimeForLayer("swift", new Position(5, 5), 2);
    assertEquals(EnemyKind.SWIFT_SLIME, swift.kind());
    assertEquals(7, swift.stats().maxHp(), "素早い個体は低 HP");
    assertEquals(3, swift.actionPoints().max(), "AP = 層番号 2 + 1 = 3");
  }

  @Test
  void newToughSlimeForLayerHasHighHpAndDefense() {
    Enemy tough = InitialStateFactory.newToughSlimeForLayer("tough", new Position(5, 5), 2);
    assertEquals(EnemyKind.TOUGH_SLIME, tough.kind());
    assertEquals(18, tough.stats().maxHp(), "頑強な個体は高 HP");
    assertEquals(2, tough.stats().physicalDefense(), "頑強な個体は高物防");
    assertEquals(2, tough.actionPoints().max(), "AP = 層番号どおり 2");
  }

  @Test
  void swiftSlimeApIncreasesWithLayer() {
    Enemy swift5 = InitialStateFactory.newSwiftSlimeForLayer("s5", new Position(1, 1), 5);
    assertEquals(6, swift5.actionPoints().max(), "5 層の素早い個体 AP = 5 + 1");
  }

  @Test
  void newSwiftSlimeRejectsLayerNumberZero() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InitialStateFactory.newSwiftSlimeForLayer("z", new Position(1, 1), 0));
  }

  @Test
  void newToughSlimeRejectsLayerNumberZero() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InitialStateFactory.newToughSlimeForLayer("z", new Position(1, 1), 0));
  }

  @Test
  void firstFloorContainsMultipleEnemyKinds() {
    // 層 1 は雑魚 3 体 (index 0/1/2 → 通常 / 素早い / 頑強)。3 種すべてが混在する。
    DungeonState layer1 = InitialStateFactory.firstFloor(new Random(42));
    Set<EnemyKind> kinds = layer1.enemies().stream().map(Enemy::kind).collect(Collectors.toSet());
    assertTrue(kinds.contains(EnemyKind.SLIME), "通常スライムが含まれる");
    assertTrue(kinds.contains(EnemyKind.SWIFT_SLIME), "素早い個体が含まれる");
    assertTrue(kinds.contains(EnemyKind.TOUGH_SLIME), "頑強な個体が含まれる");
  }

  @Test
  void newEnemyKindsHaveProvisionalRewards() {
    assertEquals(1, EnemyKind.SWIFT_SLIME.soulReward());
    assertEquals(6, EnemyKind.SWIFT_SLIME.goldReward());
    assertEquals(2, EnemyKind.TOUGH_SLIME.soulReward());
    assertEquals(8, EnemyKind.TOUGH_SLIME.goldReward());
  }
}
