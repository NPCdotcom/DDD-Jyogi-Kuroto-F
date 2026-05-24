package core.domain.battle;

import core.domain.card.CardElement;
import core.domain.entity.Stats;
import java.util.Objects;

/**
 * ダメージ計算の単一ソース (§15-4 / ADR-17 改訂)。
 *
 * <p>計算式 {@code max(1, base + 攻撃側ステ - 防御側ステ)} を本クラスに集約する (Single Source of Truth)。 以前は {@code
 * CardEffect.Damage#resolve} / {@code TurnEngine#resolveSkillDamage} / {@code
 * PlacedTrap#resolveDamage} の 3 箇所に同一ロジックが分散していたが、 仕様変更時の差分齟齬を防ぐためここへ統合した (DRY)。
 *
 * <p>純関数: 状態を持たず、入力 → 出力のみ。Random / 時刻 / I/O 不使用。
 *
 * <h2>2 つの式バリエーション</h2>
 *
 * <ul>
 *   <li>{@link #resolve}: カード攻撃用。攻撃側ステを加算する完全式 {@code max(1, base + atk - def)}。
 *   <li>{@link #resolveWithoutAttacker}: スキル / 罠用。固定 baseValue から防御のみを引く式 {@code max(1, base -
 *       def)}。 ADR-17 改訂 (スキル) / ADR-22 (罠) で「攻撃側ステに依存しない固定ダメージ」を意図的に選択。
 * </ul>
 *
 * <p>属性 (PHYSICAL / MAGICAL) は {@link Stats#attackBy} / {@link Stats#defenseAgainst} に委ねる。 本クラスで
 * switch を持たないことで、新属性追加時の修正箇所を Stats と CardElement に局所化する。
 */
public final class DamageFormula {

  private DamageFormula() {}

  /**
   * 攻撃側ステ込みの最終ダメージを確定する (カード攻撃経路)。
   *
   * <p>計算式: {@code max(1, base + attacker.attackBy(element) - defender.defenseAgainst(element))}。
   *
   * @param base カード基礎値 (1 以上)
   * @param attacker 攻撃側 Stats (物攻 / 魔攻を参照)
   * @param defender 防御側 Stats (物防 / 魔防を参照)
   * @param element カード属性
   * @return 最終ダメージ ({@code >= 1})、最低 1 ダメ保証
   */
  public static int resolve(int base, Stats attacker, Stats defender, CardElement element) {
    Objects.requireNonNull(attacker, "attacker");
    Objects.requireNonNull(defender, "defender");
    Objects.requireNonNull(element, "element");
    int raw = base + attacker.attackBy(element) - defender.defenseAgainst(element);
    return Math.max(1, raw);
  }

  /**
   * 攻撃側ステを使わない最終ダメージを確定する (スキル / 罠経路)。
   *
   * <p>計算式: {@code max(1, base - defender.defenseAgainst(element))}。 スキル (ADR-17 改訂) と罠 (ADR-22)
   * は「設置時 or 定義時にダメージが固定され、 攻撃側の現ステに依存しない」設計のため、加算項を持たない。
   *
   * @param base 基礎値 (スキル amount / 罠 baseValue、1 以上)
   * @param defender 防御側 Stats
   * @param element 属性
   * @return 最終ダメージ ({@code >= 1})
   */
  public static int resolveWithoutAttacker(int base, Stats defender, CardElement element) {
    Objects.requireNonNull(defender, "defender");
    Objects.requireNonNull(element, "element");
    return Math.max(1, base - defender.defenseAgainst(element));
  }
}
