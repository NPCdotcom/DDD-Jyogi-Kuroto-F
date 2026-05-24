package core.domain.card;

/**
 * 罠の残存ルール。
 *
 * <p>物理罠 ({@link UntilStepped}) は「踏まれるまで残る」 1 回発動型、魔法罠 ({@link Turns}) は「N ターン経過で消滅」型。
 *
 * <p>sealed で 2 形態に限定。boolean フラグや単一 enum ではなくサブタイプで表現することで、 case ごとに必要なパラメータ (Turns の残ターン数) を型で持てる
 * (驚き最小)。
 */
public sealed interface TrapLifetime permits TrapLifetime.UntilStepped, TrapLifetime.Turns {

  /**
   * 物理罠の残存ルール: 踏まれるまで永続。
   *
   * <p>追加状態を持たないため、 {@link #INSTANCE} を再利用する (record の identity
   * は意味を持たず、参照同一性に依存しないためシングルトン的扱いで問題ない)。
   */
  record UntilStepped() implements TrapLifetime {
    /** 共有インスタンス。生成コスト削減のため使用側はこちらを参照する。 */
    public static final UntilStepped INSTANCE = new UntilStepped();
  }

  /**
   * 魔法罠の残存ルール: 残り {@code remaining} ターンで消滅。
   *
   * <p>remaining は 0 以上を許容する (0 は「期限切れ直前」の中間値として {@link
   * core.domain.dungeon.PlacedTrap#decrementedLifetime()} が生成する。その後 {@link
   * core.domain.dungeon.PlacedTrap#isAlive()} が 0 を除去する)。設置時 ({@code CardEffect.Trap}) に 0 以下を
   * 渡すのは別クラス側で防ぐ。
   */
  record Turns(int remaining) implements TrapLifetime {
    public Turns {
      if (remaining < 0) {
        throw new IllegalArgumentException("remaining must be >= 0 (got " + remaining + ")");
      }
    }
  }
}
