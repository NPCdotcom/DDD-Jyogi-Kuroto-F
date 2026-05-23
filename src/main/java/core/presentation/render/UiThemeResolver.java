package core.presentation.render;

import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentSlot;
import java.util.Map;

/**
 * 現在の装備ロードアウトから {@link UiTheme} を解決するユーティリティ (§7-2 / W4-ε)。
 *
 * <p>優先順位: MAIN → HAND → HEAD → BODY → FEET → ACCESSORY。 この順で先に {@code themeName}
 * が設定されている装備を探し、最初に見つかったテーマを返す。 全装備の {@code themeName} が空なら {@link UiTheme#defaultTheme()}
 * にフォールバックする。
 *
 * <p>純粋関数として実装しているため、インスタンス生成は不要。
 */
public final class UiThemeResolver {

  /** スロット優先順位 (主武器 → 手 → 頭 → 胴 → 足 → 装飾)。 */
  private static final EquipmentSlot[] PRIORITY_ORDER = {
    EquipmentSlot.MAIN,
    EquipmentSlot.HAND,
    EquipmentSlot.HEAD,
    EquipmentSlot.BODY,
    EquipmentSlot.FEET,
    EquipmentSlot.ACCESSORY
  };

  private UiThemeResolver() {}

  /**
   * ロードアウトから UI テーマを解決する。
   *
   * @param loadout 装備スロット → 装備の Map (防御コピー不要、参照のみ)
   * @return 解決した {@link UiTheme}、未設定時は {@link UiTheme#defaultTheme()}
   */
  public static UiTheme resolve(Map<EquipmentSlot, Equipment> loadout) {
    if (loadout == null || loadout.isEmpty()) {
      return UiTheme.defaultTheme();
    }
    for (EquipmentSlot slot : PRIORITY_ORDER) {
      Equipment eq = loadout.get(slot);
      if (eq == null) {
        continue;
      }
      if (eq.themeName().isPresent()) {
        return fromName(eq.themeName().get());
      }
    }
    return UiTheme.defaultTheme();
  }

  /**
   * テーマ名文字列から {@link UiTheme} を返す。未知の名前はデフォルトにフォールバックする。
   *
   * @param name equipment.json の {@code themeName} 値 (例: {@code "fire"})
   * @return 対応する {@link UiTheme}
   */
  private static UiTheme fromName(String name) {
    return switch (name) {
      case "fire" -> UiTheme.fire();
      case "frost" -> UiTheme.frost();
      case "earth" -> UiTheme.earth();
      case "dark" -> UiTheme.dark();
      default -> UiTheme.defaultTheme();
    };
  }
}
