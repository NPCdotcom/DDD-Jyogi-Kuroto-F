package core.application;

import java.util.Objects;
import java.util.Random;

/**
 * ラン単位で再構築される application 層の状態を集約する不変 record (Wave 9 W9-α)。
 *
 * <p>従来 {@code DddGame} に個別フィールド ({@link GameContext} + {@link TurnDirector} + {@link Random}) として
 * 散在していた「ラン単位の揮発状態」を 1 つの集約に統合。ラン未開始の意味は {@code DddGame} 側で {@code Optional<RunSession>}
 * として表現することで、{@code if (context == null)} の散在を撤廃する。
 *
 * <p>不変性: record として宣言、null チェックは compact constructor で集約。状態遷移は {@code DddGame.startNewRun()} /
 * {@code loadFromSave()} で **新インスタンス** を作って Optional に詰め替える (PlayerProgress の with* と同型の更新パターン)。
 *
 * <p>ライフサイクル:
 *
 * <ul>
 *   <li>生成: {@code DddGame.startNewRun()} / {@code loadFromSave()} で `Optional.of(new
 *       RunSession(...))`
 *   <li>破棄: {@code DddGame.onRunEnded()} で `Optional.empty()` に切り替え (揮発メモリの即時クリア)
 * </ul>
 *
 * <p>{@link RunId} はラン 1 回を一意に識別する (SAVE-03A)。{@code ProfileData.activeRunId} と 突き合わせることで、終了済みランの
 * Checkpoint からの再開と、同じランの二重精算を拒否できる (レビュー P0-1)。
 */
public record RunSession(
    RunId runId, GameContext context, TurnDirector director, Random rng, int initialRunSoul) {

  public RunSession {
    Objects.requireNonNull(runId, "runId");
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(director, "director");
    Objects.requireNonNull(rng, "rng");
  }

  public RunSession(RunId runId, GameContext context, TurnDirector director, Random rng) {
    this(runId, context, director, rng, 0);
  }
}
