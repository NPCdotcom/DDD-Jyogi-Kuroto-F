package core.infrastructure.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.common.Position;
import core.domain.dungeon.DungeonMap;
import core.domain.dungeon.Tile;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** §15-6 BSP ダンジョン生成の検証。最重要は「全シードで spawn → 階段 / 全敵が到達可能」= ソフトロックが構造的に起こり得ないことの実証。 */
class DungeonGeneratorTest {

  /** マップ全走査で階段タイルを探す。無ければ null (ボス層)。 */
  private static Position findStairs(DungeonMap map) {
    for (int y = 0; y < map.height(); y++) {
      for (int x = 0; x < map.width(); x++) {
        if (map.tileAt(new Position(x, y)) == Tile.STAIRS_DOWN) {
          return new Position(x, y);
        }
      }
    }
    return null;
  }

  /** 床 (FLOOR + STAIRS_DOWN = 歩行可能タイル) の数を数える。 */
  private static int countFloor(DungeonMap map) {
    int count = 0;
    for (int y = 0; y < map.height(); y++) {
      for (int x = 0; x < map.width(); x++) {
        if (map.isWalkable(new Position(x, y))) {
          count++;
        }
      }
    }
    return count;
  }

  @Test
  void spawnIsAlwaysWalkableForManySeeds() {
    for (int seed = 0; seed < 30; seed++) {
      DungeonGenerator.GeneratedDungeon d =
          DungeonGenerator.generate(14, 12, 3, true, new Random(seed));
      assertTrue(d.map().isWalkable(d.spawn()), "seed " + seed + ": spawn は歩行可能でなければならない");
    }
  }

  @Test
  void nonBossLayerStairsAreReachableFromSpawn() {
    // §15-6 ソフトロック排除の核心: 全シードで spawn → 階段が到達可能。
    for (int seed = 0; seed < 30; seed++) {
      DungeonGenerator.GeneratedDungeon d =
          DungeonGenerator.generate(19, 15, 5, true, new Random(seed));
      Position stairs = findStairs(d.map());
      assertNotNull(stairs, "seed " + seed + ": 非ボス層には階段がある");
      assertTrue(
          d.map().reachable(d.spawn(), stairs), "seed " + seed + ": spawn → 階段が到達可能でなければならない");
    }
  }

  @Test
  void allEnemySpawnsAreReachableFromSpawn() {
    for (int seed = 0; seed < 30; seed++) {
      DungeonGenerator.GeneratedDungeon d =
          DungeonGenerator.generate(19, 15, 6, true, new Random(seed));
      for (Position e : d.enemySpawns()) {
        assertTrue(
            d.map().reachable(d.spawn(), e),
            "seed " + seed + ": spawn → 敵 " + e + " が到達可能でなければならない");
      }
    }
  }

  @Test
  void bossLayerHasNoStairs() {
    // ボス層に階段は無い (ボス撃破が唯一の進行手段)。
    for (int seed = 0; seed < 20; seed++) {
      DungeonGenerator.GeneratedDungeon d =
          DungeonGenerator.generate(26, 20, 8, false, new Random(seed));
      assertNull(findStairs(d.map()), "seed " + seed + ": ボス層に階段は無い");
    }
  }

  @Test
  void bossLayerFirstEnemyIsReachable() {
    // ボス層: 先頭の敵 (= spawn から最遠の部屋中心、ボス配置位置) が到達可能。
    for (int seed = 0; seed < 20; seed++) {
      DungeonGenerator.GeneratedDungeon d =
          DungeonGenerator.generate(26, 20, 8, false, new Random(seed));
      assertFalse(d.enemySpawns().isEmpty(), "seed " + seed + ": ボス層に敵が配置される");
      assertTrue(d.map().reachable(d.spawn(), d.enemySpawns().get(0)));
    }
  }

  @Test
  void requestedEnemyCountIsPlaced() {
    DungeonGenerator.GeneratedDungeon d = DungeonGenerator.generate(19, 15, 6, true, new Random(7));
    assertEquals(6, d.enemySpawns().size(), "要求した敵数ぶん配置される");
  }

  @Test
  void enemySpawnsAreWalkableDistinctAndNotOnSpawn() {
    DungeonGenerator.GeneratedDungeon d = DungeonGenerator.generate(19, 15, 6, true, new Random(7));
    for (Position e : d.enemySpawns()) {
      assertTrue(d.map().isWalkable(e), "敵座標は歩行可能");
      assertFalse(e.equals(d.spawn()), "敵は spawn 上に置かれない");
    }
    assertEquals(
        d.enemySpawns().size(), d.enemySpawns().stream().distinct().count(), "敵座標は互いに重複しない");
  }

  @Test
  void borderIsAllWalls() {
    DungeonGenerator.GeneratedDungeon d = DungeonGenerator.generate(19, 15, 5, true, new Random(1));
    DungeonMap map = d.map();
    for (int x = 0; x < map.width(); x++) {
      assertEquals(Tile.WALL, map.tileAt(new Position(x, 0)), "下辺は壁");
      assertEquals(Tile.WALL, map.tileAt(new Position(x, map.height() - 1)), "上辺は壁");
    }
    for (int y = 0; y < map.height(); y++) {
      assertEquals(Tile.WALL, map.tileAt(new Position(0, y)), "左辺は壁");
      assertEquals(Tile.WALL, map.tileAt(new Position(map.width() - 1, y)), "右辺は壁");
    }
  }

  @Test
  void floorAreaGrowsWithGridSize() {
    // §15-6: 層が深いほど (グリッドが大きいほど) 床面積が拡大する。
    int small = countFloor(DungeonGenerator.generate(14, 12, 3, true, new Random(1)).map());
    int large = countFloor(DungeonGenerator.generate(26, 20, 8, true, new Random(1)).map());
    assertTrue(large > small, "大きいグリッドの方が床面積が大きい (small=" + small + " large=" + large + ")");
  }

  @Test
  void sameSeedProducesSameDungeon() {
    // 決定性: 同一シード + 同一引数 → 同一マップ + 同一敵配置。
    DungeonGenerator.GeneratedDungeon a =
        DungeonGenerator.generate(19, 15, 5, true, new Random(99));
    DungeonGenerator.GeneratedDungeon b =
        DungeonGenerator.generate(19, 15, 5, true, new Random(99));
    assertEquals(a.spawn(), b.spawn(), "同シードは同じ spawn");
    assertEquals(a.enemySpawns(), b.enemySpawns(), "同シードは同じ敵配置");
    for (int y = 0; y < a.map().height(); y++) {
      for (int x = 0; x < a.map().width(); x++) {
        Position p = new Position(x, y);
        assertEquals(a.map().tileAt(p), b.map().tileAt(p), "同シードは同じマップ " + p);
      }
    }
  }

  @Test
  void zeroEnemyCountProducesEmptyEnemyList() {
    DungeonGenerator.GeneratedDungeon d = DungeonGenerator.generate(14, 12, 0, true, new Random(3));
    assertTrue(d.enemySpawns().isEmpty());
  }

  @Test
  void gridTooSmallIsRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DungeonGenerator.generate(11, 12, 0, true, new Random(0)));
    assertThrows(
        IllegalArgumentException.class,
        () -> DungeonGenerator.generate(12, 11, 0, true, new Random(0)));
  }

  @Test
  void negativeEnemyCountIsRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DungeonGenerator.generate(14, 12, -1, true, new Random(0)));
  }

  // Phase 1 回帰: pickEnemies フォールバック (MIN_ENEMY_DISTANCE 緩和による再収集)

  @Test
  void minimumGridFulfillsEnemyCountAcrossSeeds() {
    // §15-6 フォールバック: 最小グリッド (12x12) でも要求敵数が必ず配置されることを多シードで確認。
    // 通常候補 (MIN_ENEMY_DISTANCE 制約あり) で不足する場合、フォールバックが距離制約を緩和して
    // 補充するため、小さいマップでも要求数を満たせる。
    for (int seed = 0; seed < 20; seed++) {
      int requested = 6;
      DungeonGenerator.GeneratedDungeon d =
          DungeonGenerator.generate(12, 12, requested, true, new Random(seed));
      assertEquals(
          requested,
          d.enemySpawns().size(),
          "seed " + seed + ": 最小グリッドでも要求数 " + requested + " 体が配置される (フォールバック含む)");
    }
  }

  @Test
  void minimumGridEnemySpawnsAreReachableFromSpawn() {
    // フォールバックで配置された敵も spawn から到達可能であることを検証 (距離緩和後も到達性を壊さない)
    for (int seed = 0; seed < 20; seed++) {
      DungeonGenerator.GeneratedDungeon d =
          DungeonGenerator.generate(12, 12, 4, true, new Random(seed));
      for (Position e : d.enemySpawns()) {
        assertTrue(
            d.map().reachable(d.spawn(), e),
            "seed " + seed + ": フォールバック配置の敵 " + e + " も spawn から到達可能");
      }
    }
  }
}
