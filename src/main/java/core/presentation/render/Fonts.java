package core.presentation.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Disposable;
import core.domain.entity.EnemyKind;
import core.domain.layer.LayerEndNode;
import core.domain.tree.SoulTree;
import core.domain.tree.TreeNode;
import core.infrastructure.bootstrap.InitialStateFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.TreeSet;

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
          // 追加 (Strings.Ja の旧 GAME_GLYPH_CHARS 欠落分、5 視点レビューで指摘された豆腐文字対策)
          + "踏成功敗北帰新挑拒否付与数設置矢印累計返却戻黄倒次向指中手札"
          // 記号 (一部 ASCII にないもの)
          + "→←↑↓・×÷±％—〜";

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
    // 2 層カバレッジ: 層 1 = 静的ベース (GAME_GLYPH_CHARS + ASCII)、層 2 = Strings.Ja + ドメイン
    // displayName を Reflection / 動的呼出で収集。例外時は層 1 のみで動作 (fallback)。
    param.incremental = false;
    param.characters = buildAllCharacters();
    // ピクセルフォントなので Nearest フィルタでドット感を維持 (Linear だとぼやける)。
    param.minFilter = Texture.TextureFilter.Nearest;
    param.magFilter = Texture.TextureFilter.Nearest;
    return generator.generateFont(param);
  }

  /**
   * 事前グリフ生成に渡す全文字集合を構築する (2 層カバレッジ)。
   *
   * <ul>
   *   <li>層 1: 静的ベース (ASCII + ひらがな全 + カタカナ全 + 主要漢字 + 記号)
   *   <li>層 2: Reflection で {@code Strings.Ja} の全 String + ドメインの動的 displayName を収集
   * </ul>
   *
   * <p>層 2 の動的収集で例外が出た場合は層 1 のみで動作 ({@code Gdx.app.log} でログ出力)。
   * これで Card / Equipment / SoulTree の追加時にも自動的にグリフ生成され、豆腐文字を予防する。
   */
  private static String buildAllCharacters() {
    TreeSet<Character> chars = new TreeSet<>();
    addAllChars(chars, ASCII_CHARS);
    addAllChars(chars, GAME_GLYPH_CHARS);
    try {
      collectDynamicCharacters(chars);
    } catch (RuntimeException e) {
      Gdx.app.log(
          "Fonts",
          "Dynamic char collection failed, falling back to static base only: " + e.getMessage());
    }
    StringBuilder sb = new StringBuilder(chars.size());
    chars.forEach(sb::append);
    return sb.toString();
  }

  private static void addAllChars(TreeSet<Character> set, String source) {
    for (int i = 0; i < source.length(); i++) {
      set.add(source.charAt(i));
    }
  }

  /**
   * 層 2: Strings.Ja の全 String + ドメイン (EnemyKind / SoulTree / InitialStateFactory のカード &
   * 装備マスタ / LayerEndNode.Rest) の displayName を Reflection / 動的呼出で集約し、{@code set} に
   * 追加する。
   *
   * <p>新カード / 新装備を追加した場合、本メソッドが自動で文字を拾うため、{@code GAME_GLYPH_CHARS} の手動拡張が
   * 基本的に不要になる (将来の事故予防、5 視点レビュー teammate-pov の懸念対応)。
   */
  private static void collectDynamicCharacters(TreeSet<Character> set) {
    // Strings.Ja の全 static final String を抽出
    for (Field f : Strings.Ja.class.getDeclaredFields()) {
      if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
        try {
          Object value = f.get(null);
          if (value instanceof String s) {
            addAllChars(set, s);
          }
        } catch (IllegalAccessException ignored) {
          // 個別フィールド失敗は無視 (他のフィールドは続行)
        }
      }
    }
    // EnemyKind.displayName (SLIME / ELITE_SLIME)
    for (EnemyKind kind : EnemyKind.values()) {
      addAllChars(set, kind.displayName());
    }
    // SoulTree.allNodes() の displayName (17 ノード、§15-7)
    for (TreeNode node : SoulTree.allNodes().values()) {
      addAllChars(set, node.displayName());
    }
    // LayerEndNode 全 5 種の動的 displayName を網羅 (NodeChoicePopup 文字黒化対策、
    // §15-8 5 候補抽選で表示される全選択肢のグリフを事前生成)。
    addAllChars(set, new LayerEndNode.HpMaxUp(5).displayName()); // "HP +5"
    addAllChars(set, new LayerEndNode.SpeedUp(1).displayName()); // "速度 +1"
    addAllChars(set, new LayerEndNode.Rest().displayName()); // "HP 全回復"
    // Shop の固定フォーマット "ショップ: %s (金貨 %d)" の文字を網羅
    // (引数 Card の displayName は別途 InitialStateFactory のマスタから収集済)。
    addAllChars(
        set, new LayerEndNode.Shop(0, InitialStateFactory.dashCard()).displayName());
    // Event の displayLabel は DungeonScreen.createNodeChoicePopup でハードコード渡し。
    // 現状の唯一の Event displayLabel をここに転写 (M2 で候補プールを集約して自動化予定)。
    addAllChars(set, "ソウルの祠 (ソウル +30 / HP -5)");
    // InitialStateFactory のカードマスタ (11 種、ADR-24 + ADR-30)
    addAllChars(set, InitialStateFactory.zangetuCard().displayName());
    addAllChars(set, InitialStateFactory.magicBoltCard().displayName());
    addAllChars(set, InitialStateFactory.strongStrikeCard().displayName());
    addAllChars(set, InitialStateFactory.fireballCard().displayName());
    addAllChars(set, InitialStateFactory.emberShotCard().displayName());
    addAllChars(set, InitialStateFactory.blazeNovaCard().displayName());
    addAllChars(set, InitialStateFactory.blinkStepCard().displayName());
    addAllChars(set, InitialStateFactory.flameCircleCard().displayName());
    addAllChars(set, InitialStateFactory.dashCard().displayName());
    addAllChars(set, InitialStateFactory.ironSkinCard().displayName());
    addAllChars(set, InitialStateFactory.arcaneVeilCard().displayName());
    // InitialStateFactory の装備マスタ (2 種)
    addAllChars(set, InitialStateFactory.tatteredBoots().displayName());
    addAllChars(set, InitialStateFactory.tatteredDagger().displayName());
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
