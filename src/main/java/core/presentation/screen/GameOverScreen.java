package core.presentation.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import core.presentation.render.Fonts;
import core.presentation.render.RenderLayout;
import core.presentation.render.Strings;

/**
 * ラン終了画面 (死亡 or クリア)。ENTER で新しいラン (タイトルに戻る) に遷移する。
 *
 * <p>GAME_DESIGN §5-3 の死亡時挙動に従い「ソウル・スキル・スキル枠は保持」だが、MVP では新ランで 完全初期化する (ソウル使用 UI
 * が未実装のため、保持しても活かす場面が無い)。これは MVP の YAGNI 判断。
 */
public final class GameOverScreen extends ScreenAdapter {

  private final DddGame game;
  private final boolean cleared;

  private int soulSnapshot;
  private OrthographicCamera camera;
  private Viewport viewport;
  private SpriteBatch batch;

  public GameOverScreen(DddGame game, boolean cleared) {
    this.game = game;
    this.cleared = cleared;
  }

  @Override
  public void show() {
    // ソウル数のスナップショットは show() 時点で確定させる (Screen ライフサイクル準拠)。
    soulSnapshot = game.context().state().player().soul().amount();
    camera = new OrthographicCamera();
    viewport = new FitViewport(RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT, camera);
    batch = new SpriteBatch();
  }

  @Override
  public void render(float delta) {
    ScreenUtils.clear(0.02f, 0.02f, 0.03f, 1f);
    viewport.apply();
    batch.setProjectionMatrix(camera.combined);

    Fonts fonts = game.fonts();
    boolean jp = fonts.isJapaneseAvailable();
    BitmapFont title = fonts.title();
    BitmapFont large = fonts.large();
    BitmapFont hud = fonts.hud();

    batch.begin();
    if (cleared) {
      title.setColor(0.9f, 0.85f, 0.4f, 1f);
      title.draw(batch, jp ? Strings.Ja.CLEARED_HEADER : Strings.En.CLEARED_HEADER, RenderLayout.GAMEOVER_HEADER_X, RenderLayout.GAMEOVER_HEADER_Y);
    } else {
      title.setColor(0.9f, 0.3f, 0.3f, 1f);
      title.draw(batch, jp ? Strings.Ja.GAME_OVER_HEADER : Strings.En.GAME_OVER_HEADER, RenderLayout.GAMEOVER_HEADER_X, RenderLayout.GAMEOVER_HEADER_Y);
    }

    large.setColor(Color.LIGHT_GRAY);
    String soulLabel = jp ? Strings.Ja.SOULS_KEPT : Strings.En.SOULS_KEPT;
    large.draw(batch, soulLabel + soulSnapshot, RenderLayout.GAMEOVER_SOUL_X, RenderLayout.GAMEOVER_SOUL_Y);

    hud.setColor(Color.GRAY);
    hud.draw(batch, jp ? Strings.Ja.NEW_RUN_HINT : Strings.En.NEW_RUN_HINT, RenderLayout.GAMEOVER_HINT_X, RenderLayout.GAMEOVER_HINT_Y);
    batch.end();

    if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
      game.startNewRun();
      game.setScreen(new TitleScreen(game));
    }
  }

  @Override
  public void resize(int width, int height) {
    // true でカメラ位置をリセット (FitViewport の黒帯を正しく配置)
    viewport.update(width, height, true);
  }

  @Override
  public void dispose() {
    if (batch != null) {
      batch.dispose();
    }
  }
}
