package core.domain.entity;

/**
 * 敵の種別。撃破時のソウル報酬・表示名などをまとめる。
 *
 * <p>MVP では 1 種 (スライム)。追加時はこの enum に値を増やす + EnemyRepository で能力値テンプレ を差し替えるだけで済むよう、能力値そのものはここに持たない
 * (DRY: 同じ値が定義 / インスタンスに二重に出ない)。
 */
public enum EnemyKind {
  // §15-2 撃破レート (ADR-30 数値仕様乖離修正): 雑魚 SLIME = Soul 1 / Gold 5。
  // 仕様 GAME_DESIGN.md §15-2 の「雑魚 Soul 0.5」を整数 record 制約のため切り上げ 1 とし、
  // §15-7 ノードコスト (HP+5 = 6 / 物攻+1 = 5 / 速度+1 = 35 等) との整合バランスを取る。
  // ELITE_SLIME / BOSS は §15-6 強化個体実装時に別 enum 値で追加 (本コミット未対応)。
  SLIME("スライム", 1, 5);

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
