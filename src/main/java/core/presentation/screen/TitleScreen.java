package core.presentation.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import core.presentation.render.Fonts;
import core.presentation.render.RenderLayout;
import core.presentation.render.Strings;

/** タイトル画面。ENTER でダンジョンに入る。 */
public final class TitleScreen extends ScreenAdapter {

  private final DddGame game;
  private OrthographicCamera camera;
  private SpriteBatch batch;

  public TitleScreen(DddGame game) {
    this.game = game;
  }

  @Override
  public void show() {
    camera = new OrthographicCamera();
    camera.setToOrtho(false, RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT);
    batch = new SpriteBatch();
  }

  @Override
  public void render(float delta) {
    ScreenUtils.clear(0.05f, 0.05f, 0.08f, 1f);
    camera.update();
    batch.setProjectionMatrix(camera.combined);

    Fonts fonts = game.fonts();
    boolean jp = fonts.isJapaneseAvailable();
    BitmapFont title = fonts.title();
    BitmapFont large = fonts.large();
    BitmapFont hud = fonts.hud();

    batch.begin();
    title.setColor(Color.WHITE);
    title.draw(batch, jp ? Strings.Ja.TITLE : Strings.En.TITLE, 200, 470);

    large.setColor(Color.LIGHT_GRAY);
    large.draw(batch, jp ? Strings.Ja.SUBTITLE : Strings.En.SUBTITLE, 240, 420);

    hud.setColor(Color.LIGHT_GRAY);
    hud.draw(batch, jp ? Strings.Ja.START_HINT : Strings.En.START_HINT, 320, 330);

    hud.setColor(Color.GRAY);
    hud.draw(batch, jp ? Strings.Ja.CONTROLS_HEADER : Strings.En.CONTROLS_HEADER, 370, 270);
    hud.draw(batch, jp ? Strings.Ja.CONTROLS_MOVE : Strings.En.CONTROLS_MOVE, 240, 240);
    hud.draw(batch, jp ? Strings.Ja.CONTROLS_SKILL : Strings.En.CONTROLS_SKILL, 240, 220);
    hud.draw(batch, jp ? Strings.Ja.CONTROLS_WAIT : Strings.En.CONTROLS_WAIT, 240, 200);
    hud.draw(batch, jp ? Strings.Ja.CONTROLS_END : Strings.En.CONTROLS_END, 240, 180);
    batch.end();

    if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
      game.setScreen(new DungeonScreen(game));
    }
  }

  @Override
  public void dispose() {
    if (batch != null) {
      batch.dispose();
    }
  }
}
