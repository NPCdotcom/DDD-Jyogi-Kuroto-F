package core.infrastructure.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.common.Position;
import core.domain.dungeon.DungeonMap;
import core.domain.dungeon.DungeonState;
import core.domain.dungeon.Tile;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Wave 11 W11-α: ダンジョン生成時に BREAKABLE_WALL が配置されることを Seed 固定で確認。 */
class InitialStateFactoryBreakableWallTest {

  private static int countBreakableWalls(DungeonMap map) {
    int count = 0;
    for (int y = 0; y < map.height(); y++) {
      for (int x = 0; x < map.width(); x++) {
        if (map.tileAt(new Position(x, y)) == Tile.BREAKABLE_WALL) {
          count++;
        }
      }
    }
    return count;
  }

  @Test
  void firstFloorContainsBreakableWalls() {
    DungeonState layer1 = InitialStateFactory.firstFloor(new Random(42));
    int count = countBreakableWalls(layer1.map());
    // 仕様: 1 層あたり 2 or 3 個 (placeBreakableWalls 内で rng.nextInt(2) で決定)
    assertTrue(count >= 2 && count <= 3, "1 層目に BREAKABLE_WALL が 2-3 個配置される、実測: " + count);
  }

  @Test
  void breakableWallsAreOnlyOnInternalTiles() {
    // 外枠 (周囲 1 マス) には BREAKABLE_WALL を置かない (DungeonMap.withTileAt の境界防御で IAE になる)。
    DungeonState layer1 = InitialStateFactory.firstFloor(new Random(42));
    DungeonMap map = layer1.map();
    for (int x = 0; x < map.width(); x++) {
      assertTrue(map.tileAt(new Position(x, 0)) != Tile.BREAKABLE_WALL, "下端枠は BREAKABLE_WALL 不在");
      assertTrue(
          map.tileAt(new Position(x, map.height() - 1)) != Tile.BREAKABLE_WALL,
          "上端枠は BREAKABLE_WALL 不在");
    }
    for (int y = 0; y < map.height(); y++) {
      assertTrue(map.tileAt(new Position(0, y)) != Tile.BREAKABLE_WALL, "左端枠は BREAKABLE_WALL 不在");
      assertTrue(
          map.tileAt(new Position(map.width() - 1, y)) != Tile.BREAKABLE_WALL,
          "右端枠は BREAKABLE_WALL 不在");
    }
  }
}
