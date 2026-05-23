package core.presentation.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import core.domain.battle.BattleEvent;
import core.domain.dungeon.DungeonState;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.Stats;
import core.presentation.effect.DamagePopup;
import core.presentation.effect.LowHpWarning;
import core.presentation.render.RenderLayout;
import core.presentation.render.UiTheme;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * DungeonScreen の副作用エフェクト群を集約するクラス (Wave 4 W4-β)。
 *
 * <p>DungeonScreen から以下を切り出し、render パイプラインを薄くする:
 *
 * <ul>
 *   <li>ダメージポップアップ ({@link DamagePopup} のライフサイクル管理)
 *   <li>画面シェイク (shake 残り時間 / 振幅)
 *   <li>フラッシュメッセージ (Shop 失敗通知 / Elite カード獲得通知)
 *   <li>HP 低下警告アニメ時刻
 * </ul>
 *
 * <p><b>描画ルール</b>: {@code drawPopups} / {@code drawFlash} は呼出側が {@code batch.begin()} 済みの前提で
 * 動作する。内部で {@code begin()} / {@code end()} を呼ばない。同様に {@code drawLowHpWarning} は {@code
 * shapes.begin(Filled)} 済みの前提。
 */
public final class ScreenEffects {

  /** 画面シェイクの継続時間 (秒、§15-5)。被弾 / 与ダメ共通。 */
  public static final float SHAKE_DURATION = 0.18f;

  /** 与ダメ時のシェイク振幅 (px)。 */
  public static final float SHAKE_AMP_DEAL = 6f;

  /** 被弾時のシェイク振幅 (px)。プレイヤーへのフィードバックを強調するため与ダメより大きい。 */
  public static final float SHAKE_AMP_RECEIVE = 14f;

  /** ダメージポップアップの最大同時表示数 (古いものから破棄、§15-5 描画コスト上限)。 */
  public static final int MAX_POPUPS = 16;

  /** ダメージポップアップ群 (§15-5 / E-8)。BattleEvent.DamageDealt 発火で push、isExpired で除去。 */
  private final List<DamagePopup> popups = new ArrayList<>();

  /** 画面シェイクの残り秒数 (0 で停止)。 */
  private float shakeRemaining = 0f;

  /** 画面シェイクの振幅 (px、被弾 / 与ダメで切替)。 */
  private float shakeAmplitude = 0f;

  /** §15-9 / Shop silent fail 通知: 一時的なフラッシュメッセージ (~2.5 秒で消える)。 */
  private String flashMessage;

  private float flashTimer;

  /** §7-2 / C-2: HP 低下警告フレームの脈動アニメ累積時間 (秒)。 */
  private float lowHpAnimTime = 0f;

  // -------------------------------------------------------------------------
  // 更新系
  // -------------------------------------------------------------------------

  /**
   * DamageDealt イベントからダメージポップアップを spawn し、画面シェイクをトリガする。
   *
   * <p>popup 生成と shake トリガを 1 メソッドに内包し、呼出側の記述量を削減する。
   *
   * @param d ダメージイベント
   * @param playerId プレイヤーの ActorId (被弾判定用)
   * @param state 現在の DungeonState (エネミー座標解決用)
   */
  public void spawnPopup(BattleEvent.DamageDealt d, ActorId playerId, DungeonState state) {
    spawnPopupInternal(d, playerId, state);
    triggerShakeInternal(d, playerId);
  }

  /** フラッシュメッセージを設定する。既存メッセージがあっても上書きする。 */
  public void showFlash(String message) {
    this.flashMessage = message;
    this.flashTimer = 2.5f;
  }

  /**
   * popup の age 加算 + 期限切れ除去 + shake デクリメント + flash デクリメント + lowHpAnimTime 更新 (毎フレーム呼出)。
   *
   * @param delta フレーム経過秒数
   */
  public void advanceEffects(float delta) {
    // popup age 更新と期限切れ除去
    if (!popups.isEmpty()) {
      popups.replaceAll(p -> p.advanced(delta));
      popups.removeIf(DamagePopup::isExpired);
    }
    // shake デクリメント
    if (shakeRemaining > 0f) {
      shakeRemaining = Math.max(0f, shakeRemaining - delta);
    }
    // flash デクリメント
    if (flashTimer > 0f) {
      flashTimer = Math.max(0f, flashTimer - delta);
      if (flashTimer <= 0f) {
        flashMessage = null;
      }
    }
    // lowHpAnim 更新
    lowHpAnimTime += delta;
  }

  // -------------------------------------------------------------------------
  // カメラシェイク用クエリ
  // -------------------------------------------------------------------------

  /** 現在シェイク中なら true。 */
  public boolean isShaking() {
    return shakeRemaining > 0f;
  }

  /**
   * カメラに加算するシェイク offset X (px)。呼出前に {@link #isShaking()} で確認推奨。
   *
   * @param rng 乱数源 (ADR-19: 注入済みインスタンスを使うことで同一シードで再現可)
   */
  public float currentShakeOffsetX(Random rng) {
    float intensity = shakeAmplitude * (shakeRemaining / SHAKE_DURATION);
    double angle = rng.nextDouble() * Math.PI * 2.0;
    return (float) (Math.cos(angle) * intensity);
  }

  /**
   * カメラに加算するシェイク offset Y (px)。
   *
   * @param rng 乱数源
   */
  public float currentShakeOffsetY(Random rng) {
    float intensity = shakeAmplitude * (shakeRemaining / SHAKE_DURATION);
    double angle = rng.nextDouble() * Math.PI * 2.0;
    return (float) (Math.sin(angle) * intensity);
  }

  // -------------------------------------------------------------------------
  // フラッシュ用クエリ
  // -------------------------------------------------------------------------

  /** フラッシュメッセージ表示中なら true。 */
  public boolean isShowingFlash() {
    return flashMessage != null && flashTimer > 0f;
  }

  /** 現在のフラッシュメッセージ文字列。非表示時は null。 */
  public String currentFlashMessage() {
    return flashMessage;
  }

  /**
   * フラッシュの透明度 (0.0〜1.0)。最後 0.5 秒でフェードアウト。
   *
   * <p>非表示時は 0 を返す。
   */
  public float flashAlpha() {
    if (!isShowingFlash()) {
      return 0f;
    }
    return Math.min(1f, flashTimer / 0.5f);
  }

  // -------------------------------------------------------------------------
  // 描画系 (begin/end は呼出側制御)
  // -------------------------------------------------------------------------

  /**
   * ダメージポップアップ群を描画する。
   *
   * <p><b>前提</b>: {@code batch.begin()} 済み。内部で begin/end しない。
   *
   * @param batch 描画中の SpriteBatch
   * @param font 描画フォント (large 推奨)
   */
  public void drawPopups(SpriteBatch batch, BitmapFont font) {
    if (popups.isEmpty()) {
      return;
    }
    for (DamagePopup p : popups) {
      Color c = p.baseColor();
      font.setColor(c.r, c.g, c.b, p.alpha());
      font.draw(batch, String.valueOf(p.amount()), p.worldX(), p.currentY());
    }
    font.setColor(Color.WHITE); // 後続描画への影響を避ける
  }

  /**
   * フラッシュメッセージを画面中央上部に描画する。
   *
   * <p><b>前提</b>: {@code batch.begin()} 済み。内部で begin/end しない。
   *
   * @param batch 描画中の SpriteBatch
   * @param font 描画フォント (large 推奨)
   * @param theme 現在の UI テーマ (accentColor をフラッシュ色に使う、null でデフォルト)
   */
  public void drawFlash(SpriteBatch batch, BitmapFont font, UiTheme theme) {
    if (!isShowingFlash()) {
      return;
    }
    float alpha = flashAlpha();
    UiTheme t = (theme != null) ? theme : UiTheme.defaultTheme();
    Color ac = t.accentColor();
    font.setColor(ac.r, ac.g, ac.b, alpha);
    font.draw(batch, flashMessage, 80f, RenderLayout.SCREEN_HEIGHT - 200f);
    font.setColor(Color.WHITE);
  }

  /**
   * フラッシュメッセージを画面中央上部に描画する (後方互換オーバーロード、theme=null → defaultTheme)。
   *
   * @param batch 描画中の SpriteBatch
   * @param font 描画フォント (large 推奨)
   */
  public void drawFlash(SpriteBatch batch, BitmapFont font) {
    drawFlash(batch, font, null);
  }

  /**
   * HP 低下警告フレームを描画する。
   *
   * <p><b>前提</b>: {@code shapes.begin(Filled)} 済み、GL_BLEND 有効。内部で begin/end しない。
   *
   * @param shapes 描画中の ShapeRenderer (Filled モードで begin 済み)
   * @param playerStats プレイヤーの Stats (HP 比率判定用)
   * @param theme 現在の UI テーマ (warnFrameColor を警告枠色に使う、null でデフォルト)
   */
  public void drawLowHpWarning(ShapeRenderer shapes, Stats playerStats, UiTheme theme) {
    if (LowHpWarning.shouldDraw(playerStats.currentHp(), playerStats.maxHp())) {
      UiTheme t = (theme != null) ? theme : UiTheme.defaultTheme();
      LowHpWarning.draw(shapes, lowHpAnimTime, t.warnFrameColor());
    }
  }

  /**
   * HP 低下警告フレームを描画する (後方互換オーバーロード、theme=null → defaultTheme)。
   *
   * @param shapes 描画中の ShapeRenderer (Filled モードで begin 済み)
   * @param playerStats プレイヤーの Stats (HP 比率判定用)
   */
  public void drawLowHpWarning(ShapeRenderer shapes, Stats playerStats) {
    drawLowHpWarning(shapes, playerStats, null);
  }

  // -------------------------------------------------------------------------
  // private helpers
  // -------------------------------------------------------------------------

  /** DamageDealt から target タイル座標を逆引きして popup を spawn する。 */
  private void spawnPopupInternal(BattleEvent.DamageDealt d, ActorId playerId, DungeonState state) {
    boolean toPlayer = d.to().equals(state.player().id());
    int tileX;
    int tileY;
    if (toPlayer) {
      tileX = state.player().position().x();
      tileY = state.player().position().y();
    } else {
      Optional<Enemy> enemyOpt = state.findEnemy(d.to());
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
  private void triggerShakeInternal(BattleEvent.DamageDealt d, ActorId playerId) {
    boolean toPlayer = d.to().equals(playerId);
    shakeAmplitude = toPlayer ? SHAKE_AMP_RECEIVE : SHAKE_AMP_DEAL;
    shakeRemaining = SHAKE_DURATION;
  }

  /**
   * §15-5 色分け方針: 被弾=赤、暴力的 (>=8) =黄、それ以外=白。
   *
   * <p>閾値 8 は ADR-30 修正後の物攻 2 で「強打 + Buff 物攻 +2 → ダメ 6」が通常、「Elite 物攻 3 + 斬撃 → 8」が 暴力的ヒットの閾値。
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
}
