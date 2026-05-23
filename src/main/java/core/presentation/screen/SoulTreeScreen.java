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
import core.domain.tree.NodeEffect;
import core.domain.tree.NodeId;
import core.domain.tree.SoulTree;
import core.domain.tree.TreeNode;
import core.presentation.render.Fonts;
import core.presentation.render.NodeIconPathResolver;
import core.presentation.render.RenderLayout;
import core.presentation.render.Strings;
import core.presentation.window.ConfirmationDialog;
import core.presentation.window.SoulNodeUnlockDialog;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

  // §UI 改善: viewport.unproject の浮動小数点誤差 (±数 px) + ユーザーがアイコン (64x64) の縁を狙った
  // クリックが判定円 40px 外に落ちる症状を解消するため、判定円を 50f に拡大。ノード間最短距離 200px
  // (ステ軸 60° 等間隔) で判定円 50 同士でも余裕 100px、隣接ノード誤判定なし。
  /** ノードクリック判定の半径 (px、ワールド座標)。 */
  private static final float NODE_RADIUS = 50f;

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

  /** §15-7 UI 改善: ノード種別ごとのアイコンテクスチャ。CardGrant は CardImageRegistry 経由のため本マップには入れない。 */
  private final Map<NodeId, Texture> nodeIconTextures = new HashMap<>();

  /** 自前ロードに失敗したノード用の fallback (test.png)。CardImageRegistry のテクスチャとは別所有。 */
  private Texture fallbackNodeTexture;

  /** §15-7 UI 改善: ノード解放確認モーダル (null = 非表示、非 null = 表示中、入力モーダル化)。 */
  private SoulNodeUnlockDialog pendingUnlock;

  /** ダイアログで「解放する」と返した時に実際に解放するためのノード ID 保持。 */
  private NodeId pendingUnlockId;

  /** §UI 改善 (M2 backlog): R キー押下時のツリーリセット確認モーダル (誤押下防止)。 */
  private ConfirmationDialog pendingResetConfirm;

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
    // §15-7 UI 改善: LinkedHashMap で挿入順を保持し、tryUnlockAt のイテレーションを決定的にする
    // (HashMap だとシルエットノードが先にヒットして可視ノードを無視するバグの温床)。
    Map<NodeId, Vector2> m = new LinkedHashMap<>();
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
    return Collections.unmodifiableMap(m);
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
    // §15-7 UI 改善: ノード種別ごとに NodeIconPathResolver でアイコンパスを解決し、自前ロード。
    // CardGrant は CardImageRegistry から都度参照 (本マップには入れない、二重解放を避ける)。
    fallbackNodeTexture = loadOrPlaceholder("icons/cards/test.png");
    for (Map.Entry<NodeId, TreeNode> entry : SoulTree.allNodes().entrySet()) {
      NodeEffect effect = entry.getValue().effect();
      String path = NodeIconPathResolver.resolve(effect);
      if (path == null) {
        continue; // CardGrantEffect は CardImageRegistry 経由
      }
      Texture tex = null;
      try {
        if (Gdx.files.internal(path).exists()) {
          tex = new Texture(Gdx.files.internal(path));
          nodeIconTextures.put(entry.getKey(), tex);
          tex = null; // 所有権が map に移った、後段の cleanup スキップ
        }
      } catch (RuntimeException ignored) {
        // 欠損時は fallbackNodeTexture で代替
      } finally {
        // §UI 改善 (devils-advocate 検出): new Texture 直後の put が万一例外で失敗した場合に
        // Texture が dispose されずリークするのを防ぐ。put が成功した時点で tex=null になり no-op。
        if (tex != null) {
          tex.dispose();
        }
      }
    }
  }

  /**
   * 指定ノードに対応する描画用 Texture を返す。CardGrant は CardImageRegistry から取得、 Stats/Slot/None は自前ロード、それ以外は
   * fallback。
   */
  private Texture nodeIconFor(NodeId id, NodeEffect effect) {
    if (effect instanceof NodeEffect.CardGrantEffect cg) {
      Texture tex =
          game.cardImageRegistry() != null ? game.cardImageRegistry().get(cg.cardId()) : null;
      return tex != null ? tex : fallbackNodeTexture;
    }
    Texture tex = nodeIconTextures.get(id);
    return tex != null ? tex : fallbackNodeTexture;
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
      // §UI 改善: 中央 (ROOT) ノードは画像も「中央」テキストも描画しない (浮いて見えるとのユーザー指摘)。
      // 枝線描画ループ (上) は ROOT 位置を参照し続けるため、中央から外周への枝線は維持される。
      if (id.equals(SoulTree.ROOT)) {
        continue;
      }
      // 段階的開示: 可視 = 前提全解放 / シルエット = 前提が全て可視 (1 段先の予告) / それ以外は非表示。
      boolean visible = tree.isVisible(id);
      boolean silhouette = !visible && allPrereqsVisible(node, tree);
      if (!visible && !silhouette) {
        continue;
      }
      Texture nodeTex = nodeIconFor(id, node.effect());
      if (silhouette) {
        batch.setColor(0.3f, 0.3f, 0.36f, 0.75f);
        batch.draw(
            nodeTex,
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
          nodeTex,
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

    // 5) §15-7 UI: 解放確認ダイアログ (最前面、モーダル) + 結果取得
    renderAndConsumeUnlockDialog(delta);
    // 6) §UI 改善: R キーリセット確認ダイアログ (最前面、モーダル) + 結果取得
    renderAndConsumeResetConfirm(delta);
  }

  /** モーダルダイアログ (解放確認 or リセット確認) が表示中か。入力凍結判定に使う。 */
  private boolean hasActiveModal() {
    return pendingUnlock != null || pendingResetConfirm != null;
  }

  /** WASD / 矢印キーでパン、Z / X でズーム。 */
  private void handleCameraInput(float delta) {
    if (hasActiveModal()) {
      return; // モーダル中はパン/ズーム凍結
    }
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
    if (hasActiveModal()) {
      // モーダル中はポインタ凍結 (ダイアログが入力を取る)。
      // 「固まる」バグ防止: pointerDown / dragged を消費しないと、ダイアログ閉じた次フレームで
      // `down=false / pointerDown=true / dragged=false` のまま else-if (pointerDown && !dragged)
      // 分岐が真になり、同じワールド座標で tryUnlockAt が再発火 → ダイアログ無限再オープン。
      // ここで両者をリセットすることで、ダイアログ完了後の初回フレームは「押下されていない」状態から始まる。
      pointerDown = false;
      dragged = false;
      return;
    }
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
      Gdx.app.log(
          "SoulTree",
          "Click detected screen=("
              + Gdx.input.getX()
              + ","
              + Gdx.input.getY()
              + ") world=("
              + world.x
              + ","
              + world.y
              + ") camera=("
              + camera.position.x
              + ","
              + camera.position.y
              + ") zoom="
              + camera.zoom);
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
    if (hasActiveModal()) {
      return; // モーダル中は ESC / R を飲み込む (ダイアログが Y/N/ESC を解決する)
    }
    if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
      game.changeScreen(new TitleScreen(game));
      return;
    }
    if (Gdx.input.isKeyJustPressed(Keys.R)) {
      // §UI 改善: R キーは無確認リセットだと誤押下でラン外貯蓄を消失するため、確認ダイアログを挟む。
      boolean jp = game.fonts().isJapaneseAvailable();
      pendingResetConfirm =
          new ConfirmationDialog(
              game.fonts().large(),
              game.fonts().large(),
              jp ? "ツリーリセット" : "Tree Reset",
              jp
                  ? "解放した全ノードをリセットしてソウルを払い戻します。よろしいですか?"
                  : "Reset all unlocked nodes and refund their soul cost?",
              jp ? "[Y] 確定   [N] / [ESC] やめる" : "[Y] Confirm   [N] / [ESC] Cancel");
    }
  }

  /**
   * §15-7 UI 改善: 解放確認ダイアログの描画・結果消費。ダイアログが Y/N/ESC で完了したら、結果に 応じて {@code game.unlockTreeNode}
   * を呼ぶ。フラッシュメッセージは日英ローカライズ済を使う。
   */
  private void renderAndConsumeUnlockDialog(float delta) {
    if (pendingUnlock == null) {
      return;
    }
    Gdx.app.log("SoulTree", "renderAndConsumeUnlockDialog render() call");
    // §UI 改善 (仮説 V 対策): Dialog.render() が例外で沈黙して dispose まで進むと
    // 「ダイアログが出た形跡すら見えない」になるため、try-catch で可視化する。
    try {
      pendingUnlock.render(delta);
    } catch (RuntimeException ex) {
      Gdx.app.error("SoulTree", "Dialog render failed", ex);
      boolean jp = game.fonts().isJapaneseAvailable();
      showFlash((jp ? "ダイアログ描画失敗: " : "Dialog render failed: ") + ex.getClass().getSimpleName());
      pendingUnlock.dispose();
      pendingUnlock = null;
      pendingUnlockId = null;
      pointerDown = false;
      dragged = false;
      return;
    }
    java.util.Optional<Boolean> r = pendingUnlock.consume();
    if (r.isEmpty()) {
      return;
    }
    Gdx.app.log("SoulTree", "Dialog consumed result=" + r.get());
    if (r.get()) {
      try {
        game.unlockTreeNode(pendingUnlockId);
        // 防衛的 null チェック: pendingUnlockId は tryUnlockAt 由来で allNodes() に必ず存在するため
        // 実際には null にならないが、SoulTree の API 変更で取りこぼした場合の NPE を回避する。
        TreeNode unlocked = SoulTree.allNodes().get(pendingUnlockId);
        boolean jp = game.fonts().isJapaneseAvailable();
        String displayName = unlocked != null ? unlocked.displayName() : pendingUnlockId.value();
        showFlash(
            (jp ? Strings.Ja.SOUL_TREE_UNLOCKED_FORMAT : Strings.En.SOUL_TREE_UNLOCKED_FORMAT)
                .formatted(displayName));
      } catch (IllegalStateException ex) {
        boolean jp = game.fonts().isJapaneseAvailable();
        // SoulTree.unlock は「ソウル不足」「前提未解放」「解放済み」の 3 種を IllegalStateException
        // で投げる。メッセージ文字列で判別して適切な flash を出す。
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        if (msg.startsWith("Insufficient soul")) {
          showFlash(jp ? "ソウルが不足しています" : "Insufficient soul");
        } else if (msg.startsWith("Prerequisite not unlocked")) {
          showFlash(jp ? "前提ノードが未解放です" : "Prerequisite not unlocked");
        } else {
          showFlash(jp ? "解放できません" : "Cannot unlock");
        }
      } catch (IllegalArgumentException ex) {
        boolean jp = game.fonts().isJapaneseAvailable();
        showFlash(jp ? "無効なノードです" : "Invalid node");
      }
    }
    pendingUnlock.dispose();
    pendingUnlock = null;
    pendingUnlockId = null;
    // 二重防御: ダイアログ閉じ直後にポインタ状態を確実に同期する (handlePointer の早期 return と対)。
    pointerDown = false;
    dragged = false;
  }

  /**
   * §UI 改善: R キーリセット確認ダイアログの描画・結果消費。Y で確定なら {@code game.resetTree()} を呼び、 払い戻し flash を出す。N/ESC
   * でキャンセルなら何もしない。
   */
  private void renderAndConsumeResetConfirm(float delta) {
    if (pendingResetConfirm == null) {
      return;
    }
    try {
      pendingResetConfirm.render(delta);
    } catch (RuntimeException ex) {
      Gdx.app.error("SoulTree", "Reset confirm dialog render failed", ex);
      pendingResetConfirm.dispose();
      pendingResetConfirm = null;
      pointerDown = false;
      dragged = false;
      return;
    }
    java.util.Optional<Boolean> r = pendingResetConfirm.consume();
    if (r.isEmpty()) {
      return;
    }
    if (r.get()) {
      game.resetTree();
      boolean jp = game.fonts().isJapaneseAvailable();
      showFlash(jp ? Strings.Ja.SOUL_TREE_FLASH_RESET : Strings.En.SOUL_TREE_FLASH_RESET);
    }
    pendingResetConfirm.dispose();
    pendingResetConfirm = null;
    pointerDown = false;
    dragged = false;
  }

  private void tryUnlockAt(float worldX, float worldY) {
    // §15-7 UI 改善: 「最近傍の可視ノード」採用 (旧: HashMap 順で最初にヒットしたノードを即解放
    // → シルエットノードが先にヒットすると可視ノードを無視するバグの温床)。
    NodeId bestId = null;
    float bestDistSq = Float.MAX_VALUE;
    for (Map.Entry<NodeId, Vector2> entry : POSITIONS.entrySet()) {
      NodeId candidate = entry.getKey();
      // §UI 改善: 中央 ROOT は描画スキップしているが、POSITIONS には残るためクリック判定でも除外する。
      // 入れないと NODE_RADIUS=50 への拡大で中央付近クリックが見えない ROOT に吸い込まれ、
      // unlock(ROOT) が「Node already unlocked」(IllegalStateException) を投げて「解放できません」 flash
      // (renderAndConsumeUnlockDialog の else 分岐) が出る謎挙動になる。
      if (candidate.equals(SoulTree.ROOT)) {
        continue;
      }
      Vector2 pos = entry.getValue();
      float dx = worldX - pos.x;
      float dy = worldY - pos.y;
      float distSq = dx * dx + dy * dy;
      if (distSq > NODE_RADIUS * NODE_RADIUS) {
        continue;
      }
      if (!game.soulTree().isVisible(candidate)) {
        continue; // シルエットノードは候補から除外
      }
      if (distSq < bestDistSq) {
        bestDistSq = distSq;
        bestId = candidate;
      }
    }
    Gdx.app.log(
        "SoulTree",
        "tryUnlockAt world=("
            + worldX
            + ","
            + worldY
            + ") bestId="
            + (bestId == null ? "null" : bestId.value())
            + " bestDistSq="
            + bestDistSq);
    if (bestId == null) {
      // §UI 改善 (仮説 P 対策): クリックがどのノードにもヒットしなかった時、ユーザー目線は
      // 「クリックしても何も起きない=固まった」と誤認するので flash で視覚的に伝える。
      boolean jp = game.fonts().isJapaneseAvailable();
      showFlash(
          jp ? "ノードが見つかりません (アイコンの中央寄りをクリック)" : "No node found (click closer to icon center)");
      return;
    }
    // §15-7 UI 改善: 即解放せず確認ダイアログを開く (誤クリック対策)
    TreeNode node = SoulTree.allNodes().get(bestId);
    // 防衛: POSITIONS と allNodes() の定義ズレ (将来の追加時に片方を忘れるケース) に備える。
    if (node == null) {
      return;
    }
    NodeEffect effect = node.effect();
    Texture iconTex = nodeIconFor(bestId, effect);
    boolean isCardGrant = effect instanceof NodeEffect.CardGrantEffect;
    this.pendingUnlock =
        new SoulNodeUnlockDialog(
            game.fonts().title(),
            game.fonts().large(),
            node,
            iconTex,
            game.playerSoul().amount(),
            isCardGrant);
    this.pendingUnlockId = bestId;
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
    // §UI 改善: ダイアログ表示中にウィンドウサイズ変更があった場合、Dialog の viewport も同期する
    // (DungeonScreen が nodeChoice / statusPopup を resize しているのと同じパターン)。
    if (pendingUnlock != null) {
      pendingUnlock.resize(width, height);
    }
    if (pendingResetConfirm != null) {
      pendingResetConfirm.resize(width, height);
    }
  }

  @Override
  public void dispose() {
    if (batch != null) {
      batch.dispose();
    }
    if (shape != null) {
      shape.dispose();
    }
    if (fallbackNodeTexture != null) {
      fallbackNodeTexture.dispose();
    }
    for (Texture t : nodeIconTextures.values()) {
      try {
        t.dispose();
      } catch (RuntimeException ignored) {
        // 二重 dispose 等の安全弁
      }
    }
    nodeIconTextures.clear();
    if (pendingUnlock != null) {
      pendingUnlock.dispose();
      pendingUnlock = null;
    }
    if (pendingResetConfirm != null) {
      pendingResetConfirm.dispose();
      pendingResetConfirm = null;
    }
  }
}
