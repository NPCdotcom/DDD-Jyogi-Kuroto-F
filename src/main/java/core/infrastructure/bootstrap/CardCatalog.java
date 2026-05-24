package core.infrastructure.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.domain.card.Card;
import core.domain.card.CardEffect;
import core.domain.card.CardElement;
import core.domain.card.CardId;
import core.domain.card.CardRarity;
import core.domain.card.CardTag;
import core.domain.card.TrapLifetime;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * カードマスタ (§15-3)。クラスパス上の {@code /cards.json} を 1 度ロードし、{@code CardId → Card} のマップを構築する。
 *
 * <p>カード定義をハードコード Java から JSON に分離することで、チームメイトが {@code cards.json} を編集するだけで カードを追加できる。不正な
 * JSON・未知の効果型・重複 ID はロード時に例外を投げ、起動時に早期検出する。
 *
 * <p>{@link #all()} は {@code cards.json} の登録順を保つ (カード図鑑の表示順に使う)。
 */
public final class CardCatalog {

  private static final Logger LOG = Logger.getLogger(CardCatalog.class.getName());

  private static final String RESOURCE = "/cards.json";

  private final Map<CardId, Card> byId;

  private CardCatalog(Map<CardId, Card> byId) {
    this.byId = byId;
  }

  /**
   * クラスパスの {@code /cards.json} を読み込んで CardCatalog を構築する。
   *
   * <p>ファイル欠損 / I/O エラー時は SEVERE ログを出力して**最小フォールバックカタログ** (斬撃 1 種のみ) を返す。提出 zip
   * にファイル抜けがあっても起動だけは保証する (graceful)。フォールバック時はゲーム性は崩れるが、 ユーザーに「カードマスタが読めません」と気付いてもらうための救済措置。
   */
  public static CardCatalog load() {
    try (InputStream in = CardCatalog.class.getResourceAsStream(RESOURCE)) {
      if (in == null) {
        LOG.severe("card master not found on classpath: " + RESOURCE + " (loading fallback)");
        return fallback();
      }
      JsonNode root = new ObjectMapper().readTree(in);
      JsonNode cards = root.get("cards");
      if (cards == null || !cards.isArray()) {
        LOG.severe("cards.json must contain a 'cards' array (loading fallback)");
        return fallback();
      }
      Map<CardId, Card> map = new LinkedHashMap<>();
      for (JsonNode node : cards) {
        Card card = parseCard(node);
        if (map.put(card.id(), card) != null) {
          throw new IllegalStateException("duplicate card id in cards.json: " + card.id().value());
        }
      }
      return new CardCatalog(map);
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "failed to load " + RESOURCE + " (loading fallback)", e);
      return fallback();
    }
  }

  /**
   * フォールバックカタログ。「斬撃」1 種だけのミニマル構成で、起動不能を回避する。
   *
   * <p>本来のカードマスタが読めない場合の救済措置で、本番運用では使われない想定。チームメイトが cards.json を 編集してビルドし直すまでのつなぎ。
   */
  private static CardCatalog fallback() {
    Map<CardId, Card> map = new LinkedHashMap<>();
    Card fallbackCard =
        new Card(
            CardId.of("zangeki"),
            "斬撃 (fallback)",
            1,
            CardTag.ATTACK,
            CardElement.PHYSICAL,
            new CardEffect.Damage(3));
    map.put(fallbackCard.id(), fallbackCard);
    return new CardCatalog(map);
  }

  /** 指定 ID のカードを返す。未登録なら {@link IllegalArgumentException}。 */
  public Card get(CardId id) {
    Objects.requireNonNull(id, "id");
    Card card = byId.get(id);
    if (card == null) {
      throw new IllegalArgumentException("unknown card id: " + id.value());
    }
    return card;
  }

  /** 登録順 (cards.json 記載順) の全カード。図鑑表示・抽選プールに使う。 */
  public List<Card> all() {
    return List.copyOf(byId.values());
  }

  private static Card parseCard(JsonNode n) {
    return new Card(
        CardId.of(text(n, "id")),
        text(n, "displayName"),
        requiredInt(n, "apCost"),
        CardTag.valueOf(text(n, "tag")),
        CardElement.valueOf(text(n, "element")),
        parseEffect(n.get("effect")),
        parseRarity(n.get("rarity")));
  }

  /**
   * カードレアリティを graceful 読込 (Wave 11 W11-β)。JSON で未指定なら {@link Optional#empty()}、 値が enum 名と一致しなければ
   * WARNING ログ + Optional.empty() でフォールバック。
   *
   * <p>cards.json は全カードへの rarity 付与が完了していないため (チームメイト領域)、未指定許容を維持。 起動時クラッシュ回避が最優先 ({@link
   * EquipmentCatalog} の themeName / iconPath と同型)。
   */
  private static Optional<CardRarity> parseRarity(JsonNode r) {
    if (r == null || r.isNull()) {
      return Optional.empty();
    }
    String raw = r.asText();
    try {
      return Optional.of(CardRarity.valueOf(raw.toUpperCase()));
    } catch (IllegalArgumentException e) {
      LOG.warning("cards.json: unknown rarity '" + raw + "' (falling back to empty/COMMON)");
      return Optional.empty();
    }
  }

  private static CardEffect parseEffect(JsonNode e) {
    String type = text(e, "type");
    return switch (type) {
      case "Damage" ->
          new CardEffect.Damage(
              requiredInt(e, "baseValue"),
              optionalInt(e, "range", 1),
              optionalInt(e, "areaRadius", 0));
      case "Move" -> new CardEffect.Move(requiredInt(e, "distance"));
      case "Buff" ->
          new CardEffect.Buff(
              CardEffect.BuffKind.valueOf(text(e, "kind")),
              requiredInt(e, "amount"),
              requiredInt(e, "durationTurns"));
      case "Trap" ->
          new CardEffect.Trap(requiredInt(e, "baseValue"), parseLifetime(e.get("lifetime")));
      default -> throw new IllegalStateException("unknown card effect type: " + type);
    };
  }

  private static TrapLifetime parseLifetime(JsonNode l) {
    String type = text(l, "type");
    return switch (type) {
      case "UntilStepped" -> TrapLifetime.UntilStepped.INSTANCE;
      case "Turns" -> new TrapLifetime.Turns(requiredInt(l, "remaining"));
      default -> throw new IllegalStateException("unknown trap lifetime type: " + type);
    };
  }

  private static String text(JsonNode n, String field) {
    JsonNode v = n == null ? null : n.get(field);
    if (v == null || v.isNull()) {
      throw new IllegalStateException("cards.json: missing field '" + field + "'");
    }
    return v.asText();
  }

  /**
   * オプション数値フィールドを取得する (Wave 12 W12-α、graceful 読込)。null / 欠落 / 非数値型は {@code defaultValue} に
   * フォールバック。非数値型のみ WARN ログを残す (欠落は仕様上正常なのでログ不要)。
   *
   * <p>射程 (range) や爆風半径 (areaRadius) のように「未指定 = デフォルト値」が許容される field 向け。cards.json の 起動時クラッシュ回避が最優先
   * (CardRarity の parseRarity と同型)。
   */
  private static int optionalInt(JsonNode n, String field, int defaultValue) {
    JsonNode v = n == null ? null : n.get(field);
    if (v == null || v.isNull()) {
      return defaultValue;
    }
    if (!v.isNumber()) {
      LOG.warning(
          "cards.json: field '"
              + field
              + "' is not a number (falling back to "
              + defaultValue
              + "): "
              + v);
      return defaultValue;
    }
    return v.asInt();
  }

  /**
   * 必須数値フィールドを取得する。null / 欠落 / 非数値型は {@link IllegalStateException} で早期検出。
   *
   * <p>素の {@code .get(field).asInt()} は JsonNode が null だと NPE を投げ、起動時にスタックトレース無しで詰む。
   * 本ヘルパはタイポ・キー欠落を cards.json のどのフィールドかメッセージで明示する。
   */
  private static int requiredInt(JsonNode n, String field) {
    JsonNode v = n == null ? null : n.get(field);
    if (v == null || v.isNull()) {
      throw new IllegalStateException("cards.json: missing int field '" + field + "'");
    }
    if (!v.isNumber()) {
      throw new IllegalStateException(
          "cards.json: field '" + field + "' must be a number, got: " + v);
    }
    return v.asInt();
  }
}
