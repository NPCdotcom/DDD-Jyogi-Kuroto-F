package core.presentation.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TreeLayoutGeometryTest {

  @Test
  void polarCalculatesCoordinatesCorrectly() {
    TreeLayoutGeometry.Point p0 = TreeLayoutGeometry.polar(0, 100);
    assertTrue(Math.abs(p0.x() - 100.0) < 1e-4);
    assertTrue(Math.abs(p0.y() - 0.0) < 1e-4);

    TreeLayoutGeometry.Point p90 = TreeLayoutGeometry.polar(90, 100);
    assertTrue(Math.abs(p90.x() - 0.0) < 1e-4);
    assertTrue(Math.abs(p90.y() - 100.0) < 1e-4);
  }

  @Test
  void segmentIntersectsCircleDetectsMiddleOverlap() {
    TreeLayoutGeometry.Point start = new TreeLayoutGeometry.Point(0, 0);
    TreeLayoutGeometry.Point end = new TreeLayoutGeometry.Point(100, 0);
    TreeLayoutGeometry.Point middleNode = new TreeLayoutGeometry.Point(50, 0);

    assertTrue(TreeLayoutGeometry.segmentIntersectsCircle(start, end, middleNode, 32.0));
  }

  @Test
  void segmentIntersectsCircleIgnoresEndpoints() {
    TreeLayoutGeometry.Point start = new TreeLayoutGeometry.Point(0, 0);
    TreeLayoutGeometry.Point end = new TreeLayoutGeometry.Point(100, 0);

    // 両端ノード自体は貫通とはみなさない
    assertFalse(TreeLayoutGeometry.segmentIntersectsCircle(start, end, start, 32.0));
    assertFalse(TreeLayoutGeometry.segmentIntersectsCircle(start, end, end, 32.0));
  }

  @Test
  void noEdgePenetratesIntermediateNodes() {
    Map<String, TreeLayoutGeometry.Point> positions = TreeLayoutGeometry.defaultNodePositions();
    Map<String, List<String>> prerequisites = TreeLayoutGeometry.defaultPrerequisites();

    List<String> violations =
        TreeLayoutGeometry.findPenetrationViolations(positions, prerequisites, 32.0);
    assertTrue(violations.isEmpty(), "辺が中間ノードの円 (半径 32) を貫通していないこと: " + violations);
  }

  @Test
  void minimumDistanceBetweenNodesIsMaintained() {
    Map<String, TreeLayoutGeometry.Point> positions = TreeLayoutGeometry.defaultNodePositions();
    double minDistance = TreeLayoutGeometry.calculateMinimumNodeDistance(positions);
    assertTrue(minDistance >= 100.0, "全ノード間距離は 100px 以上であること: " + minDistance);
  }

  @Test
  void nodeCountMatchesSoulTreeScreen() {
    Map<String, TreeLayoutGeometry.Point> positions = TreeLayoutGeometry.defaultNodePositions();
    // root(1) + 内輪(6) + 中輪(5) + 魂の刻印(2) + Lv2(6) + 層拡張(2) = 22
    assertEquals(22, positions.size(), "ノード数は SoulTreeScreen と一致すること: " + positions.size());
  }

  @Test
  void prerequisiteCountMatchesTreeJson() {
    Map<String, List<String>> prerequisites = TreeLayoutGeometry.defaultPrerequisites();
    // 21 ノード分の前提条件がある (root は前提なし)
    assertEquals(
        21, prerequisites.size(), "前提条件の数は tree.json のノード数 - 1 と一致すること: " + prerequisites.size());
  }
}
