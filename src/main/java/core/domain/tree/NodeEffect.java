package core.domain.tree;

import core.domain.card.Card;
import core.domain.card.CardId;
import core.domain.card.CardPileState;
import core.domain.card.DrawPile;
import core.domain.entity.Player;
import core.domain.equipment.StatsBonus;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * ソウルツリーのノード効果 (§15-7 / E-2)。{@link #apply(Player)} で新 Player を返す純関数。
 *
 * <p>sealed で 4 種 (None / StatsBonusEffect / CardGrantEffect / SlotExpandEffect) に限定し、
 * 新効果追加時は switch 網羅性で全箇所コンパイルエラー化 (驚き最小)。
 *
 * <p>{@link CardGrantEffect} は カード ID から Card を解決する関数 ({@code cardResolver}) を
 * apply 時に必要とする。本パッケージはドメイン層なので Card の物理的な生成方法は知らない
 * (infrastructure 層の {@code InitialStateFactory.resolveCard} を application 層で
 * 渡してもらう想定、ADR-26 整合)。
 */
public sealed interface NodeEffect {

  /**
   * 効果を Player に適用する純関数。{@code cardResolver} はカード ID から {@link Card} を返す
   * 関数 (CardGrantEffect で使用、他の効果では未使用)。
   */
  Player apply(Player player, Function<CardId, Card> cardResolver);

  /**
   * 効果なし (中央 root ノード用)。{@link #apply} は引数の Player をそのまま返す。
   */
  record None() implements NodeEffect {
    public static final None INSTANCE = new None();

    @Override
    public Player apply(Player player, Function<CardId, Card> cardResolver) {
      return player;
    }
  }

  /**
   * ステ補正効果 ({@link StatsBonus} を {@link Player#stats()} に加算)。
   * 装備の statsBonus と同型ロジック (ADR-25 整合)。
   */
  record StatsBonusEffect(StatsBonus bonus) implements NodeEffect {
    public StatsBonusEffect {
      Objects.requireNonNull(bonus, "bonus");
    }

    @Override
    public Player apply(Player player, Function<CardId, Card> cardResolver) {
      return player.withStats(player.stats().plus(bonus));
    }
  }

  /**
   * カード獲得効果 (指定カードを 1 枚 {@link Player#cardPileState()} の DrawPile に追加)。
   * デッキ枚数が増えるが、シャッフル位置の決定は呼出側 (将来のラン開始時) に委ねる。
   */
  record CardGrantEffect(CardId cardId) implements NodeEffect {
    public CardGrantEffect {
      Objects.requireNonNull(cardId, "cardId");
    }

    @Override
    public Player apply(Player player, Function<CardId, Card> cardResolver) {
      Objects.requireNonNull(cardResolver, "cardResolver");
      Card resolved = cardResolver.apply(cardId);
      CardPileState pile = player.cardPileState();
      List<Card> newDrawCards = new ArrayList<>(pile.drawPile().cards().size() + 1);
      newDrawCards.addAll(pile.drawPile().cards());
      newDrawCards.add(resolved);
      DrawPile newDraw = new DrawPile(newDrawCards);
      return player.withCardPileState(new CardPileState(newDraw, pile.hand(), pile.discardPile()));
    }
  }

  /**
   * スキル枠拡張効果 ({@link Player#skillSlot()} の {@code maxSize} を {@code amount} 増やす)。
   * amount は 1 以上。
   */
  record SlotExpandEffect(int amount) implements NodeEffect {
    public SlotExpandEffect {
      if (amount < 1) {
        throw new IllegalArgumentException("amount must be >= 1: " + amount);
      }
    }

    @Override
    public Player apply(Player player, Function<CardId, Card> cardResolver) {
      return player.withStatuses(
          player.statuses().withSkillSlot(player.skillSlot().expandedBy(amount)));
    }
  }
}
