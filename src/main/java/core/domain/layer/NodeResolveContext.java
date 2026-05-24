package core.domain.layer;

import core.domain.card.Card;
import core.domain.card.CardId;
import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentId;
import java.util.Objects;
import java.util.function.Function;

/**
 * {@link LayerEndNode} の {@code apply} / {@code displayName} に渡すカタログ解決コンテキスト (§15-8 / Wave 3 Task
 * A)。
 *
 * <p>cards / equipments の resolver を 1 record にラップすることで、将来 (item / buff / soulTree 等) の resolver
 * 追加時に各 LayerEndNode variant および呼出元の signature 変更を避ける。OCP (Open/Closed) を満たすための DTO 風 record。
 *
 * <p>resolver の実体は {@code CardCatalog::get} / {@code EquipmentCatalog::get} 等を method reference
 * で渡す想定。ドメイン層は {@link Function} のみに依存し、infrastructure 層のカタログ実装に逆依存しない。
 */
public record NodeResolveContext(
    Function<CardId, Card> cards, Function<EquipmentId, Equipment> equipments) {

  public NodeResolveContext {
    Objects.requireNonNull(cards, "cards");
    Objects.requireNonNull(equipments, "equipments");
  }
}
