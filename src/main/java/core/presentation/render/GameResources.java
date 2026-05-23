package core.presentation.render;

import com.badlogic.gdx.utils.Disposable;
import core.infrastructure.audio.SoundManager;
import core.infrastructure.bootstrap.CardCatalog;
import core.infrastructure.bootstrap.CardImageRegistry;
import core.infrastructure.bootstrap.EquipmentCatalog;
import core.infrastructure.save.Settings;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * LibGDX リソース系 3 コンポーネント (Fonts / SoundManager / CardImageRegistry) を集約する holder (Wave 8 W8-β)。
 *
 * <p>従来 {@code DddGame} に個別フィールドとして散在していた 3 コンポーネントを 1 つの集約クラスにまとめ、 dispose ライフサイクルを 1 箇所に閉じる。
 * record にしない理由は dispose() メソッドを持つため (final class が筋)。
 *
 * <p>防衛的 dispose: 各リソースの dispose を try-catch で個別に囲み、例外連鎖を完全防止する。load 失敗による 半作成状態からの dispose
 * でも安全に解放できる。
 *
 * <p>解放順序: CardImageRegistry → SoundManager → Fonts (重い・無依存なリソースから順、フォントは最後 = テキスト系のクラッシュログを
 * 最後まで出せる)。
 */
public final class GameResources implements Disposable {

  private static final Logger LOG = Logger.getLogger(GameResources.class.getName());

  private final Fonts fonts;
  private final SoundManager soundManager;
  private final CardImageRegistry cardImageRegistry;

  private GameResources(
      Fonts fonts, SoundManager soundManager, CardImageRegistry cardImageRegistry) {
    this.fonts = fonts;
    this.soundManager = soundManager;
    this.cardImageRegistry = cardImageRegistry;
  }

  /**
   * 3 リソースを順次初期化して GameResources を返す。
   *
   * @param settings 起動時設定 (BGM / SE 音量に使う)
   * @param cardCatalog Fonts のグリフ事前生成で displayName を集める用
   * @param equipmentCatalog 同上
   */
  public static GameResources load(
      Settings settings, CardCatalog cardCatalog, EquipmentCatalog equipmentCatalog) {
    Objects.requireNonNull(settings, "settings");
    Objects.requireNonNull(cardCatalog, "cardCatalog");
    Objects.requireNonNull(equipmentCatalog, "equipmentCatalog");
    Fonts fonts = new Fonts(cardCatalog, equipmentCatalog);
    SoundManager soundManager = new SoundManager(settings);
    CardImageRegistry cardImageRegistry = CardImageRegistry.load();
    return new GameResources(fonts, soundManager, cardImageRegistry);
  }

  public Fonts fonts() {
    return fonts;
  }

  public SoundManager soundManager() {
    return soundManager;
  }

  public CardImageRegistry cardImageRegistry() {
    return cardImageRegistry;
  }

  /**
   * 完全なる防衛的 dispose (Wave 8 W8-β CTO レビュー反映)。
   *
   * <p>各リソースの dispose を try-catch で個別に囲み、1 つが例外を投げても後続のリソース解放がスキップ されないようにする。ハッカソン後の長期運用 (Phase D
   * Android) でのクラッシュ率に直結する重要箇所、 メモリリーク危険性を完全にゼロにする思想。
   *
   * <p>解放順序: CardImageRegistry (Texture[]) → SoundManager (Music + Sound) → Fonts
   * (FreeTypeFontGenerator + BitmapFont)。
   */
  @Override
  public void dispose() {
    if (cardImageRegistry != null) {
      try {
        cardImageRegistry.dispose();
      } catch (Exception e) {
        LOG.warning("CardImageRegistry dispose failed: " + e.getMessage());
      }
    }
    if (soundManager != null) {
      try {
        soundManager.dispose();
      } catch (Exception e) {
        LOG.warning("SoundManager dispose failed: " + e.getMessage());
      }
    }
    if (fonts != null) {
      try {
        fonts.dispose();
      } catch (Exception e) {
        LOG.warning("Fonts dispose failed: " + e.getMessage());
      }
    }
  }
}
