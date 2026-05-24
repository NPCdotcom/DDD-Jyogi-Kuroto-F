package core.presentation.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import core.domain.card.CardElement;
import core.domain.common.Position;
import core.domain.dungeon.DungeonMap;
import core.domain.dungeon.DungeonState;
import core.domain.dungeon.PlacedTrap;
import core.domain.dungeon.Tile;
import core.domain.entity.Enemy;
import core.domain.entity.EnemyKind;
import java.util.Map;

/**
 * ダンジョン (マップ + アクター) を描画するユーティリティ。
 *
 * <p>State から SpriteBatch / ShapeRenderer への一方向描画のみ。状態を変更しない。
 *
 * <p>Wave 10 W10-β: drawPlayer / drawEnemies を ShapeRenderer 矩形 → SpriteBatch + Texture 描画に置換。
 *
 * <p>2026-05-24: 階段 / 罠も専用テクスチャに置換。描画は 3 フェーズ構造 ((1) マップテクスチャ batch (床/壁/階段) / (2) 境界線 shapes / (3)
 * 罠 + アクター batch) で構成。
 *
 * <p>ELITE_SLIME はスライム画像を {@link SpriteBatch#setColor} で赤くティントして描画する (テクスチャ加工不要)。 Stateful な
 * setColor は描画後に必ず {@link Color#WHITE} へリセットして「色リーク」を防ぐ (Wave 4 W4-ε 同型)。
 */
public final class DungeonRenderer {

  private static final Color ELITE_TINT = new Color(0.95f, 0.3f, 0.3f, 1f);

  /** Wave 11 W11-α: 壊れる壁の茶色ティント (既存 wall.png を流用、新素材なし)。 */
  private static final Color BREAKABLE_WALL_TINT = new Color(0.55f, 0.35f, 0.2f, 1f);

  private static final Color TRAP_PHYSICAL_COLOR = new Color(0.95f, 0.55f, 0.15f, 1f);
  private static final Color TRAP_MAGICAL_COLOR = new Color(0.70f, 0.35f, 0.95f, 1f);

  /** 壁と床の境界エッジに描く黒線の太さ (px、視認性向上 / チームメイト要望)。 */
  private static final float WALL_BORDER_WIDTH = 4f;

  /** 壁と床の境界エッジ色。 */
  private static final Color WALL_BORDER_COLOR = new Color(0f, 0f, 0f, 1f);

  private DungeonRenderer() {}

  /**
   * ダンジョン全体を描画する (Wave 10 W10-β: 3 フェーズ構造)。
   *
   * @param batch SpriteBatch (begin/end は本メソッド内で複数回切替)
   * @param shapes ShapeRenderer (begin/end も同様)
   * @param state 現在のダンジョン状態
   * @param wallTexture 壁テクスチャ
   * @param floorTexture 床テクスチャ
   * @param playerTexture プレイヤースプライト
   * @param enemyTextures EnemyKind → Texture マッピング (ELITE_SLIME 含む、SLIME 画像を流用する case は呼出側で同じ
   *     Texture を共有)
   */
  public static void draw(
      SpriteBatch batch,
      ShapeRenderer shapes,
      DungeonState state,
      Texture wallTexture,
      Texture floorTexture,
      Texture stairsTexture,
      Texture trapTexture,
      Texture playerTexture,
      Map<EnemyKind, Texture> enemyTextures) {
    // フェーズ 1: マップテクスチャ (batch、床 + 壁 + 階段)。
    batch.begin();
    drawMapTextures(batch, state.map(), wallTexture, floorTexture, stairsTexture);
    batch.end();

    // フェーズ 2: 境界線 (shapes)。
    shapes.begin(ShapeType.Filled);
    drawWallFloorBorders(shapes, state.map());
    shapes.end();

    // フェーズ 3: 罠 + アクター (batch + Texture)。
    batch.begin();
    drawTraps(batch, state, trapTexture);
    drawEnemies(batch, state, enemyTextures);
    drawPlayer(batch, state.player().position(), playerTexture);
    batch.end();
  }

  /**
   * マップタイルをテクスチャで描画する (チームメイト素材投入)。
   *
   * <p>壁は wall.png、床は floor.png、階段は stairs.png を 80×80 に引き伸ばして敷き詰める。
   */
  private static void drawMapTextures(
      SpriteBatch batch, DungeonMap map, Texture wallTex, Texture floorTex, Texture stairsTex) {
    int tile = RenderLayout.TILE_SIZE;
    for (int y = 0; y < map.height(); y++) {
      for (int x = 0; x < map.width(); x++) {
        Tile t = map.tileAt(new Position(x, y));
        Texture tex;
        if (t == Tile.WALL || t == Tile.BREAKABLE_WALL) {
          tex = wallTex;
        } else if (t == Tile.STAIRS_DOWN) {
          tex = stairsTex;
        } else {
          tex = floorTex;
        }
        int sx = RenderLayout.MAP_ORIGIN_X + x * tile;
        int sy = RenderLayout.MAP_ORIGIN_Y + y * tile;
        // Wave 11 W11-α: BREAKABLE_WALL は wall.png を茶色ティントで区別 (色変えハック、新素材なし)。
        // setColor の状態を必ず Color.WHITE にリセットして色リーク防止 (Wave 4 W4-ε 同型原則)。
        if (t == Tile.BREAKABLE_WALL) {
          batch.setColor(BREAKABLE_WALL_TINT);
          batch.draw(tex, sx, sy, tile, tile);
          batch.setColor(Color.WHITE);
        } else {
          batch.draw(tex, sx, sy, tile, tile);
        }
      }
    }
  }

  /**
   * 壁と床の境界エッジに黒線を描画する (チームメイト要望: 視認性向上)。
   *
   * <p>各 WALL タイルの 4 辺について、隣接タイルが WALL 以外 (FLOOR / STAIRS_DOWN) なら そのエッジに {@link
   * #WALL_BORDER_WIDTH} 太さの黒矩形を描く。壁同士の境界には線が 出ないので、ユーザーは壁の塊と床を即座に区別できる。
   *
   * <p>マップ端 (隣接タイルがマップ外) はスキップ。要望に応じて 1 行修正で「マップ外 = 床扱い」 に切り替え可能 (plan 参照)。
   */
  private static void drawWallFloorBorders(ShapeRenderer shapes, DungeonMap map) {
    shapes.setColor(WALL_BORDER_COLOR);
    int tile = RenderLayout.TILE_SIZE;
    for (int y = 0; y < map.height(); y++) {
      for (int x = 0; x < map.width(); x++) {
        if (!isWallLike(map.tileAt(new Position(x, y)))) {
          continue;
        }
        int sx = RenderLayout.MAP_ORIGIN_X + x * tile;
        int sy = RenderLayout.MAP_ORIGIN_Y + y * tile;
        // 下辺: 下隣が床
        if (y > 0 && !isWallLike(map.tileAt(new Position(x, y - 1)))) {
          shapes.rect(sx, sy, tile, WALL_BORDER_WIDTH);
        }
        // 上辺: 上隣が床
        if (y < map.height() - 1 && !isWallLike(map.tileAt(new Position(x, y + 1)))) {
          shapes.rect(sx, sy + tile - WALL_BORDER_WIDTH, tile, WALL_BORDER_WIDTH);
        }
        // 左辺: 左隣が床
        if (x > 0 && !isWallLike(map.tileAt(new Position(x - 1, y)))) {
          shapes.rect(sx, sy, WALL_BORDER_WIDTH, tile);
        }
        // 右辺: 右隣が床
        if (x < map.width() - 1 && !isWallLike(map.tileAt(new Position(x + 1, y)))) {
          shapes.rect(sx + tile - WALL_BORDER_WIDTH, sy, WALL_BORDER_WIDTH, tile);
        }
      }
    }
  }

  /** Wave 11 W11-α: 境界線描画上は WALL と BREAKABLE_WALL を同じ「壁」として扱う (床との境目だけ黒線)。 */
  private static boolean isWallLike(Tile t) {
    return t == Tile.WALL || t == Tile.BREAKABLE_WALL;
  }

  /**
   * 設置済みの罠を trap.png で描画する (§15-3、物理 = 橙ティント / 魔法 = 紫ティント)。
   *
   * <p>setColor の状態を必ず Color.WHITE にリセットして色リーク防止 (Wave 4 W4-ε 同型原則)。
   */
  private static void drawTraps(SpriteBatch batch, DungeonState state, Texture trapTexture) {
    int tile = RenderLayout.TILE_SIZE;
    for (PlacedTrap trap : state.placedTraps()) {
      Color tint =
          trap.element() == CardElement.PHYSICAL ? TRAP_PHYSICAL_COLOR : TRAP_MAGICAL_COLOR;
      int sx = RenderLayout.MAP_ORIGIN_X + trap.position().x() * tile;
      int sy = RenderLayout.MAP_ORIGIN_Y + trap.position().y() * tile;
      batch.setColor(tint);
      batch.draw(trapTexture, sx, sy, tile, tile);
      batch.setColor(Color.WHITE);
    }
  }

  /**
   * 敵を Texture で描画する (Wave 10 W10-β)。EnemyKind → Texture マッピング。
   *
   * <p>ELITE_SLIME は SLIME と同じスライム Texture を使い、{@link SpriteBatch#setColor} で赤くティント。 描画後に必ず {@link
   * Color#WHITE} へリセットする (色リーク防止、Wave 4 W4-ε と同型原則)。
   */
  private static void drawEnemies(
      SpriteBatch batch, DungeonState state, Map<EnemyKind, Texture> enemyTextures) {
    int tile = RenderLayout.TILE_SIZE;
    for (Enemy e : state.enemies()) {
      Texture tex = enemyTextures.get(e.kind());
      if (tex == null) {
        continue; // マッピング欠落は描画スキップ (起動時の存在チェックで普通は発生しない)
      }
      int sx = RenderLayout.MAP_ORIGIN_X + e.position().x() * tile;
      int sy = RenderLayout.MAP_ORIGIN_Y + e.position().y() * tile;
      if (e.kind() == EnemyKind.ELITE_SLIME) {
        batch.setColor(ELITE_TINT); // エリート赤ティント
        batch.draw(tex, sx, sy, tile, tile);
        batch.setColor(Color.WHITE); // ★ 必ずリセット (色リーク防止)
      } else {
        batch.draw(tex, sx, sy, tile, tile);
      }
    }
  }

  /** プレイヤースプライトを描画する (Wave 10 W10-β、Texture)。 */
  private static void drawPlayer(SpriteBatch batch, Position p, Texture playerTexture) {
    int tile = RenderLayout.TILE_SIZE;
    int sx = RenderLayout.MAP_ORIGIN_X + p.x() * tile;
    int sy = RenderLayout.MAP_ORIGIN_Y + p.y() * tile;
    batch.draw(playerTexture, sx, sy, tile, tile);
  }
}
