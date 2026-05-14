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
 */
public record Player(
    ActorId id,
    Position position,
    Stats stats,
    ActionPoints actionPoints,
    SkillSlot skillSlot,
    Soul soul,
    CardPileState cardPileState) {

  public Player {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(position, "position");
    Objects.requireNonNull(stats, "stats");
    Objects.requireNonNull(actionPoints, "actionPoints");
    Objects.requireNonNull(skillSlot, "skillSlot");
    Objects.requireNonNull(soul, "soul");
    Objects.requireNonNull(cardPileState, "cardPileState");
  }

  public Player withPosition(Position newPosition) {
    return new Player(id, newPosition, stats, actionPoints, skillSlot, soul, cardPileState);
  }

  public Player withStats(Stats newStats) {
    return new Player(id, position, newStats, actionPoints, skillSlot, soul, cardPileState);
  }

  public Player withActionPoints(ActionPoints newActionPoints) {
    return new Player(id, position, stats, newActionPoints, skillSlot, soul, cardPileState);
  }

  public Player addSoul(Soul delta) {
    return new Player(id, position, stats, actionPoints, skillSlot, soul.add(delta), cardPileState);
  }

  public Player withCardPileState(CardPileState newCardPileState) {
    return new Player(id, position, stats, actionPoints, skillSlot, soul, newCardPileState);
  }
}
