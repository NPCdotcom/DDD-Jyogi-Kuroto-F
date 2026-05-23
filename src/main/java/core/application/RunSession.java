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
 *   <li>破棄: {@code DddGame.onRunEnded()} で `Optional.empty()` に切り替え (揮発メモリの即時クリア、Android メモリ圧迫回避)
 * </ul>
 */
public record RunSession(GameContext context, TurnDirector director, Random rng) {

  public RunSession {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(director, "director");
    Objects.requireNonNull(rng, "rng");
  }
}
