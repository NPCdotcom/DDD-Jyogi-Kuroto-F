package core.presentation.render;

import com.badlogic.gdx.graphics.Color;
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

  private static final Color FLOOR_COLOR = new Color(0.2f, 0.2f, 0.22f, 1f);
  private static final Color WALL_COLOR = new Color(0.45f, 0.32f, 0.22f, 1f);
  private static final Color STAIRS_COLOR = new Color(0.85f, 0.75f, 0.25f, 1f);
  private static final Color PLAYER_COLOR = new Color(0.30f, 0.65f, 1.00f, 1f);
  private static final Color ENEMY_COLOR = new Color(0.40f, 0.85f, 0.40f, 1f);
  private static final Color SWIFT_COLOR = new Color(0.40f, 0.85f, 0.80f, 1f);
  private static final Color TOUGH_COLOR = new Color(0.45f, 0.55f, 0.62f, 1f);
  private static final Color ELITE_COLOR = new Color(0.85f, 0.55f, 0.20f, 1f);
  private static final Color BOSS_COLOR = new Color(0.85f, 0.25f, 0.30f, 1f);
  private static final Color TRAP_PHYSICAL_COLOR = new Color(0.95f, 0.55f, 0.15f, 1f);
  private static final Color TRAP_MAGICAL_COLOR = new Color(0.70f, 0.35f, 0.95f, 1f);

  private DungeonRenderer() {}

  public static void draw(ShapeRenderer shapes, DungeonState state) {
    shapes.begin(ShapeType.Filled);
    drawMap(shapes, state.map());
    drawTraps(shapes, state);
    drawEnemies(shapes, state);
    drawPlayer(shapes, state.player().position());
    shapes.end();
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

  private static Color colorOf(Tile t) {
    return switch (t) {
      case FLOOR -> FLOOR_COLOR;
      case WALL -> WALL_COLOR;
      case STAIRS_DOWN -> STAIRS_COLOR;
    };
  }
}
