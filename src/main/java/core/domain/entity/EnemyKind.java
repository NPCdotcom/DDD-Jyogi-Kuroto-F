package core.domain.entity;

/**
 * 敵の種別。撃破時のソウル報酬・表示名などをまとめる。
 *
 * <p>MVP では 1 種 (スライム)。追加時はこの enum に値を増やす + EnemyRepository で能力値テンプレ を差し替えるだけで済むよう、能力値そのものはここに持たない
 * (DRY: 同じ値が定義 / インスタンスに二重に出ない)。
 */
public enum EnemyKind {
  // §15-2 撃破レート: 雑魚 = Soul 5 / Gold 5、強化個体 = Gold 15、ボス = Gold 50 (SLIME は雑魚枠)。
  SLIME("スライム", 5, 5);

  private final String displayName;
  private final int soulReward;
  private final int goldReward;

  EnemyKind(String displayName, int soulReward, int goldReward) {
    this.displayName = displayName;
    this.soulReward = soulReward;
    this.goldReward = goldReward;
  }

  public String displayName() {
    return displayName;
  }

  public int soulReward() {
    return soulReward;
  }

  /** 撃破時に Player に加算される {@link core.domain.meta.Gold} 量 (§15-2 / §15-9 Shop の前提)。 */
  public int goldReward() {
    return goldReward;
  }
}
