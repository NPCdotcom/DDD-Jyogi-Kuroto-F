package core.presentation.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
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
import core.domain.layer.LayerEndNode;
import core.presentation.input.PlayerInputs;
import core.presentation.render.DungeonRenderer;
import core.presentation.render.HudRenderer;
import core.presentation.render.RenderLayout;
import core.presentation.render.Strings;
import core.presentation.window.NodeChoicePopup;
import java.util.List;
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
 *
 * <p>{@link NodeChoicePopup} は CLEARED 状態 (階段踏破直後) に lazy 初期化され、 1〜3 数字キーで選択された後 {@link
 * DddGame#resolveLayerEndChoice} → 自動的に次層へ遷移し、ポップアップは dispose される (§15-8 / E-6)。
 */
public final class DungeonScreen extends ScreenAdapter {

  private final DddGame game;

  private OrthographicCamera camera;
  private Viewport viewport;
  private SpriteBatch batch;
  private ShapeRenderer shapes;
  private PlayerInputs playerInputs;
  private NodeChoicePopup nodeChoice;

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
    // Popup は HUD の上に重ねて描画する (CLEARED 中の前面 UI)。drawFrame() で batch.end() 済みのため、
    // Stage の SpriteBatch とは衝突しない (描画スタックの順序: ダンジョン → HUD → Popup)。
    if (nodeChoice != null) {
      nodeChoice.render(delta);
    }
    transitionIfGameOver();
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
    } else if (phase == TurnPhase.CLEARED) {
      // 階段踏破直後の層末ノード選択フロー (§15-8 / E-6)。
      // この間 ENEMY_TURN への遷移は起きず、敵は静止する (CLEARED は層遷移の前段で全行動凍結)。
      handleLayerEndChoice();
    }
  }

  /**
   * CLEARED 状態の入力ハンドリング:
   *
   * <ol>
   *   <li>ポップアップが未生成なら固定 3 提示で lazy 初期化
   *   <li>数字キー 1/2/3 で {@link NodeChoicePopup#select(int)}
   *   <li>選択結果があれば {@link DddGame#resolveLayerEndChoice} を呼び、ポップアップを dispose
   * </ol>
   */
  private void handleLayerEndChoice() {
    if (nodeChoice == null) {
      nodeChoice = createNodeChoicePopup();
    }
    for (int i = 0; i < NodeChoicePopup.CHOICE_COUNT; i++) {
      if (Gdx.input.isKeyJustPressed(numKey(i))) {
        nodeChoice.select(i);
      }
    }
    nodeChoice
        .consume()
        .ifPresent(
            choice -> {
              game.resolveLayerEndChoice(choice);
              nodeChoice.dispose();
              nodeChoice = null;
            });
  }

  /**
   * 固定 3 提示の {@link NodeChoicePopup} を生成する (§15-8 ミニマム実装、E-6)。
   *
   * <p>抽選ロジック (4 種から Random で 3 提示) は M2 送り、本 PR では「HP +5 / 速度 +1 / HP 全回復」の 3 つで固定。
   * タイトルは Fonts の日英判定で {@link Strings} の Ja/En を解決して渡す。
   */
  private NodeChoicePopup createNodeChoicePopup() {
    List<LayerEndNode> choices =
        List.of(new LayerEndNode.HpMaxUp(5), new LayerEndNode.SpeedUp(1), new LayerEndNode.Rest());
    String title =
        game.fonts().isJapaneseAvailable() ? Strings.Ja.LAYER_END_TITLE : Strings.En.LAYER_END_TITLE;
    return new NodeChoicePopup(game.fonts().hud(), title, choices);
  }

  /** 0-indexed の i (0〜2) を NUM_1〜NUM_3 にマップ。範囲外は IAE。 */
  private static int numKey(int i) {
    return switch (i) {
      case 0 -> Keys.NUM_1;
      case 1 -> Keys.NUM_2;
      case 2 -> Keys.NUM_3;
      default -> throw new IllegalArgumentException("i must be 0..2: " + i);
    };
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

  private void transitionIfGameOver() {
    TurnPhase phase = game.context().state().phase();
    if (phase == TurnPhase.GAME_OVER) {
      game.setScreen(new GameOverScreen(game, false));
    }
    // CLEARED は階段踏破直後の「層末ノード選択待ち」状態 (§15-8 / E-6)。
    // 画面遷移は行わず、updateState() → handleLayerEndChoice() で選択 → 次層遷移を発火する。
    // GameOverScreen(game, true) の呼出は最終層クリア概念が定義されるまで保留 (現状最終層なし)。
  }

  @Override
  public void resize(int width, int height) {
    // true でカメラ位置をリセット (FitViewport の黒帯を正しく配置)
    viewport.update(width, height, true);
    if (nodeChoice != null) {
      nodeChoice.resize(width, height);
    }
  }

  @Override
  public void hide() {
    // 画面非表示時にカード選択状態 + ノード選択ポップアップをリセット (再表示時の誤入力対策)
    if (playerInputs != null) {
      playerInputs.reset();
    }
    if (nodeChoice != null) {
      nodeChoice.dispose();
      nodeChoice = null;
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
    if (nodeChoice != null) {
      nodeChoice.dispose();
      nodeChoice = null;
    }
    // playerInputs は LibGDX リソースを持たないので dispose 不要、reset のみ
    if (playerInputs != null) {
      playerInputs.reset();
    }
  }
}
