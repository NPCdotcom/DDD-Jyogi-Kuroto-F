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

/**
 * 装備マスタ (§15-9)。クラスパス上の {@code /equipment.json} を 1 度ロードし、{@code EquipmentId → Equipment} を構築する。
 *
 * <p>{@link CardCatalog} と同型。装備定義を JSON に集約し、チームが {@code equipment.json} を編集するだけで装備を 追加できる。不正な
 * JSON・重複 ID はロード時に例外を投げ起動時に早期検出する。{@link #all()} は登録順を保つ。
 */
public final class EquipmentCatalog {

  private static final String RESOURCE = "/equipment.json";

  private final Map<EquipmentId, Equipment> byId;

  private EquipmentCatalog(Map<EquipmentId, Equipment> byId) {
    this.byId = byId;
  }

  /** クラスパスの {@code /equipment.json} を読み込んで EquipmentCatalog を構築する。 */
  public static EquipmentCatalog load() {
    try (InputStream in = EquipmentCatalog.class.getResourceAsStream(RESOURCE)) {
      if (in == null) {
        throw new IllegalStateException("equipment master not found on classpath: " + RESOURCE);
      }
      JsonNode root = new ObjectMapper().readTree(in);
      JsonNode arr = root.get("equipment");
      if (arr == null || !arr.isArray()) {
        throw new IllegalStateException("equipment.json must contain an 'equipment' array");
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
      throw new IllegalStateException("failed to load " + RESOURCE, e);
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
            sb.get("maxHp").asInt(),
            sb.get("speed").asInt(),
            sb.get("physicalAttack").asInt(),
            sb.get("magicalAttack").asInt(),
            sb.get("physicalDefense").asInt(),
            sb.get("magicalDefense").asInt());
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
}
