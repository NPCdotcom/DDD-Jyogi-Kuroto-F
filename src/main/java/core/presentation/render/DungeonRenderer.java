package core.presentation.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import core.domain.common.Position;
import core.domain.dungeon.DungeonMap;
import core.domain.dungeon.DungeonState;
import core.domain.dungeon.Tile;
import core.domain.entity.Enemy;

/**
 * ダンジョン (マップ + アクター) を {@link ShapeRenderer} で描画するユーティリティ。
 *
 * <p>State から ShapeRenderer への一方向描画のみ。状態を変更しない。
 */
public final class DungeonRenderer {

  private static final Color FLOOR_COLOR = new Color(0.2f, 0.2f, 0.22f, 1f);
  private static final Color WALL_COLOR = new Color(0.45f, 0.32f, 0.22f, 1f);
  private static final Color STAIRS_COLOR = new Color(0.85f, 0.75f, 0.25f, 1f);
  private static final Color PLAYER_COLOR = new Color(0.30f, 0.65f, 1.00f, 1f);
  private static final Color ENEMY_COLOR = new Color(0.40f, 0.85f, 0.40f, 1f);

  private DungeonRenderer() {}

  public static void draw(ShapeRenderer shapes, DungeonState state) {
    shapes.begin(ShapeType.Filled);
    drawMap(shapes, state.map());
    drawEnemies(shapes, state);
    drawPlayer(shapes, state.player().position());
    shapes.end();
  }

  private static void drawMap(ShapeRenderer shapes, DungeonMap map) {
    for (int y = 0; y < map.height(); y++) {
      for (int x = 0; x < map.width(); x++) {
        Tile t = map.tileAt(new Position(x, y));
        shapes.setColor(colorOf(t));
        int sx = RenderLayout.MAP_ORIGIN_X + x * RenderLayout.TILE_SIZE;
        int sy = RenderLayout.MAP_ORIGIN_Y + y * RenderLayout.TILE_SIZE;
        shapes.rect(sx, sy, RenderLayout.TILE_SIZE - 1, RenderLayout.TILE_SIZE - 1);
      }
    }
  }

  private static void drawEnemies(ShapeRenderer shapes, DungeonState state) {
    shapes.setColor(ENEMY_COLOR);
    int inset = 4;
    for (Enemy e : state.enemies()) {
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

  private static Color colorOf(Tile t) {
    return switch (t) {
      case FLOOR -> FLOOR_COLOR;
      case WALL -> WALL_COLOR;
      case STAIRS_DOWN -> STAIRS_COLOR;
    };
  }
}
