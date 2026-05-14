package core.domain.entity;

/**
 * 敵の種別。撃破時のソウル報酬・表示名などをまとめる。
 *
 * <p>MVP では 1 種 (スライム)。追加時はこの enum に値を増やす + EnemyRepository で能力値テンプレ を差し替えるだけで済むよう、能力値そのものはここに持たない
 * (DRY: 同じ値が定義 / インスタンスに二重に出ない)。
 */
public enum EnemyKind {
  SLIME("スライム", 5);

  private final String displayName;
  private final int soulReward;

  EnemyKind(String displayName, int soulReward) {
    this.displayName = displayName;
    this.soulReward = soulReward;
  }

  public String displayName() {
    return displayName;
  }

  public int soulReward() {
    return soulReward;
  }
}
