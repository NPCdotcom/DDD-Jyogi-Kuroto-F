package core.domain.entity;

/**
 * ステ種別 (§15-4 6 ステ)。{@code Stats} の各フィールドを enum で表現し、文字列マッチを避ける。
 *
 * <p>用途: {@code StatsBonus.dominantStat()} で最大補正のステを返す、SoulTree ノードアイコンの分岐、 装備差分表示の見出しなど。
 */
public enum StatKind {
  HP("HP", "HP"),
  SPEED("速度", "Speed"),
  PHYSICAL_ATTACK("物攻", "Phys ATK"),
  MAGICAL_ATTACK("魔攻", "Mag ATK"),
  PHYSICAL_DEFENSE("物防", "Phys DEF"),
  MAGICAL_DEFENSE("魔防", "Mag DEF");

  private final String displayNameJa;
  private final String displayNameEn;

  StatKind(String displayNameJa, String displayNameEn) {
    this.displayNameJa = displayNameJa;
    this.displayNameEn = displayNameEn;
  }

  public String displayNameJa() {
    return displayNameJa;
  }

  public String displayNameEn() {
    return displayNameEn;
  }
}
