package core.domain.card;

import java.util.Objects;

/**
 * カード 1 枚を表す不変値。
 *
 * <p>カードの多態性 (攻撃 / 移動 / バフ / 罠) は {@link CardEffect} (sealed) に集約しており、 {@code Card} 自体はサブクラスを持たない。タグ ({@link
 * CardTag}) と効果 ({@link CardEffect}) は独立した次元として扱う (例: 同じ ATTACK タグでも、効果は Damage だったり Move 込みのこともある)。
 *
 * <p>仕様: §15-3。AP コストは必ず 1 以上 (0 コストカードは「ターン終了条件 = AP 枯渇」と相性が悪いため禁止)。
 */
public record Card(
    CardId id,
    String displayName,
    int apCost,
    CardTag tag,
    CardElement element,
    CardEffect effect) {

  public Card {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(displayName, "displayName");
    Objects.requireNonNull(tag, "tag");
    Objects.requireNonNull(element, "element");
    Objects.requireNonNull(effect, "effect");
    if (displayName.isBlank()) {
      throw new IllegalArgumentException("displayName must not be blank");
    }
    if (apCost < 1) {
      throw new IllegalArgumentException("apCost must be >= 1 (got " + apCost + ")");
    }
  }
}
