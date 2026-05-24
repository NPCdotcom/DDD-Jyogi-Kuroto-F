package core.presentation.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.graphics.Color;
import core.infrastructure.save.ThemeMode;
import org.junit.jupiter.api.Test;

/**
 * Wave 17 W17-β / #8: {@link UiThemeResolver} の ThemeMode 直参照テスト (旧装備依存テーマ廃止)。
 *
 * <p>装備の themeName / loadout を引数に取る旧 API は完全廃止、Settings.themeMode (LIGHT/DARK) のみで解決。
 */
class UiThemeResolverTest {

  @Test
  void resolveDarkReturnsDarkTheme() {
    UiTheme theme = UiThemeResolver.resolve(ThemeMode.DARK);
    // dark() のテキスト色 (薄紫白) と一致
    assertEquals(new Color(0.85f, 0.75f, 1f, 1f), theme.textColor());
  }

  @Test
  void resolveLightReturnsLightTheme() {
    UiTheme theme = UiThemeResolver.resolve(ThemeMode.LIGHT);
    // light() のテキスト色 (濃灰、明色背景に対して高コントラスト) と一致
    assertEquals(new Color(0.1f, 0.1f, 0.12f, 1f), theme.textColor());
  }

  @Test
  void resolveNullFallsBackToDark() {
    // null は安全弁として DARK にフォールバック (graceful)
    UiTheme theme = UiThemeResolver.resolve(null);
    assertEquals(UiThemeResolver.resolve(ThemeMode.DARK).textColor(), theme.textColor());
  }
}
