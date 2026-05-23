package core.domain.meta;

import core.domain.entity.EnemyKind;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * 撃破済み敵種の集合 (§15-5 / E-7、雛形)。
 *
 * <p>本セッションでは「撃破済 {@link EnemyKind} を記録する」最小機能のみ実装。次行動予告 UI / 図鑑画面は M2 で別途実装する ({@code
 * tasks/m2_backlog.md})。
 *
 * <p>不変 record + 防御コピーで保持。新しい撃破は {@link #withDefeated(EnemyKind)} で新インスタンスを返す。
 */
public record Bestiary(Set<EnemyKind> defeatedKinds) {

  public Bestiary {
    Objects.requireNonNull(defeatedKinds, "defeatedKinds");
    defeatedKinds = Set.copyOf(defeatedKinds);
  }

  /** 空の Bestiary を返す。 */
  public static Bestiary empty() {
    return new Bestiary(EnumSet.noneOf(EnemyKind.class));
  }

  /** 指定の敵種を撃破済として記録した新しい Bestiary を返す (immutable 更新)。 */
  public Bestiary withDefeated(EnemyKind kind) {
    Objects.requireNonNull(kind, "kind");
    if (defeatedKinds.contains(kind)) {
      return this;
    }
    EnumSet<EnemyKind> next = EnumSet.noneOf(EnemyKind.class);
    next.addAll(defeatedKinds);
    next.add(kind);
    return new Bestiary(next);
  }

  /** 指定の敵種が撃破済 (= 次行動予告の表示資格あり) なら true。 */
  public boolean isRegistered(EnemyKind kind) {
    return defeatedKinds.contains(kind);
  }

  /** 撃破済敵種の数。 */
  public int size() {
    return defeatedKinds.size();
  }
}
