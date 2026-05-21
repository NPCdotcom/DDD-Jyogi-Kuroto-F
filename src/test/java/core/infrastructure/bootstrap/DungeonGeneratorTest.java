package core.infrastructure.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.common.Position;
import core.domain.dungeon.DungeonMap;
import core.domain.dungeon.Tile;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** §15-6 手続き型ダンジョン生成の検証。最重要は「全シードで spawn → 階段 / 敵が到達可能」= ソフトロックが構造的に起こり得ないことの実証。 */
class DungeonGeneratorTest {

  /** マップ全走査で階段タイルを探す。無ければ null (ボス部屋)。 */
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

  @Test
  void spawnIsAlwaysWalkableForManySeeds() {
    for (int seed = 0; seed < 30; seed++) {
      DungeonGenerator.GeneratedRoom room = DungeonGenerator.generate(2, false, new Random(seed));
      assertEquals(DungeonGenerator.SPAWN, room.spawn(), "spawn は常に (1,1)");
      assertTrue(room.map().isWalkable(room.spawn()), "seed " + seed + ": spawn は歩行可能でなければならない");
    }
  }

  @Test
  void nonBossRoomStairsAreReachableFromSpawn() {
    // §15-6 ソフトロック排除の核心: 全シードで spawn → 階段が到達可能。
    for (int seed = 0; seed < 30; seed++) {
      DungeonGenerator.GeneratedRoom room = DungeonGenerator.generate(3, false, new Random(seed));
      Position stairs = findStairs(room.map());
      assertNotNull(stairs, "seed " + seed + ": 非ボス部屋には階段がある");
      assertTrue(
          room.map().reachable(room.spawn(), stairs),
          "seed " + seed + ": spawn → 階段が到達可能でなければならない");
    }
  }

  @Test
  void allEnemySpawnsAreReachableFromSpawn() {
    for (int seed = 0; seed < 30; seed++) {
      DungeonGenerator.GeneratedRoom room = DungeonGenerator.generate(3, false, new Random(seed));
      for (Position e : room.enemySpawns()) {
        assertTrue(
            room.map().reachable(room.spawn(), e),
            "seed " + seed + ": spawn → 敵 " + e + " が到達可能でなければならない");
      }
    }
  }

  @Test
  void bossRoomHasNoStairs() {
    // ボス部屋に階段は無い (ボス撃破が唯一の進行手段)。
    for (int seed = 0; seed < 20; seed++) {
      DungeonGenerator.GeneratedRoom room = DungeonGenerator.generate(1, true, new Random(seed));
      assertNull(findStairs(room.map()), "seed " + seed + ": ボス部屋に階段は無い");
    }
  }

  @Test
  void bossRoomEnemyIsReachable() {
    // ボス部屋は開けたアリーナ、ボスは spawn から到達可能。
    for (int seed = 0; seed < 20; seed++) {
      DungeonGenerator.GeneratedRoom room = DungeonGenerator.generate(1, true, new Random(seed));
      assertEquals(1, room.enemySpawns().size(), "ボス部屋の敵は 1 体");
      assertTrue(room.map().reachable(room.spawn(), room.enemySpawns().get(0)));
    }
  }

  @Test
  void requestedEnemyCountIsPlaced() {
    DungeonGenerator.GeneratedRoom room = DungeonGenerator.generate(3, false, new Random(7));
    assertEquals(3, room.enemySpawns().size(), "要求した敵数ぶん配置される");
  }

  @Test
  void enemySpawnsAreWalkableDistinctAndNotOnSpawn() {
    DungeonGenerator.GeneratedRoom room = DungeonGenerator.generate(3, false, new Random(7));
    for (Position e : room.enemySpawns()) {
      assertTrue(room.map().isWalkable(e), "敵座標は歩行可能");
      assertFalse(e.equals(room.spawn()), "敵は spawn 上に置かれない");
    }
    assertEquals(
        room.enemySpawns().size(), room.enemySpawns().stream().distinct().count(), "敵座標は互いに重複しない");
  }

  @Test
  void borderIsAllWalls() {
    DungeonGenerator.GeneratedRoom room = DungeonGenerator.generate(2, false, new Random(1));
    DungeonMap map = room.map();
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
  void sameSeedProducesSameRoom() {
    // 決定性: 同一シード → 同一マップ + 同一敵配置。
    DungeonGenerator.GeneratedRoom a = DungeonGenerator.generate(3, false, new Random(99));
    DungeonGenerator.GeneratedRoom b = DungeonGenerator.generate(3, false, new Random(99));
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
    DungeonGenerator.GeneratedRoom room = DungeonGenerator.generate(0, false, new Random(3));
    assertTrue(room.enemySpawns().isEmpty());
  }
}
