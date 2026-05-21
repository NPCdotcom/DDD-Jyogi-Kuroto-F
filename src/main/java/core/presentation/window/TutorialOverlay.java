package core.presentation.window;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import core.presentation.render.RenderLayout;
import java.util.Objects;

/**
 * 初回起動チュートリアル (§15-10 / E-10)。{@link NodeChoicePopup} と同じ Scene2D Window 構成で、 タイトル / 本文 (複数行) /
 * 閉じるヒント を 1 枚にまとめて表示。閉じる入力 (ENTER/ESC) の検知は 親 (TitleScreen) が担当する (Scene2D Actor input
 * を使わず外部キー入力主体、NodeChoicePopup と同型)。
 */
public final class TutorialOverlay implements Disposable {

  private final Stage stage;
  private final Skin skin;

  /**
   * @param font HUD と同じ {@link BitmapFont} を使い回す想定。Label.setFontScale(2f) で 32px 相当に拡大
   * @param title Window タイトル (例「操作説明」)
   * @param body 本文 (改行可、複数行)
   * @param closeHint 閉じる方法のヒント文 (例「ENTER または ESC で閉じる」)
   */
  public TutorialOverlay(BitmapFont font, String title, String body, String closeHint) {
    Objects.requireNonNull(font, "font");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(body, "body");
    Objects.requireNonNull(closeHint, "closeHint");

    Viewport viewport = new FitViewport(RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT);
    this.stage = new Stage(viewport);
    this.skin = new Skin();
    skin.add("default-font", font);

    Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
    skin.add("default", labelStyle);

    Window.WindowStyle windowStyle = new Window.WindowStyle();
    windowStyle.titleFont = font;
    windowStyle.titleFontColor = new Color(0.95f, 0.85f, 0.3f, 1f);
    windowStyle.background = solidDrawable("popup-bg", new Color(0.08f, 0.08f, 0.12f, 0.95f));
    skin.add("default", windowStyle);

    Window window = new Window(title, windowStyle);
    window.setMovable(false);
    window.setResizable(false);
    window.padTop(112f);
    window.padLeft(56f).padRight(56f).padBottom(40f);
    window.getTitleLabel().setFontScale(2f);

    Label content = new Label(body, labelStyle);
    content.setColor(Color.WHITE);
    content.setFontScale(2f);
    window.add(content).left().padBottom(28f).row();

    Label hint = new Label(closeHint, labelStyle);
    hint.setColor(new Color(0.65f, 0.65f, 0.7f, 1f));
    hint.setFontScale(2f);
    window.add(hint).left().padTop(16f).row();

    window.pack();
    window.setPosition(
        (RenderLayout.SCREEN_WIDTH - window.getWidth()) / 2f,
        (RenderLayout.SCREEN_HEIGHT - window.getHeight()) / 2f);
    stage.addActor(window);
  }

  /** Stage.act + Stage.draw を 1 フレーム分実行。TitleScreen.render から呼ぶ。 */
  public void render(float delta) {
    stage.act(delta);
    stage.draw();
  }

  /** Viewport を更新 (ウィンドウ解像度変更時)。 */
  public void resize(int width, int height) {
    stage.getViewport().update(width, height, true);
  }

  @Override
  public void dispose() {
    stage.dispose();
    skin.dispose();
  }

  private TextureRegionDrawable solidDrawable(String name, Color color) {
    Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pm.setColor(color);
    pm.fill();
    Texture tex = new Texture(pm);
    pm.dispose();
    skin.add(name, tex);
    return new TextureRegionDrawable(new TextureRegion(tex));
  }
}
