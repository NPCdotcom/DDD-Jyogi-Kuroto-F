package core.application;

import core.domain.battle.BattleEvent;
import core.domain.battle.TurnEngine.StepResult;
import core.domain.dungeon.DungeonState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * 1 ラン (=1 回のダンジョン挑戦) の可変状態を保持する。
 *
 * <p>ドメインオブジェクトは不変なので、状態遷移はこのクラス内に閉じる (副作用の分離: ドメインは 純関数、application 層が "今" の状態を保持する)。
 */
public final class GameContext {

  private static final int MAX_LOG_LINES = 64;

  /**
   * ラン全体の最大層数のデフォルト値 (§15-6「初期 3 層」)。SoulTree の LayerExtendEffect を 解放すると {@link #extendMaxLayer} で
   * +1 ずつ加算される。infrastructure 層の {@code InitialStateFactory.DEFAULT_MAX_LAYER} と同値だが、application 層が
   * infrastructure に依存しないよう本クラス内に定数として複製する (依存方向: application は domain のみ)。
   */
  public static final int DEFAULT_MAX_LAYER = 3;

  private DungeonState state;
  private final Deque<BattleEvent> recentEvents = new ArrayDeque<>();

  /**
   * 累積イベント発火数 (§15-5 / E-8)。循環バッファ ({@link #recentEvents} = MAX_LOG_LINES) で古いイベントが 破棄されても、本値は減らず
   * monotonic に増加する。DungeonScreen が「未処理 DamageDealt」を検知して popup / shake を発火するためのカーソルとして使う。
   */
  private long totalEventsEmitted = 0;

  /**
   * ラン全体の最大層数 (§15-6 / SoulTree LayerExtendEffect)。デフォルト = 3、 SoulTree の LayerExtend ノード解放で {@link
   * #extendMaxLayer} を呼び加算する。 ラン内の可変状態 (ラン跨ぎは SoulTree 経由で間接的に再計算)。
   */
  private int maxLayer = DEFAULT_MAX_LAYER;

  private GameContext(DungeonState initial) {
    this.state = initial;
  }

  public static GameContext startNewRun(DungeonState initial) {
    Objects.requireNonNull(initial, "initial");
    return new GameContext(initial);
  }

  public DungeonState state() {
    return state;
  }

  /** 現在のラン最大層数 (§15-6 / LayerExtendEffect)。 */
  public int maxLayer() {
    return maxLayer;
  }

  /**
   * 最大層数を {@code amount} 増やす (SoulTree.LayerExtendEffect の合計を反映する用)。
   *
   * @param amount 増加量 (1 以上)
   * @throws IllegalArgumentException amount が 0 以下
   */
  public void extendMaxLayer(int amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("amount must be > 0: " + amount);
    }
    this.maxLayer += amount;
  }

  /** {@link TurnEngine} の出力を受けて状態を更新し、発生イベントを記録する。 */
  public void applyResult(StepResult result) {
    Objects.requireNonNull(result, "result");
    this.state = result.state();
    for (BattleEvent event : result.events()) {
      recentEvents.addLast(event);
      totalEventsEmitted++;
      while (recentEvents.size() > MAX_LOG_LINES) {
        recentEvents.removeFirst();
      }
    }
  }

  /**
   * 累積イベント発火数 (§15-5 / E-8 用カーソル)。{@link #recentEvents} の循環で古いイベントが破棄されても 減らない。新規 DamageDealt
   * 検知に使う。
   */
  public long totalEventsEmitted() {
    return totalEventsEmitted;
  }

  /** 直近に発生した最大 n 件のイベントを新しい順で返す。 */
  public List<BattleEvent> latestEvents(int n) {
    int size = Math.min(n, recentEvents.size());
    List<BattleEvent> out = new ArrayList<>(size);
    int skip = recentEvents.size() - size;
    int i = 0;
    for (BattleEvent e : recentEvents) {
      if (i++ < skip) {
        continue;
      }
      out.add(e);
    }
    return List.copyOf(out);
  }
}
