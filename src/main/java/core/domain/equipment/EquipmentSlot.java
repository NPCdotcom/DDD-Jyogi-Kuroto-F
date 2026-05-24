package core.domain.equipment;

/**
 * 装備スロット (§15-9 / ADR-25)。1 部位スタート (ADR-08) でも種別を区別することで、将来 {@code Map<EquipmentSlot, Equipment>}
 * への拡張余地を残す。
 *
 * <p>{@code docs/templates/equipments.json} の {@code slot} 値と 1:1 対応する。
 */
public enum EquipmentSlot {
  /** 足装備 (ぼろ靴など)。 */
  FEET,
  /** 手装備 (ぼろい短剣など、片手の物理武器)。 */
  HAND,
  /** 主装備 (魔法系の主武器)。 */
  MAIN,
  /** 頭装備 (魔導師の額冠など)。 */
  HEAD,
  /** 体装備 (見習いローブ・影のマントなど)。 */
  BODY,
  /** 装飾品 (指輪・護符・腕輪など)。 */
  ACCESSORY
}
