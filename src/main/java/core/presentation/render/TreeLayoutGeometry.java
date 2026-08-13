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

  /**
   * SoulTreeScreen.positions() と 1:1 で対応するノード座標マップ（中心原点）を返す。
   *
   * <p>SoulTreeScreen は CENTER_X/CENTER_Y をオフセットとして加えるが、本メソッドは原点 (0,0) を
   * 中心として返す（オフセットは表示上の問題であり幾何判定に影響しない）。
   */
  public static Map<String, Point> defaultNodePositions() {
    Map<String, Point> pos = new HashMap<>();
    pos.put("root", new Point(0, 0));

    // 内輪 r=200 (SoulTreeScreen L139-L144 と同一)
    pos.put("hp_up_1", polar(0, 200));
    pos.put("speed_up_1", polar(60, 200));
    pos.put("phys_atk_up_1", polar(120, 200));
    pos.put("mag_atk_up_1", polar(180, 200));
    pos.put("phys_def_up_1", polar(240, 200));
    pos.put("mag_def_up_1", polar(300, 200));

    // 中輪 r=380 (SoulTreeScreen L145-L149 と同一)
    pos.put("card_grant_strong_strike", polar(120, 380));
    pos.put("card_grant_fireball", polar(170, 380));
    pos.put("card_grant_magic_bolt", polar(190, 380));
    pos.put("card_grant_iron_skin", polar(240, 380));
    pos.put("card_grant_arcane_veil", polar(300, 380));

    // 外輪 r=540 (SoulTreeScreen L150-L154 と同一)
    pos.put("slot_expand_1", polar(120, 540));
    pos.put("slot_expand_2", polar(170, 540));
    pos.put("slot_expand_3", polar(190, 540));
    pos.put("slot_expand_4", polar(240, 540));
    pos.put("slot_expand_5", polar(300, 540));

    // ステ Lv2 r=700 (SoulTreeScreen L155-L161 と同一、貫通回避角度)
    pos.put("hp_up_2", polar(0, 700));
    pos.put("speed_up_2", polar(60, 700));
    pos.put("phys_atk_up_2", polar(105, 700));
    pos.put("mag_atk_up_2", polar(180, 700));
    pos.put("phys_def_up_2", polar(225, 700));
    pos.put("mag_def_up_2", polar(315, 700));

    // 層拡張 r=860 (SoulTreeScreen L163-L164 と同一)
    pos.put("layer_extend_4", polar(30, 860));
    pos.put("layer_extend_5", polar(150, 860));

    return pos;
  }

  /**
   * tree.json の prerequisites と 1:1 で対応する前提条件マップを返す。
   *
   * <p>layer_extend_4 は hp_up_2 と phys_atk_up_2 の 2 前提を持つ（tree.json L32 参照）。
   */
  public static Map<String, List<String>> defaultPrerequisites() {
    Map<String, List<String>> prereqs = new HashMap<>();
    // 内輪 → root
    prereqs.put("hp_up_1", List.of("root"));
    prereqs.put("speed_up_1", List.of("root"));
    prereqs.put("phys_atk_up_1", List.of("root"));
    prereqs.put("mag_atk_up_1", List.of("root"));
    prereqs.put("phys_def_up_1", List.of("root"));
    prereqs.put("mag_def_up_1", List.of("root"));

    // 中輪 → 内輪 (tree.json L13-L17)
    prereqs.put("card_grant_strong_strike", List.of("phys_atk_up_1"));
    prereqs.put("card_grant_fireball", List.of("mag_atk_up_1"));
    prereqs.put("card_grant_magic_bolt", List.of("mag_atk_up_1"));
    prereqs.put("card_grant_iron_skin", List.of("phys_def_up_1"));
    prereqs.put("card_grant_arcane_veil", List.of("mag_def_up_1"));

    // 外輪 → 中輪 (tree.json L19-L23)
    prereqs.put("slot_expand_1", List.of("card_grant_strong_strike"));
    prereqs.put("slot_expand_2", List.of("card_grant_fireball"));
    prereqs.put("slot_expand_3", List.of("card_grant_magic_bolt"));
    prereqs.put("slot_expand_4", List.of("card_grant_iron_skin"));
    prereqs.put("slot_expand_5", List.of("card_grant_arcane_veil"));

    // ステ Lv2 → ステ Lv1 (tree.json L25-L30)
    prereqs.put("hp_up_2", List.of("hp_up_1"));
    prereqs.put("speed_up_2", List.of("speed_up_1"));
    prereqs.put("phys_atk_up_2", List.of("phys_atk_up_1"));
    prereqs.put("mag_atk_up_2", List.of("mag_atk_up_1"));
    prereqs.put("phys_def_up_2", List.of("phys_def_up_1"));
    prereqs.put("mag_def_up_2", List.of("mag_def_up_1"));

    // 層拡張 (tree.json L32-L33)
    prereqs.put("layer_extend_4", List.of("hp_up_2", "phys_atk_up_2"));
    prereqs.put("layer_extend_5", List.of("layer_extend_4"));
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
