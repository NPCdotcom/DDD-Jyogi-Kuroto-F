package core.infrastructure.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.tree.NodeEffect;
import core.domain.tree.NodeId;
import core.domain.tree.SoulTree;
import core.domain.tree.TreeNode;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** {@link SoulTreeCatalog} が tree.json を正しくロードし、25 ノードのマスタ定義を構築することの検証。 */
class SoulTreeCatalogTest {

  private static final SoulTreeCatalog CATALOG = SoulTreeCatalog.load();

  /**
   * Wave 5 W5-γ: {@code soulTreeAllNodesDelegatesToCatalog} が SoulTree.allNodes() を呼ぶため Supplier
   * 注入が必要。
   */
  @BeforeAll
  static void initSoulTreeProvider() {
    SoulTree.setNodeProvider(InitialStateFactory::soulTreeNodes);
  }

  @Test
  void loadsExactly25Nodes() {
    // §15-7 仕様: root + ステ軸 Lv1 (6) + カード獲得 (5) + 枠拡張 (5) + ステ軸 Lv2 (6) + 層数拡張 (2) = 25 ノード
    assertEquals(25, CATALOG.allNodes().size(), "tree.json は 25 ノード定義を含む");
  }

  @Test
  void rootNodeHasZeroCostAndEmptyPrerequisites() {
    TreeNode root = CATALOG.allNodes().get(SoulTree.ROOT);
    assertNotNull(root, "root ノードが存在する");
    assertEquals(0, root.soulCost(), "root の soulCost は 0");
    assertTrue(root.prerequisites().isEmpty(), "root の前提ノードは空");
    assertTrue(root.effect() instanceof NodeEffect.None, "root の効果は None");
  }

  @Test
  void parsesAllFiveNodeEffectTypes() {
    Map<NodeId, TreeNode> all = CATALOG.allNodes();
    long none = all.values().stream().filter(n -> n.effect() instanceof NodeEffect.None).count();
    long stats =
        all.values().stream()
            .filter(n -> n.effect() instanceof NodeEffect.StatsBonusEffect)
            .count();
    long card =
        all.values().stream().filter(n -> n.effect() instanceof NodeEffect.CardGrantEffect).count();
    long slot =
        all.values().stream()
            .filter(n -> n.effect() instanceof NodeEffect.SlotExpandEffect)
            .count();
    long layerExtend =
        all.values().stream()
            .filter(n -> n.effect() instanceof NodeEffect.LayerExtendEffect)
            .count();

    assertEquals(1, none, "None 効果は root の 1 件");
    assertEquals(12, stats, "StatsBonus 効果は 6 ステ軸 × 2 Lv = 12 件");
    assertEquals(5, card, "CardGrant 効果は 5 件");
    assertEquals(5, slot, "SlotExpand 効果は 5 件");
    assertEquals(2, layerExtend, "LayerExtend 効果は 2 件");
  }

  @Test
  void parsesLayerExtendNodesWithCorrectAmount() {
    // tree.json の layer_extend_4 / layer_extend_5 が LayerExtendEffect として正しくパースされ、
    // amountToAdd = 1 で読み込まれる (タスク B 仕様)。
    Map<NodeId, TreeNode> all = CATALOG.allNodes();
    TreeNode le4 = all.get(NodeId.of("layer_extend_4"));
    TreeNode le5 = all.get(NodeId.of("layer_extend_5"));
    assertNotNull(le4, "layer_extend_4 が存在する");
    assertNotNull(le5, "layer_extend_5 が存在する");
    assertTrue(le4.effect() instanceof NodeEffect.LayerExtendEffect, "le4 の効果は LayerExtendEffect");
    assertTrue(le5.effect() instanceof NodeEffect.LayerExtendEffect, "le5 の効果は LayerExtendEffect");
    assertEquals(1, ((NodeEffect.LayerExtendEffect) le4.effect()).amountToAdd());
    assertEquals(1, ((NodeEffect.LayerExtendEffect) le5.effect()).amountToAdd());
    assertEquals(100, le4.soulCost(), "le4 のソウルコストは 100");
    assertEquals(200, le5.soulCost(), "le5 のソウルコストは 200");
  }

  @Test
  void allNodeIdsAreUnique() {
    long distinct = CATALOG.allNodes().keySet().stream().distinct().count();
    assertEquals(CATALOG.allNodes().size(), distinct, "ノード ID は一意");
  }

  @Test
  void allPrerequisitesExistInCatalog() {
    // 前提ノードが catalog 内に存在しないと、isVisible / unlock の前提検証が壊れる。
    Map<NodeId, TreeNode> all = CATALOG.allNodes();
    for (TreeNode node : all.values()) {
      for (NodeId prereq : node.prerequisites()) {
        assertTrue(
            all.containsKey(prereq),
            node.id().value() + " の前提 " + prereq.value() + " が catalog に存在する");
      }
    }
  }

  @Test
  void everyCardGrantEffectResolvesInCardCatalog() {
    // CardGrant の cardId が cards.json に無いと、ラン開始時の SoulTree.applyTo で resolveCard が例外を投げる。
    for (TreeNode node : CATALOG.allNodes().values()) {
      if (node.effect() instanceof NodeEffect.CardGrantEffect cg) {
        assertNotNull(
            InitialStateFactory.cardCatalog().get(cg.cardId()),
            node.id().value() + " の付与カード " + cg.cardId().value() + " は cards.json に存在する");
      }
    }
  }

  @Test
  void firstEntryIsRoot() {
    // LinkedHashMap の挿入順を保つ: tree.json 先頭が root であり、UI 描画の中心ノード前提を満たす。
    NodeId firstKey = CATALOG.allNodes().keySet().iterator().next();
    assertEquals(SoulTree.ROOT, firstKey, "登録順の先頭は root ノード");
  }

  @Test
  void soulTreeAllNodesDelegatesToCatalog() {
    // SoulTree.allNodes() は InitialStateFactory.soulTreeNodes() 経由で同一カタログを返す。
    assertEquals(
        CATALOG.allNodes().size(),
        SoulTree.allNodes().size(),
        "SoulTree.allNodes() は catalog と同一サイズ");
    assertFalse(SoulTree.allNodes().isEmpty(), "SoulTree.allNodes() は空でない");
  }
}
