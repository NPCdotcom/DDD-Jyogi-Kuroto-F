package core.domain.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** {@link TreeNode} の単体テスト (§15-7 / E-2)。 */
class TreeNodeTest {

  @Test
  void acceptsAllFiveFields() {
    NodeId id = NodeId.of("test_node");
    Set<NodeId> prereqs = Set.of(SoulTree.ROOT);
    TreeNode node = new TreeNode(id, "テストノード", 10, NodeEffect.None.INSTANCE, prereqs);

    assertEquals(id, node.id());
    assertEquals("テストノード", node.displayName());
    assertEquals(10, node.soulCost());
    assertEquals(NodeEffect.None.INSTANCE, node.effect());
    assertEquals(prereqs, node.prerequisites());
  }

  @Test
  void allowsZeroSoulCostForRoot() {
    // root は cost=0 (初期解放、入口)
    TreeNode root = new TreeNode(SoulTree.ROOT, "中央", 0, NodeEffect.None.INSTANCE, Set.of());
    assertEquals(0, root.soulCost());
  }

  @Test
  void rejectsBlankDisplayName() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new TreeNode(NodeId.of("x"), "", 5, NodeEffect.None.INSTANCE, Set.of()));
  }

  @Test
  void rejectsNegativeSoulCost() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new TreeNode(NodeId.of("x"), "x", -1, NodeEffect.None.INSTANCE, Set.of()));
  }

  @Test
  void rejectsNullFields() {
    assertThrows(
        NullPointerException.class,
        () -> new TreeNode(null, "x", 5, NodeEffect.None.INSTANCE, Set.of()));
    assertThrows(
        NullPointerException.class,
        () -> new TreeNode(NodeId.of("x"), null, 5, NodeEffect.None.INSTANCE, Set.of()));
    assertThrows(
        NullPointerException.class,
        () -> new TreeNode(NodeId.of("x"), "x", 5, null, Set.of()));
    assertThrows(
        NullPointerException.class,
        () -> new TreeNode(NodeId.of("x"), "x", 5, NodeEffect.None.INSTANCE, null));
  }

  @Test
  void prerequisitesAreDefensivelyCopied() {
    Set<NodeId> mutable = new HashSet<>();
    mutable.add(SoulTree.ROOT);
    TreeNode node =
        new TreeNode(NodeId.of("x"), "x", 5, NodeEffect.None.INSTANCE, mutable);

    mutable.add(NodeId.of("intruder"));
    assertEquals(1, node.prerequisites().size(), "外部 Set 変更は防御コピーで遮断される");
    assertNotSame(mutable, node.prerequisites());
  }

  @Test
  void nodeIdRejectsBlankValue() {
    assertThrows(IllegalArgumentException.class, () -> new NodeId(""));
    assertThrows(IllegalArgumentException.class, () -> new NodeId("   "));
  }

  @Test
  void nodeIdRejectsNullValue() {
    assertThrows(NullPointerException.class, () -> new NodeId(null));
  }
}
