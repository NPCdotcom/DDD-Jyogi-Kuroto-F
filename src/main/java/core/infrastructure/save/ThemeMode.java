package core.infrastructure.save;

/**
 * UI テーマモード (Wave 17、SpecificationIssues.md #8)。
 *
 * <p>装備依存テーマ (旧 fire / frost / earth / dark 等) を廃止し、ユーザーが {@link
 * core.presentation.screen.SettingsScreen} で明示的に切り替える 2 値の単純化トグル。
 *
 * <ul>
 *   <li>{@link #DARK}: 暗色背景 + 明色テキスト (現状の Wave 16 まで標準だった HUD 色)。
 *   <li>{@link #LIGHT}: 明色背景 + 濃色テキスト (新規追加、明るい環境向け)。
 * </ul>
 */
public enum ThemeMode {
  LIGHT,
  DARK
}
