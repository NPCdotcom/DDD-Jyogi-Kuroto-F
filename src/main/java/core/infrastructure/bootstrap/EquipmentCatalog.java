package core.infrastructure.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.domain.card.CardId;
import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentId;
import core.domain.equipment.EquipmentSlot;
import core.domain.equipment.StatsBonus;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 装備マスタ (§15-9)。クラスパス上の {@code /equipment.json} を 1 度ロードし、{@code EquipmentId → Equipment} を構築する。
 *
 * <p>{@link CardCatalog} と同型。装備定義を JSON に集約し、チームが {@code equipment.json} を編集するだけで装備を 追加できる。不正な
 * JSON・重複 ID はロード時に例外を投げ起動時に早期検出する。{@link #all()} は登録順を保つ。
 */
public final class EquipmentCatalog {

  private static final Logger LOG = Logger.getLogger(EquipmentCatalog.class.getName());

  private static final String RESOURCE = "/equipment.json";

  private final Map<EquipmentId, Equipment> byId;

  private EquipmentCatalog(Map<EquipmentId, Equipment> byId) {
    this.byId = byId;
  }

  /**
   * クラスパスの {@code /equipment.json} を読み込んで EquipmentCatalog を構築する。
   *
   * <p>ファイル欠損 / I/O エラー時は SEVERE ログ + 空カタログを返す graceful fallback。InitialStateFactory の デフォルト装備
   * (ぼろい短剣) はハードコードなので、空カタログでも起動は可能。
   */
  public static EquipmentCatalog load() {
    try (InputStream in = EquipmentCatalog.class.getResourceAsStream(RESOURCE)) {
      if (in == null) {
        LOG.severe("equipment master not found on classpath: " + RESOURCE + " (loading empty)");
        return new EquipmentCatalog(new LinkedHashMap<>());
      }
      JsonNode root = new ObjectMapper().readTree(in);
      JsonNode arr = root.get("equipment");
      if (arr == null || !arr.isArray()) {
        LOG.severe("equipment.json must contain an 'equipment' array (loading empty)");
        return new EquipmentCatalog(new LinkedHashMap<>());
      }
      Map<EquipmentId, Equipment> map = new LinkedHashMap<>();
      for (JsonNode node : arr) {
        Equipment e = parse(node);
        if (map.put(e.id(), e) != null) {
          throw new IllegalStateException("duplicate equipment id: " + e.id().value());
        }
      }
      return new EquipmentCatalog(map);
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "failed to load " + RESOURCE + " (loading empty)", e);
      return new EquipmentCatalog(new LinkedHashMap<>());
    }
  }

  /** 指定 ID の装備を返す。未登録なら {@link IllegalArgumentException}。 */
  public Equipment get(EquipmentId id) {
    Objects.requireNonNull(id, "id");
    Equipment e = byId.get(id);
    if (e == null) {
      throw new IllegalArgumentException("unknown equipment id: " + id.value());
    }
    return e;
  }

  /** 登録順 (equipment.json 記載順) の全装備。装備画面の一覧に使う。 */
  public List<Equipment> all() {
    return List.copyOf(byId.values());
  }

  private static Equipment parse(JsonNode n) {
    JsonNode sb = n.get("statsBonus");
    if (sb == null) {
      throw new IllegalStateException("equipment.json: missing 'statsBonus'");
    }
    StatsBonus bonus =
        new StatsBonus(
            requiredInt(sb, "maxHp"),
            requiredInt(sb, "speed"),
            requiredInt(sb, "physicalAttack"),
            requiredInt(sb, "magicalAttack"),
            requiredInt(sb, "physicalDefense"),
            requiredInt(sb, "magicalDefense"));
    List<CardId> granted = new ArrayList<>();
    JsonNode grantedCardsNode = n.get("grantedCards");
    if (grantedCardsNode != null && grantedCardsNode.isArray()) {
      for (JsonNode c : grantedCardsNode) {
        granted.add(CardId.of(c.asText()));
      }
    }
    return new Equipment(
        EquipmentId.of(text(n, "id")),
        text(n, "displayName"),
        EquipmentSlot.valueOf(text(n, "slot")),
        bonus,
        granted);
  }

  private static String text(JsonNode n, String field) {
    JsonNode v = n == null ? null : n.get(field);
    if (v == null || v.isNull()) {
      throw new IllegalStateException("equipment.json: missing field '" + field + "'");
    }
    return v.asText();
  }

  /**
   * 必須数値フィールドを取得する。null / 欠落 / 非数値型は {@link IllegalStateException} で早期検出。 (CardCatalog.requiredInt
   * と同型、equipment.json のタイポ / 書き忘れによる起動 NPE を防ぐ)
   */
  private static int requiredInt(JsonNode n, String field) {
    JsonNode v = n == null ? null : n.get(field);
    if (v == null || v.isNull()) {
      throw new IllegalStateException("equipment.json: missing int field '" + field + "'");
    }
    if (!v.isNumber()) {
      throw new IllegalStateException(
          "equipment.json: field '" + field + "' must be a number, got: " + v);
    }
    return v.asInt();
  }
}
