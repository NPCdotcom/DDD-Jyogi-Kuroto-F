package core.infrastructure.bootstrap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.domain.card.CardId;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * カード ID → {@link Texture} のマッピングを保持するレジストリ (§15-3 カード画像統合)。
 *
 * <p>起動時に classpath の {@code /card_image_map.json} を読み、{@code assets/cards/} 配下の PNG を Texture
 * として一括ロードする。未マッピング ID やファイル欠損は fallback テクスチャ ({@code assets/icons/cards/test.png}) を返す graceful
 * 設計 ({@code Fonts} と同じ思想)。
 *
 * <p>テクスチャはピクセルアート前提で {@code Nearest} フィルタを適用する。{@link #dispose()} で すべての Texture を解放する。
 */
public final class CardImageRegistry implements Disposable {

  private static final Logger LOG = Logger.getLogger(CardImageRegistry.class.getName());
  private static final String MAP_RESOURCE = "/card_image_map.json";
  private static final String DEFAULT_IMAGE_DIR = "cards/";
  private static final String DEFAULT_EXT = ".png";
  private static final String FALLBACK_PATH = "icons/cards/test.png";

  /** Wave 10 W10-γ: 全カード共通の枠テクスチャ (合成描画用、{@link core.presentation.render.CardRenderer} 経由)。 */
  private static final String FRAME_PATH = "icons/cards/card_frame.png";

  private final Map<CardId, Texture> textures = new HashMap<>();
  private Texture fallbackTexture;

  /** Wave 10 W10-γ: カード枠テクスチャ (起動時に 1 度だけロード、null 時は枠なし描画にフォールバック)。 */
  private Texture frameTexture;

  private CardImageRegistry() {}

  /** カード枠テクスチャ (Wave 10 W10-γ)。欠落時は null、呼出側は null チェックで描画スキップ。 */
  public Texture frame() {
    return frameTexture;
  }

  /**
   * クラスパスからマッピングをロードし、各 PNG を Texture 化する。LibGDX の {@code Gdx} 初期化後に呼ぶこと。 欠損は警告ログ + fallback で吸収する。
   */
  public static CardImageRegistry load() {
    CardImageRegistry registry = new CardImageRegistry();
    try (InputStream is = CardImageRegistry.class.getResourceAsStream(MAP_RESOURCE)) {
      if (is == null) {
        LOG.warning("card_image_map.json not found on classpath; using fallback only");
      } else {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(is);
        String imageDir = root.has("imageDir") ? root.get("imageDir").asText() : DEFAULT_IMAGE_DIR;
        String ext = root.has("fileExtension") ? root.get("fileExtension").asText() : DEFAULT_EXT;
        JsonNode mappings = root.get("mappings");
        if (mappings != null && mappings.isObject()) {
          Iterator<Map.Entry<String, JsonNode>> it = mappings.fields();
          while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String cardId = entry.getKey();
            String fileBase = entry.getValue().asText();
            String path = imageDir + fileBase + ext;
            registry.tryLoadTexture(CardId.of(cardId), path);
          }
        }
      }
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Failed to load card_image_map.json (using fallback only)", e);
    }
    registry.loadFallback();
    registry.loadFrame();
    return registry;
  }

  /** Wave 10 W10-γ: 枠 Texture をロード。欠落時は null のまま (CardRenderer 側で null チェック → 枠なし描画)。 */
  private void loadFrame() {
    try {
      FileHandle fh = Gdx.files.internal(FRAME_PATH);
      if (fh.exists()) {
        frameTexture = new Texture(fh);
        applyPixelFilter(frameTexture);
      } else {
        LOG.warning("Card frame image not found: " + FRAME_PATH);
      }
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Failed to load card frame texture", e);
    }
  }

  private void loadFallback() {
    try {
      FileHandle fh = Gdx.files.internal(FALLBACK_PATH);
      if (fh.exists()) {
        fallbackTexture = new Texture(fh);
        applyPixelFilter(fallbackTexture);
      } else {
        LOG.warning("Fallback card image not found: " + FALLBACK_PATH);
      }
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Failed to load fallback card image", e);
    }
  }

  private void tryLoadTexture(CardId id, String path) {
    try {
      FileHandle fh = Gdx.files.internal(path);
      if (!fh.exists()) {
        LOG.fine("Card image not found (using fallback): " + path);
        return;
      }
      Texture tex = new Texture(fh);
      applyPixelFilter(tex);
      textures.put(id, tex);
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Failed to load card image " + path, e);
    }
  }

  private static void applyPixelFilter(Texture tex) {
    tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
  }

  /**
   * カード ID に対応する Texture を返す。未マッピング・欠損時は fallback を返す。 fallback も欠損なら {@code null}。
   *
   * @param id カード ID
   * @return Texture または null
   */
  public Texture get(CardId id) {
    if (id == null) {
      return fallbackTexture;
    }
    Texture tex = textures.get(id);
    return tex != null ? tex : fallbackTexture;
  }

  /** マッピング数 (テスト用)。 */
  public int mappedCount() {
    return textures.size();
  }

  /** マッピング済みカード ID 集合 (テスト・図鑑ヘッダ表示用、防御コピー)。 */
  public java.util.Set<CardId> mappedIds() {
    return Collections.unmodifiableSet(textures.keySet());
  }

  /** fallback テクスチャの有無 (テスト用)。 */
  public boolean hasFallback() {
    return fallbackTexture != null;
  }

  @Override
  public void dispose() {
    for (Texture t : textures.values()) {
      t.dispose();
    }
    textures.clear();
    if (fallbackTexture != null) {
      fallbackTexture.dispose();
      fallbackTexture = null;
    }
    if (frameTexture != null) {
      frameTexture.dispose();
      frameTexture = null;
    }
  }
}
