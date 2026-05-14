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

/** タイトル画面。ENTER でダンジョンに入る。 */
public final class TitleScreen extends ScreenAdapter {

  private final DddGame game;
  private OrthographicCamera camera;
  private Viewport viewport;
  private SpriteBatch batch;

  public TitleScreen(DddGame game) {
    this.game = game;
  }

  @Override
  public void show() {
    camera = new OrthographicCamera();
    viewport = new FitViewport(RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT, camera);
    batch = new SpriteBatch();
  }

  @Override
  public void render(float delta) {
    ScreenUtils.clear(0.05f, 0.05f, 0.08f, 1f);
    viewport.apply();
    batch.setProjectionMatrix(camera.combined);

    Fonts fonts = game.fonts();
    boolean jp = fonts.isJapaneseAvailable();
    BitmapFont title = fonts.title();
    BitmapFont large = fonts.large();
    BitmapFont hud = fonts.hud();

    batch.begin();
    title.setColor(Color.WHITE);
    title.draw(batch, jp ? Strings.Ja.TITLE : Strings.En.TITLE, RenderLayout.TITLE_TEXT_X, RenderLayout.TITLE_TEXT_Y);

    large.setColor(Color.LIGHT_GRAY);
    large.draw(batch, jp ? Strings.Ja.SUBTITLE : Strings.En.SUBTITLE, RenderLayout.SUBTITLE_X, RenderLayout.SUBTITLE_Y);

    hud.setColor(Color.LIGHT_GRAY);
    hud.draw(batch, jp ? Strings.Ja.START_HINT : Strings.En.START_HINT, RenderLayout.START_HINT_X, RenderLayout.START_HINT_Y);

    hud.setColor(Color.GRAY);
    hud.draw(batch, jp ? Strings.Ja.CONTROLS_HEADER : Strings.En.CONTROLS_HEADER, RenderLayout.CONTROLS_HEADER_X, RenderLayout.CONTROLS_HEADER_Y);
    hud.draw(batch, jp ? Strings.Ja.CONTROLS_MOVE : Strings.En.CONTROLS_MOVE, RenderLayout.CONTROLS_X, RenderLayout.CONTROLS_ROW1_Y);
    hud.draw(batch, jp ? Strings.Ja.CONTROLS_SKILL : Strings.En.CONTROLS_SKILL, RenderLayout.CONTROLS_X, RenderLayout.CONTROLS_ROW1_Y - RenderLayout.CONTROLS_LINE_HEIGHT);
    hud.draw(batch, jp ? Strings.Ja.CONTROLS_WAIT : Strings.En.CONTROLS_WAIT, RenderLayout.CONTROLS_X, RenderLayout.CONTROLS_ROW1_Y - RenderLayout.CONTROLS_LINE_HEIGHT * 2);
    hud.draw(batch, jp ? Strings.Ja.CONTROLS_END : Strings.En.CONTROLS_END, RenderLayout.CONTROLS_X, RenderLayout.CONTROLS_ROW1_Y - RenderLayout.CONTROLS_LINE_HEIGHT * 3);
    batch.end();

    if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
      game.setScreen(new DungeonScreen(game));
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
