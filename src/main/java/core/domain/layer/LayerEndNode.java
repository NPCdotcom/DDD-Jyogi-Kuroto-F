package core.domain.layer;

import core.domain.card.Card;
import core.domain.card.CardId;
import core.domain.card.CardPileState;
import core.domain.card.DrawPile;
import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.domain.meta.Gold;
import core.domain.meta.Soul;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 層末ノードの効果 (§15-8 / E-6)。階段踏破後にプレイヤーが 3 択から 1 つ選ぶノードを表現する。
 *
 * <p>本 PR では「最終層クリア」概念がない段階の最小スコープとして、固定 3 提示 (HP 上限 +5 / 速度 +1 / HP 全回復) のみ実装する。 §15-8 仕様の「4 種
 * (ステ強化 / 休憩 / イベント / ショップ) から 3 提示 → 1 選択」のフル抽選ロジックは M2 送り (Event / Shop は未実装)。
 *
 * <p>各実装は純関数 {@link #apply(Player, NodeResolveContext)} で新 Player を返す (副作用分離)。ステ強化は {@link Stats}
 * の不変ヘルパ ({@code withMaxHpRaised} / {@code withSpeedRaised} / {@code healed}) に委譲し、引数順ミスをコンパイラで防ぐ。
 *
 * <p>{@link #displayName(NodeResolveContext)} はノード選択 UI (NodeChoicePopup) で表示する短い和文。i18n 対応は Plan
 * Part 2 C-3 を反映する形で M2 に持ち越す (ドメイン層に表示文字列を置く負債は意図的)。
 *
 * <p>Wave 3 Task A: Shop は {@link Card} 直接保持から {@link CardId} 保持に変更し、ドメイン層が Card 実装に依存しない構造に統一。
 * apply / displayName は {@link NodeResolveContext} を受け取り、cards / equipments resolver から動的解決する。
 */
public sealed interface LayerEndNode
    permits LayerEndNode.HpMaxUp,
        LayerEndNode.SpeedUp,
        LayerEndNode.Rest,
        LayerEndNode.Shop,
        LayerEndNode.Event {

  /**
   * 効果を適用した新しい Player を返す (純関数)。
   *
   * @param player 適用前の Player
   * @param context Card / Equipment 解決コンテキスト (Shop など resolver を必要とする variant が利用)
   */
  Player apply(Player player, NodeResolveContext context);

  /**
   * UI 表示用の短い和文ラベル (例: "HP +5")。
   *
   * @param context Card / Equipment 解決コンテキスト (Shop のカード名表示などに利用)
   */
  String displayName(NodeResolveContext context);

  /**
   * 最大 HP を {@code amount} 加算する。現在 HP も同量加算し「上限上昇分が即座に空きスロットを埋める」体験にする (Slay the Spire の永続バフと同型)。
   *
   * <p>amount は 1 以上 (0 や負数は「強化」として無意味)。
   */
  record HpMaxUp(int amount) implements LayerEndNode {
    public HpMaxUp {
      if (amount < 1) {
        throw new IllegalArgumentException("amount must be >= 1 (got " + amount + ")");
      }
    }

    @Override
    public Player apply(Player player, NodeResolveContext context) {
      // context は未使用 (signature 追従のみ): HpMaxUp は内部状態だけで完結する純関数
      return player.withStats(player.stats().withMaxHpRaised(amount));
    }

    @Override
    public String displayName(NodeResolveContext context) {
      return "HP +" + amount;
    }
  }

  /**
   * 速度を {@code amount} 加算する (= 1 ターンの最大 AP が増える、§15-4)。
   *
   * <p>速度は最強ステ (DPT を直接押し上げる)、§15-7 のソウルツリーでも高コスト設計。本 record では amount は 1 以上を強制 (層末ノードの強化値は通常 +1)。
   */
  record SpeedUp(int amount) implements LayerEndNode {
    public SpeedUp {
      if (amount < 1) {
        throw new IllegalArgumentException("amount must be >= 1 (got " + amount + ")");
      }
    }

    @Override
    public Player apply(Player player, NodeResolveContext context) {
      // context は未使用 (signature 追従のみ)
      return player.withStats(player.stats().withSpeedRaised(amount));
    }

    @Override
    public String displayName(NodeResolveContext context) {
      return "速度 +" + amount;
    }
  }

  /**
   * 休憩ノード。現在 HP を最大 HP まで全回復する (§15-8)。Stats.healed(maxHp) で {@code min(maxHp, current + maxHp)} =
   * maxHp。
   *
   * <p>パラメータなしの marker record (Slay the Spire の Rest と同型、量は固定)。
   */
  record Rest() implements LayerEndNode {

    @Override
    public Player apply(Player player, NodeResolveContext context) {
      // context は未使用 (signature 追従のみ)
      Stats s = player.stats();
      return player.withStats(s.healed(s.maxHp()));
    }

    @Override
    public String displayName(NodeResolveContext context) {
      return "HP 全回復";
    }
  }

  /**
   * ショップノード (§15-8 / §15-9 縮退実装)。{@code goldCost} 分の金貨を消費して {@code cardId} で示されるカードを DrawPile に 1
   * 枚追加する。完成仕様の「装備変更 UI + デッキ再生成」(ADR-26) は M2 送り、本セッションでは 単発カード追加に縮退。
   *
   * <p>Wave 3 Task A: 旧 {@code Shop(int, Card)} を {@code Shop(int, CardId)} に変更。ドメイン層が Card
   * 実装に依存しない 設計に統一し、ID 保持 + resolver 解決パターンに揃える ({@link
   * core.domain.equipment.Equipment#grantedCards()} と同型)。
   *
   * <p>Gold 不足時は silent fail (apply が引数の Player をそのまま返す、ActionRejected はドメイン層で発火しない)。
   */
  record Shop(int goldCost, CardId cardId) implements LayerEndNode {
    public Shop {
      if (goldCost < 0) {
        throw new IllegalArgumentException("goldCost must be non-negative: " + goldCost);
      }
      Objects.requireNonNull(cardId, "cardId");
    }

    @Override
    public Player apply(Player player, NodeResolveContext context) {
      Objects.requireNonNull(context, "context");
      if (player.gold().amount() < goldCost) {
        return player; // silent fail
      }
      Card granted = context.cards().apply(cardId);
      Objects.requireNonNull(granted, "context.cards() returned null for " + cardId.value());
      Player afterGold = goldCost > 0 ? player.spendGold(new Gold(goldCost)) : player;
      CardPileState pile = afterGold.cardPileState();
      List<Card> newDrawCards = new ArrayList<>(pile.drawPile().cards());
      newDrawCards.add(granted);
      return afterGold.withCardPileState(
          new CardPileState(new DrawPile(newDrawCards), pile.hand(), pile.discardPile()));
    }

    @Override
    public String displayName(NodeResolveContext context) {
      Objects.requireNonNull(context, "context");
      Card granted = context.cards().apply(cardId);
      Objects.requireNonNull(granted, "context.cards() returned null for " + cardId.value());
      return "ショップ: %s (金貨 %d)".formatted(granted.displayName(), goldCost);
    }
  }

  /**
   * イベントノード (§15-8)。Soul / HP / Gold を相対変動させる即時単発効果。「ソウルの祠 (Soul +30 / HP -5)」のような
   * リスク・リワードのトレードオフを表現する。
   *
   * <p>各 delta:
   *
   * <ul>
   *   <li>{@code soulDelta} = 正値のみ (負値は §15-2 で許容しない、Soul.subtract が IAE)
   *   <li>{@code hpDelta} = 正値で healed、負値で damaged (Stats の純関数)
   *   <li>{@code goldDelta} = 正値のみ (本セッション設計、Gold 喪失イベントは将来追加可)
   *   <li>{@code displayLabel} = ノード選択画面に表示する短文 (例「ソウルの祠 (HP -5)」)
   * </ul>
   */
  record Event(int soulDelta, int hpDelta, int goldDelta, String displayLabel)
      implements LayerEndNode {
    public Event {
      if (soulDelta < 0) {
        throw new IllegalArgumentException("soulDelta must be non-negative: " + soulDelta);
      }
      if (goldDelta < 0) {
        throw new IllegalArgumentException("goldDelta must be non-negative: " + goldDelta);
      }
      Objects.requireNonNull(displayLabel, "displayLabel");
      if (displayLabel.isBlank()) {
        throw new IllegalArgumentException("displayLabel must not be blank");
      }
    }

    @Override
    public Player apply(Player player, NodeResolveContext context) {
      // context は未使用 (signature 追従のみ)
      Player after = player;
      if (soulDelta > 0) {
        after = after.addSoul(new Soul(soulDelta));
      }
      if (hpDelta != 0) {
        Stats currentStats = after.stats();
        Stats newStats =
            hpDelta > 0 ? currentStats.healed(hpDelta) : currentStats.damaged(-hpDelta);
        after = after.withStats(newStats);
      }
      if (goldDelta > 0) {
        after = after.addGold(new Gold(goldDelta));
      }
      return after;
    }

    @Override
    public String displayName(NodeResolveContext context) {
      return displayLabel;
    }
  }
}
