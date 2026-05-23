package core.infrastructure.save;

/**
 * UI 情報密度プリセット (§15-1 / §15-8)。
 *
 * <p>初回起動時に選択させ、{@link Settings} に永続化する。HUD の情報量を 3 段階で制御する。
 */
public enum UiPreset {

  /** 最小限の情報のみ表示 (HP / AP だけ)。 */
  MINIMAL,

  /** 標準的な情報量 (HP / AP / Soul / Phase)。デフォルト。 */
  STANDARD,

  /** 全情報を表示 (HP / AP / Soul / Phase / Gold + 操作ヒント常時表示)。 */
  INFO_RICH;

  /**
   * プリセット表示名 (日本語)。
   *
   * @return 日本語表示名
   */
  public String displayNameJa() {
    return switch (this) {
      case MINIMAL -> "ミニマル (最小限)";
      case STANDARD -> "標準";
      case INFO_RICH -> "情報マシマシ";
    };
  }

  /**
   * プリセット表示名 (英語)。
   *
   * @return 英語表示名
   */
  public String displayNameEn() {
    return switch (this) {
      case MINIMAL -> "Minimal";
      case STANDARD -> "Standard";
      case INFO_RICH -> "Information-rich";
    };
  }

  /**
   * プリセット説明文 (日本語)。
   *
   * @return 日本語説明
   */
  public String descriptionJa() {
    return switch (this) {
      case MINIMAL -> "HP と AP のみ表示";
      case STANDARD -> "HP / AP / ソウル / フェーズ";
      case INFO_RICH -> "全情報 + 操作ヒント常時表示";
    };
  }

  /**
   * プリセット説明文 (英語)。
   *
   * @return 英語説明
   */
  public String descriptionEn() {
    return switch (this) {
      case MINIMAL -> "Show HP and AP only";
      case STANDARD -> "HP / AP / Soul / Phase";
      case INFO_RICH -> "All info + persistent controls hint";
    };
  }
}
