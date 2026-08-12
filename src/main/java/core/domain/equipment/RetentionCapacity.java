package core.domain.equipment;

/**
 * 装備保護枠の容量 (EQUIP-00 / §15-7 / §15-9)。値オブジェクト。
 *
 * <p>死亡時に持込装備を失わずに済む枠の数を表す。初期値は 0 枠で、ソウルツリーの「魂の刻印 I」(30 Soul) と 「魂の刻印 II」(60 Soul) で 1 枠ずつ増え、上限は
 * {@value #MAX} 枠。
 *
 * <p>上限をここで強制するのは、ツリー側の解放効果の合計が壊れても保護枠が青天井にならないようにするため (驚き最小)。 ツリーの解放状態から容量を導く処理は SOUL-03 が担当する。
 */
public record RetentionCapacity(int value) {

  /** 保護枠の上限。「魂の刻印」ノードが 2 つしかないため 2 (§15-7)。 */
  public static final int MAX = 2;

  public RetentionCapacity {
    if (value < 0) {
      throw new IllegalArgumentException("value must be >= 0: " + value);
    }
    if (value > MAX) {
      throw new IllegalArgumentException("value must be <= " + MAX + ": " + value);
    }
  }

  public static RetentionCapacity of(int value) {
    return new RetentionCapacity(value);
  }

  /** 保護枠なし (新規 Profile の初期状態)。 */
  public static RetentionCapacity none() {
    return new RetentionCapacity(0);
  }

  /** 指定した保護 ID 数がこの容量に収まるか。 */
  public boolean canHold(int protectedCount) {
    return protectedCount <= value;
  }
}
