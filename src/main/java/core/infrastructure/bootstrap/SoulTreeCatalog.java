package core.infrastructure.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.domain.card.CardId;
import core.domain.equipment.StatsBonus;
import core.domain.tree.NodeEffect;
import core.domain.tree.NodeId;
import core.domain.tree.SoulTree;
import core.domain.tree.TreeNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ソウルツリーマスタ (§15-7 / E-2)。クラスパス上の {@code /tree.json} を 1 度ロードし、{@code NodeId → TreeNode}
 * の挿入順保持マップを構築する。
 *
 * <p>{@link CardCatalog} / {@link EquipmentCatalog} と同型。ノード定義をハードコード Java から JSON に分離することで、
 * ステ・コスト・前提グラフをチームが {@code tree.json} を編集するだけで調整できる。不正な JSON・未知の効果型・重複 ID は ロード時に例外を投げ、起動時に早期検出する。
 *
 * <p>{@link #allNodes()} は {@code tree.json} の登録順を保つ ({@link SoulTree#applyTo} の順序依存と {@code
 * SoulTreeScreen} の配置順前提)。
 */
public final class SoulTreeCatalog {

  private static final Logger LOG = Logger.getLogger(SoulTreeCatalog.class.getName());

  private static final String RESOURCE = "/tree.json";

  private final Map<NodeId, TreeNode> byId;

  private SoulTreeCatalog(Map<NodeId, TreeNode> byId) {
    this.byId = byId;
  }

  /**
   * クラスパスの {@code /tree.json} を読み込んで SoulTreeCatalog を構築する。
   *
   * <p>ファイル欠損 / I/O エラー / スキーマ不正時は SEVERE ログ + **root ノード 1 件のみのフォールバック** を返す。提出 zip にファイル抜けが
   * あっても起動だけは保証する (graceful)。フォールバック時はメタ進行が機能しないが、{@link SoulTree#empty()} が前提とする root のみは確保される。
   */
  public static SoulTreeCatalog load() {
    try (InputStream in = SoulTreeCatalog.class.getResourceAsStream(RESOURCE)) {
      if (in == null) {
        LOG.severe("soul tree master not found on classpath: " + RESOURCE + " (loading fallback)");
        return fallback();
      }
      JsonNode root = new ObjectMapper().readTree(in);
      JsonNode nodes = root.get("nodes");
      if (nodes == null || !nodes.isArray()) {
        LOG.severe("tree.json must contain a 'nodes' array (loading fallback)");
        return fallback();
      }
      Map<NodeId, TreeNode> map = new LinkedHashMap<>();
      for (JsonNode node : nodes) {
        TreeNode parsed = parseNode(node);
        if (map.put(parsed.id(), parsed) != null) {
          throw new IllegalStateException("duplicate node id in tree.json: " + parsed.id().value());
        }
      }
      return new SoulTreeCatalog(Collections.unmodifiableMap(map));
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "failed to load " + RESOURCE + " (loading fallback)", e);
      return fallback();
    }
  }

  /**
   * フォールバックカタログ。root ノード 1 件だけのミニマル構成で、{@link SoulTree#empty()} の不変条件 (root 必須) と起動を保証する。
   *
   * <p>本来のツリー定義が読めない場合の救済措置。本番では使われない想定で、tree.json を編集し直すまでのつなぎ。
   */
  private static SoulTreeCatalog fallback() {
    Map<NodeId, TreeNode> map = new LinkedHashMap<>();
    map.put(
        SoulTree.ROOT, new TreeNode(SoulTree.ROOT, "中央", 0, NodeEffect.None.INSTANCE, Set.of()));
    return new SoulTreeCatalog(Collections.unmodifiableMap(map));
  }

  /** 登録順 (tree.json 記載順) の全ノード。{@link SoulTree#allNodes()} がそのまま委譲する。 */
  public Map<NodeId, TreeNode> allNodes() {
    return byId;
  }

  private static TreeNode parseNode(JsonNode n) {
    NodeId id = NodeId.of(text(n, "id"));
    String displayName = text(n, "displayName");
    int soulCost = requiredInt(n, "soulCost");
    NodeEffect effect = parseEffect(n.get("effect"));
    Set<NodeId> prerequisites = parsePrerequisites(n.get("prerequisites"));
    return new TreeNode(id, displayName, soulCost, effect, prerequisites);
  }

  private static Set<NodeId> parsePrerequisites(JsonNode arr) {
    if (arr == null || arr.isNull()) {
      return Set.of();
    }
    if (!arr.isArray()) {
      throw new IllegalStateException("tree.json: 'prerequisites' must be an array");
    }
    Set<NodeId> set = new HashSet<>();
    for (JsonNode v : arr) {
      set.add(NodeId.of(v.asText()));
    }
    return set;
  }

  private static NodeEffect parseEffect(JsonNode e) {
    String type = text(e, "type");
    return switch (type) {
      case "None" -> NodeEffect.None.INSTANCE;
      case "StatsBonus" ->
          new NodeEffect.StatsBonusEffect(
              new StatsBonus(
                  requiredInt(e, "maxHp"),
                  requiredInt(e, "speed"),
                  requiredInt(e, "physicalAttack"),
                  requiredInt(e, "magicalAttack"),
                  requiredInt(e, "physicalDefense"),
                  requiredInt(e, "magicalDefense")));
      case "CardGrant" -> new NodeEffect.CardGrantEffect(CardId.of(text(e, "cardId")));
      case "SlotExpand" -> new NodeEffect.SlotExpandEffect(requiredInt(e, "amount"));
      case "LayerExtend" -> new NodeEffect.LayerExtendEffect(requiredInt(e, "amountToAdd"));
      default -> throw new IllegalStateException("unknown node effect type: " + type);
    };
  }

  private static String text(JsonNode n, String field) {
    JsonNode v = n == null ? null : n.get(field);
    if (v == null || v.isNull()) {
      throw new IllegalStateException("tree.json: missing field '" + field + "'");
    }
    return v.asText();
  }

  /**
   * 必須数値フィールドを取得する。null / 欠落 / 非数値型は {@link IllegalStateException} で早期検出。
   *
   * <p>{@link CardCatalog#requiredInt} と同型。tree.json のタイポ / キー欠落を起動時にメッセージ付きで検出する。
   */
  private static int requiredInt(JsonNode n, String field) {
    JsonNode v = n == null ? null : n.get(field);
    if (v == null || v.isNull()) {
      throw new IllegalStateException("tree.json: missing int field '" + field + "'");
    }
    if (!v.isNumber()) {
      throw new IllegalStateException(
          "tree.json: field '" + field + "' must be a number, got: " + v);
    }
    return v.asInt();
  }
}
