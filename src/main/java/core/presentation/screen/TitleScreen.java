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

    batch.begin();
    title.setColor(Color.WHITE);
    title.draw(batch, jp ? Strings.Ja.TITLE : Strings.En.TITLE, RenderLayout.TITLE_TEXT_X, RenderLayout.TITLE_TEXT_Y);

    large.setColor(Color.LIGHT_GRAY);
    large.draw(batch, jp ? Strings.Ja.SUBTITLE : Strings.En.SUBTITLE, RenderLayout.SUBTITLE_X, RenderLayout.SUBTITLE_Y);

    // §UI 拡大方針: 旧 hud() (16px) → large() (32px) で操作系を全て表示。large の行高は約 40px。
    final int largeLineHeight = 48;
    large.setColor(Color.LIGHT_GRAY);
    large.draw(batch, jp ? Strings.Ja.START_HINT : Strings.En.START_HINT, RenderLayout.START_HINT_X, RenderLayout.START_HINT_Y);

    // §15-7 / E-2: ソウルツリーへの動線。所持ソウルも表示してプレイヤーに解放可能性を示す。
    large.setColor(0.9f, 0.85f, 0.4f, 1f);
    large.draw(
        batch,
        (jp ? Strings.Ja.TITLE_OPEN_TREE_HINT_FORMAT : Strings.En.TITLE_OPEN_TREE_HINT_FORMAT)
            .formatted(game.playerSoul().amount()),
        RenderLayout.START_HINT_X,
        RenderLayout.START_HINT_Y - largeLineHeight);

    large.setColor(Color.GRAY);
    large.draw(batch, jp ? Strings.Ja.CONTROLS_HEADER : Strings.En.CONTROLS_HEADER, RenderLayout.CONTROLS_HEADER_X, RenderLayout.CONTROLS_HEADER_Y);
    large.draw(batch, jp ? Strings.Ja.CONTROLS_MOVE : Strings.En.CONTROLS_MOVE, RenderLayout.CONTROLS_X, RenderLayout.CONTROLS_ROW1_Y);
    large.draw(batch, jp ? Strings.Ja.CONTROLS_SKILL : Strings.En.CONTROLS_SKILL, RenderLayout.CONTROLS_X, RenderLayout.CONTROLS_ROW1_Y - largeLineHeight);
    large.draw(batch, jp ? Strings.Ja.CONTROLS_WAIT : Strings.En.CONTROLS_WAIT, RenderLayout.CONTROLS_X, RenderLayout.CONTROLS_ROW1_Y - largeLineHeight * 2);
    large.draw(batch, jp ? Strings.Ja.CONTROLS_END : Strings.En.CONTROLS_END, RenderLayout.CONTROLS_X, RenderLayout.CONTROLS_ROW1_Y - largeLineHeight * 3);
    batch.end();

    if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
      game.setScreen(new DungeonScreen(game));
    } else if (Gdx.input.isKeyJustPressed(Keys.T)) {
      game.setScreen(new SoulTreeScreen(game));
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
