package core.presentation.render;

import core.domain.card.Card;
import core.domain.card.CardEffect;
import core.domain.card.CardElement;
import core.domain.card.TrapLifetime;
import java.util.logging.Logger;

/**
 * カードの効果を人間可読な短い説明文に変換する (§15-3 / §15-4)。
 *
 * <p>{@link Card} に説明文フィールドを持たせず、{@link CardEffect} の sealed switch から動的生成する (DRY:
 * 効果が説明の唯一の真実)。手札表示 ({@code HudRenderer}) とカード図鑑 ({@code CardCollectionScreen}) が利用する。
 *
 * <p>Wave 12 W12-γ: 射程 ({@code range > 1}) / 爆風 ({@code areaRadius > 0}) を Damage カード詳細欄に
 * サフィックス表示する (例「物理ダメージ 5 (射程 3)」「魔法ダメージ 9 (射程 4 / 範囲 1)」)。近接単体 (range=1, areaRadius=0) は表示なしで UI
 * ノイズを最小化する。
 */
public final class CardDescriber {

  private static final Logger LOG = Logger.getLogger(CardDescriber.class.getName());

  private CardDescriber() {}

  /** カード効果の日本語説明 (例「物理ダメージ 5 (射程 3)」「3 マス移動」「物攻 +3 (3T)」「罠 物理 5 / 踏むまで」)。 */
  public static String describe(Card card) {
    return describe(card, true);
  }

  /** カード効果を指定言語で説明する。 */
  public static String describe(Card card, boolean japanese) {
    String elem = elementLabel(card.element(), japanese);
    return switch (card.effect()) {
      case CardEffect.Damage d ->
          japanese
              ? elem + "ダメージ " + d.baseValue() + rangeAreaSuffix(d, true)
              : elem + " damage " + d.baseValue() + rangeAreaSuffix(d, false);
      case CardEffect.Move m ->
          japanese
              ? m.distance() + " マス移動"
              : "Move " + m.distance() + (m.distance() == 1 ? " tile" : " tiles");
      case CardEffect.Buff b ->
          BuffKindLabels.labelOf(b.kind(), japanese)
              + " "
              + signed(b.amount())
              + " ("
              + b.durationTurns()
              + "T)";
      case CardEffect.Trap t ->
          (japanese ? "罠 " : "Trap ")
              + elem
              + " "
              + t.baseValue()
              + " / "
              + trapLifetimeLabel(t.lifetime(), japanese);
    };
  }

  /**
   * Damage カードの射程 / 範囲サフィックスを生成する (Wave 12 W12-γ、CTO チェックポイント #3 graceful 防衛)。
   *
   * <p>range=1, areaRadius=0 (近接単体) は空文字、range>1 のみは "(射程 N)"、areaRadius>0 は "(射程 N / 範囲 R)"。
   * cards.json 由来データの破損・将来 Optional 化等で String.format が失敗してもフロントエンドをクラッシュさせず、 空文字にフォールバック (graceful
   * degradation)。
   */
  private static String rangeAreaSuffix(CardEffect.Damage d, boolean japanese) {
    int range = d.range();
    int areaRadius = d.areaRadius();
    if (range <= 1 && areaRadius <= 0) {
      return "";
    }
    try {
      if (areaRadius > 0) {
        return japanese
            ? String.format(" (射程 %d / 範囲 %d)", range, areaRadius)
            : String.format(" (Range %d / Area %d)", range, areaRadius);
      }
      return japanese ? String.format(" (射程 %d)", range) : String.format(" (Range %d)", range);
    } catch (RuntimeException e) {
      LOG.warning(
          "CardDescriber range/area format failed (range="
              + range
              + ", areaRadius="
              + areaRadius
              + "): "
              + e.getMessage());
      return ""; // graceful: 表示なしにフォールバック、画面クラッシュ回避
    }
  }

  private static String signed(int amount) {
    return amount >= 0 ? "+" + amount : String.valueOf(amount);
  }

  private static String elementLabel(CardElement element, boolean japanese) {
    return switch (element) {
      case PHYSICAL -> japanese ? "物理" : Strings.En.CARD_ELEMENT_PHYSICAL;
      case MAGICAL -> japanese ? "魔法" : Strings.En.CARD_ELEMENT_MAGICAL;
    };
  }

  private static String trapLifetimeLabel(TrapLifetime lifetime, boolean japanese) {
    return switch (lifetime) {
      case TrapLifetime.UntilStepped ignored -> japanese ? "踏むまで" : "Until stepped";
      case TrapLifetime.Turns turns ->
          japanese ? turns.remaining() + "T 残" : turns.remaining() + "T remaining";
    };
  }
}
