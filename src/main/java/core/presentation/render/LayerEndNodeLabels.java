package core.presentation.render;

import core.domain.card.Card;
import core.domain.equipment.Equipment;
import core.domain.layer.EventKind;
import core.domain.layer.LayerEndNode;
import core.domain.layer.NodeResolveContext;
import java.util.Objects;

/**
 * {@link LayerEndNode} の日英表示ラベル解決 (§15-8 / W8-α、{@link BuffKindLabels} と同型)。
 *
 * <p>Wave 8 W8-α: 旧 {@code LayerEndNode#displayName(NodeResolveContext)} を domain 層から撤去し、 表示ラベルの解決を
 * presentation 層に移譲。{@link Strings.Ja} / {@link Strings.En} のキーを sealed switch で参照し、Format パターンの引数は
 * variant のフィールドから組み立てる。
 *
 * <p>純関数 (LibGDX 描画依存なし)。
 */
public final class LayerEndNodeLabels {

  private LayerEndNodeLabels() {}

  /**
   * LayerEndNode の表示ラベルを解決する。
   *
   * @param node 解決対象の variant
   * @param context Shop / ShopEquipment が Card / Equipment 名を解決する際に使う
   * @param jp true なら日本語、false なら英語
   * @return 日英いずれかのラベル文字列
   */
  public static String labelOf(LayerEndNode node, NodeResolveContext context, boolean jp) {
    Objects.requireNonNull(node, "node");
    Objects.requireNonNull(context, "context");
    return switch (node) {
      case LayerEndNode.HpMaxUp hp ->
          (jp ? Strings.Ja.LAYER_END_HP_MAX_UP_FORMAT : Strings.En.LAYER_END_HP_MAX_UP_FORMAT)
              .formatted(hp.amount());
      case LayerEndNode.SpeedUp s ->
          (jp ? Strings.Ja.LAYER_END_SPEED_UP_FORMAT : Strings.En.LAYER_END_SPEED_UP_FORMAT)
              .formatted(s.amount());
      case LayerEndNode.Rest ignored -> jp ? Strings.Ja.LAYER_END_REST : Strings.En.LAYER_END_REST;
      case LayerEndNode.Shop shop -> {
        Card card = context.cards().apply(shop.cardId());
        Objects.requireNonNull(card, "context.cards() returned null for " + shop.cardId().value());
        yield (jp ? Strings.Ja.LAYER_END_SHOP_FORMAT : Strings.En.LAYER_END_SHOP_FORMAT)
            .formatted(card.displayName(), shop.goldCost());
      }
      case LayerEndNode.Event ev -> eventLabel(ev.kind(), jp);
      case LayerEndNode.ShopEquipment se -> {
        Equipment eq = context.equipments().apply(se.equipmentId());
        Objects.requireNonNull(
            eq, "context.equipments() returned null for " + se.equipmentId().value());
        yield (jp
                ? Strings.Ja.LAYER_END_SHOP_EQUIPMENT_FORMAT
                : Strings.En.LAYER_END_SHOP_EQUIPMENT_FORMAT)
            .formatted(eq.displayName(), se.goldCost());
      }
    };
  }

  /** {@link EventKind} 単体のラベル解決 (Event variant 内部で利用)。 */
  private static String eventLabel(EventKind kind, boolean jp) {
    return switch (kind) {
      case HEALING_SPRING -> jp ? Strings.Ja.EVENT_HEALING_SPRING : Strings.En.EVENT_HEALING_SPRING;
      case GOLDEN_CHEST -> jp ? Strings.Ja.EVENT_GOLDEN_CHEST : Strings.En.EVENT_GOLDEN_CHEST;
      case SOUL_SHRINE -> jp ? Strings.Ja.EVENT_SOUL_SHRINE : Strings.En.EVENT_SOUL_SHRINE;
    };
  }
}
