package core.presentation.screen;

import com.badlogic.gdx.Gdx;
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
 * ソウルツリー画面 (§15-7 / E-2)。タイトル画面 (2 周目以降) またはラン終了後に遷移し、23 ノードを 円樹形配置で描画。マウスクリックでノード解放、WASD /
 * 矢印キー・マウスドラッグでパン、Z / X で ズーム、R キーでツリーリセット、ESC でタイトルに戻る。
 *
 * <p>カメラは 2 系統: ツリー本体 (枝線 + ノード + ノード文字) は {@link #camera} (パン / ズーム可能) で 描画し、画面下部の HUD (タイトル /
 * ソウル残量 / ヒント / フラッシュ) は {@link #hudCamera} (固定) で 描画する。これにより HUD はパン /
 * ズームに追従せず常に同じ位置に出る。ツリーノードを増やしても カメラを動かして全体を見渡せる (拡張時の破綻防止)。
 *
 * <p>描画は 3 層:
 *
 * <ol>
 *   <li>{@link ShapeRenderer} で前提ノードから子ノードへ枝線を描く (解放済み = 黄、未解放 = 暗灰)
 *   <li>{@link SpriteBatch} で {@link Texture}(test.png) をノード位置に描画 (解放済み = 白、解放可 = 緑、未解放 = 暗灰)
 *   <li>同 batch を固定カメラに切り替えてノード displayName + soulCost + 画面下部 HUD を描画
 * </ol>
 *
 * <p>{@code assets/icons/cards/test.png} を全ノードで使い回す (Plan の {@code test.png} 使い回し方針)。
 * チームメイトが本素材に入れ替える際は同パスで上書きすれば差し替わる。
 */
public final class SoulTreeScreen extends ScreenAdapter {

  private static final float CENTER_X = 960f;
  private static final float CENTER_Y = 540f;

  /** ノードクリック判定の半径 (px、ワールド座標)。 */
  private static final float NODE_RADIUS = 32f;

  /** ノードテクスチャ描画サイズ (px)。 */
  private static final float NODE_TEX_SIZE = 64f;

  /** 枝線の太さ。 */
  private static final float BRANCH_WIDTH = 4f;

  /** キーパンの速度 (ワールド単位 / 秒)。zoom 倍率を掛けて見かけのパン速度を一定に保つ。 */
  private static final float PAN_SPEED = 700f;

  /** ズーム下限 (寄り) / 上限 (引き)。1.0 = 等倍。 */
  private static final float ZOOM_MIN = 0.6f;

  private static final float ZOOM_MAX = 2.2f;

  /** キーズームの速度 (zoom / 秒)。 */
  private static final float ZOOM_SPEED = 1.3f;

  /** カメラ中心がツリー中心 (CENTER_X, CENTER_Y) から離れられる最大距離 (px)。 */
  private static final float PAN_LIMIT = 760f;

  /** クリックとドラッグを区別するスクリーン移動量しきい値 (px)。 */
  private static final float CLICK_DRAG_THRESHOLD = 8f;

  /** 23 ノードの画面座標 (放射状配置、§15-7 円樹形 UI)。 */
  private static final Map<NodeId, Vector2> POSITIONS = positions();

  private final DddGame game;
  private OrthographicCamera camera;

  /** HUD 専用の固定カメラ。ツリーのパン / ズームに追従しない。 */
  private OrthographicCamera hudCamera;

  private Viewport viewport;
  private SpriteBatch batch;
  private ShapeRenderer shape;
  private Texture nodeTexture;

  /** マウスクリック直後の一時メッセージ ("Soul 不足" 等、~2 秒で消える)。 */
  private String flashMessage;

  private float flashTimer;

  /** 前フレームでポインタが押下されていたか (押下→解放のクリック検出用)。 */
  private boolean pointerDown;

  /** ドラッグ判定用: 押下開始スクリーン座標。 */
  private float touchStartX;

  private float touchStartY;

  /** 押下開始からしきい値を超えて移動したか (true = ドラッグ、離してもノード解放しない)。 */
  private boolean dragged;

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
    // ステ軸 Lv2 (各スポークの最外周、§15-7 充実化)
    m.put(NodeId.of("hp_up_2"), polar(0, 700));
    m.put(NodeId.of("speed_up_2"), polar(60, 700));
    m.put(NodeId.of("phys_atk_up_2"), polar(120, 700));
    m.put(NodeId.of("mag_atk_up_2"), polar(180, 700));
    m.put(NodeId.of("phys_def_up_2"), polar(240, 700));
    m.put(NodeId.of("mag_def_up_2"), polar(300, 700));
    return Map.copyOf(m);
  }

  private static Vector2 polar(double degrees, float radius) {
    double rad = Math.toRadians(degrees);
    return new Vector2(
        CENTER_X + radius * (float) Math.cos(rad), CENTER_Y + radius * (float) Math.sin(rad));
  }

  @Override
  public void show() {
    camera = new OrthographicCamera();
    viewport = new FitViewport(RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT, camera);
    // ツリー中心を画面中央に置いて開始する。
    camera.position.set(CENTER_X, CENTER_Y, 0f);
    camera.update();
    // HUD は固定カメラ (パン / ズームに追従しない)。
    hudCamera = new OrthographicCamera();
    hudCamera.setToOrtho(false, RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT);
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
    // カメラ操作 (キーパン / ズーム → ドラッグパン / クリック → 範囲クランプ) を先に確定する。
    handleCameraInput(delta);
    handlePointer();
    clampCamera();

    ScreenUtils.clear(0.03f, 0.03f, 0.06f, 1f);
    viewport.apply();
    camera.update();

    SoulTree tree = game.soulTree();
    Map<NodeId, TreeNode> defs = SoulTree.allNodes();

    // 1) 枝線 (前提ノード → 子ノード) — パンカメラ
    shape.setProjectionMatrix(camera.combined);
    shape.begin(ShapeRenderer.ShapeType.Filled);
    for (Map.Entry<NodeId, TreeNode> entry : defs.entrySet()) {
      TreeNode node = entry.getValue();
      Vector2 to = POSITIONS.get(node.id());
      if (to == null) {
        continue;
      }
      if (!tree.isVisible(node.id()) && !allPrereqsVisible(node, tree)) {
        continue; // 段階的開示: 子ノードが未開示なら枝も描かない
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

    // 2) ノードテクスチャ + テキスト — パンカメラ
    batch.setProjectionMatrix(camera.combined);
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
      // 段階的開示: 可視 = 前提全解放 / シルエット = 前提が全て可視 (1 段先の予告) / それ以外は非表示。
      boolean visible = tree.isVisible(id);
      boolean silhouette = !visible && allPrereqsVisible(node, tree);
      if (!visible && !silhouette) {
        continue;
      }
      if (silhouette) {
        batch.setColor(0.3f, 0.3f, 0.36f, 0.75f);
        batch.draw(
            nodeTexture,
            pos.x - NODE_TEX_SIZE / 2,
            pos.y - NODE_TEX_SIZE / 2,
            NODE_TEX_SIZE,
            NODE_TEX_SIZE);
        large.setColor(0.55f, 0.55f, 0.6f, 1f);
        large.draw(batch, "?", pos.x - 10, pos.y + 12);
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
            (jp ? Strings.Ja.SOUL_COST_FORMAT : Strings.En.SOUL_COST_FORMAT)
                .formatted(node.soulCost());
        large.draw(batch, costText, pos.x - 90, pos.y - NODE_TEX_SIZE / 2 - 56);
      }
    }
    batch.setColor(Color.WHITE);
    batch.end();

    // 3) HUD — 固定カメラ (パン / ズームに追従しない)
    batch.setProjectionMatrix(hudCamera.combined);
    batch.begin();
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
    batch.setColor(Color.WHITE);
    batch.end();

    // 4) 非カメラ入力 (ESC / R / flash タイマ)
    handleInput(delta);
  }

  /** WASD / 矢印キーでパン、Z / X でズーム。 */
  private void handleCameraInput(float delta) {
    float pan = PAN_SPEED * delta * camera.zoom;
    if (Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A)) {
      camera.position.x -= pan;
    }
    if (Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)) {
      camera.position.x += pan;
    }
    if (Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W)) {
      camera.position.y += pan;
    }
    if (Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.S)) {
      camera.position.y -= pan;
    }
    // Z = 寄り (zoom 減)、X = 引き (zoom 増)。
    if (Gdx.input.isKeyPressed(Keys.Z)) {
      camera.zoom = Math.max(ZOOM_MIN, camera.zoom - ZOOM_SPEED * delta);
    }
    if (Gdx.input.isKeyPressed(Keys.X)) {
      camera.zoom = Math.min(ZOOM_MAX, camera.zoom + ZOOM_SPEED * delta);
    }
  }

  /**
   * マウス操作を処理する。押下→しきい値超えの移動でドラッグパン、押下→ほぼ静止のまま解放で クリック扱いとしてノード解放を試みる。クリック判定の {@link #viewport}
   * unproject は前フレームの カメラ行列を使う (クリック中はカメラ移動がほぼ無いため 1 フレーム遅れは無視できる)。
   */
  private void handlePointer() {
    boolean down = Gdx.input.isTouched();
    if (down && !pointerDown) {
      touchStartX = Gdx.input.getX();
      touchStartY = Gdx.input.getY();
      dragged = false;
    } else if (down) {
      float moved =
          Math.abs(Gdx.input.getX() - touchStartX) + Math.abs(Gdx.input.getY() - touchStartY);
      if (moved > CLICK_DRAG_THRESHOLD) {
        dragged = true;
      }
      if (dragged) {
        float worldPerPixel = RenderLayout.SCREEN_WIDTH / (float) Gdx.graphics.getWidth();
        camera.position.x -= Gdx.input.getDeltaX() * worldPerPixel * camera.zoom;
        camera.position.y += Gdx.input.getDeltaY() * worldPerPixel * camera.zoom;
      }
    } else if (pointerDown && !dragged) {
      // 押下していたポインタを移動量しきい値未満で離した = クリック → ノード解放。
      Vector3 world = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
      viewport.unproject(world);
      tryUnlockAt(world.x, world.y);
    }
    pointerDown = down;
  }

  /** カメラ中心をツリー中心まわりの可動範囲にクランプする。 */
  private void clampCamera() {
    camera.position.x =
        Math.max(CENTER_X - PAN_LIMIT, Math.min(CENTER_X + PAN_LIMIT, camera.position.x));
    camera.position.y =
        Math.max(CENTER_Y - PAN_LIMIT, Math.min(CENTER_Y + PAN_LIMIT, camera.position.y));
  }

  private void handleInput(float delta) {
    if (flashTimer > 0f) {
      flashTimer -= delta;
      if (flashTimer <= 0f) {
        flashMessage = null;
      }
    }
    if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
      dispose(); // setScreen は旧 Screen を dispose しないため明示的に解放 (LibGDX 規約)
      game.setScreen(new TitleScreen(game));
      return;
    }
    if (Gdx.input.isKeyJustPressed(Keys.R)) {
      game.resetTree();
      boolean jp = game.fonts().isJapaneseAvailable();
      showFlash(jp ? Strings.Ja.SOUL_TREE_FLASH_RESET : Strings.En.SOUL_TREE_FLASH_RESET);
    }
  }

  private void tryUnlockAt(float worldX, float worldY) {
    for (Map.Entry<NodeId, Vector2> entry : POSITIONS.entrySet()) {
      Vector2 pos = entry.getValue();
      float dx = worldX - pos.x;
      float dy = worldY - pos.y;
      if (dx * dx + dy * dy <= NODE_RADIUS * NODE_RADIUS) {
        NodeId clicked = entry.getKey();
        if (!game.soulTree().isVisible(clicked)) {
          // 未開示ノード (シルエット) のクリック: 無反応だと混乱するため、前提解放を促す。
          showFlash(
              game.fonts().isJapaneseAvailable()
                  ? Strings.Ja.SOUL_TREE_LOCKED_FLASH
                  : Strings.En.SOUL_TREE_LOCKED_FLASH);
          return;
        }
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

  /** ノードの全前提が「可視」か (段階的開示: 1 段先のシルエット表示判定に使う)。 */
  private static boolean allPrereqsVisible(TreeNode node, SoulTree tree) {
    for (NodeId prereq : node.prerequisites()) {
      if (!tree.isVisible(prereq)) {
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
    // false: カメラ位置を再センタリングしない (ユーザーのパン位置を保持する)。
    viewport.update(width, height, false);
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
