package core.domain.entity;

/**
 * 敵 AI の行動状態 (Wave 13 W13-α)。プレイヤーに対する認識度を 3 段階で表現する。
 *
 * <ul>
 *   <li>{@link #IDLE}: プレイヤーをまだ視認していない。その場で待機 (Wave 13 はランダムウォークなし、KISS)。
 *   <li>{@link #ALERT}: プレイヤーを視界内に捉えている。{@link EnemyAiProfile} に応じて追跡 / kite 等の行動。
 *   <li>{@link #SEARCHING}: 一度視認したが見失った。{@code lastKnownPlayerPos} に向かって BFS 移動、到達後 IDLE 戻り。
 * </ul>
 *
 * <p>状態遷移は純関数 ({@code EnemyAi.computeNewState}) で計算、Enemy record に immutable update で反映する。
 */
public enum EnemyAiState {
  IDLE,
  ALERT,
  SEARCHING
}
