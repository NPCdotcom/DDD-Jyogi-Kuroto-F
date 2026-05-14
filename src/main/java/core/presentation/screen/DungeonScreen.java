package core.presentation.screen;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import core.application.TurnDirector;
import core.domain.battle.BattleAction;
import core.domain.battle.TurnPhase;
import core.presentation.input.PlayerInputs;
import core.presentation.render.DungeonRenderer;
import core.presentation.render.HudRenderer;
import core.presentation.render.RenderLayout;
import java.util.Optional;

/**
 * ゲーム本編画面。
 *
 * <p>毎フレーム: 入力受付 → TurnEngine 解決 → 描画 → フェーズ遷移チェック の流れで動く。 描画と入力以外のロジックは {@link TurnDirector} に委譲する。
 *
 * <p>{@code TurnDirector} と {@code Fonts} は {@link DddGame} 側で保持されている個体を都度参照する
 * (新ラン後に古い参照を見続ける事故を構造的に防ぐ)。
 *
 * <p>{@link PlayerInputs} はインスタンスとして保持し、2 ステートモデル (通常 / カード選択中) のカード選択状態を
 * フレーム間で維持する。{@link #show()} / {@link #hide()} / {@link #dispose()} でリセットする。
 */
public final class DungeonScreen extends ScreenAdapter {

  private final DddGame game;

  private OrthographicCamera camera;
  private Viewport viewport;
  private SpriteBatch batch;
  private ShapeRenderer shapes;
  private PlayerInputs playerInputs;

  public DungeonScreen(DddGame game) {
    this.game = game;
  }

  @Override
  public void show() {
    camera = new OrthographicCamera();
    viewport = new FitViewport(RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT, camera);
    batch = new SpriteBatch();
    shapes = new ShapeRenderer();
    playerInputs = new PlayerInputs();
  }

  @Override
  public void render(float delta) {
    updateState();
    drawFrame();
    transitionIfRunEnded();
  }

  private void updateState() {
    TurnDirector director = game.director();
    TurnPhase phase = game.context().state().phase();
    if (phase == TurnPhase.PLAYER_TURN) {
      // poll(state) に移行: 状態2 (移動権保持中) の判定にドメインの pendingMoveCount を使う (ADR-21)
      Optional<BattleAction> action = playerInputs.poll(game.context().state());
      action.ifPresent(director::applyPlayerAction);
    } else if (phase == TurnPhase.ENEMY_TURN) {
      director.runEnemyTurn();
    }
  }

  private void drawFrame() {
    ScreenUtils.clear(0.08f, 0.08f, 0.1f, 1f);
    viewport.apply();
    shapes.setProjectionMatrix(camera.combined);
    batch.setProjectionMatrix(camera.combined);

    DungeonRenderer.draw(shapes, game.context().state());

    batch.begin();
    HudRenderer.draw(batch, game.fonts(), game.context(), playerInputs.pendingCardIndex());
    batch.end();
  }

  private void transitionIfRunEnded() {
    TurnPhase phase = game.context().state().phase();
    if (phase == TurnPhase.GAME_OVER) {
      game.setScreen(new GameOverScreen(game, false));
    } else if (phase == TurnPhase.CLEARED) {
      game.setScreen(new GameOverScreen(game, true));
    }
  }

  @Override
  public void resize(int width, int height) {
    // true でカメラ位置をリセット (FitViewport の黒帯を正しく配置)
    viewport.update(width, height, true);
  }

  @Override
  public void hide() {
    // 画面非表示時にカード選択状態をリセット (再表示時に誤入力状態が残らないように)
    if (playerInputs != null) {
      playerInputs.reset();
    }
  }

  @Override
  public void dispose() {
    if (batch != null) {
      batch.dispose();
    }
    if (shapes != null) {
      shapes.dispose();
    }
    // playerInputs は LibGDX リソースを持たないので dispose 不要、reset のみ
    if (playerInputs != null) {
      playerInputs.reset();
    }
  }
}
