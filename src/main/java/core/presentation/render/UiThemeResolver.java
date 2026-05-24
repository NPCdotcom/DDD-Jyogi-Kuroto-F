package core.presentation.render;

import core.infrastructure.save.ThemeMode;

/**
 * {@link ThemeMode} から {@link UiTheme} を解決するユーティリティ (Wave 17 W17-β、§7-2 / W4-ε 改訂)。
 *
 * <p>Wave 17 W17-β / SpecificationIssues.md #8: 旧仕様の「装備依存テーマ (fire/frost/earth/dark)」は廃止し、 ユーザー が
 * SettingsScreen で明示的に切替える「LIGHT / DARK」の 2 トグル化に統一した。装備テーマ機能は
 * 「個性的な見た目」を提供したが、新装備追加時のテーマ追加コストと「気づかぬうちに色が変わる」UX 問題から Settings 直参照のシンプル設計に置換。
 *
 * <p>Equipment.themeName field は equipment.json + Equipment record に残置 (record API 維持、後方互換確保) するが、
 * 本リゾルバからは参照されなくなる。UiTheme の旧 4 factory (fire/frost/earth/dark) は将来の装備ビジュアル機能復活 余地として残置。
 *
 * <p>純粋関数として実装。インスタンス生成は不要。
 */
public final class UiThemeResolver {

  private UiThemeResolver() {}

  /**
   * テーマモードから UI テーマを解決する (Wave 17 W17-β、新仕様)。
   *
   * @param mode テーマモード (LIGHT / DARK、null は DARK にフォールバック)
   * @return 対応する {@link UiTheme}
   */
  public static UiTheme resolve(ThemeMode mode) {
    if (mode == null) {
      return UiTheme.dark();
    }
    return switch (mode) {
      case LIGHT -> UiTheme.light();
      case DARK -> UiTheme.dark();
    };
  }
}
