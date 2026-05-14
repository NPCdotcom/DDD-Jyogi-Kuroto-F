package core.domain.battle;

/** 現在進行中のターン位相。 */
public enum TurnPhase {
  /** プレイヤーが入力を受け付け可能。 */
  PLAYER_TURN,
  /** 敵 AI の行動解決中。 */
  ENEMY_TURN,
  /** プレイヤーが死亡してランが終了した状態。 */
  GAME_OVER,
  /** 敵を全滅させた等でこの階層を踏破した状態 (MVP では同時にラン終了)。 */
  CLEARED
}
