package core.domain.entity;

import core.domain.battle.ActionPoints;
import core.domain.card.CardPileState;
import core.domain.common.Position;
import core.domain.meta.Gold;
import core.domain.meta.Soul;
import core.domain.skill.SkillSlot;
import java.util.Objects;

/**
 * プレイヤーキャラクター。ソウルの保持・加算ができる点が Enemy と異なる。
 *
 * <p>共通インターフェース (Actor) は MVP で実利用が無いため設けない (YAGNI)。共通処理が必要になった段階で sealed interface として復活させる。
 *
 * <p>ADR-25 で record を 6 引数に縮退 (旧 8 引数)。能力値関連 (Stats / ActionPoints / SkillSlot / Optional&lt;Equipment&gt; /
 * List&lt;ActiveBuff&gt;) は {@link PlayerStatuses} に集約し、Buff (§15-3) + E-5 装備 (§15-9) を 1 つのレイヤーで扱う。
 * 既存呼出箇所互換のため {@link #stats()} / {@link #actionPoints()} / {@link #skillSlot()} は {@link PlayerStatuses} への
 * 委譲アクセサとして残す (ADR-25 Decision #5)。
 *
 * <p>§15-3 のカードシステム導入に伴い {@link CardPileState} を保持する (ADR-18)。戦闘中の山札・手札・捨て札の動的状態を Player record 内に持つ。
 *
 * <p>{@code pendingMoveCount} は移動カード (§15-5 / ADR-21) を切ったあとに残る「無料移動権の残量」。移動カードの {@code distance} ぶんが付与され、
 * WASD/方向キーで 1 マス移動するごとに -1 される。0 になれば通常モード復帰。
 */
public record Player(
    ActorId id,
    Position position,
    Soul soul,
    Gold gold,
    PlayerStatuses statuses,
    CardPileState cardPileState,
    int pendingMoveCount) {

  public Player {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(position, "position");
    Objects.requireNonNull(soul, "soul");
    Objects.requireNonNull(gold, "gold");
    Objects.requireNonNull(statuses, "statuses");
    Objects.requireNonNull(cardPileState, "cardPileState");
    if (pendingMoveCount < 0) {
      throw new IllegalArgumentException(
          "pendingMoveCount must be non-negative: " + pendingMoveCount);
    }
  }

  // ---- 委譲アクセサ (ADR-25 Decision #5): 既存呼出箇所のシグネチャ互換のため維持 ----

  public Stats stats() {
    return statuses.stats();
  }

  public ActionPoints actionPoints() {
    return statuses.actionPoints();
  }

  public SkillSlot skillSlot() {
    return statuses.skillSlot();
  }

  /**
   * 素ステ + 装備補正 + Buff 合算後の {@link Stats} を返す純関数 (§15-4 / ADR-25)。
   *
   * <p>戦闘ダメージ計算系では本メソッド経由で攻撃側 / 防御側のステを取得する。素の {@link #stats()} は表示やテストでの素ステ確認用。
   */
  public Stats effectiveStats() {
    return statuses.effectiveStats();
  }

  // ---- with* (内部で PlayerStatuses 経由) ----

  public Player withPosition(Position newPosition) {
    return new Player(id, newPosition, soul, gold, statuses, cardPileState, pendingMoveCount);
  }

  public Player withStats(Stats newStats) {
    return new Player(
        id, position, soul, gold, statuses.withStats(newStats), cardPileState, pendingMoveCount);
  }

  public Player withActionPoints(ActionPoints newActionPoints) {
    return new Player(
        id,
        position,
        soul,
        gold,
        statuses.withActionPoints(newActionPoints),
        cardPileState,
        pendingMoveCount);
  }

  public Player addSoul(Soul delta) {
    return new Player(
        id, position, soul.add(delta), gold, statuses, cardPileState, pendingMoveCount);
  }

  /** §15-2 / §15-9: 金貨を加算した新 Player を返す純関数。 */
  public Player addGold(Gold delta) {
    return new Player(
        id, position, soul, gold.add(delta), statuses, cardPileState, pendingMoveCount);
  }

  /** §15-9 Shop: 金貨を消費した新 Player を返す。残量不足は {@link IllegalStateException}。 */
  public Player spendGold(Gold cost) {
    return new Player(
        id, position, soul, gold.subtract(cost), statuses, cardPileState, pendingMoveCount);
  }

  public Player withCardPileState(CardPileState newCardPileState) {
    return new Player(id, position, soul, gold, statuses, newCardPileState, pendingMoveCount);
  }

  public Player withPendingMoveCount(int newPendingMoveCount) {
    return new Player(id, position, soul, gold, statuses, cardPileState, newPendingMoveCount);
  }

  /** Buff / 装備の追加・除去で {@link PlayerStatuses} 全体を差し替える時に使う (ADR-25)。 */
  public Player withStatuses(PlayerStatuses newStatuses) {
    return new Player(id, position, soul, gold, newStatuses, cardPileState, pendingMoveCount);
  }
}
