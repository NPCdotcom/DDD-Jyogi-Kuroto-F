package core.application;

import java.util.Objects;
import java.util.UUID;

/**
 * ラン 1 回を一意に識別する値オブジェクト (SAVE-03A)。
 *
 * <p>レビュー P0-1 の防止条件。{@code ProfileData.activeRunId} と {@code RunCheckpoint.runId} が
 * 一致する場合だけ再開と精算を許すことで、終了済みランの Checkpoint が残っていても「つづき」から 復帰できないようにする。同じ ID の再精算を拒否することで、Soul
 * と周回数の二重加算も防ぐ。
 *
 * <p>{@link #newRandom()} は {@link UUID} を使うため呼び出しごとに結果が変わる。ドメインの純粋性を 保つため、生成の呼出は {@code
 * presentation} / {@code infrastructure} 側で行い、テストは {@link #of(String)} で 固定値を渡す。
 */
public record RunId(String value) {

  public RunId {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("value must not be blank");
    }
  }

  public static RunId of(String value) {
    return new RunId(value);
  }

  /** 新しいランへ割り当てる一意な ID を作る。呼出は起動側 (presentation / infrastructure) から行う。 */
  public static RunId newRandom() {
    return new RunId(UUID.randomUUID().toString());
  }
}
