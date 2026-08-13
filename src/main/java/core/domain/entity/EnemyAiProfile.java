package core.domain.entity;

/**
 * 敵 AI の行動プロファイル (Wave 13 W13-α)。{@link EnemyKind} ごとに 1 つ持ち、{@link EnemyAiState#ALERT}
 * 中の振る舞いを分岐する。
 *
 * <ul>
 *   <li>{@link #AGGRESSIVE}: 隣接攻撃 + BFS でプレイヤーへ詰める (全敵種共通)。
 *   <li>{@link #CAUTIOUS}: プレイヤーから距離 2-3 を保つ kite 型 (敵側 range 実装まで未使用)。
 * </ul>
 *
 * <p>3 値以上のプロファイル (PATROL / DEFENDER 等) は Wave 14+ で必要に応じて追加。
 */
public enum EnemyAiProfile {
  AGGRESSIVE,
  CAUTIOUS
}
