package core.presentation.render;

import core.presentation.render.TreeLayoutGeometry.Point;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ソウルツリー全ノードの幾何座標を定義する単一ソース (SSoT, Phase 4-3)。
 *
 * <p>{@link core.presentation.screen.SoulTreeScreen} の描画と {@link TreeLayoutGeometry} の幾何検証で共通利用する。
 */
public final class SoulTreeLayout {

  private static final Map<String, Point> POSITIONS;

  static {
    Map<String, Point> map = new LinkedHashMap<>();
    // 中央 root ノード
    map.put("root", new Point(0, 0));

    // 内輪 r=200 (6 ノード)
    map.put("hp_up_1", TreeLayoutGeometry.polar(0, 200));
    map.put("speed_up_1", TreeLayoutGeometry.polar(60, 200));
    map.put("phys_atk_up_1", TreeLayoutGeometry.polar(120, 200));
    map.put("mag_atk_up_1", TreeLayoutGeometry.polar(180, 200));
    map.put("phys_def_up_1", TreeLayoutGeometry.polar(240, 200));
    map.put("mag_def_up_1", TreeLayoutGeometry.polar(300, 200));

    // 中輪 カード獲得 r=380 (5 ノード)
    map.put("card_grant_strong_strike", TreeLayoutGeometry.polar(120, 380));
    map.put("card_grant_fireball", TreeLayoutGeometry.polar(170, 380));
    map.put("card_grant_magic_bolt", TreeLayoutGeometry.polar(190, 380));
    map.put("card_grant_iron_skin", TreeLayoutGeometry.polar(240, 380));
    map.put("card_grant_arcane_veil", TreeLayoutGeometry.polar(300, 380));

    // 装備保護枠ノード (2 ノード、北東 45度方向)
    map.put("seal_of_soul_1", TreeLayoutGeometry.polar(45, 380));
    map.put("seal_of_soul_2", TreeLayoutGeometry.polar(45, 540));

    // 外輪 Lv2 強化 r=700 (6 ノード、貫通回避オフセット適用)
    map.put("hp_up_2", TreeLayoutGeometry.polar(0, 700));
    map.put("speed_up_2", TreeLayoutGeometry.polar(75, 700));
    map.put("phys_atk_up_2", TreeLayoutGeometry.polar(105, 700));
    map.put("mag_atk_up_2", TreeLayoutGeometry.polar(180, 700));
    map.put("phys_def_up_2", TreeLayoutGeometry.polar(225, 700));
    map.put("mag_def_up_2", TreeLayoutGeometry.polar(315, 700));

    // 最外郭 層拡張 r=860 (2 ノード)
    map.put("layer_extend_4", TreeLayoutGeometry.polar(30, 860));
    map.put("layer_extend_5", TreeLayoutGeometry.polar(150, 860));

    POSITIONS = Collections.unmodifiableMap(map);
  }

  private SoulTreeLayout() {}

  /** 全ノードの描画座標マップ (SSoT) を返す。 */
  public static Map<String, Point> nodePositions() {
    return POSITIONS;
  }
}
