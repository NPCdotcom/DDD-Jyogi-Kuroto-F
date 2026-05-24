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
import core.infrastructure.save.Settings;
import core.infrastructure.save.UiPreset;
import core.presentation.render.ButtonBounds;
import core.presentation.render.Fonts;
import core.presentation.render.RenderLayout;
import core.presentation.render.Strings;

/**
 * 初回起動時の UI プリセット選択画面 (§15-1)。
 *
 * <p>{@code settings.json} が存在しない初回起動時のみ表示される。3 種のプリセット (MINIMAL / STANDARD / INFO_RICH) を
 * 数字キーまたはクリックで選択すると {@link Settings} を保存してタイトル画面へ遷移する。
 *
 * <p>2 回目以降の起動では {@link DddGame#create()} が既存設定をロードして直接 {@link TitleScreen} に遷移する。
 */
public final class FirstRunPresetScreen extends ScreenAdapter {

  private final DddGame game;
  private OrthographicCamera camera;
  private Viewport viewport;
  private SpriteBatch batch;

  private static final UiPreset[] PRESETS = UiPreset.values();

  public FirstRunPresetScreen(DddGame game) {
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
    ScreenUtils.clear(0.03f, 0.03f, 0.08f, 1f);
    viewport.apply();
    batch.setProjectionMatrix(camera.combined);

    Fonts fonts = game.fonts();
    boolean jp = fonts.isJapaneseAvailable();
    BitmapFont large = fonts.large();
    BitmapFont title = fonts.title();

    batch.begin();

    // タイトル
    title.setColor(Color.WHITE);
    title.draw(
        batch,
        jp ? Strings.Ja.FIRST_RUN_PRESET_TITLE : Strings.En.FIRST_RUN_PRESET_TITLE,
        RenderLayout.PRESET_TITLE_X,
        RenderLayout.PRESET_TITLE_Y);

    // プリセット選択肢
    for (int i = 0; i < PRESETS.length; i++) {
      UiPreset preset = PRESETS[i];
      int y = RenderLayout.PRESET_CHOICE_TOP_Y - i * RenderLayout.PRESET_CHOICE_HEIGHT;
      String numberPrefix = "[" + (i + 1) + "] ";
      String name = jp ? preset.displayNameJa() : preset.displayNameEn();
      String desc = jp ? preset.descriptionJa() : preset.descriptionEn();

      large.setColor(Color.YELLOW);
      large.draw(batch, numberPrefix + name, RenderLayout.PRESET_CHOICE_X, y);

      large.setColor(Color.LIGHT_GRAY);
      large.draw(
          batch, "    " + desc, RenderLayout.PRESET_CHOICE_X, y - RenderLayout.LARGE_LINE_HEIGHT);
    }

    // 操作ヒント
    large.setColor(Color.GRAY);
    large.draw(
        batch,
        jp ? Strings.Ja.FIRST_RUN_PRESET_HINT : Strings.En.FIRST_RUN_PRESET_HINT,
        RenderLayout.PRESET_CHOICE_X,
        RenderLayout.SETTINGS_HINT_Y);

    batch.end();

    handleInput();
  }

  private void handleInput() {
    if (Gdx.input.isKeyJustPressed(Keys.NUM_1)) {
      selectPreset(UiPreset.MINIMAL);
      return;
    }
    if (Gdx.input.isKeyJustPressed(Keys.NUM_2)) {
      selectPreset(UiPreset.STANDARD);
      return;
    }
    if (Gdx.input.isKeyJustPressed(Keys.NUM_3)) {
      selectPreset(UiPreset.INFO_RICH);
      return;
    }
    // Wave 14 W14-β: マウスクリックで該当プリセット行を選択
    if (Gdx.input.justTouched()) {
      for (int i = 0; i < PRESETS.length; i++) {
        int rowY = RenderLayout.PRESET_CHOICE_TOP_Y - i * RenderLayout.PRESET_CHOICE_HEIGHT;
        ButtonBounds bounds =
            new ButtonBounds(
                RenderLayout.PRESET_CHOICE_X - 20,
                rowY - RenderLayout.PRESET_CHOICE_HEIGHT + 8,
                900,
                RenderLayout.PRESET_CHOICE_HEIGHT);
        if (bounds.containsScreenInput(Gdx.input.getX(), Gdx.input.getY())) {
          selectPreset(PRESETS[i]);
          return;
        }
      }
    }
  }

  private void selectPreset(UiPreset preset) {
    Settings newSettings = Settings.DEFAULT.withUiPreset(preset);
    game.applySettings(newSettings);
    game.saveSettings();
    game.changeScreen(new TitleScreen(game));
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
  }
}
