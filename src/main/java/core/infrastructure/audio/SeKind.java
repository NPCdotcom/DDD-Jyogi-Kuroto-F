package core.infrastructure.audio;

/**
 * SE (効果音) 種別。{@link SoundManager#playSe(SeKind)} に渡す。
 *
 * <p>対応ファイルパスは {@link SoundManager} のコメントおよび {@code assets/audio/README.md} を参照。
 */
public enum SeKind {
  /** 敵を撃破したとき (ActorDied イベント、対象が敵)。 */
  ENEMY_DEFEATED("se/enemy_defeated.ogg"),

  /** プレイヤーがダメージを受けたとき (DamageDealt.to == playerId)。 */
  PLAYER_DAMAGED("se/player_damaged.ogg"),

  /** プレイヤーが敵にダメージを与えたとき (DamageDealt.from == playerId)。 */
  DEAL_DAMAGE("se/deal_damage.ogg"),

  /** カードを使用したとき (SkillUsed イベント)。 */
  CARD_USED("se/card_used.ogg"),

  /** ボタン操作・画面遷移・選択確定。 */
  BUTTON("se/button.ogg"),

  /** 層遷移 (FloorAdvanced イベント)。 */
  FLOOR_ADVANCE("se/floor_advance.ogg");

  /** {@code assets/audio/} からの相対パス。 */
  final String path;

  SeKind(String path) {
    this.path = path;
  }
}
