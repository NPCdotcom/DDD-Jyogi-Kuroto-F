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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * ゲーム本編画面。
 *
 * <p>毎フレーム: 入力受付 → TurnEngine 解決 → 描画 → フェーズ遷移チェック の流れで動く。 描画と入力以外のロジックは {@link TurnDirector}
 * に委譲する。
 *
 * <p>{@code TurnDirector} と {@code Fonts} は {@link DddGame} 側で保持されている個体を都度参照する
 * (新ラン後に古い参照を見続ける事故を構造的に防ぐ)。
 *
 * <p>{@link PlayerInputs} はインスタンスとして保持し、2 ステートモデル (通常 / カード選択中) のカード選択状態を フレーム間で維持する。{@link
 * #show()} / {@link #hide()} / {@link #dispose()} でリセットする。
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

  /** §15-3 / §15-6: 強化個体撃破時のカード追加 UI (1 ノード選択 = 1 枚 DrawPile 追加)。 */
  private NodeChoicePopup eliteCardChoice;

  /** §15-9 / Shop silent fail 通知: 一時的なフラッシュメッセージ (~2 秒で消える)。 */
  private String flashMessage;

  private float flashTimer;

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
    processNewEvents(); // §15-5 / E-8: 新規 DamageDealt → popup + shake / EliteDefeated → カード追加 UI
    advanceEffects(delta); // popup の age 加算 + 期限切れ除去 + shake デクリメント + flash デクリメント
    handleEliteCardChoice(); // §15-3 / §15-6: Elite 撃破 popup の入力処理
    drawFrame();
    // Popup は HUD の上に重ねて描画する (CLEARED 中の前面 UI)。drawFrame() で batch.end() 済みのため、
    // Stage の SpriteBatch とは衝突しない (描画スタックの順序: ダンジョン → HUD → Popup)。
    if (nodeChoice != null) {
      nodeChoice.render(delta);
    }
    if (eliteCardChoice != null) {
      eliteCardChoice.render(delta);
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
    } else if (phase == TurnPhase.ROOM_CLEARED) {
      // §15-6: 途中部屋の階段踏破 → 同層の次部屋へ即遷移 (ポップアップなし)。
      game.advanceRoom();
      showFlash(
          game.fonts().isJapaneseAvailable()
              ? Strings.Ja.ROOM_ADVANCE_FLASH
              : Strings.En.ROOM_ADVANCE_FLASH);
    }
    // RUN_CLEARED / GAME_OVER は transitionIfGameOver() が画面遷移を担う。
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
              // §15-9 / Shop silent fail 通知: 適用前後の Player を比較し、変化が無ければ
              // Gold 不足等で apply が no-op だったと判断、HUD フラッシュで通知する。
              core.domain.entity.Player before = game.context().state().player();
              game.resolveLayerEndChoice(choice);
              core.domain.entity.Player after = game.context().state().player();
              if (choice instanceof LayerEndNode.Shop shop
                  && before.gold().amount() == after.gold().amount()
                  && before.cardPileState().drawPile().size()
                      == after.cardPileState().drawPile().size()) {
                showFlash("金貨が足りません (必要 " + shop.goldCost() + ")");
              }
              nodeChoice.dispose();
              nodeChoice = null;
            });
  }

  /**
   * §15-3 / §15-6: 強化個体撃破時のカード追加 UI 入力処理。{@link NodeChoicePopup} を流用し、 {@link
   * LayerEndNode.Shop}(goldCost=0, ...) を 3 つ提示 → 1 つ選択 → DrawPile に追加。
   */
  private void handleEliteCardChoice() {
    if (eliteCardChoice == null) {
      return;
    }
    for (int i = 0; i < NodeChoicePopup.CHOICE_COUNT; i++) {
      if (Gdx.input.isKeyJustPressed(numKey(i))) {
        eliteCardChoice.select(i);
      }
    }
    eliteCardChoice
        .consume()
        .ifPresent(
            choice -> {
              game.applyEliteCardReward(choice);
              eliteCardChoice.dispose();
              eliteCardChoice = null;
              if (choice instanceof LayerEndNode.Shop shop) {
                showFlash("カード獲得: " + shop.grantedCard().displayName());
              }
            });
  }

  /**
   * Elite 撃破時のカード追加候補 (3 種) を {@link NodeChoicePopup} で生成する。
   *
   * <p>候補プールはチームメイト提案カード 5+ 種からシャッフルで 3 つ選び、それぞれ {@link LayerEndNode.Shop}(goldCost=0, card)
   * でラップ。Gold 0 = 無料カード追加。
   */
  private NodeChoicePopup createEliteCardChoicePopup() {
    List<core.domain.card.Card> allRewards =
        new ArrayList<>(
            List.of(
                core.infrastructure.bootstrap.InitialStateFactory.emberShotCard(),
                core.infrastructure.bootstrap.InitialStateFactory.blazeNovaCard(),
                core.infrastructure.bootstrap.InitialStateFactory.blinkStepCard(),
                core.infrastructure.bootstrap.InitialStateFactory.flameCircleCard(),
                core.infrastructure.bootstrap.InitialStateFactory.ironSkinCard(),
                core.infrastructure.bootstrap.InitialStateFactory.arcaneVeilCard(),
                core.infrastructure.bootstrap.InitialStateFactory.fireballCard()));
    Collections.shuffle(allRewards, new Random());
    List<LayerEndNode> choices = new ArrayList<>();
    for (int i = 0; i < NodeChoicePopup.CHOICE_COUNT; i++) {
      choices.add(new LayerEndNode.Shop(0, allRewards.get(i)));
    }
    String title = "強化個体撃破: カード追加";
    // large(32px) 等倍。hud(16px)+setFontScale(2f) は Scene2D で漢字が黒四角化するため使えない。
    return new NodeChoicePopup(game.fonts().large(), title, List.copyOf(choices));
  }

  private void showFlash(String message) {
    this.flashMessage = message;
    this.flashTimer = 2.5f;
  }

  /**
   * 抽選 5 候補から 3 提示の {@link NodeChoicePopup} を生成する (§15-8 完成仕様縮退)。
   *
   * <p>§15-8 仕様の「4 種カテゴリ (ステ強化 / 休憩 / イベント / ショップ) から 3 提示」を、本セッション では 5 候補 (HpMaxUp / SpeedUp /
   * Rest / Shop / Event) から Random で 3 選に縮退。 候補プールに新 LayerEndNode を増やしたい場合は本メソッドの allCandidates
   * に追加するだけ。
   */
  private NodeChoicePopup createNodeChoicePopup() {
    List<LayerEndNode> allCandidates =
        new ArrayList<>(
            List.of(
                new LayerEndNode.HpMaxUp(5),
                new LayerEndNode.SpeedUp(1),
                new LayerEndNode.Rest(),
                new LayerEndNode.Shop(
                    5, core.infrastructure.bootstrap.InitialStateFactory.strongStrikeCard()),
                new LayerEndNode.Event(30, -5, 0, "ソウルの祠 (ソウル +30 / HP -5)")));
    Collections.shuffle(allCandidates, new Random());
    List<LayerEndNode> choices =
        List.copyOf(allCandidates.subList(0, NodeChoicePopup.CHOICE_COUNT));

    String title =
        game.fonts().isJapaneseAvailable()
            ? Strings.Ja.LAYER_END_TITLE
            : Strings.En.LAYER_END_TITLE;
    // large(32px) 等倍。hud(16px)+setFontScale(2f) は Scene2D で漢字が黒四角化するため使えない。
    return new NodeChoicePopup(game.fonts().large(), title, choices);
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
    drawFlash(batch);
    batch.end();
  }

  /** フラッシュメッセージを画面中央上部に描画 (Shop 失敗通知 / Elite カード獲得通知)。 */
  private void drawFlash(SpriteBatch batch) {
    if (flashMessage == null || flashTimer <= 0f) {
      return;
    }
    com.badlogic.gdx.graphics.g2d.BitmapFont font = game.fonts().large();
    float alpha = Math.min(1f, flashTimer / 0.5f); // 最後 0.5 秒でフェードアウト
    font.setColor(1f, 0.85f, 0.3f, alpha);
    font.draw(batch, flashMessage, 80f, RenderLayout.SCREEN_HEIGHT - 200f);
    font.setColor(Color.WHITE);
  }

  /**
   * 新規 {@link BattleEvent.DamageDealt} を検知してダメージポップアップを spawn + 画面シェイクをトリガする (§15-5 / E-8)。{@link
   * core.application.GameContext#totalEventsEmitted()} を前回観測値と比較し、 差分ぶんだけ {@code latestEvents}
   * で取得する。
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
      } else if (e instanceof BattleEvent.EliteDefeated && eliteCardChoice == null) {
        // §15-3 / §15-6: 強化個体撃破時にカード追加 UI を発火
        eliteCardChoice = createEliteCardChoicePopup();
      }
    }
    lastSeenEventCount = current;
  }

  /** popup の age 加算 + 期限切れ除去 + shake デクリメント + flash デクリメント (毎フレーム呼出)。 */
  private void advanceEffects(float delta) {
    if (!popups.isEmpty()) {
      popups.replaceAll(p -> p.advanced(delta));
      popups.removeIf(DamagePopup::isExpired);
    }
    if (shakeRemaining > 0f) {
      shakeRemaining = Math.max(0f, shakeRemaining - delta);
    }
    if (flashTimer > 0f) {
      flashTimer = Math.max(0f, flashTimer - delta);
      if (flashTimer <= 0f) {
        flashMessage = null;
      }
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
        RenderLayout.MAP_ORIGIN_X
            + tileX * RenderLayout.TILE_SIZE
            + RenderLayout.TILE_SIZE / 2f
            - 12f;
    float worldY =
        RenderLayout.MAP_ORIGIN_Y
            + tileY * RenderLayout.TILE_SIZE
            + RenderLayout.TILE_SIZE / 2f
            + 4f;
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

  /**
   * §15-5 色分け方針: 被弾=赤、暴力的 (>=8) =黄、それ以外=白。
   *
   * <p>閾値 8 は ADR-30 修正後の物攻 2 (素 1 + 装備 +1) で「斬撃 (基礎値 5) → ダメ 7」が 通常、「強打 (基礎値 4) + Buff 物攻 +2 → ダメ
   * 6」程度、「Elite 物攻 3 + 斬撃 → 8」が 暴力的ヒットの閾値になるよう設定。初期実装の 10 では装備 + Buff 込みでも届かなかった。
   */
  private static Color colorForDamage(boolean toPlayer, int amount) {
    if (toPlayer) {
      return new Color(1f, 0.4f, 0.35f, 1f);
    }
    if (amount >= 8) {
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
    } else if (phase == TurnPhase.RUN_CLEARED) {
      // §15-6: 最終層ボス撃破 = ラン勝利。クリア表示の GameOverScreen へ。
      game.setScreen(new GameOverScreen(game, true));
    }
    // CLEARED は層末ノード選択待ち、ROOM_CLEARED は部屋遷移待ち。どちらも updateState() で
    // 処理し、本メソッドでは画面遷移しない。
  }

  @Override
  public void resize(int width, int height) {
    // true でカメラ位置をリセット (FitViewport の黒帯を正しく配置)
    viewport.update(width, height, true);
    if (nodeChoice != null) {
      nodeChoice.resize(width, height);
    }
    if (eliteCardChoice != null) {
      eliteCardChoice.resize(width, height);
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
    if (eliteCardChoice != null) {
      eliteCardChoice.dispose();
      eliteCardChoice = null;
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
    if (eliteCardChoice != null) {
      eliteCardChoice.dispose();
      eliteCardChoice = null;
    }
    // playerInputs は LibGDX リソースを持たないので dispose 不要、reset のみ
    if (playerInputs != null) {
      playerInputs.reset();
    }
  }
}
