package core.presentation.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import core.domain.tree.NodeId;
import core.domain.tree.SoulTree;
import core.domain.tree.TreeNode;
import core.presentation.render.Fonts;
import core.presentation.render.RenderLayout;
import core.presentation.render.Strings;
import java.util.HashMap;
import java.util.Map;

/**
 * ソウルツリー画面 (§15-7 / E-2)。タイトル画面から T キーで遷移し、17 ノードを円樹形配置で描画。
 * マウスクリックでノード解放、R キーでツリーリセット、ESC でタイトルに戻る。
 *
 * <p>描画は 3 層:
 *
 * <ol>
 *   <li>{@link ShapeRenderer} で前提ノードから子ノードへ枝線を描く (解放済み = 黄、未解放 = 暗灰)
 *   <li>{@link SpriteBatch} で {@link Texture}(test.png) をノード位置に描画 (解放済み = 白、解放可 = 緑、未解放 = 暗灰)
 *   <li>同 batch でノード displayName + soulCost テキストを描画 + 画面下部 HUD (Soul 残量 / ヒント)
 * </ol>
 *
 * <p>{@code assets/icons/cards/test.png} を全ノードで使い回す (Plan の {@code test.png} 使い回し方針)。
 * チームメイトが本素材に入れ替える際は同パスで上書きすれば差し替わる。
 */
public final class SoulTreeScreen extends ScreenAdapter {

  private static final float CENTER_X = 960f;
  private static final float CENTER_Y = 540f;
  /** ノードクリック判定の半径 (px、SCREEN 1920×1080 内座標)。 */
  private static final float NODE_RADIUS = 32f;
  /** ノードテクスチャ描画サイズ (px)。 */
  private static final float NODE_TEX_SIZE = 64f;
  /** 枝線の太さ (ShapeRenderer の縦横方向は無視されるため、別途短い線分の重ね描きで実現)。 */
  private static final float BRANCH_WIDTH = 4f;

  /** 17 ノードの画面座標 (放射状配置、§15-7 円樹形 UI)。 */
  private static final Map<NodeId, Vector2> POSITIONS = positions();

  private final DddGame game;
  private OrthographicCamera camera;
  private Viewport viewport;
  private SpriteBatch batch;
  private ShapeRenderer shape;
  private Texture nodeTexture;
  /** マウスクリック直後の一時メッセージ ("Soul 不足" 等、~2 秒で消える)。 */
  private String flashMessage;
  private float flashTimer;

  public SoulTreeScreen(DddGame game) {
    this.game = game;
  }

  private static Map<NodeId, Vector2> positions() {
    Map<NodeId, Vector2> m = new HashMap<>();
    m.put(SoulTree.ROOT, new Vector2(CENTER_X, CENTER_Y));
    m.put(NodeId.of("hp_up_1"), polar(0, 200));
    m.put(NodeId.of("speed_up_1"), polar(60, 200));
    m.put(NodeId.of("phys_atk_up_1"), polar(120, 200));
    m.put(NodeId.of("mag_atk_up_1"), polar(180, 200));
    m.put(NodeId.of("phys_def_up_1"), polar(240, 200));
    m.put(NodeId.of("mag_def_up_1"), polar(300, 200));
    m.put(NodeId.of("card_grant_strong_strike"), polar(120, 380));
    m.put(NodeId.of("card_grant_fireball"), polar(170, 380));
    m.put(NodeId.of("card_grant_magic_bolt"), polar(190, 380));
    m.put(NodeId.of("card_grant_iron_skin"), polar(240, 380));
    m.put(NodeId.of("card_grant_arcane_veil"), polar(300, 380));
    m.put(NodeId.of("slot_expand_1"), polar(120, 540));
    m.put(NodeId.of("slot_expand_2"), polar(170, 540));
    m.put(NodeId.of("slot_expand_3"), polar(190, 540));
    m.put(NodeId.of("slot_expand_4"), polar(240, 540));
    m.put(NodeId.of("slot_expand_5"), polar(300, 540));
    return Map.copyOf(m);
  }

  private static Vector2 polar(double degrees, float radius) {
    double rad = Math.toRadians(degrees);
    return new Vector2(
        CENTER_X + radius * (float) Math.cos(rad),
        CENTER_Y + radius * (float) Math.sin(rad));
  }

  @Override
  public void show() {
    camera = new OrthographicCamera();
    viewport = new FitViewport(RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT, camera);
    batch = new SpriteBatch();
    shape = new ShapeRenderer();
    // assets/icons/cards/test.png をプレースホルダとして全ノードで使い回し (Plan 方針)。
    // ファイル欠損時のフォールバックとして 1x1 灰色テクスチャを生成。
    nodeTexture = loadOrPlaceholder("icons/cards/test.png");
  }

  private static Texture loadOrPlaceholder(String path) {
    try {
      if (Gdx.files.internal(path).exists()) {
        return new Texture(Gdx.files.internal(path));
      }
    } catch (RuntimeException ignored) {
      // フォールバックへ
    }
    Pixmap pm = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
    pm.setColor(0.7f, 0.7f, 0.7f, 1f);
    pm.fill();
    Texture tex = new Texture(pm);
    pm.dispose();
    return tex;
  }

  @Override
  public void render(float delta) {
    ScreenUtils.clear(0.03f, 0.03f, 0.06f, 1f);
    viewport.apply();
    camera.update();
    batch.setProjectionMatrix(camera.combined);
    shape.setProjectionMatrix(camera.combined);

    SoulTree tree = game.soulTree();
    Map<NodeId, TreeNode> defs = SoulTree.allNodes();

    // 1) 枝線 (前提ノード → 子ノード)
    shape.begin(ShapeRenderer.ShapeType.Filled);
    for (Map.Entry<NodeId, TreeNode> entry : defs.entrySet()) {
      TreeNode node = entry.getValue();
      Vector2 to = POSITIONS.get(node.id());
      if (to == null) {
        continue;
      }
      for (NodeId prereq : node.prerequisites()) {
        Vector2 from = POSITIONS.get(prereq);
        if (from == null) {
          continue;
        }
        boolean bothUnlocked =
            tree.unlockedNodes().contains(node.id()) && tree.unlockedNodes().contains(prereq);
        if (bothUnlocked) {
          shape.setColor(0.85f, 0.7f, 0.25f, 1f); // 解放済 = 金黄
        } else {
          shape.setColor(0.3f, 0.3f, 0.35f, 1f); // 未解放 = 暗灰
        }
        shape.rectLine(from.x, from.y, to.x, to.y, BRANCH_WIDTH);
      }
    }
    shape.end();

    // 2) ノードテクスチャ + テキスト
    batch.begin();
    Fonts fonts = game.fonts();
    boolean jp = fonts.isJapaneseAvailable();
    BitmapFont large = fonts.large();
    BitmapFont title = fonts.title();

    for (Map.Entry<NodeId, TreeNode> entry : defs.entrySet()) {
      NodeId id = entry.getKey();
      TreeNode node = entry.getValue();
      Vector2 pos = POSITIONS.get(id);
      if (pos == null) {
        continue;
      }
      boolean unlocked = tree.unlockedNodes().contains(id);
      boolean unlockable = !unlocked && allPrereqsUnlocked(node, tree);

      // テクスチャ色: 解放済 = 白、解放可 = 緑、未解放 = 暗
      if (unlocked) {
        batch.setColor(Color.WHITE);
      } else if (unlockable) {
        batch.setColor(0.5f, 0.9f, 0.5f, 1f);
      } else {
        batch.setColor(0.35f, 0.35f, 0.4f, 1f);
      }
      batch.draw(
          nodeTexture,
          pos.x - NODE_TEX_SIZE / 2,
          pos.y - NODE_TEX_SIZE / 2,
          NODE_TEX_SIZE,
          NODE_TEX_SIZE);

      // テキスト (displayName + コスト) — large 32px、ノード画像と重ならないよう Y を下げる
      if (unlocked) {
        large.setColor(Color.WHITE);
      } else if (unlockable) {
        large.setColor(0.7f, 1f, 0.7f, 1f);
      } else {
        large.setColor(Color.LIGHT_GRAY);
      }
      large.draw(batch, node.displayName(), pos.x - 90, pos.y - NODE_TEX_SIZE / 2 - 16);
      if (node.soulCost() > 0) {
        String costText =
            (jp ? Strings.Ja.SOUL_COST_FORMAT : Strings.En.SOUL_COST_FORMAT).formatted(node.soulCost());
        large.draw(batch, costText, pos.x - 90, pos.y - NODE_TEX_SIZE / 2 - 56);
      }
    }
    batch.setColor(Color.WHITE);

    // 画面下部 HUD: タイトル / 操作ヒント / Soul 残量 / フラッシュメッセージ (large 経由で拡大)
    title.setColor(0.9f, 0.85f, 0.4f, 1f);
    title.draw(batch, jp ? Strings.Ja.SOUL_TREE_TITLE : Strings.En.SOUL_TREE_TITLE, 60, 1020);

    large.setColor(Color.LIGHT_GRAY);
    String inventoryText =
        (jp ? Strings.Ja.SOUL_TREE_INVENTORY_FORMAT : Strings.En.SOUL_TREE_INVENTORY_FORMAT)
            .formatted(game.playerSoul().amount());
    large.draw(batch, inventoryText, 60, 120);
    large.draw(
        batch,
        jp ? Strings.Ja.SOUL_TREE_CONTROLS_HINT : Strings.En.SOUL_TREE_CONTROLS_HINT,
        60,
        60);

    if (flashMessage != null && flashTimer > 0f) {
      large.setColor(1f, 0.55f, 0.35f, 1f);
      large.draw(batch, flashMessage, 60, 940);
    }
    batch.end();

    // 3) 入力処理
    handleInput(delta);
  }

  private void handleInput(float delta) {
    if (flashTimer > 0f) {
      flashTimer -= delta;
      if (flashTimer <= 0f) {
        flashMessage = null;
      }
    }
    if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
      game.setScreen(new TitleScreen(game));
      return;
    }
    if (Gdx.input.isKeyJustPressed(Keys.R)) {
      game.resetTree();
      boolean jp = game.fonts().isJapaneseAvailable();
      showFlash(jp ? Strings.Ja.SOUL_TREE_FLASH_RESET : Strings.En.SOUL_TREE_FLASH_RESET);
      return;
    }
    if (Gdx.input.justTouched() && Gdx.input.isButtonPressed(Buttons.LEFT)) {
      // スクリーン座標 → ワールド座標へ変換
      Vector3 world = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
      viewport.unproject(world);
      tryUnlockAt(world.x, world.y);
    }
  }

  private void tryUnlockAt(float worldX, float worldY) {
    for (Map.Entry<NodeId, Vector2> entry : POSITIONS.entrySet()) {
      Vector2 pos = entry.getValue();
      float dx = worldX - pos.x;
      float dy = worldY - pos.y;
      if (dx * dx + dy * dy <= NODE_RADIUS * NODE_RADIUS) {
        NodeId clicked = entry.getKey();
        try {
          game.unlockTreeNode(clicked);
          showFlash("解放: " + SoulTree.allNodes().get(clicked).displayName());
        } catch (IllegalStateException ex) {
          showFlash("解放不可: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
          showFlash("無効ノード: " + ex.getMessage());
        }
        return;
      }
    }
  }

  private static boolean allPrereqsUnlocked(TreeNode node, SoulTree tree) {
    for (NodeId prereq : node.prerequisites()) {
      if (!tree.unlockedNodes().contains(prereq)) {
        return false;
      }
    }
    return true;
  }

  private void showFlash(String message) {
    this.flashMessage = message;
    this.flashTimer = 2.5f;
  }

  @Override
  public void resize(int width, int height) {
    viewport.update(width, height, true);
  }

  @Override
  public void dispose() {
    if (batch != null) {
      batch.dispose();
    }
    if (shape != null) {
      shape.dispose();
    }
    if (nodeTexture != null) {
      nodeTexture.dispose();
    }
  }
}
