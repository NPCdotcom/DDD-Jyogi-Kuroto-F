package core.presentation.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Disposable;

/**
 * プレゼン層が使うフォント群の集約。
 *
 * <p>{@code assets/fonts/DotGothic16-Regular.ttf} が存在すれば FreeType で日本語ピクセルフォントを 生成し、存在しなければ LibGDX
 * デフォルトの BitmapFont (英数のみ) にフォールバックする。
 *
 * <p>DotGothic16 は 16 ドット格子のピクセルフォントなので、ぼかし防止に Nearest フィルタを使い、 サイズも 16 / 32 / 48 と 16 の倍数で生成する
 * (pixel-perfect)。
 *
 * <p>フォント取得手順は {@code assets/fonts/README.md} および {@code docs/AssetGuidelines.md} を参照。
 */
public final class Fonts implements Disposable {

  private static final String JP_FONT_PATH = "fonts/DotGothic16-Regular.ttf";

  // DotGothic16 は 16 ドットフォント。倍数サイズだとアンチエイリアスをかけずに綺麗に出る。
  private static final int HUD_SIZE = 16;
  private static final int LARGE_SIZE = 32;
  private static final int TITLE_SIZE = 48;

  private final BitmapFont hud;
  private final BitmapFont large;
  private final BitmapFont title;
  private final FreeTypeFontGenerator generator;
  private final boolean japaneseAvailable;

  public Fonts() {
    FileHandle jp = Gdx.files.internal(JP_FONT_PATH);
    if (jp.exists()) {
      generator = new FreeTypeFontGenerator(jp);
      hud = generate(HUD_SIZE);
      large = generate(LARGE_SIZE);
      title = generate(TITLE_SIZE);
      japaneseAvailable = true;
    } else {
      generator = null;
      hud = new BitmapFont();
      large = new BitmapFont();
      large.getData().setScale(1.4f);
      title = new BitmapFont();
      title.getData().setScale(2.5f);
      japaneseAvailable = false;
      Gdx.app.log(
          "Fonts",
          "Pixel JP font not found at "
              + JP_FONT_PATH
              + ". Falling back to default BitmapFont (ASCII only). "
              + "See assets/fonts/README.md for setup.");
    }
  }

  private BitmapFont generate(int size) {
    FreeTypeFontParameter param = new FreeTypeFontParameter();
    param.size = size;
    // incremental: draw 時に必要な文字を都度ビットマップ化。日本語の全グリフを事前生成する
    // メモリコストを避けるため。
    param.incremental = true;
    // ピクセルフォントなので Nearest フィルタでドット感を維持 (Linear だとぼやける)。
    param.minFilter = Texture.TextureFilter.Nearest;
    param.magFilter = Texture.TextureFilter.Nearest;
    return generator.generateFont(param);
  }

  public boolean isJapaneseAvailable() {
    return japaneseAvailable;
  }

  public BitmapFont hud() {
    return hud;
  }

  public BitmapFont large() {
    return large;
  }

  public BitmapFont title() {
    return title;
  }

  @Override
  public void dispose() {
    hud.dispose();
    large.dispose();
    title.dispose();
    if (generator != null) {
      generator.dispose();
    }
  }
}
