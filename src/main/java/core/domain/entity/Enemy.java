package core.domain.entity;

import core.domain.battle.ActionPoints;
import core.domain.common.Position;
import core.domain.skill.SkillSlot;
import java.util.Objects;
import java.util.Optional;

/**
 * 敵キャラクター。種別 (kind) が撃破報酬や見た目を決定する。
 *
 * <p>Wave 13 W13-α: AI 状態 ({@link EnemyAiState}) + 最後にプレイヤーを目撃した位置 ({@code lastKnownPlayerPos}) を
 * 保持する。既存 6 引数コンストラクタは {@code IDLE} + {@code Optional.empty()} でデフォルト初期化する 後方互換コンストラクタ経由で残置
 * (InitialStateFactory / 既存テスト fixture 無修正)。
 */
public record Enemy(
    ActorId id,
    Position position,
    Stats stats,
    ActionPoints actionPoints,
    SkillSlot skillSlot,
    EnemyKind kind,
    EnemyAiState aiState,
    Optional<Position> lastKnownPlayerPos) {

  public Enemy {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(position, "position");
    Objects.requireNonNull(stats, "stats");
    Objects.requireNonNull(actionPoints, "actionPoints");
    Objects.requireNonNull(skillSlot, "skillSlot");
    Objects.requireNonNull(kind, "kind");
    // null は IDLE / Optional.empty() に正規化 (Wave 11 W11-β Card.rarity と同型 graceful)
    aiState = (aiState == null) ? EnemyAiState.IDLE : aiState;
    lastKnownPlayerPos = (lastKnownPlayerPos == null) ? Optional.empty() : lastKnownPlayerPos;
  }

  /**
   * 後方互換コンストラクタ (Wave 13 W13-α 以前の呼出はこちらを通る)。{@code aiState = IDLE} + {@code lastKnownPlayerPos =
   * Optional.empty()} で初期化。InitialStateFactory / DomainFixtures / 既存テスト 多数は無修正で通る。
   */
  public Enemy(
      ActorId id,
      Position position,
      Stats stats,
      ActionPoints actionPoints,
      SkillSlot skillSlot,
      EnemyKind kind) {
    this(id, position, stats, actionPoints, skillSlot, kind, EnemyAiState.IDLE, Optional.empty());
  }

  public Enemy withPosition(Position newPosition) {
    return new Enemy(
        id, newPosition, stats, actionPoints, skillSlot, kind, aiState, lastKnownPlayerPos);
  }

  public Enemy withStats(Stats newStats) {
    return new Enemy(
        id, position, newStats, actionPoints, skillSlot, kind, aiState, lastKnownPlayerPos);
  }

  public Enemy withActionPoints(ActionPoints newActionPoints) {
    return new Enemy(
        id, position, stats, newActionPoints, skillSlot, kind, aiState, lastKnownPlayerPos);
  }

  /** AI 状態を差し替えた新インスタンス (Wave 13 W13-α)。 */
  public Enemy withAiState(EnemyAiState newAiState) {
    return new Enemy(
        id, position, stats, actionPoints, skillSlot, kind, newAiState, lastKnownPlayerPos);
  }

  /** 最後にプレイヤーを目撃した位置を差し替えた新インスタンス (Wave 13 W13-α)。 */
  public Enemy withLastKnownPlayerPos(Optional<Position> newLastKnownPlayerPos) {
    return new Enemy(
        id, position, stats, actionPoints, skillSlot, kind, aiState, newLastKnownPlayerPos);
  }
}
