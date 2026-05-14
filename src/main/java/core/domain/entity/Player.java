package core.domain.entity;

import core.domain.battle.ActionPoints;
import core.domain.card.CardPileState;
import core.domain.common.Position;
import core.domain.meta.Soul;
import core.domain.skill.SkillSlot;
import java.util.Objects;

/**
 * プレイヤーキャラクター。ソウルの保持・加算ができる点が Enemy と異なる。
 *
 * <p>共通インターフェース (Actor) は MVP で実利用が無いため設けない (YAGNI)。共通処理が必要になった段階で sealed interface として復活させる。
 *
 * <p>§15-3 のカードシステム導入に伴い {@link CardPileState} を保持する (ADR-18)。戦闘中の山札・手札・捨て札の動的状態を Player record 内に持つ
 * 設計判断は、§15-9 Equipment 系列との所有レイヤー統一と、`new Player(...)` 直接呼出 2 箇所 + `with*` 内部 4 箇所伝播のみで済む実装コスト最小性が
 * 根拠 (3 並列サブエージェントレビュー結論)。
 *
 * <p>{@code pendingMoveCount} は移動カード (§15-5 / ADR-21) を切ったあとに残る「無料移動権の残量」。移動カードの {@code distance} ぶんが付与され、
 * WASD/方向キーで 1 マス移動するごとに -1 される。0 になれば通常モード復帰。ドメインに状態を持つ判断は libgdx-implementer の指摘 (セーブ整合 + AP
 * 切れ自動ターン終了との競合回避) を採用。
 */
public record Player(
    ActorId id,
    Position position,
    Stats stats,
    ActionPoints actionPoints,
    SkillSlot skillSlot,
    Soul soul,
    CardPileState cardPileState,
    int pendingMoveCount) {

  public Player {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(position, "position");
    Objects.requireNonNull(stats, "stats");
    Objects.requireNonNull(actionPoints, "actionPoints");
    Objects.requireNonNull(skillSlot, "skillSlot");
    Objects.requireNonNull(soul, "soul");
    Objects.requireNonNull(cardPileState, "cardPileState");
    if (pendingMoveCount < 0) {
      throw new IllegalArgumentException(
          "pendingMoveCount must be non-negative: " + pendingMoveCount);
    }
  }

  public Player withPosition(Position newPosition) {
    return new Player(
        id, newPosition, stats, actionPoints, skillSlot, soul, cardPileState, pendingMoveCount);
  }

  public Player withStats(Stats newStats) {
    return new Player(
        id, position, newStats, actionPoints, skillSlot, soul, cardPileState, pendingMoveCount);
  }

  public Player withActionPoints(ActionPoints newActionPoints) {
    return new Player(
        id, position, stats, newActionPoints, skillSlot, soul, cardPileState, pendingMoveCount);
  }

  public Player addSoul(Soul delta) {
    return new Player(
        id,
        position,
        stats,
        actionPoints,
        skillSlot,
        soul.add(delta),
        cardPileState,
        pendingMoveCount);
  }

  public Player withCardPileState(CardPileState newCardPileState) {
    return new Player(
        id, position, stats, actionPoints, skillSlot, soul, newCardPileState, pendingMoveCount);
  }

  public Player withPendingMoveCount(int newPendingMoveCount) {
    return new Player(
        id, position, stats, actionPoints, skillSlot, soul, cardPileState, newPendingMoveCount);
  }
}
