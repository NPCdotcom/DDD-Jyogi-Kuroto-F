package core.presentation.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ソウルツリーの幾何学配置およびノード・辺の交差判定を行う純粋計算クラス (LibGDX 非依存)。
 *
 * <p>{@code SoulTreeScreen.positions()} のハードコード座標テーブルを純粋関数へ複製し、 「前提線が中間ノードを物理的に貫通する
 * (一個飛ばしに見える)」問題を幾何判定で自動検証可能にする。
 *
 * <p>ノード ID・角度・半径・前提条件はすべて {@code src/main/resources/tree.json} および {@code
 * SoulTreeScreen.positions()} と 1:1 で対応させなければならない。乖離するとテストが 偽陰性 (本物のレイアウトと異なるデータでグリーン)
 * になるため、追加・変更時は両方を同期すること。
 */
public final class TreeLayoutGeometry {

  private TreeLayoutGeometry() {}

  public record Point(double x, double y) {
    public double distanceTo(Point other) {
      double dx = x - other.x;
      double dy = y - other.y;
      return Math.sqrt(dx * dx + dy * dy);
    }
  }

  /**
   * 極座標 (角度, 半径) から直交座標 Point を算出する。
   *
   * @param angleDegrees 角度 (度数法、0度 = 右、90度 = 上)
   * @param radiusPixels 中心からの距離 (ピクセル)
   * @return 算出された Point
   */
  public static Point polar(double angleDegrees, double radiusPixels) {
    double rad = Math.toRadians(angleDegrees);
    double x = radiusPixels * Math.cos(rad);
    double y = radiusPixels * Math.sin(rad);
    return new Point(x, y);
  }

  /**
   * 線分 (start - end) が、指定された中心と半径を持つ円と交差・貫通するかを判定する。 両端点 (start, end) に位置する円は「両端ノード」なので貫通対象外とする。
   */
  public static boolean segmentIntersectsCircle(
      Point start, Point end, Point center, double radius) {
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(end, "end");
    Objects.requireNonNull(center, "center");

    // 両端点自体である場合は対象外
    if (start.distanceTo(center) < 1e-4 || end.distanceTo(center) < 1e-4) {
      return false;
    }

    double dx = end.x() - start.x();
    double dy = end.y() - start.y();
    double lenSq = dx * dx + dy * dy;

    if (lenSq < 1e-8) {
      return start.distanceTo(center) <= radius;
    }

    // 線分上の最近接点 t (0.0 <= t <= 1.0)
    double t = ((center.x() - start.x()) * dx + (center.y() - start.y()) * dy) / lenSq;
    t = Math.max(0.0, Math.min(1.0, t));

    Point closest = new Point(start.x() + t * dx, start.y() + t * dy);
    return closest.distanceTo(center) < radius - 1e-4;
  }

  /** SoulTreeLayout (SSoT) のノード座標マップを返す。 */
  public static Map<String, Point> defaultNodePositions() {
    return SoulTreeLayout.nodePositions();
  }

  /** 実際の SoulTree ノード定義から前提条件マップを動的構築する。 */
  public static Map<String, List<String>> defaultPrerequisites() {
    Map<String, List<String>> prereqs = new HashMap<>();
    for (core.domain.tree.TreeNode node : core.domain.tree.SoulTree.allNodes().values()) {
      if (!node.prerequisites().isEmpty()) {
        List<String> parentIds =
            node.prerequisites().stream().map(core.domain.tree.NodeId::value).toList();
        prereqs.put(node.id().value(), parentIds);
      }
    }
    return prereqs;
  }

  /** 前提条件で結ばれた全線分について、他の中間ノード円 (半径 nodeRadius) を貫通している違反リストを返す。 */
  public static List<String> findPenetrationViolations(
      Map<String, Point> positions, Map<String, List<String>> prerequisites, double nodeRadius) {
    List<String> violations = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : prerequisites.entrySet()) {
      String targetId = entry.getKey();
      Point targetPos = positions.get(targetId);
      if (targetPos == null) continue;

      for (String parentId : entry.getValue()) {
        Point parentPos = positions.get(parentId);
        if (parentPos == null) continue;

        for (Map.Entry<String, Point> nodeEntry : positions.entrySet()) {
          String otherId = nodeEntry.getKey();
          if (otherId.equals(targetId) || otherId.equals(parentId)) {
            continue;
          }
          if (segmentIntersectsCircle(parentPos, targetPos, nodeEntry.getValue(), nodeRadius)) {
            violations.add("Edge " + parentId + "->" + targetId + " penetrates node " + otherId);
          }
        }
      }
    }
    return violations;
  }

  /** ノード位置マップ内の最も近い 2 ノード間の距離を求める。 */
  public static double calculateMinimumNodeDistance(Map<String, Point> positions) {
    List<Point> points = new ArrayList<>(positions.values());
    double minDist = Double.MAX_VALUE;
    for (int i = 0; i < points.size(); i++) {
      for (int j = i + 1; j < points.size(); j++) {
        double d = points.get(i).distanceTo(points.get(j));
        if (d < minDist) {
          minDist = d;
        }
      }
    }
    return minDist;
  }
}
