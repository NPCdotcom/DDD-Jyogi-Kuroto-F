package core.domain.entity;

/**
 * 敵の種別。撃破時のソウル報酬・表示名などをまとめる。
 *
 * <p>MVP では 1 種 (スライム)。追加時はこの enum に値を増やす + EnemyRepository で能力値テンプレ を差し替えるだけで済むよう、能力値そのものはここに持たない
 * (DRY: 同じ値が定義 / インスタンスに二重に出ない)。
 */
public enum EnemyKind {
  // §15-2 撃破レート (ADR-30 数値仕様乖離修正):
  //   SLIME = Soul 1 / Gold 5、ELITE_SLIME = Soul 3 / Gold 15、BOSS = Soul 20 / Gold 50
  // 仕様 GAME_DESIGN.md §15-2 の「雑魚 Soul 0.5」を整数 record 制約のため切り上げ 1 とし、
  // §15-7 ノードコスト (HP+5 = 6 / 物攻+1 = 5 / 速度+1 = 35 等) との整合バランスを取る。
  // BOSS は §15-2「ボス撃破 (ラン勝利) Soul 20 / Gold 50」を Single Source of Truth として採用。
  SLIME("スライム", 1, 5),
  /** 素早い個体 (§15-5 敵バリエーション、部分実装)。AP が高く手数で攻める雑魚。 報酬・能力値は暫定で、バランス確定はチームメイトの敵設計に委ねる。 */
  SWIFT_SLIME("はやスライム", 1, 6),
  /** 頑強な個体 (§15-5 敵バリエーション、部分実装)。HP・物防が高く打たれ強い雑魚。 報酬・能力値は暫定で、バランス確定はチームメイトの敵設計に委ねる。 */
  TOUGH_SLIME("かたスライム", 2, 8),
  /** 強化個体 (§15-3 / §15-6、5 層ごとに 1 体出現、撃破時にカード追加 UI を発火)。 */
  ELITE_SLIME("強化スライム", 3, 15),
  /** 最終層のボス (§15-6 / §15-2、撃破でラン勝利 = RUN_CLEARED)。 */
  BOSS("ボス", 20, 50);

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

  /**
   * 強化個体かを返す (§15-3 / §15-6 カード追加 UI の発火判定)。
   *
   * <p>呼出側で {@code kind() == EnemyKind.ELITE_SLIME} と書く代わりにこのヘルパを使うことで、新規 Elite 系敵 を追加する際に判定箇所が漏れない
   * (SOLID / 驚き最小)。
   */
  public boolean isElite() {
    return this == ELITE_SLIME;
  }

  /**
   * ボスかを返す (§15-6 RUN_CLEARED 判定の前提)。
   *
   * <p>新規ボス系敵を追加する際にこのヘルパに集約することで判定箇所が漏れない。
   */
  public boolean isBoss() {
    return this == BOSS;
  }
}
