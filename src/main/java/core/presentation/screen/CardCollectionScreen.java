package core.presentation.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import core.domain.card.Card;
import core.domain.card.CardElement;
import core.domain.card.CardId;
import core.infrastructure.bootstrap.CardImageRegistry;
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

  // §15-3 カード画像 (card_image_map.json) のサムネイル寸法。row 高さ 46 内に収まる程度。
  private static final float THUMB_WIDTH = 32f;
  private static final float THUMB_HEIGHT = 42f;
  private static final float TEXT_GAP = 12f;

  private static final Color OBTAINED_PHYSICAL = new Color(1f, 0.78f, 0.45f, 1f);
  private static final Color OBTAINED_MAGICAL = new Color(0.72f, 0.6f, 1f, 1f);
  private static final Color LOCKED = new Color(0.38f, 0.38f, 0.44f, 1f);

  private final DddGame game;
  private final List<Card> allCards;

  private OrthographicCamera camera;
  private Viewport viewport;
  private SpriteBatch batch;
  private float scrollOffset;
  private float maxScroll;

  public CardCollectionScreen(DddGame game) {
    this.game = game;
    // presentation → infrastructure 依存方向違反を解消するため、DddGame 経由で間接化。
    this.allCards = game.cardCatalog().all();
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
    Set<CardId> obtained = game.progress().obtainedCards();
    CardImageRegistry images = game.cardImageRegistry();
    float textX = LIST_X + THUMB_WIDTH + TEXT_GAP;
    for (int i = 0; i < allCards.size(); i++) {
      float y = LIST_TOP_Y - i * ROW_HEIGHT + scrollOffset;
      if (y < LIST_BOTTOM_Y || y > LIST_TOP_Y + ROW_HEIGHT) {
        continue; // リスト帯の外 → 描かない
      }
      Card card = allCards.get(i);
      boolean isObtained = obtained.contains(card.id());
      // §15-3: カード画像のサムネイル描画 (未取得は暗色シルエット)
      if (images != null) {
        Texture tex = images.get(card.id());
        if (tex != null) {
          if (isObtained) {
            batch.setColor(Color.WHITE);
          } else {
            batch.setColor(0.30f, 0.30f, 0.34f, 1f);
          }
          batch.draw(tex, LIST_X, y - 8f, THUMB_WIDTH, THUMB_HEIGHT);
          batch.setColor(Color.WHITE);
        }
      }
      if (isObtained) {
        font.setColor(
            card.element() == CardElement.PHYSICAL ? OBTAINED_PHYSICAL : OBTAINED_MAGICAL);
        font.draw(
            batch,
            "%s   AP%d   %s"
                .formatted(card.displayName(), card.apCost(), CardDescriber.describe(card)),
            textX,
            y);
      } else {
        font.setColor(LOCKED);
        font.draw(
            batch, jp ? Strings.Ja.COLLECTION_LOCKED : Strings.En.COLLECTION_LOCKED, textX, y);
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
                game.progress().obtainedCards().size(),
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
