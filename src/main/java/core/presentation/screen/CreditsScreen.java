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
 * クレジット画面 (M2 提出チェックリスト「使用素材クレジット表示」)。
 *
 * <p>同梱フォント・素材・コードライセンスの簡易表示。ESC でタイトルへ戻る。 詳細な素材一覧は {@code LICENSES/INDEX.md} を参照。
 */
public final class CreditsScreen extends ScreenAdapter {

  private static final float LINE_Y_TOP = 880f;
  private static final float LINE_X = 160f;

  private final DddGame game;
  private OrthographicCamera camera;
  private Viewport viewport;
  private SpriteBatch batch;

  public CreditsScreen(DddGame game) {
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
    if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
      game.changeScreen(new TitleScreen(game));
      return;
    }

    ScreenUtils.clear(0.06f, 0.06f, 0.09f, 1f);
    viewport.apply();
    batch.setProjectionMatrix(camera.combined);

    Fonts fonts = game.fonts();
    boolean jp = fonts.isJapaneseAvailable();
    BitmapFont title = fonts.title();
    BitmapFont large = fonts.large();

    batch.begin();

    title.setColor(Color.WHITE);
    title.draw(batch, jp ? Strings.Ja.CREDITS_TITLE : Strings.En.CREDITS_TITLE, LINE_X, LINE_Y_TOP);

    String[] lines =
        jp
            ? new String[] {
              "",
              "フォント: DotGothic16",
              "    Copyright Fontworks Inc.",
              "    SIL Open Font License 1.1",
              "",
              "スキルカードアート: チームメイト供与 (2026-05-23)",
              "",
              "コード本体ライセンス: TBD (チーム決定)",
              "",
              "詳細は LICENSES/INDEX.md を参照"
            }
            : new String[] {
              "",
              "Font: DotGothic16",
              "    Copyright Fontworks Inc.",
              "    SIL Open Font License 1.1",
              "",
              "Skill card art: Provided by team (2026-05-23)",
              "",
              "Code license: TBD (team decision)",
              "",
              "See LICENSES/INDEX.md for details"
            };
    large.setColor(0.85f, 0.85f, 0.85f, 1f);
    float y = LINE_Y_TOP - 80f;
    for (String line : lines) {
      large.draw(batch, line, LINE_X, y);
      y -= RenderLayout.LARGE_LINE_HEIGHT;
    }

    large.setColor(0.7f, 0.7f, 0.78f, 1f);
    large.draw(
        batch, jp ? Strings.Ja.CREDITS_BACK_HINT : Strings.En.CREDITS_BACK_HINT, LINE_X, 80f);

    batch.end();
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
