package core.presentation.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import core.application.TurnDirector;
import core.domain.battle.BattleAction;
import core.domain.battle.BattleEvent;
import core.domain.battle.TurnPhase;
import core.domain.dungeon.DungeonState;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.layer.LayerEndNode;
import core.presentation.effect.DamagePopup;
import core.presentation.input.PlayerInputs;
import core.presentation.render.DungeonRenderer;
import core.presentation.render.HudRenderer;
import core.presentation.render.RenderLayout;
import core.presentation.render.Strings;
import core.presentation.window.NodeChoicePopup;
import java.util.ArrayList;
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

  /** 画面シェイクの継続時間 (秒、§15-5)。被弾 / 与ダメ共通。 */
  private static final float SHAKE_DURATION = 0.18f;

  /** 与ダメ時のシェイク振幅 (px)。 */
  private static final float SHAKE_AMP_DEAL = 6f;

  /** 被弾時のシェイク振幅 (px)。プレイヤーへのフィードバックを強調するため与ダメより大きい。 */
  private static final float SHAKE_AMP_RECEIVE = 14f;

  /** ダメージポップアップの最大同時表示数 (古いものから破棄、§15-5 描画コスト上限)。 */
  private static final int MAX_POPUPS = 16;

  private final DddGame game;

  /** ダメージポップアップ群 (§15-5 / E-8)。BattleEvent.DamageDealt 発火で push、isExpired で除去。 */
  private final List<DamagePopup> popups = new ArrayList<>();

  /** GameContext.totalEventsEmitted の前回観測値。新規 DamageDealt 検知のカーソル。 */
  private long lastSeenEventCount = 0;

  /** 画面シェイクの残り秒数 (0 で停止)。 */
  private float shakeRemaining = 0f;

  /** 画面シェイクの振幅 (px、被弾 / 与ダメで切替)。 */
  private float shakeAmplitude = 0f;

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
    processNewEvents();   // §15-5 / E-8: 新規 DamageDealt → popup + shake
    advanceEffects(delta); // popup の age 加算 + 期限切れ除去 + shake デクリメント
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
    // §15-5 / E-8: 画面シェイク。残り時間に応じて振幅を線形減衰させ、ランダム角度で揺らす。
    applyCameraShake();

    ScreenUtils.clear(0.08f, 0.08f, 0.1f, 1f);
    viewport.apply();
    shapes.setProjectionMatrix(camera.combined);
    batch.setProjectionMatrix(camera.combined);

    DungeonRenderer.draw(shapes, game.context().state());

    batch.begin();
    HudRenderer.draw(batch, game.fonts(), game.context(), playerInputs.pendingCardIndex());
    drawPopups(batch);
    batch.end();
  }

  /**
   * 新規 {@link BattleEvent.DamageDealt} を検知してダメージポップアップを spawn + 画面シェイクをトリガする
   * (§15-5 / E-8)。{@link core.application.GameContext#totalEventsEmitted()} を前回観測値と比較し、
   * 差分ぶんだけ {@code latestEvents} で取得する。
   */
  private void processNewEvents() {
    long current = game.context().totalEventsEmitted();
    if (current <= lastSeenEventCount) {
      return;
    }
    int delta = (int) Math.min(current - lastSeenEventCount, 64L);
    List<BattleEvent> newEvents = game.context().latestEvents(delta);
    for (BattleEvent e : newEvents) {
      if (e instanceof BattleEvent.DamageDealt d) {
        spawnPopup(d);
        triggerShake(d);
      }
    }
    lastSeenEventCount = current;
  }

  /** popup の age 加算 + 期限切れ除去 + shake デクリメント (毎フレーム呼出)。 */
  private void advanceEffects(float delta) {
    if (!popups.isEmpty()) {
      popups.replaceAll(p -> p.advanced(delta));
      popups.removeIf(DamagePopup::isExpired);
    }
    if (shakeRemaining > 0f) {
      shakeRemaining = Math.max(0f, shakeRemaining - delta);
    }
  }

  /** DamageDealt から target タイル座標を逆引きして popup を spawn する。 */
  private void spawnPopup(BattleEvent.DamageDealt d) {
    DungeonState s = game.context().state();
    int tileX;
    int tileY;
    boolean toPlayer = d.to().equals(s.player().id());
    if (toPlayer) {
      tileX = s.player().position().x();
      tileY = s.player().position().y();
    } else {
      Optional<Enemy> enemyOpt = s.findEnemy(d.to());
      if (enemyOpt.isEmpty()) {
        return; // 既に死亡で除去された敵 → popup スキップ
      }
      tileX = enemyOpt.get().position().x();
      tileY = enemyOpt.get().position().y();
    }
    float worldX =
        RenderLayout.MAP_ORIGIN_X + tileX * RenderLayout.TILE_SIZE + RenderLayout.TILE_SIZE / 2f - 12f;
    float worldY =
        RenderLayout.MAP_ORIGIN_Y + tileY * RenderLayout.TILE_SIZE + RenderLayout.TILE_SIZE / 2f + 4f;
    Color color = colorForDamage(toPlayer, d.damage());
    if (popups.size() >= MAX_POPUPS) {
      popups.remove(0);
    }
    popups.add(new DamagePopup(worldX, worldY, d.damage(), 0f, color));
  }

  /** 被弾は強シェイク、与ダメは弱シェイクをセット。残り時間は最大値で上書き (連続ヒット時の強調)。 */
  private void triggerShake(BattleEvent.DamageDealt d) {
    ActorId playerId = game.context().state().player().id();
    boolean toPlayer = d.to().equals(playerId);
    shakeAmplitude = toPlayer ? SHAKE_AMP_RECEIVE : SHAKE_AMP_DEAL;
    shakeRemaining = SHAKE_DURATION;
  }

  /** §15-5 色分け方針: 被弾=赤、暴力的 (>=10) =黄、それ以外=白。 */
  private static Color colorForDamage(boolean toPlayer, int amount) {
    if (toPlayer) {
      return new Color(1f, 0.4f, 0.35f, 1f);
    }
    if (amount >= 10) {
      return new Color(1f, 0.9f, 0.2f, 1f);
    }
    return new Color(1f, 1f, 1f, 1f);
  }

  /** カメラを揺らす (FitViewport の中心を基準に dx/dy を加算)。残り時間で線形減衰。 */
  private void applyCameraShake() {
    float dx = 0f;
    float dy = 0f;
    if (shakeRemaining > 0f) {
      float intensity = shakeAmplitude * (shakeRemaining / SHAKE_DURATION);
      float angle = (float) (Math.random() * Math.PI * 2.0);
      dx = (float) (Math.cos(angle) * intensity);
      dy = (float) (Math.sin(angle) * intensity);
    }
    camera.position.set(
        RenderLayout.SCREEN_WIDTH / 2f + dx, RenderLayout.SCREEN_HEIGHT / 2f + dy, 0f);
    camera.update();
  }

  /** popup 群を large フォントで描画 (HUD の上)。 */
  private void drawPopups(SpriteBatch batch) {
    if (popups.isEmpty()) {
      return;
    }
    BitmapFont font = game.fonts().large();
    for (DamagePopup p : popups) {
      Color c = p.baseColor();
      font.setColor(c.r, c.g, c.b, p.alpha());
      font.draw(batch, String.valueOf(p.amount()), p.worldX(), p.currentY());
    }
    font.setColor(Color.WHITE); // 後続描画への影響を避ける
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
