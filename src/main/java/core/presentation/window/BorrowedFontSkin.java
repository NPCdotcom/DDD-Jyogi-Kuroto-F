package core.presentation.window;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import java.util.Objects;

/** 借用した共有フォントを破棄せず、自身が所有する Skin リソースだけを破棄する。 */
final class BorrowedFontSkin extends Skin {

  private static final String DEFAULT_FONT_NAME = "default-font";

  BorrowedFontSkin(BitmapFont borrowedFont) {
    add(DEFAULT_FONT_NAME, Objects.requireNonNull(borrowedFont, "borrowedFont"), BitmapFont.class);
  }

  @Override
  public void dispose() {
    if (has(DEFAULT_FONT_NAME, BitmapFont.class)) {
      remove(DEFAULT_FONT_NAME, BitmapFont.class);
    }
    super.dispose();
  }
}
