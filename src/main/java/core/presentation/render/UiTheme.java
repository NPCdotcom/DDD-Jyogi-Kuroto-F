package core.presentation.render;

import com.badlogic.gdx.graphics.Color;

/**
 * 装備カテゴリ別 UI テーマ (§7-2 / W4-ε)。4 種の主要色を保持し、{@link HudRenderer} / {@link
 * core.presentation.screen.ScreenEffects} の色参照を一元化する。
 *
 * <p>LibGDX の {@link Color} はミュータブルだが、各ファクトリメソッドが {@code new Color(...)} で
 * 新しいインスタンスを返すため、呼出側が色を書き換えても他テーマへの影響はない。
 *
 * <p>テーマの文字列識別子 (例: {@code "fire"}) は equipment.json の {@code themeName} フィールドと 1:1 対応し、{@link
 * UiThemeResolver} が解決する。
 */
public record UiTheme(
    Color textColor, Color accentColor, Color skillSlotColor, Color warnFrameColor) {

  /** デフォルトテーマ。既存 {@link HudRenderer} の色定数と一致させ、テーマ未設定時に 視覚的変化が起きないことを保証する。 */
  public static UiTheme defaultTheme() {
    return new UiTheme(
        new Color(1f, 1f, 1f, 1f), // textColor  = WHITE
        new Color(0.95f, 0.85f, 0.3f, 1f), // accentColor = HudRenderer の CLEARED YELLOW
        new Color(0.55f, 1f, 0.6f, 1f), // skillSlotColor = HudRenderer のスキル装着済 GREEN
        new Color(0.85f, 0.15f, 0.20f, 1f) // warnFrameColor = LowHpWarning の赤
        );
  }

  /** 火炎テーマ (赤〜オレンジ系)。ダークブレード / クリムゾンボウ 等に対応。 */
  public static UiTheme fire() {
    return new UiTheme(
        new Color(1f, 0.9f, 0.7f, 1f), // textColor  = 暖色白
        new Color(1f, 0.45f, 0.1f, 1f), // accentColor = 深オレンジ
        new Color(1f, 0.6f, 0.2f, 1f), // skillSlotColor = 炎オレンジ
        new Color(0.95f, 0.2f, 0.05f, 1f) // warnFrameColor = 炎赤
        );
  }

  /** 氷結テーマ (青〜水色系)。フロストガントレット等に対応。 */
  public static UiTheme frost() {
    return new UiTheme(
        new Color(0.85f, 0.95f, 1f, 1f), // textColor  = 冷白
        new Color(0.3f, 0.8f, 1f, 1f), // accentColor = 氷青
        new Color(0.5f, 0.9f, 1f, 1f), // skillSlotColor = 水色
        new Color(0.1f, 0.4f, 0.85f, 1f) // warnFrameColor = 深海青
        );
  }

  /** 大地テーマ (茶〜緑系)。ドルイドのローブ / アースブレイサー等に対応。 */
  public static UiTheme earth() {
    return new UiTheme(
        new Color(0.9f, 0.85f, 0.7f, 1f), // textColor  = 砂白
        new Color(0.55f, 0.75f, 0.2f, 1f), // accentColor = 草緑
        new Color(0.6f, 0.85f, 0.3f, 1f), // skillSlotColor = 新緑
        new Color(0.55f, 0.35f, 0.1f, 1f) // warnFrameColor = 土茶
        );
  }

  /** 闇テーマ (紫〜濃紺系)。デーモンリング / シャドウクローク等に対応。 */
  public static UiTheme dark() {
    return new UiTheme(
        new Color(0.85f, 0.75f, 1f, 1f), // textColor  = 薄紫白
        new Color(0.7f, 0.3f, 1f, 1f), // accentColor = 紫
        new Color(0.65f, 0.4f, 1f, 1f), // skillSlotColor = 明紫
        new Color(0.4f, 0.05f, 0.55f, 1f) // warnFrameColor = 深紫
        );
  }

  /**
   * ライトテーマ (Wave 17 W17-β / #8、SettingsScreen の LIGHT 切替で適用される明色背景前提)。
   *
   * <p>明色背景でも視認できる十分濃いテキスト色 (text=濃灰) と、コントラスト高い accent (深青) で 構成。dark() との明確な色相差で「フォント色
   * Color.WHITE ハードコード残し」を視覚的に気づきやすくする (CTO #2 ステルステキストバグ防衛、ライト背景に白文字が溶ける現象を未然に検出)。
   */
  public static UiTheme light() {
    return new UiTheme(
        new Color(0.1f, 0.1f, 0.12f, 1f), // textColor  = 濃灰 (明色背景に対して高コントラスト)
        new Color(0.15f, 0.35f, 0.75f, 1f), // accentColor = 深青
        new Color(0.1f, 0.5f, 0.25f, 1f), // skillSlotColor = 深緑
        new Color(0.7f, 0.1f, 0.15f, 1f) // warnFrameColor = 深赤
        );
  }
}
