package core.domain.card;

/**
 * カードの属性 (物理 / 魔法)。
 *
 * <p>ダメージ計算で参照される側のステ (物攻/物防 か 魔攻/魔防 か) を決定する。ハイブリッド属性は §15-3 / §15-4 で扱わない (YAGNI)。
 */
public enum CardElement {
  /** 物理 (物攻 / 物防 を参照)。 */
  PHYSICAL,
  /** 魔法 (魔攻 / 魔防 を参照)。 */
  MAGICAL
}
