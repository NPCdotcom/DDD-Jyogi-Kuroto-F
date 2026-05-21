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
import core.domain.card.Card;
import core.domain.card.CardElement;
import core.domain.card.CardId;
import core.infrastructure.bootstrap.InitialStateFactory;
import core.presentation.render.CardDescriber;
import core.presentation.render.RenderLayout;
import core.presentation.render.Strings;
import java.util.List;
import java.util.Set;

/**
 * カード図鑑 (§15-3)。cards.json の全カードを一覧表示する。入手済みカードは名前・AP・効果を表示し、未入手は伏せ字で表示する (解放条件 =
 * そのカードを入手すること)。タイトル画面から C キーで開く。
 *
 * <p>固定カメラ 1 本 + スクロールオフセットで縦スクロールを実現する。各行はオフセット込みの Y で描画し、リスト帯 (上端 {@link #LIST_TOP_Y} / 下端 {@link
 * #LIST_BOTTOM_Y}) の外に出た行は描かない。ヘッダ (件数) とフッタ (操作ヒント) はリスト帯の外側に固定描画する。
 */
public final class CardCollectionScreen extends ScreenAdapter {

  private static final float ROW_HEIGHT = 46f;
  private static final float LIST_X = 120f;
  private static final float LIST_TOP_Y = 930f;
  private static final float LIST_BOTTOM_Y = 110f;
  private static final float SCROLL_SPEED = 1100f;

  private static final Color OBTAINED_PHYSICAL = new Color(1f, 0.78f, 0.45f, 1f);
  private static final Color OBTAINED_MAGICAL = new Color(0.72f, 0.6f, 1f, 1f);
  private static final Color LOCKED = new Color(0.38f, 0.38f, 0.44f, 1f);

  private final DddGame game;
  private final List<Card> allCards = InitialStateFactory.cardCatalog().all();

  private OrthographicCamera camera;
  private Viewport viewport;
  private SpriteBatch batch;
  private float scrollOffset;
  private float maxScroll;

  public CardCollectionScreen(DddGame game) {
    this.game = game;
  }

  @Override
  public void show() {
    camera = new OrthographicCamera();
    viewport = new FitViewport(RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT, camera);
    batch = new SpriteBatch();
    float listSpan = allCards.size() * ROW_HEIGHT;
    maxScroll = Math.max(0f, listSpan - (LIST_TOP_Y - LIST_BOTTOM_Y));
  }

  @Override
  public void render(float delta) {
    if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
      dispose(); // setScreen は旧 Screen を dispose しないため明示的に解放 (LibGDX 規約)
      game.setScreen(new TitleScreen(game));
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
      dy += SCROLL_SPEED * delta; // 下キー = 後ろのカードを見る = リストを上へ送る
    }
    if (Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W)) {
      dy -= SCROLL_SPEED * delta;
    }
    scrollOffset = Math.max(0f, Math.min(maxScroll, scrollOffset + dy));
  }

  private void drawList(SpriteBatch batch) {
    boolean jp = game.fonts().isJapaneseAvailable();
    BitmapFont font = game.fonts().large();
    Set<CardId> obtained = game.obtainedCards();
    for (int i = 0; i < allCards.size(); i++) {
      float y = LIST_TOP_Y - i * ROW_HEIGHT + scrollOffset;
      if (y < LIST_BOTTOM_Y || y > LIST_TOP_Y + ROW_HEIGHT) {
        continue; // リスト帯の外 → 描かない
      }
      Card card = allCards.get(i);
      if (obtained.contains(card.id())) {
        font.setColor(
            card.element() == CardElement.PHYSICAL ? OBTAINED_PHYSICAL : OBTAINED_MAGICAL);
        font.draw(
            batch,
            "%s   AP%d   %s"
                .formatted(card.displayName(), card.apCost(), CardDescriber.describe(card)),
            LIST_X,
            y);
      } else {
        font.setColor(LOCKED);
        font.draw(
            batch, jp ? Strings.Ja.COLLECTION_LOCKED : Strings.En.COLLECTION_LOCKED, LIST_X, y);
      }
    }
    font.setColor(Color.WHITE);
  }

  private void drawHeaderAndFooter(SpriteBatch batch) {
    boolean jp = game.fonts().isJapaneseAvailable();
    BitmapFont font = game.fonts().large();
    font.setColor(0.95f, 0.85f, 0.3f, 1f);
    font.draw(
        batch,
        "%s    %d / %d"
            .formatted(
                jp ? Strings.Ja.COLLECTION_TITLE : Strings.En.COLLECTION_TITLE,
                game.obtainedCards().size(),
                allCards.size()),
        LIST_X,
        RenderLayout.SCREEN_HEIGHT - 28f);
    font.setColor(0.7f, 0.7f, 0.78f, 1f);
    font.draw(batch, jp ? Strings.Ja.COLLECTION_HINT : Strings.En.COLLECTION_HINT, LIST_X, 58f);
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
