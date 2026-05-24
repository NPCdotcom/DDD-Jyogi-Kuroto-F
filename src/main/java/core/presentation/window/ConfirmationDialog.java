package core.presentation.window;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import core.presentation.render.RenderLayout;
import java.util.Optional;

/**
 * 汎用 Yes/No 確認モーダルダイアログ (Wave 1 / M2 backlog B)。
 *
 * <p>Y キーで確定、N キーまたは ESC でキャンセル。シンプルな「タイトル + 本文 + ヒント」構成で、 ソウルツリーリセット / セーブ上書き / ラン放棄 等の汎用確認に使い回す。
 *
 * <p>SoulNodeUnlockDialog はコスト表示 + アイコン + canAfford ガードの特有処理があるため別クラスとして据え置く (将来共通化候補は
 * tasks/ai_log/lessons.md 参照)。
 *
 * <p>{@link SoulNodeUnlockDialog} と同型 API: {@code render(delta)} / {@code consume():
 * Optional<Boolean>} / {@code dispose()} / {@code resize(width, height)}。
 *
 * <p>viewport は LibGDX framework の resize() ライフサイクル外で動的生成されるため、コンストラクタで一度 {@code
 * update(Gdx.graphics.getWidth/Height, true)} を呼んで初期化する (commit e92cb23 教訓)。
 */
public final class ConfirmationDialog {

  private static final int DIALOG_W = 800;
  private static final int DIALOG_H = 400;
  private static final int DIALOG_X = (RenderLayout.SCREEN_WIDTH - DIALOG_W) / 2;
  private static final int DIALOG_Y = (RenderLayout.SCREEN_HEIGHT - DIALOG_H) / 2;

  private final BitmapFont titleFont;
  private final BitmapFont bodyFont;
  private final String title;
  private final String body;
  private final String hint;

  private final SpriteBatch batch;
  private final ShapeRenderer shapes;
  private final OrthographicCamera camera;
  private final FitViewport viewport;

  private Optional<Boolean> result = Optional.empty();
  private boolean done = false;

  /**
   * @param titleFont タイトル描画用フォント (large/title 推奨)
   * @param bodyFont 本文 + ヒント描画用フォント
   * @param title ダイアログタイトル (例: "ツリーリセット")
   * @param body 本文 (例: "解放した全ノードをリセットしてソウルを払い戻します。よろしいですか?")
   * @param hint 操作ヒント (例: "[Y] 確定 [N] / [ESC] やめる")
   */
  public ConfirmationDialog(
      BitmapFont titleFont, BitmapFont bodyFont, String title, String body, String hint) {
    this.titleFont = titleFont;
    this.bodyFont = bodyFont;
    this.title = title;
    this.body = body;
    this.hint = hint;
    this.batch = new SpriteBatch();
    this.shapes = new ShapeRenderer();
    this.camera = new OrthographicCamera();
    this.viewport = new FitViewport(RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT, camera);
    camera.setToOrtho(false, RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT);
    this.viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
  }

  /** 入力受付 + 描画。Y/N/ESC で {@code done = true} になる。 */
  public void render(float delta) {
    if (titleFont == null || bodyFont == null) {
      Gdx.app.error("ConfirmationDialog", "Fonts not initialized, skipping render");
      result = Optional.of(false);
      done = true;
      return;
    }
    if (!done) {
      if (Gdx.input.isKeyJustPressed(Keys.Y)) {
        result = Optional.of(true);
        done = true;
      } else if (Gdx.input.isKeyJustPressed(Keys.N) || Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
        result = Optional.of(false);
        done = true;
      }
    }

    viewport.apply();
    Gdx.gl.glEnable(GL20.GL_BLEND);
    shapes.setProjectionMatrix(camera.combined);
    shapes.begin(ShapeRenderer.ShapeType.Filled);
    shapes.setColor(0f, 0f, 0f, 0.75f);
    shapes.rect(0f, 0f, RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT);
    shapes.setColor(0.12f, 0.12f, 0.16f, 1f);
    shapes.rect(DIALOG_X, DIALOG_Y, DIALOG_W, DIALOG_H);
    shapes.setColor(0.7f, 0.7f, 0.8f, 1f);
    shapes.rect(DIALOG_X, DIALOG_Y + DIALOG_H - 4, DIALOG_W, 4);
    shapes.rect(DIALOG_X, DIALOG_Y, DIALOG_W, 4);
    shapes.rect(DIALOG_X, DIALOG_Y, 4, DIALOG_H);
    shapes.rect(DIALOG_X + DIALOG_W - 4, DIALOG_Y, 4, DIALOG_H);
    shapes.end();

    batch.setProjectionMatrix(camera.combined);
    batch.begin();
    titleFont.setColor(Color.WHITE);
    titleFont.draw(batch, title, DIALOG_X + 40, DIALOG_Y + DIALOG_H - 40);
    bodyFont.setColor(0.92f, 0.92f, 0.92f, 1f);
    bodyFont.draw(batch, body, DIALOG_X + 40, DIALOG_Y + DIALOG_H - 130, DIALOG_W - 80, -1, true);
    bodyFont.setColor(new Color(0.6f, 1f, 0.6f, 1f));
    bodyFont.draw(batch, hint, DIALOG_X + 40, DIALOG_Y + 60);
    batch.setColor(Color.WHITE);
    batch.end();
  }

  public void resize(int width, int height) {
    viewport.update(width, height, true);
  }

  /** 完了状態なら一度だけ true (確定) / false (キャンセル) を返す。連続呼び出しでは empty。 */
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
