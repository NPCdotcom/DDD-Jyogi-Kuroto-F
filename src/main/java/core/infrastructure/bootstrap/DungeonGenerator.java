package core.infrastructure.bootstrap;

import core.domain.common.Position;
import core.domain.dungeon.DungeonMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * 手続き型ダンジョン部屋生成器 (§15-6)。10×10 の部屋を生成する。外周は壁、内部は床 + ランダムな 孤立ピラー。プレイヤー spawn は常に (1,1)。非ボス部屋には階段
 * {@code >} を 1 つ、ボス部屋には 階段を置かない (ボス撃破が唯一の進行手段)。
 *
 * <p><b>ソフトロックの構造的排除</b>: ピラー配置後に BFS ({@link DungeonMap#reachable}) で spawn
 * から階段・全敵への到達性を検証し、不到達なら最大 {@link #MAX_ATTEMPTS} 回まで再生成する。全試行 が失敗してもピラー 0 個の開けた部屋にフォールバックする —
 * 開けた部屋は spawn から全内部床が必ず 到達可能なため、生成失敗 (到達不能な階段 / 敵) は数学的に起こり得ない。
 *
 * <p>{@link Random} は引数注入 (ADR-19)。同一シード → 同一部屋。
 */
public final class DungeonGenerator {

  /** 部屋の一辺 (正方形)。10×10 固定で RenderLayout を変えずに済ませる。 */
  public static final int SIZE = 10;

  /** プレイヤー開始位置。常に内部の角 (1,1)。 */
  public static final Position SPAWN = new Position(1, 1);

  /** 階段を spawn からこの manhattan 距離以上離す (近すぎる即踏破を防ぐ)。 */
  private static final int MIN_STAIRS_DISTANCE = 6;

  /** 敵を spawn からこの manhattan 距離以上離す (開幕の即時被弾を防ぐ)。 */
  private static final int MIN_ENEMY_DISTANCE = 3;

  /** 非ボス部屋のピラー数の下限・上限。 */
  private static final int MIN_PILLARS = 3;

  private static final int MAX_PILLARS = 6;

  /** ピラー入り生成のリトライ上限。超過したらピラー 0 個にフォールバックする。 */
  private static final int MAX_ATTEMPTS = 20;

  private DungeonGenerator() {}

  /** 生成された 1 部屋。{@code map} に階段・壁・床が埋め込まれ、{@code enemySpawns} は敵を置く座標。 */
  public record GeneratedRoom(DungeonMap map, Position spawn, List<Position> enemySpawns) {
    public GeneratedRoom {
      Objects.requireNonNull(map, "map");
      Objects.requireNonNull(spawn, "spawn");
      enemySpawns = List.copyOf(enemySpawns);
    }
  }

  /**
   * 1 部屋を生成する。
   *
   * @param enemyCount 配置する敵の数 (&gt;= 0)
   * @param bossRoom true ならボス部屋 (階段なし・ピラーなしの開けたアリーナ)
   * @param rng 乱数源 (ADR-19: 引数注入)
   */
  public static GeneratedRoom generate(int enemyCount, boolean bossRoom, Random rng) {
    Objects.requireNonNull(rng, "rng");
    if (enemyCount < 0) {
      throw new IllegalArgumentException("enemyCount must be >= 0: " + enemyCount);
    }
    if (!bossRoom) {
      for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
        int pillars = MIN_PILLARS + rng.nextInt(MAX_PILLARS - MIN_PILLARS + 1);
        GeneratedRoom room = build(enemyCount, false, pillars, rng);
        if (room != null) {
          return room;
        }
      }
    }
    // ボス部屋、または非ボス部屋で全リトライ失敗 → ピラー 0 個の開けた部屋。
    // 開けた部屋は spawn から全内部床が BFS 到達可能 = build は必ず非 null を返す。
    GeneratedRoom open = build(enemyCount, bossRoom, 0, rng);
    return Objects.requireNonNull(open, "0-pillar open room must always pass reachability");
  }

  /**
   * 1 回ぶんの部屋構築を試みる。ピラー配置後の到達性検証に失敗したら {@code null} を返す (呼出側がリトライを判断する)。ピラー 0 個なら開けた部屋となり必ず非 null。
   */
  private static GeneratedRoom build(
      int enemyCount, boolean bossRoom, int pillarCount, Random rng) {
    // grid[y][x]: ゲーム座標 (y 上向き、(0,0) 左下)。外周壁・内部床で初期化。
    char[][] grid = new char[SIZE][SIZE];
    for (int y = 0; y < SIZE; y++) {
      for (int x = 0; x < SIZE; x++) {
        boolean border = x == 0 || y == 0 || x == SIZE - 1 || y == SIZE - 1;
        grid[y][x] = border ? '#' : '.';
      }
    }

    // 階段 (非ボス部屋のみ): spawn から MIN_STAIRS_DISTANCE 以上離れた内部床。
    Position stairs = bossRoom ? null : pickStairs(rng);
    if (stairs != null) {
      grid[stairs.y()][stairs.x()] = '>';
    }

    // ピラー: 内部床から pillarCount 個を孤立配置 (既存ピラーの 4 近傍・spawn / 階段の隣接は避ける)。
    placePillars(grid, stairs, pillarCount, rng);

    DungeonMap map = DungeonMap.of(toRows(grid));

    // 敵配置: ピラー確定後の内部床から、spawn 近接を避けた候補を列挙・シャッフルして先頭 N 個。
    List<Position> enemies = pickEnemies(grid, enemyCount, rng);

    // 到達性検証: spawn → 階段、spawn → 各敵。1 つでも不到達なら null (リトライへ)。
    if (stairs != null && !map.reachable(SPAWN, stairs)) {
      return null;
    }
    for (Position e : enemies) {
      if (!map.reachable(SPAWN, e)) {
        return null;
      }
    }
    return new GeneratedRoom(map, SPAWN, enemies);
  }

  /** spawn から MIN_STAIRS_DISTANCE 以上離れた内部タイルを選ぶ。 */
  private static Position pickStairs(Random rng) {
    for (int i = 0; i < 500; i++) {
      Position p = randomInterior(rng);
      if (SPAWN.manhattanDistanceTo(p) >= MIN_STAIRS_DISTANCE) {
        return p;
      }
    }
    // フォールバック (8,8): spawn から manhattan 14 ≥ MIN_STAIRS_DISTANCE。
    return new Position(SIZE - 2, SIZE - 2);
  }

  /**
   * ピラーを配置する。候補 (内部床・spawn / 階段から距離 2 以上) を列挙・シャッフルし、既存ピラーの 4 近傍に重ならないよう貪欲に最大 {@code pillarCount}
   * 個まで置く。
   */
  private static void placePillars(char[][] grid, Position stairs, int pillarCount, Random rng) {
    if (pillarCount <= 0) {
      return;
    }
    List<Position> candidates = new ArrayList<>();
    for (int y = 1; y < SIZE - 1; y++) {
      for (int x = 1; x < SIZE - 1; x++) {
        Position p = new Position(x, y);
        boolean nearStairs = stairs != null && stairs.manhattanDistanceTo(p) < 2;
        if (isFloor(grid, p) && SPAWN.manhattanDistanceTo(p) >= 2 && !nearStairs) {
          candidates.add(p);
        }
      }
    }
    Collections.shuffle(candidates, rng);
    List<Position> placed = new ArrayList<>();
    for (Position p : candidates) {
      if (placed.size() >= pillarCount) {
        break;
      }
      if (!adjacentToAny(p, placed)) {
        grid[p.y()][p.x()] = '#';
        placed.add(p);
      }
    }
  }

  /** 敵配置候補 (内部床・spawn から距離 MIN_ENEMY_DISTANCE 以上) を列挙・シャッフルし先頭 N 個。 */
  private static List<Position> pickEnemies(char[][] grid, int enemyCount, Random rng) {
    List<Position> candidates = new ArrayList<>();
    for (int y = 1; y < SIZE - 1; y++) {
      for (int x = 1; x < SIZE - 1; x++) {
        Position p = new Position(x, y);
        if (isFloor(grid, p) && SPAWN.manhattanDistanceTo(p) >= MIN_ENEMY_DISTANCE) {
          candidates.add(p);
        }
      }
    }
    Collections.shuffle(candidates, rng);
    return new ArrayList<>(candidates.subList(0, Math.min(enemyCount, candidates.size())));
  }

  private static Position randomInterior(Random rng) {
    return new Position(1 + rng.nextInt(SIZE - 2), 1 + rng.nextInt(SIZE - 2));
  }

  /** {@code grid} の指定座標が床 ('.') か。階段 ('&gt;') や壁 ('#') は false。 */
  private static boolean isFloor(char[][] grid, Position p) {
    return grid[p.y()][p.x()] == '.';
  }

  private static boolean adjacentToAny(Position p, List<Position> others) {
    for (Position o : others) {
      if (o.manhattanDistanceTo(p) == 1) {
        return true;
      }
    }
    return false;
  }

  /** grid[y][x] (y 上向き) を DungeonMap.of 用の行リスト (上 = 先頭) に変換する。 */
  private static List<String> toRows(char[][] grid) {
    List<String> rows = new ArrayList<>(SIZE);
    for (int y = SIZE - 1; y >= 0; y--) {
      rows.add(new String(grid[y]));
    }
    return rows;
  }
}
