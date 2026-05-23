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

/**
 * ダンジョン (マップ + アクター) を {@link ShapeRenderer} で描画するユーティリティ。
 *
 * <p>State から ShapeRenderer への一方向描画のみ。状態を変更しない。
 */
public final class DungeonRenderer {

  private static final Color STAIRS_COLOR = new Color(0.85f, 0.75f, 0.25f, 1f);
  private static final Color PLAYER_COLOR = new Color(0.30f, 0.65f, 1.00f, 1f);
  private static final Color ENEMY_COLOR = new Color(0.40f, 0.85f, 0.40f, 1f);
  private static final Color SWIFT_COLOR = new Color(0.40f, 0.85f, 0.80f, 1f);
  private static final Color TOUGH_COLOR = new Color(0.45f, 0.55f, 0.62f, 1f);
  private static final Color ELITE_COLOR = new Color(0.85f, 0.55f, 0.20f, 1f);
  private static final Color BOSS_COLOR = new Color(0.85f, 0.25f, 0.30f, 1f);
  private static final Color TRAP_PHYSICAL_COLOR = new Color(0.95f, 0.55f, 0.15f, 1f);
  private static final Color TRAP_MAGICAL_COLOR = new Color(0.70f, 0.35f, 0.95f, 1f);

  /** 壁と床の境界エッジに描く黒線の太さ (px、視認性向上 / チームメイト要望)。 */
  private static final float WALL_BORDER_WIDTH = 4f;

  /** 壁と床の境界エッジ色。 */
  private static final Color WALL_BORDER_COLOR = new Color(0f, 0f, 0f, 1f);

  private DungeonRenderer() {}

  public static void draw(
      SpriteBatch batch,
      ShapeRenderer shapes,
      DungeonState state,
      Texture wallTexture,
      Texture floorTexture) {
    // 1. テクスチャフェーズ (床 + 壁、SpriteBatch のみ)。階段も床テクスチャで描き、識別マーカーは
    //    シェイプフェーズで重ねる。
    batch.begin();
    drawMapTextures(batch, state.map(), wallTexture, floorTexture);
    batch.end();

    // 2. シェイプフェーズ (境界線 + 階段マーカー + アクター、ShapeRenderer のみ)。
    shapes.begin(ShapeType.Filled);
    drawWallFloorBorders(shapes, state.map());
    drawStairsMarker(shapes, state.map());
    drawTraps(shapes, state);
    drawEnemies(shapes, state);
    drawPlayer(shapes, state.player().position());
    shapes.end();
  }

  /**
   * マップタイルをテクスチャで描画する (チームメイト素材投入)。
   *
   * <p>壁は wall.png、床と階段は floor.png を 80×80 に引き伸ばして敷き詰める。階段の識別マーカーは {@link #drawStairsMarker}
   * がシェイプフェーズで重ねる。
   */
  private static void drawMapTextures(
      SpriteBatch batch, DungeonMap map, Texture wallTex, Texture floorTex) {
    int tile = RenderLayout.TILE_SIZE;
    for (int y = 0; y < map.height(); y++) {
      for (int x = 0; x < map.width(); x++) {
        Tile t = map.tileAt(new Position(x, y));
        Texture tex = (t == Tile.WALL) ? wallTex : floorTex;
        int sx = RenderLayout.MAP_ORIGIN_X + x * tile;
        int sy = RenderLayout.MAP_ORIGIN_Y + y * tile;
        batch.draw(tex, sx, sy, tile, tile);
      }
    }
  }

  /**
   * 階段タイルの識別マーカーを床テクスチャの上に描画する (黄色矩形、タイルの中央 1/2)。
   *
   * <p>階段専用テクスチャは未投入のため、識別マーカーで視認性を確保する。M2 で素材投入時にこの メソッドを削除し、{@link #drawMapTextures} に階段専用
   * Texture を渡すパターンに切り替える。
   */
  private static void drawStairsMarker(ShapeRenderer shapes, DungeonMap map) {
    shapes.setColor(STAIRS_COLOR);
    int tile = RenderLayout.TILE_SIZE;
    int inset = tile / 4;
    for (int y = 0; y < map.height(); y++) {
      for (int x = 0; x < map.width(); x++) {
        if (map.tileAt(new Position(x, y)) != Tile.STAIRS_DOWN) {
          continue;
        }
        int sx = RenderLayout.MAP_ORIGIN_X + x * tile + inset;
        int sy = RenderLayout.MAP_ORIGIN_Y + y * tile + inset;
        shapes.rect(sx, sy, tile - inset * 2, tile - inset * 2);
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
        if (map.tileAt(new Position(x, y)) != Tile.WALL) {
          continue;
        }
        int sx = RenderLayout.MAP_ORIGIN_X + x * tile;
        int sy = RenderLayout.MAP_ORIGIN_Y + y * tile;
        // 下辺: 下隣が床
        if (y > 0 && map.tileAt(new Position(x, y - 1)) != Tile.WALL) {
          shapes.rect(sx, sy, tile, WALL_BORDER_WIDTH);
        }
        // 上辺: 上隣が床
        if (y < map.height() - 1 && map.tileAt(new Position(x, y + 1)) != Tile.WALL) {
          shapes.rect(sx, sy + tile - WALL_BORDER_WIDTH, tile, WALL_BORDER_WIDTH);
        }
        // 左辺: 左隣が床
        if (x > 0 && map.tileAt(new Position(x - 1, y)) != Tile.WALL) {
          shapes.rect(sx, sy, WALL_BORDER_WIDTH, tile);
        }
        // 右辺: 右隣が床
        if (x < map.width() - 1 && map.tileAt(new Position(x + 1, y)) != Tile.WALL) {
          shapes.rect(sx + tile - WALL_BORDER_WIDTH, sy, WALL_BORDER_WIDTH, tile);
        }
      }
    }
  }

  /** 設置済みの罠を小マーカーで描画する (§15-3、物理 = 橙 / 魔法 = 紫)。空タイル上の罠を可視化する。 */
  private static void drawTraps(ShapeRenderer shapes, DungeonState state) {
    int inset = RenderLayout.TILE_SIZE / 4;
    for (PlacedTrap trap : state.placedTraps()) {
      shapes.setColor(
          trap.element() == CardElement.PHYSICAL ? TRAP_PHYSICAL_COLOR : TRAP_MAGICAL_COLOR);
      int sx = RenderLayout.MAP_ORIGIN_X + trap.position().x() * RenderLayout.TILE_SIZE + inset;
      int sy = RenderLayout.MAP_ORIGIN_Y + trap.position().y() * RenderLayout.TILE_SIZE + inset;
      shapes.rect(sx, sy, RenderLayout.TILE_SIZE - inset * 2, RenderLayout.TILE_SIZE - inset * 2);
    }
  }

  private static void drawEnemies(ShapeRenderer shapes, DungeonState state) {
    for (Enemy e : state.enemies()) {
      // §15-6 / §15-5 視認性: ボス=赤大 / 頑強=灰 / 強化個体=橙 / 雑魚=緑 / 素早い=水色小 で描き分ける。
      int inset =
          switch (e.kind()) {
            case BOSS -> 1;
            case TOUGH_SLIME -> 2;
            case ELITE_SLIME -> 3;
            case SLIME -> 4;
            case SWIFT_SLIME -> 5;
          };
      shapes.setColor(
          switch (e.kind()) {
            case BOSS -> BOSS_COLOR;
            case ELITE_SLIME -> ELITE_COLOR;
            case SLIME -> ENEMY_COLOR;
            case SWIFT_SLIME -> SWIFT_COLOR;
            case TOUGH_SLIME -> TOUGH_COLOR;
          });
      int sx = RenderLayout.MAP_ORIGIN_X + e.position().x() * RenderLayout.TILE_SIZE + inset;
      int sy = RenderLayout.MAP_ORIGIN_Y + e.position().y() * RenderLayout.TILE_SIZE + inset;
      shapes.rect(sx, sy, RenderLayout.TILE_SIZE - inset * 2, RenderLayout.TILE_SIZE - inset * 2);
    }
  }

  private static void drawPlayer(ShapeRenderer shapes, Position p) {
    shapes.setColor(PLAYER_COLOR);
    int inset = 2;
    int sx = RenderLayout.MAP_ORIGIN_X + p.x() * RenderLayout.TILE_SIZE + inset;
    int sy = RenderLayout.MAP_ORIGIN_Y + p.y() * RenderLayout.TILE_SIZE + inset;
    shapes.rect(sx, sy, RenderLayout.TILE_SIZE - inset * 2, RenderLayout.TILE_SIZE - inset * 2);
  }
}
