package core.presentation.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.viewport.Viewport;
import core.domain.battle.BattleAction;
import core.domain.common.Direction;
import core.domain.common.Position;
import core.domain.dungeon.DungeonState;
import core.presentation.render.RenderLayout;
import java.util.Optional;

/**
 * キーボード入力を {@link BattleAction} にマッピングする。
 *
 * <p>3 ステートモデル (ADR-21 §15-5):
 *
 * <ul>
 *   <li>状態0 (通常): 数字キーで手札のカード選択 (状態1へ、空スロットは選べない)、WASD/矢印で移動、SPACE/ENTER は待機/ターン終了
 *   <li>状態1 (カード選択中): 方向キーで {@link BattleAction.UseCard} 発行しリセット、ESC でキャンセル、ENTER でターン終了
 *   <li>状態2 (移動権保持中): {@code DungeonState.player().pendingMoveCount() > 0} のとき自動遷移。 WASD/方向キーで
 *       {@link BattleAction.Move} 発行 (TurnEngine が AP 0 で処理)、ENTER で残り移動権を放棄してターン終了
 *       (四方を塞がれた際のソフトロック回避)。数字キー・SPACE は無視。
 * </ul>
 *
 * <p>{@code pendingCardIndex()} で現在選択中のカード番号を参照できる (HudRenderer からハイライト用に利用)。 -1 は「未選択」を意味する。
 *
 * <p>1 フレームで複数キーが押されても先勝ち。Application 層から poll される。
 *
 * <p>状態2 の判定はドメイン側 {@code pendingMoveCount} を毎フレーム読み取る方式。PlayerInputs にキャッシュを持たない
 * (ドメインが唯一の真実の源、セーブ整合を保つ)。
 */
public final class PlayerInputs {

  /** 未選択状態を表す sentinel 値。 */
  private static final int NONE = -1;

  /** カード選択中の手札インデックス (-1 = 未選択)。 */
  private int pendingCardIndex = NONE;

  /** 現在選択中のカード手札インデックス。-1 は未選択。 */
  public int pendingCardIndex() {
    return pendingCardIndex;
  }

  /** 状態をリセットする (画面非表示・dispose 時に呼ぶ)。 */
  public void reset() {
    pendingCardIndex = NONE;
  }

  /**
   * 現在フレームの入力を {@link BattleAction} に変換する (キーボードのみ、Wave 14 以前の経路)。
   *
   * <p>優先度: 状態2 (移動権保持中) > 状態1 (カード選択中) > 状態0 (通常)。
   *
   * <p>状態2 は {@code state.player().pendingMoveCount() > 0} で判定する。 この条件が成立している間は状態1 への遷移 (数字キー)
   * をブロックする。
   *
   * @param state 現在のダンジョン状態 (pendingMoveCount 参照のため必要)
   */
  public Optional<BattleAction> poll(DungeonState state) {
    return poll(state, Optional.empty());
  }

  /**
   * 現在フレームの入力を {@link BattleAction} に変換する (Wave 14 W14-α: マウス方向クリックも統合)。
   *
   * <p>{@code mouseDirection} が present なら、状態に応じて以下を行う:
   *
   * <ul>
   *   <li>状態2 (移動権保持中) → {@link BattleAction.Move}
   *   <li>状態1 (カード選択中) → {@link BattleAction.UseCard} (選択解除)
   *   <li>状態0 (通常) → {@link BattleAction.Move}
   * </ul>
   *
   * <p>{@code mouseDirection} が empty ならキーボードのみの既存パスで判定。マップクリックは {@link RenderLayout#screenToTile}
   * + {@link RenderLayout#directionToward} で事前算出して渡す。
   */
  public Optional<BattleAction> poll(DungeonState state, Optional<Direction> mouseDirection) {
    // 状態2: 移動権保持中 — ドメインの pendingMoveCount を毎フレーム参照
    if (state.player().pendingMoveCount() > 0) {
      if (mouseDirection.isPresent()) {
        return Optional.of(new BattleAction.Move(mouseDirection.get()));
      }
      return pollMovementTokenMode();
    }
    // 状態1: カード選択中
    if (pendingCardIndex >= 0) {
      if (mouseDirection.isPresent()) {
        BattleAction action = new BattleAction.UseCard(pendingCardIndex, mouseDirection.get());
        pendingCardIndex = NONE;
        return Optional.of(action);
      }
      return pollCardDirectionMode();
    }
    // 状態0: 通常
    if (mouseDirection.isPresent()) {
      return Optional.of(new BattleAction.Move(mouseDirection.get()));
    }
    return pollNormalMode(state.player().cardPileState().hand().size());
  }

  /**
   * マウスクリック位置からプレイヤーへの方向を読み取る (Wave 14 W14-α)。
   *
   * <p>{@code Gdx.input.justTouched()} が false なら empty。クリック位置をビューポート経由でタイル座標化し、 プレイヤー位置との差分から主要方向
   * ({@link RenderLayout#directionToward}) を算出する。自分マスクリックは 内部のガードで empty を返すため、誤って RIGHT 発火 ≒
   * 自爆クリック移動はしない (CTO チェックポイント #2)。
   *
   * <p>HUD カード矩形と重なるクリックは呼出側 ({@link core.presentation.screen.DungeonScreen}) でフィルタする責務。
   *
   * @param mapViewport マップカメラに紐付くビューポート
   * @param playerPos プレイヤー現在位置
   * @return クリック方向、または empty (未クリック / 自分マスクリック)
   */
  public static Optional<Direction> readMouseDirection(Viewport mapViewport, Position playerPos) {
    if (!Gdx.input.justTouched()) {
      return Optional.empty();
    }
    Position clicked = RenderLayout.screenToTile(mapViewport, Gdx.input.getX(), Gdx.input.getY());
    return RenderLayout.directionToward(playerPos, clicked);
  }

  /**
   * 状態2 (移動権保持中): WASD/方向キーで Move 発行。ENTER で残り移動権を放棄してターン終了。
   *
   * <p>ENTER 受付は移動権ソフトロック対策: 四方を壁・敵で塞がれて移動できない状態で移動カードを使うと、移動権が消費 できず {@code
   * autoEndPlayerTurnIfApDepleted} も pendingMoveCount ガードで発火しないため、ENTER による明示的な
   * ターン終了が唯一の脱出路となる。数字キー・SPACE は移動専念のため無視する。
   */
  private Optional<BattleAction> pollMovementTokenMode() {
    Direction dir = readDirection();
    if (dir != null) {
      return Optional.of(new BattleAction.Move(dir));
    }
    // ENTER で残り移動権を放棄してターン終了 (四方塞がれ時のソフトロック回避)。
    if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
      return Optional.of(new BattleAction.EndTurn());
    }
    // 数字キー・SPACE は移動権保持中は無視 (移動専念)。
    return Optional.empty();
  }

  /** 状態1 (カード選択中): 方向キーで UseCard 発行、ESC でキャンセル、ENTER でターン終了、同じ数字キーで選択トグル解除。 */
  private Optional<BattleAction> pollCardDirectionMode() {
    // ESC でカード選択をキャンセル
    if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
      pendingCardIndex = NONE;
      return Optional.empty();
    }
    // 同じ数字キーをもう一度押すとトグルで選択解除 (UX: 操作ヒントを覚えなくても直感的に解除可能)
    if (pendingCardIndex >= 0
        && pendingCardIndex <= 8
        && Gdx.input.isKeyJustPressed(numKey(pendingCardIndex))) {
      pendingCardIndex = NONE;
      return Optional.empty();
    }
    // ENTER でカード選択を解除してターン終了 (どの入力状態からも詰まないための脱出路)
    if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
      pendingCardIndex = NONE;
      return Optional.of(new BattleAction.EndTurn());
    }
    // 方向キーで UseCard 発行
    Direction dir = readDirection();
    if (dir != null) {
      BattleAction action = new BattleAction.UseCard(pendingCardIndex, dir);
      pendingCardIndex = NONE;
      return Optional.of(action);
    }
    return Optional.empty();
  }

  /**
   * マウスクリック起点のカード選択 API (§15-3 UI/UX 改善)。
   *
   * <p>境界チェックは呼出側 (DungeonScreen) の {@code handCardBounds} ヒット判定で済んでいる前提。 既に同じ index が選択中なら解除する
   * (再クリックでトグル off)、それ以外なら選択状態に設定する。
   *
   * @param handIndex 手札の 0-indexed 位置 (0〜8)
   */
  public void selectCardByMouse(int handIndex) {
    if (pendingCardIndex == handIndex) {
      pendingCardIndex = NONE;
    } else {
      pendingCardIndex = handIndex;
    }
  }

  /** 状態0 (通常): 移動 / 数字キーによるカード選択 / 待機 / ターン終了。 */
  private Optional<BattleAction> pollNormalMode(int handSize) {
    // WASD / 矢印キーは移動
    Direction dir = readDirection();
    if (dir != null) {
      return Optional.of(new BattleAction.Move(dir));
    }
    // Wave 15 W15-β / #3: F1〜F4 スキル発動を完全廃止 (デッキ・カード一本化、§15-3 整合)。
    // 物理キーを押しても何も起きない (Player.skillSlot は空 4 枠で初期化される)。
    // 数字キーでカード選択モードへ (UseCard: 0-indexed)。手札に実在するカードのみ選択可
    // (空スロットを選んでカード選択状態に入り、操作不能に陥るのを防ぐ)。
    for (int i = 0; i < handSize && i < 9; i++) {
      if (Gdx.input.isKeyJustPressed(numKey(i))) {
        pendingCardIndex = i;
        return Optional.empty(); // 方向待ち (まだアクション未確定)
      }
    }
    // 待機 / ターン終了。割当の正は InputAction で、表示文との対応は InputHelpTextTest が検証する (INPUT-01)。
    if (Gdx.input.isKeyJustPressed(InputAction.WAIT.keyCode())) {
      return Optional.of(new BattleAction.Wait());
    }
    if (Gdx.input.isKeyJustPressed(InputAction.END_TURN.keyCode())) {
      return Optional.of(new BattleAction.EndTurn());
    }
    return Optional.empty();
  }

  /**
   * 方向キー (UP/DOWN/LEFT/RIGHT + WASD) から {@link Direction} を読む。
   *
   * <p>何も押されていなければ {@code null} を返す。先勝ちでの優先順は上 > 下 > 左 > 右。
   */
  private static Direction readDirection() {
    if (isJustPressed(Keys.UP, Keys.W)) {
      return Direction.UP;
    }
    if (isJustPressed(Keys.DOWN, Keys.S)) {
      return Direction.DOWN;
    }
    if (isJustPressed(Keys.LEFT, Keys.A)) {
      return Direction.LEFT;
    }
    if (isJustPressed(Keys.RIGHT, Keys.D)) {
      return Direction.RIGHT;
    }
    return null;
  }

  /** 0-indexed の i に対応するキーコード (0→NUM_1, 1→NUM_2, ..., 8→NUM_9)。 */
  private static int numKey(int i) {
    return switch (i) {
      case 0 -> Keys.NUM_1;
      case 1 -> Keys.NUM_2;
      case 2 -> Keys.NUM_3;
      case 3 -> Keys.NUM_4;
      case 4 -> Keys.NUM_5;
      case 5 -> Keys.NUM_6;
      case 6 -> Keys.NUM_7;
      case 7 -> Keys.NUM_8;
      case 8 -> Keys.NUM_9;
      default -> throw new IllegalArgumentException("i must be 0..8: " + i);
    };
  }

  private static boolean isJustPressed(int... keys) {
    for (int k : keys) {
      if (Gdx.input.isKeyJustPressed(k)) {
        return true;
      }
    }
    return false;
  }
}
