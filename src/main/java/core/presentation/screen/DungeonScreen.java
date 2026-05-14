package core.presentation.screen;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
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
 * <p>毎フレーム: 入力受付 → TurnEngine 解決 → 描画 → フェーズ遷移チェック の流れで動く。 描画と入力以外のロジックは {@link TurnDirector}
 * に委譲する。
 *
 * <p>{@code TurnDirector} と {@code Fonts} は {@link DddGame} 側で保持されている個体を都度参照する
 * (新ラン後に古い参照を見続ける事故を構造的に防ぐ)。
 */
public final class DungeonScreen extends ScreenAdapter {

  private final DddGame game;

  private OrthographicCamera camera;
  private SpriteBatch batch;
  private ShapeRenderer shapes;

  public DungeonScreen(DddGame game) {
    this.game = game;
  }

  @Override
  public void show() {
    camera = new OrthographicCamera();
    camera.setToOrtho(false, RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT);
    batch = new SpriteBatch();
    shapes = new ShapeRenderer();
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
      Optional<BattleAction> action = PlayerInputs.poll();
      action.ifPresent(director::applyPlayerAction);
    } else if (phase == TurnPhase.ENEMY_TURN) {
      director.runEnemyTurn();
    }
  }

  private void drawFrame() {
    ScreenUtils.clear(0.08f, 0.08f, 0.1f, 1f);
    camera.update();
    shapes.setProjectionMatrix(camera.combined);
    batch.setProjectionMatrix(camera.combined);

    DungeonRenderer.draw(shapes, game.context().state());

    batch.begin();
    HudRenderer.draw(batch, game.fonts(), game.context());
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
  public void dispose() {
    if (batch != null) {
      batch.dispose();
    }
    if (shapes != null) {
      shapes.dispose();
    }
  }
}
