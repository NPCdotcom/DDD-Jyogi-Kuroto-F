package core.presentation.window;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import core.domain.tree.TreeNode;
import core.presentation.render.RenderLayout;
import java.util.Optional;

/**
 * ソウルツリーノード解放確認モーダルダイアログ (§15-7 / UI 改善)。
 *
 * <p>Y キーで解放確定、N キーまたは ESC でキャンセル。CardGrant ノードはカード画像を 240×336 で 大表示、それ以外はアイコン 200×200
 * を表示する。Y/N/ESC 入力で {@code done = true} となり、 {@link #consume()} で一度だけ結果を返す ({@link NodeChoicePopup}
 * と同型 API)。
 *
 * <p>テクスチャは外部所有 (CardImageRegistry / SoulTreeScreen) なので本クラスでは dispose しない。 自前所有の {@link
 * SpriteBatch} / {@link ShapeRenderer} のみ {@link #dispose} で解放する。
 */
public final class SoulNodeUnlockDialog {

  private static final int DIALOG_W = 800;
  private static final int DIALOG_H = 600;
  private static final int DIALOG_X = (RenderLayout.SCREEN_WIDTH - DIALOG_W) / 2;
  private static final int DIALOG_Y = (RenderLayout.SCREEN_HEIGHT - DIALOG_H) / 2;

  private final BitmapFont titleFont;
  private final BitmapFont bodyFont;
  private final TreeNode node;
  private final Texture iconTexture; // null 可
  private final int currentSoul;
  private final boolean isCardGrant;
  private final boolean canAfford;

  private final SpriteBatch batch;
  private final ShapeRenderer shapes;
  private final OrthographicCamera camera;
  private final FitViewport viewport;

  private Optional<Boolean> result = Optional.empty();
  private boolean done = false;

  public SoulNodeUnlockDialog(
      BitmapFont titleFont,
      BitmapFont bodyFont,
      TreeNode node,
      Texture iconTexture,
      int currentSoul,
      boolean isCardGrant) {
    this.titleFont = titleFont;
    this.bodyFont = bodyFont;
    this.node = node;
    this.iconTexture = iconTexture;
    this.currentSoul = currentSoul;
    this.isCardGrant = isCardGrant;
    this.canAfford = currentSoul >= node.soulCost();
    this.batch = new SpriteBatch();
    this.shapes = new ShapeRenderer();
    this.camera = new OrthographicCamera();
    this.viewport = new FitViewport(RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT, camera);
    camera.setToOrtho(false, RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT);
  }

  /** 入力受付 + 描画。Y/N/ESC で {@code done = true} になる。 */
  public void render(float delta) {
    if (done) {
      return;
    }

    if (Gdx.input.isKeyJustPressed(Keys.Y) && canAfford) {
      result = Optional.of(true);
      done = true;
      return;
    }
    if (Gdx.input.isKeyJustPressed(Keys.N) || Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
      result = Optional.of(false);
      done = true;
      return;
    }

    viewport.apply();
    Gdx.gl.glEnable(GL20.GL_BLEND);
    shapes.setProjectionMatrix(camera.combined);
    shapes.begin(ShapeRenderer.ShapeType.Filled);
    // 全面半透明オーバーレイ
    shapes.setColor(0f, 0f, 0f, 0.75f);
    shapes.rect(0f, 0f, RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT);
    // ダイアログ本体
    shapes.setColor(0.12f, 0.12f, 0.16f, 1f);
    shapes.rect(DIALOG_X, DIALOG_Y, DIALOG_W, DIALOG_H);
    // ダイアログ枠 (4 辺、4px)
    shapes.setColor(0.7f, 0.7f, 0.8f, 1f);
    shapes.rect(DIALOG_X, DIALOG_Y + DIALOG_H - 4, DIALOG_W, 4);
    shapes.rect(DIALOG_X, DIALOG_Y, DIALOG_W, 4);
    shapes.rect(DIALOG_X, DIALOG_Y, 4, DIALOG_H);
    shapes.rect(DIALOG_X + DIALOG_W - 4, DIALOG_Y, 4, DIALOG_H);
    shapes.end();

    batch.setProjectionMatrix(camera.combined);
    batch.begin();

    // タイトル: ノード displayName
    titleFont.setColor(canAfford ? Color.WHITE : Color.ORANGE);
    titleFont.draw(batch, node.displayName(), DIALOG_X + 40, DIALOG_Y + DIALOG_H - 40);

    // コスト + 所持
    bodyFont.setColor(canAfford ? Color.WHITE : Color.ORANGE);
    bodyFont.draw(
        batch,
        "コスト: ソウル " + node.soulCost() + "  /  所持: " + currentSoul,
        DIALOG_X + 40,
        DIALOG_Y + DIALOG_H - 100);

    // アイコン or カード画像 (中央配置)
    if (iconTexture != null) {
      int imgW = isCardGrant ? 240 : 200;
      int imgH = isCardGrant ? 336 : 200;
      int imgX = DIALOG_X + (DIALOG_W - imgW) / 2;
      int imgY = DIALOG_Y + 180;
      batch.setColor(Color.WHITE);
      batch.draw(iconTexture, imgX, imgY, imgW, imgH);
    }

    // ヒント (下段)
    bodyFont.setColor(canAfford ? new Color(0.6f, 1f, 0.6f, 1f) : Color.GRAY);
    bodyFont.draw(batch, "[Y] 解放する   [N] / [ESC] やめる", DIALOG_X + 40, DIALOG_Y + 60);
    if (!canAfford) {
      bodyFont.setColor(Color.ORANGE);
      bodyFont.draw(batch, "ソウルが不足しています", DIALOG_X + 40, DIALOG_Y + 120);
    }
    batch.setColor(Color.WHITE);
    batch.end();
  }

  public void resize(int width, int height) {
    viewport.update(width, height, true);
  }

  /** 完了状態なら一度だけ true (解放) / false (キャンセル) を返す。連続呼び出しでは empty。 */
  public Optional<Boolean> consume() {
    if (!done) {
      return Optional.empty();
    }
    Optional<Boolean> r = result;
    done = false;
    result = Optional.empty();
    return r;
  }

  /** ダイアログが Y/N/ESC で完了したか (consume 前の確認用)。 */
  public boolean isFinished() {
    return done;
  }

  public void dispose() {
    if (batch != null) {
      batch.dispose();
    }
    if (shapes != null) {
      shapes.dispose();
    }
  }
}
