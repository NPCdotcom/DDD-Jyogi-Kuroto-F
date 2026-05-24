package core.infrastructure.save;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * アプリ全体の設定 (§15-1 / §15-8)。セーブスロットとは独立した永続設定。
 *
 * <p>Jackson でシリアライズ / デシリアライズする。フィールド欠損時はデフォルト値で補完するため、 {@code @JsonProperty(defaultValue=...)}
 * は不使用。 {@link SettingsManager} が破損時デフォルトに戻す責務を持つ。
 *
 * <p>音量値は 0.0〜1.0 の範囲で保持する。{@link #DEFAULT} を起点にして immutable コピーを作る。
 *
 * <p>Wave 17 W17-α: {@link ThemeMode} field 追加 (SpecificationIssues.md #8 ライト/ダーク 2 トグル化)。 旧
 * settings.json (themeMode 欠落) は compact constructor で {@link ThemeMode#DARK} に正規化する graceful
 * migration (Wave 6 W6-β 同型)。
 */
public record Settings(
    @JsonProperty("bgmVolume") float bgmVolume,
    @JsonProperty("seVolume") float seVolume,
    @JsonProperty("fullscreen") boolean fullscreen,
    @JsonProperty("uiPreset") UiPreset uiPreset,
    @JsonProperty("themeMode") ThemeMode themeMode) {

  /** BGM 音量の最小値。 */
  public static final float VOLUME_MIN = 0.0f;

  /** BGM 音量の最大値。 */
  public static final float VOLUME_MAX = 1.0f;

  /** デフォルト設定: BGM 0.7, SE 0.8, ウィンドウモード, 標準プリセット, ダークテーマ。 */
  public static final Settings DEFAULT =
      new Settings(0.7f, 0.8f, false, UiPreset.STANDARD, ThemeMode.DARK);

  public Settings {
    if (Float.isNaN(bgmVolume) || bgmVolume < VOLUME_MIN || bgmVolume > VOLUME_MAX) {
      throw new IllegalArgumentException("bgmVolume out of range: " + bgmVolume);
    }
    if (Float.isNaN(seVolume) || seVolume < VOLUME_MIN || seVolume > VOLUME_MAX) {
      throw new IllegalArgumentException("seVolume out of range: " + seVolume);
    }
    if (uiPreset == null) {
      uiPreset = UiPreset.STANDARD;
    }
    // Wave 17 W17-α: 旧 settings.json (themeMode 欠落) は DARK にフォールバック (graceful migration)
    if (themeMode == null) {
      themeMode = ThemeMode.DARK;
    }
  }

  /**
   * BGM 音量を変更した新しい Settings を返す (immutable コピー)。
   *
   * @param newVolume 0.0〜1.0 の新しい音量
   * @return 更新済み Settings
   */
  public Settings withBgmVolume(float newVolume) {
    return new Settings(
        Math.clamp(newVolume, VOLUME_MIN, VOLUME_MAX), seVolume, fullscreen, uiPreset, themeMode);
  }

  /**
   * SE 音量を変更した新しい Settings を返す (immutable コピー)。
   *
   * @param newVolume 0.0〜1.0 の新しい音量
   * @return 更新済み Settings
   */
  public Settings withSeVolume(float newVolume) {
    return new Settings(
        bgmVolume, Math.clamp(newVolume, VOLUME_MIN, VOLUME_MAX), fullscreen, uiPreset, themeMode);
  }

  /**
   * フルスクリーン設定を変更した新しい Settings を返す。
   *
   * @param newFullscreen true でフルスクリーン
   * @return 更新済み Settings
   */
  public Settings withFullscreen(boolean newFullscreen) {
    return new Settings(bgmVolume, seVolume, newFullscreen, uiPreset, themeMode);
  }

  /**
   * UI プリセットを変更した新しい Settings を返す。
   *
   * @param newPreset 新しいプリセット
   * @return 更新済み Settings
   */
  public Settings withUiPreset(UiPreset newPreset) {
    java.util.Objects.requireNonNull(newPreset, "newPreset");
    return new Settings(bgmVolume, seVolume, fullscreen, newPreset, themeMode);
  }

  /**
   * テーマモードを変更した新しい Settings を返す (Wave 17 W17-α、SpecificationIssues.md #8)。
   *
   * @param newMode 新しいテーマモード (LIGHT / DARK)
   * @return 更新済み Settings
   */
  public Settings withThemeMode(ThemeMode newMode) {
    java.util.Objects.requireNonNull(newMode, "newMode");
    return new Settings(bgmVolume, seVolume, fullscreen, uiPreset, newMode);
  }
}
