package core.presentation.render;

import core.domain.card.Card;
import core.domain.card.CardEffect;
import core.domain.card.CardElement;
import core.domain.card.TrapLifetime;

/**
 * カードの効果を人間可読な短い説明文に変換する (§15-3 / §15-4)。
 *
 * <p>{@link Card} に説明文フィールドを持たせず、{@link CardEffect} の sealed switch から動的生成する (DRY:
 * 効果が説明の唯一の真実)。手札表示 ({@code HudRenderer}) とカード図鑑 ({@code CardCollectionScreen}) が利用する。
 */
public final class CardDescriber {

  private CardDescriber() {}

  /** カード効果の日本語説明 (例「物理ダメージ 5」「3 マス移動」「物攻 +3 (3T)」「罠 物理 5 / 踏むまで」)。 */
  public static String describe(Card card) {
    String elem = card.element() == CardElement.PHYSICAL ? "物理" : "魔法";
    return switch (card.effect()) {
      case CardEffect.Damage d -> elem + "ダメージ " + d.baseValue();
      case CardEffect.Move m -> m.distance() + " マス移動";
      case CardEffect.Buff b ->
          buffLabel(b.kind()) + " " + signed(b.amount()) + " (" + b.durationTurns() + "T)";
      case CardEffect.Trap t ->
          "罠 " + elem + " " + t.baseValue() + " / " + trapLifetimeLabel(t.lifetime());
    };
  }

  private static String buffLabel(CardEffect.BuffKind kind) {
    return switch (kind) {
      case PHYSICAL_ATTACK_UP -> "物攻";
      case MAGICAL_ATTACK_UP -> "魔攻";
      case PHYSICAL_DEFENSE_UP -> "物防";
      case MAGICAL_DEFENSE_UP -> "魔防";
      case SPEED_UP -> "速度";
    };
  }

  private static String signed(int amount) {
    return amount >= 0 ? "+" + amount : String.valueOf(amount);
  }

  private static String trapLifetimeLabel(TrapLifetime lifetime) {
    return switch (lifetime) {
      case TrapLifetime.UntilStepped ignored -> "踏むまで";
      case TrapLifetime.Turns turns -> turns.remaining() + "T 残";
    };
  }
}
