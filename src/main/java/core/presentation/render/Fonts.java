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

  /**
   * 事前グリフ生成用の文字集合 (本ゲームで使用する全文字を網羅)。
   *
   * <p>LibGDX 1.14.0 + JDK 25 で `param.incremental = true` を有効にすると、ノード選択ポップアップ生成時など
   * 「未生成の日本語グリフを実行時に追加生成する」パスで {@code gdx64.dll} の {@code Pixmap.drawPixmap} 内で
   * EXCEPTION_ACCESS_VIOLATION が発生する (`hs_err_pid*.log` でクラッシュ確認済)。
   *
   * <p>対策として {@code incremental = false} に切り替え、起動時に本文字集合を一括生成することで native call を
   * 戦闘中・層遷移中に発火させないようにする。本作で使う日本語文字を網羅:
   *
   * <ul>
   *   <li>ひらがな全 + 濁点・半濁点・拗音
   *   <li>カタカナ全 + 濁点・半濁点・拗音
   *   <li>Strings.Ja / 動的文字列 (Card / Equipment / Buff / SoulTree ノード / Event) で使う漢字
   *   <li>ASCII (英数 + 記号) は {@link FreeTypeFontGenerator#DEFAULT_CHARS} 由来
   * </ul>
   */
  private static final String GAME_GLYPH_CHARS =
      // ひらがな
      "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをんぁぃぅぇぉっゃゅょー"
          + "がぎぐげござじずぜぞだぢづでどばびぶべぼぱぴぷぺぽ"
          // カタカナ
          + "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲンァィゥェォッャュョー"
          + "ガギグゲゴザジズゼゾダヂヅデドバビブベボパピプペポヴ"
          // 漢字 (Strings.Ja + 動的文字列で登場するもの + ハッカソン本番までに追加されうるもの)
          + "一二三四五六七八九十百千万円層末選操作説明攻防速度物魔強化個体撃破金貨所持"
          + "斬撃移動移行進入到達敵獲得初心者上下左右待機終了開始終わり始める"
          + "魂魔法弾火球炎陣短剣靴鉄皮膚帳爆瞬歩弱攻防御回避反撃強打"
          + "効果回復消費残量必要選択画面戦闘層階段全勝負荷重死亡覚醒"
          + "見習鍛冶屋商人冒険者勇者賢者武装防具備品装備解放購入販売"
          + "祠遺跡神秘魔導師術師罠書"
          + "字使方法決定取消"
          // 記号 (一部 ASCII にないもの)
          + "→←↑↓・×÷±％";

  /** ASCII + 数字 + 記号 (FreeType デフォルト相当)。 */
  private static final String ASCII_CHARS =
      " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";

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
    // JDK 25 + LibGDX 1.14 互換: incremental グリフ生成は層遷移時 (NodeChoicePopup 生成) に
    // gdx64.dll Pixmap.drawPixmap で AV クラッシュを引き起こすため、事前生成に切替。
    // 本作で使う日本語文字を GAME_GLYPH_CHARS + ASCII_CHARS で網羅して param.characters に渡す。
    param.incremental = false;
    param.characters = ASCII_CHARS + GAME_GLYPH_CHARS;
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
