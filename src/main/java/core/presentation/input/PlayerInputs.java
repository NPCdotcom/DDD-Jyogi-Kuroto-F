package core.presentation.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import core.domain.battle.BattleAction;
import core.domain.common.Direction;
import core.domain.dungeon.DungeonState;
import java.util.Optional;

/**
 * キーボード入力を {@link BattleAction} にマッピングする。
 *
 * <p>3 ステートモデル (ADR-21 §15-5):
 *
 * <ul>
 *   <li>状態0 (通常): 数字キー 1〜9 でカード選択 (状態1へ)、WASD/矢印で移動、SPACE/ENTER は待機/ターン終了
 *   <li>状態1 (カード選択中): 方向キーで {@link BattleAction.UseCard} 発行しリセット、ESC でキャンセル
 *   <li>状態2 (移動権保持中): {@code DungeonState.player().pendingMoveCount() > 0} のとき自動遷移。
 *       WASD/方向キーで {@link BattleAction.Move} 発行 (TurnEngine が AP 0 で処理)、数字キーは無視。
 *       ESC による移動権破棄は YAGNI (ADR-21 §Decision-6)、本実装では実装しない。
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
   * 現在フレームの入力を {@link BattleAction} に変換する。
   *
   * <p>優先度: 状態2 (移動権保持中) > 状態1 (カード選択中) > 状態0 (通常)。
   *
   * <p>状態2 は {@code state.player().pendingMoveCount() > 0} で判定する。
   * この条件が成立している間は状態1 への遷移 (数字キー) をブロックする。
   *
   * @param state 現在のダンジョン状態 (pendingMoveCount 参照のため必要)
   */
  public Optional<BattleAction> poll(DungeonState state) {
    // 状態2: 移動権保持中 — ドメインの pendingMoveCount を毎フレーム参照
    if (state.player().pendingMoveCount() > 0) {
      return pollMovementTokenMode();
    }
    // 状態1: カード選択中
    if (pendingCardIndex >= 0) {
      return pollCardDirectionMode();
    }
    // 状態0: 通常
    return pollNormalMode();
  }

  /**
   * 後方互換オーバーロード。移動権を持たない状態で呼ぶ場合に使用。
   *
   * <p>移動権判定が不要な箇所 (テスト等) 向け。本番の DungeonScreen では {@link #poll(DungeonState)} を使うこと。
   *
   * @deprecated DungeonScreen は {@link #poll(DungeonState)} に移行済み。残存する呼び出しは移行すること。
   */
  @Deprecated
  public Optional<BattleAction> poll() {
    if (pendingCardIndex >= 0) {
      return pollCardDirectionMode();
    }
    return pollNormalMode();
  }

  /**
   * 状態2 (移動権保持中): WASD/方向キーで Move 発行。数字キーは無視。
   *
   * <p>ESC による移動権破棄は YAGNI (ADR-21 §Decision-6)、本実装では不要。
   */
  private Optional<BattleAction> pollMovementTokenMode() {
    Direction dir = readDirection();
    if (dir != null) {
      return Optional.of(new BattleAction.Move(dir));
    }
    // 数字キー・SPACE・ENTER は移動権保持中は無視 (仕様: 移動専念)
    return Optional.empty();
  }

  /** 状態1 (カード選択中): 方向キーで UseCard 発行、ESC でキャンセル。 */
  private Optional<BattleAction> pollCardDirectionMode() {
    // ESC でキャンセル
    if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
      pendingCardIndex = NONE;
      return Optional.empty();
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

  /** 状態0 (通常): 移動 / 数字キーによるカード選択 / 待機 / ターン終了。 */
  private Optional<BattleAction> pollNormalMode() {
    // WASD / 矢印キーは移動
    Direction dir = readDirection();
    if (dir != null) {
      return Optional.of(new BattleAction.Move(dir));
    }
    // 数字キー 1〜9 でカード選択モードへ (UseCard: 0-indexed)
    for (int i = 0; i < 9; i++) {
      if (Gdx.input.isKeyJustPressed(numKey(i))) {
        pendingCardIndex = i;
        return Optional.empty(); // 方向待ち (まだアクション未確定)
      }
    }
    // 待機
    if (Gdx.input.isKeyJustPressed(Keys.SPACE)) {
      return Optional.of(new BattleAction.Wait());
    }
    // ターン終了
    if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
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
