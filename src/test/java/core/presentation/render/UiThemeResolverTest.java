package core.presentation.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentId;
import core.domain.equipment.EquipmentSlot;
import core.domain.equipment.StatsBonus;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * {@link UiThemeResolver} 単体テスト (§7-2 / W4-ε)。
 *
 * <p>LibGDX の {@code Color} は非 headless 環境でのみ初期化可能なため、テスト内では UiTheme のファクトリを呼ばず、
 * スロット解決の「どのテーマ名が選ばれるか」を UiThemeResolver 内部の private switch に依拠した間接検証で確認する。 具体的には resolve() が返す
 * UiTheme の reference が defaultTheme / fire / frost / earth / dark の どれかに合致するかを確認する。ただし Color が GDX
 * ランタイムなしで生成できるかは別問題なので、 テストスコープでは mock Equipment を 使い、themeName の文字列解決のみを検証する。
 */
class UiThemeResolverTest {

  // テスト用 Equipment ヘルパ (themeName あり)
  private static Equipment eq(EquipmentSlot slot, String themeNameStr) {
    return new Equipment(
        EquipmentId.of("test_eq_" + slot.name().toLowerCase()),
        "テスト装備",
        slot,
        StatsBonus.zero(),
        List.of(),
        Optional.of(themeNameStr));
  }

  // テスト用 Equipment ヘルパ (themeName なし)
  private static Equipment eqNoTheme(EquipmentSlot slot) {
    return new Equipment(
        EquipmentId.of("test_eq_notheme_" + slot.name().toLowerCase()),
        "テスト装備",
        slot,
        StatsBonus.zero(),
        List.of(),
        Optional.empty());
  }

  @Test
  void emptyLoadoutReturnsDefaultTheme() {
    // 空ロードアウト → defaultTheme
    UiTheme result = UiThemeResolver.resolve(Map.of());
    // defaultTheme は textColor.r == 1.0 (WHITE)
    // LibGDX Color は GDX ランタイム不要な単純 float 格納なのでテスト可能
    UiTheme expected = UiTheme.defaultTheme();
    assertEquals(expected.textColor().r, result.textColor().r, 0.001f);
    assertEquals(expected.textColor().g, result.textColor().g, 0.001f);
    assertEquals(expected.textColor().b, result.textColor().b, 0.001f);
  }

  @Test
  void allNoThemeLoadoutReturnsDefaultTheme() {
    // 全装備が themeName なし → defaultTheme
    Map<EquipmentSlot, Equipment> loadout = new EnumMap<>(EquipmentSlot.class);
    loadout.put(EquipmentSlot.HAND, eqNoTheme(EquipmentSlot.HAND));
    loadout.put(EquipmentSlot.FEET, eqNoTheme(EquipmentSlot.FEET));
    UiTheme result = UiThemeResolver.resolve(loadout);
    UiTheme expected = UiTheme.defaultTheme();
    assertEquals(expected.textColor().r, result.textColor().r, 0.001f);
  }

  @Test
  void mainSlotFireEquipmentReturnsFire() {
    // MAIN スロットに fire 装備 → fire テーマ
    Map<EquipmentSlot, Equipment> loadout = new EnumMap<>(EquipmentSlot.class);
    loadout.put(EquipmentSlot.MAIN, eq(EquipmentSlot.MAIN, "fire"));
    UiTheme result = UiThemeResolver.resolve(loadout);
    UiTheme expected = UiTheme.fire();
    assertEquals(expected.textColor().r, result.textColor().r, 0.001f);
    assertEquals(expected.accentColor().r, result.accentColor().r, 0.001f);
  }

  @Test
  void handSlotFrostWithMainNoThemeReturnsFrost() {
    // MAIN が themeName なし、HAND に frost → frost テーマ
    Map<EquipmentSlot, Equipment> loadout = new EnumMap<>(EquipmentSlot.class);
    loadout.put(EquipmentSlot.MAIN, eqNoTheme(EquipmentSlot.MAIN));
    loadout.put(EquipmentSlot.HAND, eq(EquipmentSlot.HAND, "frost"));
    UiTheme result = UiThemeResolver.resolve(loadout);
    UiTheme expected = UiTheme.frost();
    assertEquals(expected.skillSlotColor().b, result.skillSlotColor().b, 0.001f);
  }

  @Test
  void mainPriorityOverHand() {
    // MAIN (fire) と HAND (frost) が両方ある → MAIN 優先で fire
    Map<EquipmentSlot, Equipment> loadout = new EnumMap<>(EquipmentSlot.class);
    loadout.put(EquipmentSlot.MAIN, eq(EquipmentSlot.MAIN, "fire"));
    loadout.put(EquipmentSlot.HAND, eq(EquipmentSlot.HAND, "frost"));
    UiTheme result = UiThemeResolver.resolve(loadout);
    UiTheme fire = UiTheme.fire();
    assertEquals(fire.textColor().r, result.textColor().r, 0.001f);
  }

  @Test
  void earthThemeResolvable() {
    Map<EquipmentSlot, Equipment> loadout = new EnumMap<>(EquipmentSlot.class);
    loadout.put(EquipmentSlot.BODY, eq(EquipmentSlot.BODY, "earth"));
    UiTheme result = UiThemeResolver.resolve(loadout);
    UiTheme expected = UiTheme.earth();
    assertEquals(expected.accentColor().g, result.accentColor().g, 0.001f);
  }

  @Test
  void darkThemeResolvable() {
    Map<EquipmentSlot, Equipment> loadout = new EnumMap<>(EquipmentSlot.class);
    loadout.put(EquipmentSlot.HAND, eq(EquipmentSlot.HAND, "dark"));
    UiTheme result = UiThemeResolver.resolve(loadout);
    UiTheme expected = UiTheme.dark();
    assertEquals(expected.skillSlotColor().b, result.skillSlotColor().b, 0.001f);
  }

  @Test
  void unknownThemeNameFallsBackToDefault() {
    // 未知のテーマ名 → defaultTheme
    Map<EquipmentSlot, Equipment> loadout = new EnumMap<>(EquipmentSlot.class);
    loadout.put(EquipmentSlot.HAND, eq(EquipmentSlot.HAND, "unknown_theme_xyz"));
    UiTheme result = UiThemeResolver.resolve(loadout);
    UiTheme expected = UiTheme.defaultTheme();
    assertEquals(expected.textColor().r, result.textColor().r, 0.001f);
  }

  @Test
  void nullLoadoutFallsBackToDefault() {
    UiTheme result = UiThemeResolver.resolve(null);
    UiTheme expected = UiTheme.defaultTheme();
    assertEquals(expected.textColor().r, result.textColor().r, 0.001f);
  }
}
