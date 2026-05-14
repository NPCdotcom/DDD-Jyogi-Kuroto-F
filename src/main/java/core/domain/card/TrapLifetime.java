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
   * <p>追加状態を持たないため、 {@link #INSTANCE} を再利用する (record の identity は意味を持たず、参照同一性に依存しないためシングルトン的扱いで問題ない)。
   */
  record UntilStepped() implements TrapLifetime {
    /** 共有インスタンス。生成コスト削減のため使用側はこちらを参照する。 */
    public static final UntilStepped INSTANCE = new UntilStepped();
  }

  /**
   * 魔法罠の残存ルール: 残り {@code remaining} ターンで消滅。
   *
   * <p>remaining は 1 以上を強制 (0 ターン残りの罠は「設置直後に消える」状態となり意味を成さないため、 設置時に 0 を受け取らない設計)。
   */
  record Turns(int remaining) implements TrapLifetime {
    public Turns {
      if (remaining < 1) {
        throw new IllegalArgumentException("remaining must be >= 1 (got " + remaining + ")");
      }
    }
  }
}
