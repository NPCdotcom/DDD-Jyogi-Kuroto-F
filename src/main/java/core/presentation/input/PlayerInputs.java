package core.presentation.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import core.domain.battle.BattleAction;
import core.domain.common.Direction;
import java.util.Optional;

/**
 * キーボード入力を {@link BattleAction} にマッピングする。
 *
 * <p>2 ステートモデル:
 *
 * <ul>
 *   <li>状態0 (通常): 数字キー 1〜9 でカード選択 (状態1へ)、WASD/矢印で移動、SPACE/ENTER は待機/ターン終了
 *   <li>状態1 (カード選択中): 方向キーで {@link BattleAction.UseCard} 発行しリセット、ESC でキャンセル
 * </ul>
 *
 * <p>{@code pendingCardIndex()} で現在選択中のカード番号を参照できる (HudRenderer からハイライト用に利用)。 -1 は「未選択」を意味する。
 *
 * <p>1 フレームで複数キーが押されても先勝ち。Application 層から poll される。
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
   * <p>カード選択中 (pendingCardIndex >= 0) のときは方向キーを UseCard に変換し、ESC でキャンセルする。
   * 通常状態では移動・数字キー・待機・ターン終了のみ受け付ける。
   */
  public Optional<BattleAction> poll() {
    if (pendingCardIndex >= 0) {
      return pollCardDirectionMode();
    }
    return pollNormalMode();
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
