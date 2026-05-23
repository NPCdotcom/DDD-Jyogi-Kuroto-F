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
import core.domain.entity.EnemyKind;
import core.domain.meta.Bestiary;
import core.presentation.render.RenderLayout;
import core.presentation.render.Strings;

/**
 * 撃破済み敵一覧画面 (W4-δ)。EnemyKind の全 5 種を縦スクロールリストで表示し、撃破済みは名前 / ソウル / 金貨を、未撃破は伏せ字で表示する。タイトル画面から B
 * キーで開く。
 *
 * <p>スクロール実装は {@link CardCollectionScreen} と同パターン ({@link #LIST_TOP_Y} / {@link #LIST_BOTTOM_Y} /
 * {@link #ROW_HEIGHT} / {@link #SCROLL_SPEED} / {@link #scrollOffset} / {@link #maxScroll})。
 */
public final class BestiaryScreen extends ScreenAdapter {

  private static final float ROW_HEIGHT = 50f;
  private static final float LIST_X = 120f;
  private static final float LIST_TOP_Y = 930f;
  private static final float LIST_BOTTOM_Y = 110f;
  private static final float SCROLL_SPEED = 1100f;

  private static final Color COLOR_DEFEATED = Color.WHITE;
  private static final Color COLOR_LOCKED = new Color(0.38f, 0.38f, 0.44f, 1f);

  private final DddGame game;
  private final EnemyKind[] allKinds;

  private OrthographicCamera camera;
  private Viewport viewport;
  private SpriteBatch batch;
  private float scrollOffset;
  private float maxScroll;

  public BestiaryScreen(DddGame game) {
    this.game = game;
    this.allKinds = EnemyKind.values();
  }

  @Override
  public void show() {
    camera = new OrthographicCamera();
    viewport = new FitViewport(RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT, camera);
    batch = new SpriteBatch();
    float listSpan = allKinds.length * ROW_HEIGHT;
    maxScroll = Math.max(0f, listSpan - (LIST_TOP_Y - LIST_BOTTOM_Y));
  }

  @Override
  public void render(float delta) {
    if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
      game.changeScreen(new TitleScreen(game));
      return;
    }
    handleScroll(delta);

    ScreenUtils.clear(0.06f, 0.06f, 0.09f, 1f);
    viewport.apply();
    batch.setProjectionMatrix(camera.combined);
    batch.begin();
    drawList(batch);
    drawHeaderAndFooter(batch);
    batch.end();
  }

  private void handleScroll(float delta) {
    float dy = 0f;
    if (Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.S)) {
      dy += SCROLL_SPEED * delta;
    }
    if (Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W)) {
      dy -= SCROLL_SPEED * delta;
    }
    scrollOffset = Math.max(0f, Math.min(maxScroll, scrollOffset + dy));
  }

  private void drawList(SpriteBatch batch) {
    boolean jp = game.fonts().isJapaneseAvailable();
    BitmapFont font = game.fonts().large();
    Bestiary bestiary = game.progress().bestiary();

    for (int i = 0; i < allKinds.length; i++) {
      float y = LIST_TOP_Y - i * ROW_HEIGHT + scrollOffset;
      if (y < LIST_BOTTOM_Y || y > LIST_TOP_Y + ROW_HEIGHT) {
        continue; // リスト帯の外 → 描かない
      }
      EnemyKind kind = allKinds[i];
      if (bestiary.isRegistered(kind)) {
        font.setColor(COLOR_DEFEATED);
        font.draw(
            batch,
            "%s   Soul %d   Gold %d"
                .formatted(kind.displayName(), kind.soulReward(), kind.goldReward()),
            LIST_X,
            y);
      } else {
        font.setColor(COLOR_LOCKED);
        font.draw(batch, jp ? Strings.Ja.BESTIARY_LOCKED : Strings.En.BESTIARY_LOCKED, LIST_X, y);
      }
    }
    font.setColor(Color.WHITE);
  }

  private void drawHeaderAndFooter(SpriteBatch batch) {
    boolean jp = game.fonts().isJapaneseAvailable();
    BitmapFont font = game.fonts().large();
    Bestiary bestiary = game.progress().bestiary();

    font.setColor(0.95f, 0.85f, 0.3f, 1f);
    font.draw(
        batch,
        "%s   %d / %d"
            .formatted(
                jp ? Strings.Ja.BESTIARY_TITLE : Strings.En.BESTIARY_TITLE,
                bestiary.size(),
                allKinds.length),
        LIST_X,
        RenderLayout.SCREEN_HEIGHT - 28f);

    font.setColor(0.7f, 0.7f, 0.78f, 1f);
    font.draw(batch, jp ? Strings.Ja.BESTIARY_HINT : Strings.En.BESTIARY_HINT, LIST_X, 58f);
    font.setColor(Color.WHITE);
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
