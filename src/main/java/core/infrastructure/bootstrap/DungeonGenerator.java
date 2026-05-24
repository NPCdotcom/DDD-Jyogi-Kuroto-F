package core.infrastructure.bootstrap;

import com.badlogic.gdx.Gdx;
import core.domain.common.Position;
import core.domain.dungeon.DungeonMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * BSP (Binary Space Partitioning) 方式の手続き型ダンジョン生成器 (§15-6)。1 層 = 1 枚の連続マップを生成する。
 *
 * <p>グリッドを再帰二分割し、各葉領域にサイズ可変の長方形部屋を carve、部屋リスト上で連続する部屋同士を L 字廊下で全て繋ぐ。これによりダンジョン全体が 1
 * 本のパスで連結し、ソフトロックが構造的に起こり得ない。spawn は先頭部屋の中心、階段 (非ボス層) は spawn から最遠の部屋の中心に置く。
 *
 * <p><b>到達性の保証</b>: 連結保証に加え、生成後に BFS ({@link DungeonMap#reachable}) で spawn から
 * 階段・全敵への到達性を検証し、万一失敗したら最大 {@link #MAX_ATTEMPTS} 回まで再生成、全失敗時は ピラーなしの開けた 1 部屋にフォールバックする。
 *
 * <p>{@link Random} は引数注入 (ADR-19)。同一シード・同一引数 → 同一マップ。
 */
public final class DungeonGenerator {

  /** Player 構築時の暫定 spawn (実際の spawn は {@link #generate} が返す部屋中心で上書きされる)。 */
  public static final Position SPAWN = new Position(1, 1);

  /** BSP 領域を分割可能とする最小辺長。領域は一辺が {@code 2*MIN_REGION} 以上のとき分割できる。 */
  private static final int MIN_REGION = 6;

  /** 部屋の最小辺長。 */
  private static final int MIN_ROOM = 3;

  /** BSP 再帰の深さ上限 (安全弁、実際は MIN_REGION で先に止まる)。 */
  private static final int MAX_DEPTH = 4;

  /** 敵を spawn からこの manhattan 距離以上離す (開幕の即時被弾を防ぐ)。 */
  private static final int MIN_ENEMY_DISTANCE = 3;

  /** 再生成のリトライ上限。超過したら開けた 1 部屋にフォールバックする。 */
  private static final int MAX_ATTEMPTS = 20;

  private DungeonGenerator() {}

  /** 生成された 1 層。{@code map} に階段・壁・床が埋め込まれ、{@code enemySpawns} は敵を置く座標。 */
  public record GeneratedDungeon(DungeonMap map, Position spawn, List<Position> enemySpawns) {
    public GeneratedDungeon {
      Objects.requireNonNull(map, "map");
      Objects.requireNonNull(spawn, "spawn");
      enemySpawns = List.copyOf(enemySpawns);
    }
  }

  /**
   * 1 層を生成する。
   *
   * @param gridW グリッド幅 (&gt;= 12)
   * @param gridH グリッド高さ (&gt;= 12)
   * @param enemyCount 配置する敵の数 (&gt;= 0)。{@code placeStairs} が false (ボス層) のとき先頭 1 体ぶんの座標は spawn
   *     から最遠の部屋中心になる (ボス配置用)。
   * @param placeStairs true なら次層への階段を 1 つ置く (層 1・2)。false ならボス層 (階段なし)。
   * @param rng 乱数源 (ADR-19: 引数注入)
   */
  public static GeneratedDungeon generate(
      int gridW, int gridH, int enemyCount, boolean placeStairs, Random rng) {
    Objects.requireNonNull(rng, "rng");
    if (gridW < 12 || gridH < 12) {
      throw new IllegalArgumentException("grid must be >= 12x12: " + gridW + "x" + gridH);
    }
    if (enemyCount < 0) {
      throw new IllegalArgumentException("enemyCount must be >= 0: " + enemyCount);
    }
    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      try {
        GeneratedDungeon d = tryBuild(gridW, gridH, enemyCount, placeStairs, rng);
        if (d != null) {
          return d;
        }
      } catch (IllegalArgumentException ex) {
        // §UI 改善 (devils-advocate 検出): 深い分割 + 細い領域で rng.nextInt(0) 等の IAE が
        // tryBuild 内で発生し得るため、attempt 単位で吸収して次の試行に進む。最終的に
        // 全 attempt 失敗なら buildFallback (1 部屋) に逃げて起動クラッシュを回避する。
        if (Gdx.app != null) {
          Gdx.app.log("DungeonGenerator", "tryBuild attempt failed: " + ex.getMessage());
        }
      }
    }
    return buildFallback(gridW, gridH, enemyCount, placeStairs, rng);
  }

  /** BSP で 1 回ぶんの生成を試みる。到達性検証に失敗したら {@code null} (呼出側がリトライ判断)。 */
  private static GeneratedDungeon tryBuild(
      int gridW, int gridH, int enemyCount, boolean placeStairs, Random rng) {
    char[][] grid = filledGrid(gridW, gridH, '#');
    List<int[]> rooms = new ArrayList<>();
    split(grid, 0, 0, gridW, gridH, 0, rooms, rng);
    if (rooms.size() < 2) {
      return null; // 分割できず 1 部屋 → リトライ (BSP の意味がない)
    }
    // 部屋リスト上で連続する部屋を L 字廊下で接続 → 全部屋が 1 本のパスで連結。
    for (int i = 0; i + 1 < rooms.size(); i++) {
      carveCorridor(grid, center(rooms.get(i)), center(rooms.get(i + 1)), rng);
    }
    Position spawn = center(rooms.get(0));
    Position far = center(rooms.get(farthestRoomIndex(rooms, spawn)));
    if (placeStairs) {
      grid[far.y()][far.x()] = '>';
    }
    DungeonMap map = DungeonMap.of(toRows(grid, gridH));
    List<Position> enemies =
        pickEnemies(grid, gridW, gridH, spawn, far, enemyCount, placeStairs, rng);
    if (placeStairs && !map.reachable(spawn, far)) {
      return null;
    }
    for (Position e : enemies) {
      if (!map.reachable(spawn, e)) {
        return null;
      }
    }
    return new GeneratedDungeon(map, spawn, enemies);
  }

  /** 全リトライ失敗時の保険: ピラーなしの開けた 1 部屋 (全床が spawn から必ず到達可能)。 */
  private static GeneratedDungeon buildFallback(
      int gridW, int gridH, int enemyCount, boolean placeStairs, Random rng) {
    char[][] grid = new char[gridH][gridW];
    for (int y = 0; y < gridH; y++) {
      for (int x = 0; x < gridW; x++) {
        boolean border = x == 0 || y == 0 || x == gridW - 1 || y == gridH - 1;
        grid[y][x] = border ? '#' : '.';
      }
    }
    Position spawn = new Position(1, 1);
    Position far = new Position(gridW - 2, gridH - 2);
    if (placeStairs) {
      grid[far.y()][far.x()] = '>';
    }
    DungeonMap map = DungeonMap.of(toRows(grid, gridH));
    List<Position> enemies =
        pickEnemies(grid, gridW, gridH, spawn, far, enemyCount, placeStairs, rng);
    return new GeneratedDungeon(map, spawn, enemies);
  }

  /** BSP 再帰分割。葉領域に部屋を carve し {@code rooms} に追加する。 */
  private static void split(
      char[][] grid, int x, int y, int w, int h, int depth, List<int[]> rooms, Random rng) {
    boolean canX = w >= 2 * MIN_REGION;
    boolean canY = h >= 2 * MIN_REGION;
    if (depth >= MAX_DEPTH || (!canX && !canY)) {
      rooms.add(carveRoom(grid, x, y, w, h, rng));
      return;
    }
    boolean splitX;
    if (canX && canY) {
      // 長い軸を優先して分割し、極端に細長い部屋を避ける。
      if (w > h * 5 / 4) {
        splitX = true;
      } else if (h > w * 5 / 4) {
        splitX = false;
      } else {
        splitX = rng.nextBoolean();
      }
    } else {
      splitX = canX;
    }
    if (splitX) {
      int cut = MIN_REGION + rng.nextInt(w - 2 * MIN_REGION + 1);
      split(grid, x, y, cut, h, depth + 1, rooms, rng);
      split(grid, x + cut, y, w - cut, h, depth + 1, rooms, rng);
    } else {
      int cut = MIN_REGION + rng.nextInt(h - 2 * MIN_REGION + 1);
      split(grid, x, y, w, cut, depth + 1, rooms, rng);
      split(grid, x, y + cut, w, h - cut, depth + 1, rooms, rng);
    }
  }

  /** 葉領域 (x,y,w,h) 内にサイズ・位置をランダム化した長方形部屋を carve し、その矩形 {x,y,w,h} を返す。 */
  private static int[] carveRoom(char[][] grid, int x, int y, int w, int h, Random rng) {
    // 領域の各辺から 1 マス以上の余白を残す (= 隣接領域の部屋と最低 2 マスの壁で隔てる)。
    int maxRoomW = w - 2;
    int maxRoomH = h - 2;
    int roomW = MIN_ROOM + rng.nextInt(maxRoomW - MIN_ROOM + 1);
    int roomH = MIN_ROOM + rng.nextInt(maxRoomH - MIN_ROOM + 1);
    int roomX = x + 1 + rng.nextInt(w - roomW - 1);
    int roomY = y + 1 + rng.nextInt(h - roomH - 1);
    for (int yy = roomY; yy < roomY + roomH; yy++) {
      for (int xx = roomX; xx < roomX + roomW; xx++) {
        grid[yy][xx] = '.';
      }
    }
    return new int[] {roomX, roomY, roomW, roomH};
  }

  /** 部屋矩形 {x,y,w,h} の中心座標。 */
  private static Position center(int[] room) {
    return new Position(room[0] + room[2] / 2, room[1] + room[3] / 2);
  }

  private static int farthestRoomIndex(List<int[]> rooms, Position spawn) {
    int bestIdx = 0;
    int bestDist = -1;
    for (int i = 0; i < rooms.size(); i++) {
      int d = spawn.manhattanDistanceTo(center(rooms.get(i)));
      if (d > bestDist) {
        bestDist = d;
        bestIdx = i;
      }
    }
    return bestIdx;
  }

  /** 2 点を L 字 (横→縦 または 縦→横) の 1 マス幅廊下で接続する。壁 ('#') のみ床に変える。 */
  private static void carveCorridor(char[][] grid, Position a, Position b, Random rng) {
    if (rng.nextBoolean()) {
      carveH(grid, a.x(), b.x(), a.y());
      carveV(grid, a.y(), b.y(), b.x());
    } else {
      carveV(grid, a.y(), b.y(), a.x());
      carveH(grid, a.x(), b.x(), b.y());
    }
  }

  private static void carveH(char[][] grid, int xa, int xb, int y) {
    for (int x = Math.min(xa, xb); x <= Math.max(xa, xb); x++) {
      if (grid[y][x] == '#') {
        grid[y][x] = '.';
      }
    }
  }

  private static void carveV(char[][] grid, int ya, int yb, int x) {
    for (int y = Math.min(ya, yb); y <= Math.max(ya, yb); y++) {
      if (grid[y][x] == '#') {
        grid[y][x] = '.';
      }
    }
  }

  /**
   * 敵出現座標を選ぶ。床タイルから spawn 近接 ({@link #MIN_ENEMY_DISTANCE} 未満) と最遠点を除外、シャッフルして 先頭 N 個。ボス層 ({@code
   * !placeStairs}) では先頭を最遠部屋中心に固定する (ボスを spawn から遠ざける)。
   *
   * <p>フォールバック: 候補不足で要求数に満たない場合は {@link #MIN_ENEMY_DISTANCE} 制約を緩和 (距離 1 以上まで広げる) して再収集する。 それでも
   * 不足する場合は {@code Gdx.app.log} で警告を出し、見つかった分だけ返す。
   */
  private static List<Position> pickEnemies(
      char[][] grid,
      int gridW,
      int gridH,
      Position spawn,
      Position far,
      int enemyCount,
      boolean placeStairs,
      Random rng) {
    List<Position> result = new ArrayList<>();
    if (enemyCount <= 0) {
      return result;
    }
    if (!placeStairs) {
      result.add(far); // ボス層: 先頭 = 最遠部屋中心 (ボス配置用)
    }
    // 通常収集 (MIN_ENEMY_DISTANCE 制約あり)
    List<Position> candidates = new ArrayList<>();
    for (int y = 0; y < gridH; y++) {
      for (int x = 0; x < gridW; x++) {
        if (grid[y][x] != '.') {
          continue;
        }
        Position p = new Position(x, y);
        if (spawn.manhattanDistanceTo(p) >= MIN_ENEMY_DISTANCE && !p.equals(far)) {
          candidates.add(p);
        }
      }
    }
    Collections.shuffle(candidates, rng);
    for (Position c : candidates) {
      if (result.size() >= enemyCount) {
        break;
      }
      result.add(c);
    }
    // フォールバック: 不足分を距離制約なしで補充 (spawn と far 自体は除外)
    if (result.size() < enemyCount) {
      List<Position> fallback = new ArrayList<>();
      for (int y = 0; y < gridH; y++) {
        for (int x = 0; x < gridW; x++) {
          if (grid[y][x] != '.') {
            continue;
          }
          Position p = new Position(x, y);
          if (!p.equals(spawn) && !p.equals(far) && !result.contains(p)) {
            fallback.add(p);
          }
        }
      }
      Collections.shuffle(fallback, rng);
      for (Position c : fallback) {
        if (result.size() >= enemyCount) {
          break;
        }
        result.add(c);
      }
    }
    if (result.size() < enemyCount) {
      Gdx.app.log(
          "DungeonGenerator",
          "pickEnemies: requested "
              + enemyCount
              + " but only "
              + result.size()
              + " positions found (map may be too small)");
    }
    return result;
  }

  private static char[][] filledGrid(int gridW, int gridH, char fill) {
    char[][] grid = new char[gridH][gridW];
    for (int y = 0; y < gridH; y++) {
      for (int x = 0; x < gridW; x++) {
        grid[y][x] = fill;
      }
    }
    return grid;
  }

  /** grid[y][x] (y 上向き) を DungeonMap.of 用の行リスト (上 = 先頭) に変換する。 */
  private static List<String> toRows(char[][] grid, int gridH) {
    List<String> rows = new ArrayList<>(gridH);
    for (int y = gridH - 1; y >= 0; y--) {
      rows.add(new String(grid[y]));
    }
    return rows;
  }
}
