package core.domain.entity;

/**
 * 敵 AI の行動プロファイル (Wave 13 W13-α)。{@link EnemyKind} ごとに 1 つ持ち、{@link EnemyAiState#ALERT}
 * 中の振る舞いを分岐する。
 *
 * <ul>
 *   <li>{@link #AGGRESSIVE}: 隣接攻撃 + BFS でプレイヤーへ詰める (既存挙動、SLIME / TOUGH_SLIME / ELITE_SLIME / BOSS)。
 *   <li>{@link #CAUTIOUS}: プレイヤーから距離 2-3 を保つ kite 型。隣接されたら離れる方向に移動 (SWIFT_SLIME)。
 * </ul>
 *
 * <p>3 値以上のプロファイル (PATROL / DEFENDER 等) は Wave 14+ で必要に応じて追加。
 */
public enum EnemyAiProfile {
  AGGRESSIVE,
  CAUTIOUS
}
