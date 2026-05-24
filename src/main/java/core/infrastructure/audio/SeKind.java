package core.infrastructure.audio;

/**
 * SE (効果音) 種別。{@link SoundManager#playSe(SeKind)} に渡す。
 *
 * <p>Wave 10 W10-α でチームメイト素材投入に合わせ enum を拡張。カード使用 SE は種別別に 5 分割、 ドロー SE はレアリティ別に 3 分割。
 *
 * <p>対応ファイルパスは {@code assets/audio/se/<path>}、{@link SoundManager} のロードロジックを参照。
 */
public enum SeKind {
  /** 敵を撃破したとき (ActorDied イベント、対象が敵)。 */
  ENEMY_DEFEATED("se/enemy_defeated.ogg"),

  /** プレイヤーがダメージを受けたとき (DamageDealt.to == playerId)。 */
  PLAYER_DAMAGED("se/player_damaged.ogg"),

  /** プレイヤーが敵にダメージを与えたとき (DamageDealt.from == playerId)。 */
  DEAL_DAMAGE("se/deal_damage.ogg"),

  /** ボタン操作・画面遷移・決定 (主に ENTER / クリック)。 */
  BUTTON_DECISION("se/button_decision.ogg"),

  /** 選択肢の項目移動 (主に矢印キー / WS)。 */
  BUTTON_SELECTION("se/button_selection.ogg"),

  /** 層遷移 (FloorAdvanced イベント)。 */
  FLOOR_ADVANCE("se/floor_advance.ogg"),

  /** HP 警告 (LowHpWarning 発火時)。 */
  HP_LOW("se/hp_low.ogg"),

  /** ソウルツリーノード解放時 (DddGame.unlockTreeNode 成功時)。 */
  LEVEL_UP("se/level_up.ogg"),

  /** 壁破壊 (Wave 11+ 新機能、現状ファイルだけ用意)。 */
  BLOCK_BREAK("se/block_brake.ogg"),

  /** ステータス強化時 (HpMaxUp / SpeedUp 等の LayerEndNode 適用後)。 */
  STATUS_UP("se/status_up.ogg"),

  // Wave 10 W10-α: カードドロー SE (レアリティ別 3 分割)。
  // Card / CardCatalog に rarity field が無いため、現状は CARD_DRAW_C を一律で発火し、
  // U / R は Wave 11+ で cards.json 拡張時に分岐する (素材は先行投入)。
  /** カードドロー (コモン) — 現状はドロー全般で発火。 */
  CARD_DRAW_C("se/card_draw_c.ogg"),
  /** カードドロー (アンコモン) — Wave 11+ で分岐実装予定。 */
  CARD_DRAW_U("se/card_draw_u.ogg"),
  /** カードドロー (レア) — Wave 11+ で分岐実装予定。 */
  CARD_DRAW_R("se/card_draw_r.ogg"),

  // Wave 10 W10-α: カード使用 SE (種別別 5 分割、CardEffect で分岐)。
  /** カード使用: 物理ダメージ (CardEffect.Damage + element=PHYSICAL)。 */
  CARD_PHYSICAL("se/card_phisycal.ogg"),
  /** カード使用: 魔法ダメージ (CardEffect.Damage + element=MAGICAL)。 */
  CARD_MAGIC("se/card_magic.ogg"),
  /** カード使用: 移動 (CardEffect.Move)。 */
  CARD_MOVE("se/card_move.ogg"),
  /** カード使用: バフ (CardEffect.Buff)。 */
  CARD_BUFF("se/card_buf.ogg"),
  /** カード使用: 罠設置 (CardEffect.Trap)。 */
  CARD_TRAP("se/card_trap.ogg");

  /** {@code assets/audio/} からの相対パス。 */
  final String path;

  SeKind(String path) {
    this.path = path;
  }
}
