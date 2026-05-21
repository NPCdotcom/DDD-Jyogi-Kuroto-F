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
import core.presentation.window.TutorialOverlay;

/** タイトル画面。ENTER でダンジョンに入る。 */
public final class TitleScreen extends ScreenAdapter {

  private final DddGame game;
  private OrthographicCamera camera;
  private Viewport viewport;
  private SpriteBatch batch;

  /** §15-10 / E-10: 初回起動時のチュートリアル overlay (閉じたら null)。 */
  private TutorialOverlay tutorial;

  public TitleScreen(DddGame game) {
    this.game = game;
  }

  @Override
  public void show() {
    camera = new OrthographicCamera();
    viewport = new FitViewport(RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT, camera);
    batch = new SpriteBatch();
    // §15-10 / E-10: 初回のみチュートリアル overlay を表示
    if (!game.isTutorialSeen()) {
      boolean jp = game.fonts().isJapaneseAvailable();
      tutorial =
          new TutorialOverlay(
              game.fonts().hud(),
              jp ? Strings.Ja.TUTORIAL_TITLE : Strings.En.TUTORIAL_TITLE,
              jp ? Strings.Ja.TUTORIAL_BODY : Strings.En.TUTORIAL_BODY,
              jp ? Strings.Ja.TUTORIAL_CLOSE_HINT : Strings.En.TUTORIAL_CLOSE_HINT);
    }
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
    title.draw(
        batch,
        jp ? Strings.Ja.TITLE : Strings.En.TITLE,
        RenderLayout.TITLE_TEXT_X,
        RenderLayout.TITLE_TEXT_Y);

    large.setColor(Color.LIGHT_GRAY);
    large.draw(
        batch,
        jp ? Strings.Ja.SUBTITLE : Strings.En.SUBTITLE,
        RenderLayout.SUBTITLE_X,
        RenderLayout.SUBTITLE_Y);

    // §UI 拡大方針: 旧 hud() (16px) → large() (32px) で操作系を全て表示。
    // 行高 / 各 Y 座標は RenderLayout に集約 (CONTROLS_HEADER_Y と TITLE_OPEN_TREE_HINT_Y の衝突を回避)。
    large.setColor(Color.LIGHT_GRAY);
    large.draw(
        batch,
        jp ? Strings.Ja.START_HINT : Strings.En.START_HINT,
        RenderLayout.START_HINT_X,
        RenderLayout.START_HINT_Y);

    // §15-7 / E-2: ソウルツリーへの動線。1 周目 (runCount 0) は非表示、
    // 1 周目終了後に解禁する。所持ソウルも表示して解放可能性を示す。
    if (game.runCount() >= 1) {
      large.setColor(0.9f, 0.85f, 0.4f, 1f);
      large.draw(
          batch,
          (jp ? Strings.Ja.TITLE_OPEN_TREE_HINT_FORMAT : Strings.En.TITLE_OPEN_TREE_HINT_FORMAT)
              .formatted(game.playerSoul().amount()),
          RenderLayout.START_HINT_X,
          RenderLayout.TITLE_OPEN_TREE_HINT_Y);
    }

    // §15-3: カード図鑑への動線 (周回数に関わらず常時アクセス可)。
    large.setColor(0.6f, 0.8f, 0.95f, 1f);
    large.draw(
        batch,
        jp ? Strings.Ja.TITLE_OPEN_COLLECTION_HINT : Strings.En.TITLE_OPEN_COLLECTION_HINT,
        RenderLayout.START_HINT_X,
        RenderLayout.TITLE_OPEN_TREE_HINT_Y - RenderLayout.LARGE_LINE_HEIGHT);

    large.setColor(Color.GRAY);
    large.draw(
        batch,
        jp ? Strings.Ja.CONTROLS_HEADER : Strings.En.CONTROLS_HEADER,
        RenderLayout.CONTROLS_HEADER_X,
        RenderLayout.CONTROLS_HEADER_Y);
    large.draw(
        batch,
        jp ? Strings.Ja.CONTROLS_MOVE : Strings.En.CONTROLS_MOVE,
        RenderLayout.CONTROLS_X,
        RenderLayout.CONTROLS_ROW1_Y);
    large.draw(
        batch,
        jp ? Strings.Ja.CONTROLS_SKILL : Strings.En.CONTROLS_SKILL,
        RenderLayout.CONTROLS_X,
        RenderLayout.CONTROLS_ROW1_Y - RenderLayout.CONTROLS_LINE_HEIGHT);
    large.draw(
        batch,
        jp ? Strings.Ja.CONTROLS_WAIT : Strings.En.CONTROLS_WAIT,
        RenderLayout.CONTROLS_X,
        RenderLayout.CONTROLS_ROW1_Y - RenderLayout.CONTROLS_LINE_HEIGHT * 2);
    large.draw(
        batch,
        jp ? Strings.Ja.CONTROLS_END : Strings.En.CONTROLS_END,
        RenderLayout.CONTROLS_X,
        RenderLayout.CONTROLS_ROW1_Y - RenderLayout.CONTROLS_LINE_HEIGHT * 3);
    batch.end();

    // §15-10 / E-10: チュートリアル overlay は HUD 描画の最後に重ねる。
    // 表示中は ENTER でダンジョン遷移せず、overlay を閉じる動線を優先する。
    if (tutorial != null) {
      tutorial.render(delta);
      if (Gdx.input.isKeyJustPressed(Keys.ENTER) || Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
        tutorial.dispose();
        tutorial = null;
        game.markTutorialSeen();
      }
      return;
    }

    if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
      // §15-7 / E-2: ラン開始の瞬間にここで startNewRun() を呼ぶ (ソウル消失バグの根治)。
      // ラン外で貯めた playerSoul はこの時点で Player に注入される。
      game.startNewRun();
      game.setScreen(new DungeonScreen(game));
    } else if (Gdx.input.isKeyJustPressed(Keys.T) && game.runCount() >= 1) {
      // 1 周目 (runCount 0) はソウルツリー非アクセス、1 周目終了後に解禁。
      game.setScreen(new SoulTreeScreen(game));
    } else if (Gdx.input.isKeyJustPressed(Keys.C)) {
      // §15-3: カード図鑑はいつでもアクセス可。
      game.setScreen(new CardCollectionScreen(game));
    }
  }

  @Override
  public void resize(int width, int height) {
    // true でカメラ位置をリセット (FitViewport の黒帯を正しく配置)
    viewport.update(width, height, true);
    if (tutorial != null) {
      tutorial.resize(width, height);
    }
  }

  @Override
  public void dispose() {
    if (batch != null) {
      batch.dispose();
    }
    if (tutorial != null) {
      tutorial.dispose();
      tutorial = null;
    }
  }
}
