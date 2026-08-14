package core.presentation.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.tree.NodeId;
import core.domain.tree.SoulTree;
import core.infrastructure.bootstrap.InitialStateFactory;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 座標表とノード定義のズレを検出する (SOUL-03)。
 *
 * <p>件数一致では不十分。{@code tree.json} にノードを足して座標表への追加を忘れると、描画側の null ガードで {@code continue}
 * され、<b>枝線もアイコンもクリック判定も出ない不可視ノード</b>になる。例外もログも出ないため 気付けない。実際に {@code slot_expand_*} → {@code
 * seal_of_soul_*} の移行でこれが起きた。
 */
class SoulTreeLayoutCoverageTest {

  @BeforeAll
  static void loadCatalog() {
    SoulTree.setNodeProvider(InitialStateFactory::soulTreeNodes);
  }

  @Test
  void layoutCoversExactlyTheNodesDefinedInTreeJson() {
    Set<String> defined =
        SoulTree.allNodes().keySet().stream()
            .map(NodeId::value)
            .collect(Collectors.toCollection(TreeSet::new));
    Set<String> laidOut = new TreeSet<>(SoulTreeLayout.nodePositions().keySet());

    assertEquals(defined, laidOut, "座標の無いノードは画面に出ず購入もできない。tree.json と座標表を同時に更新すること");
  }

  @Test
  void everyNodeKeepsClickableDistanceFromItsNeighbours() {
    Map<String, TreeLayoutGeometry.Point> positions = SoulTreeLayout.nodePositions();
    double min = TreeLayoutGeometry.calculateMinimumNodeDistance(positions);

    // 判定円が重なると隣ノードへクリックが吸われる。SoulTreeScreen.NODE_RADIUS = 50f。
    assertTrue(min >= 100.0, "全ノード間距離は 2 x NODE_RADIUS (100px) 以上であること: " + min);
  }
}
