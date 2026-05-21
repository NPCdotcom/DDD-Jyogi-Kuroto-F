package core.domain.card;

import core.domain.entity.Stats;
import java.util.Objects;

/**
 * カードの効果。
 *
 * <p>sealed で型を限定し、switch 式の網羅性チェックを利用する。新カードタイプ追加時は permits 一覧を更新するためコンパイラが全 switch 箇所を強制的に書き換えさせる
 * (驚き最小)。
 *
 * <p>仕様: §15-3 / §15-4。ダメージ計算式 (`max(1, baseValue + 物攻 - 物防)`) は {@link Damage#resolve(Stats,
 * Stats, CardElement)} に集約する (ADR-17)。`CardEffect.Damage` 自身が「自分の最終ダメージを確定する」責務を持ち、TurnEngine
 * 等の上位層は結果の整数を Stats へ反映するだけ。
 */
public sealed interface CardEffect
    permits CardEffect.Damage, CardEffect.Move, CardEffect.Buff, CardEffect.Trap {

  /**
   * バフが上昇させるステの種類。
   *
   * <p>最小限の列挙のみ用意し (§15-4 の 6 ステのうち、攻撃/防御/速度のみバフ対象とする)、HP バフは §15-3 のスコープ外として扱う。
   */
  enum BuffKind {
    /** 物攻 上昇。 */
    PHYSICAL_ATTACK_UP,
    /** 魔攻 上昇。 */
    MAGICAL_ATTACK_UP,
    /** 物防 上昇。 */
    PHYSICAL_DEFENSE_UP,
    /** 魔防 上昇。 */
    MAGICAL_DEFENSE_UP,
    /** 速度 上昇 (= 次ターンの最大 AP 上昇)。 */
    SPEED_UP
  }

  /**
   * 単純ダメージ効果。最終ダメ = {@code max(1, baseValue + 攻撃側ステ - 防御側ステ)} (§15-4)。
   *
   * <p>baseValue は 1 以上を強制 (0 や負値は「最低 1 ダメ保証」とも整合しないため設計時点で弾く)。
   */
  record Damage(int baseValue) implements CardEffect {
    public Damage {
      if (baseValue < 1) {
        throw new IllegalArgumentException("baseValue must be >= 1 (got " + baseValue + ")");
      }
    }

    /**
     * 最終ダメージを確定する (§15-4、ADR-17)。
     *
     * <p>計算式: {@code max(1, baseValue + 攻 - 防)}。element が PHYSICAL なら物攻/物防、MAGICAL なら魔攻/魔防を参照。最低 1
     * ダメ保証 (防御 > 攻撃でも 1 を返す)。
     *
     * @param attacker 攻撃側の Stats (物攻 / 魔攻を参照)
     * @param defender 防御側の Stats (物防 / 魔防を参照)
     * @param element カード属性 (Card.element() を渡す)
     * @return 最終ダメージ ({@code >= 1})
     */
    public int resolve(Stats attacker, Stats defender, CardElement element) {
      Objects.requireNonNull(attacker, "attacker");
      Objects.requireNonNull(defender, "defender");
      Objects.requireNonNull(element, "element");
      int raw =
          switch (element) {
            case PHYSICAL -> baseValue + attacker.physicalAttack() - defender.physicalDefense();
            case MAGICAL -> baseValue + attacker.magicalAttack() - defender.magicalDefense();
          };
      return Math.max(1, raw);
    }
  }

  /**
   * 移動効果。指定 distance タイルぶん、カード使用時の {@code Direction} (= 別途 BattleAction 経由) に進む。
   *
   * <p>distance は 1 以上。0 距離移動は無意味なため設計時点で禁止。
   */
  record Move(int distance) implements CardEffect {
    public Move {
      if (distance < 1) {
        throw new IllegalArgumentException("distance must be >= 1 (got " + distance + ")");
      }
    }
  }

  /**
   * バフ効果。指定種類のステを durationTurns ターンのあいだ {@code amount} 加算する。
   *
   * <p>amount は 0 以外 (負値で「デバフ」も同枠で扱う設計余地を残す)。durationTurns は 1 以上。
   */
  record Buff(BuffKind kind, int amount, int durationTurns) implements CardEffect {
    public Buff {
      Objects.requireNonNull(kind, "kind");
      if (amount == 0) {
        throw new IllegalArgumentException("amount must not be 0");
      }
      if (durationTurns < 1) {
        throw new IllegalArgumentException(
            "durationTurns must be >= 1 (got " + durationTurns + ")");
      }
    }
  }

  /**
   * 罠設置効果。設置タイル上で発動条件を満たした敵に baseValue ベースのダメージ。
   *
   * <p>発動条件・残存期間の差は {@link TrapLifetime} で型表現する (boolean フラグや列挙ではなく sealed)。
   */
  record Trap(int baseValue, TrapLifetime lifetime) implements CardEffect {
    public Trap {
      Objects.requireNonNull(lifetime, "lifetime");
      if (baseValue < 1) {
        throw new IllegalArgumentException("baseValue must be >= 1 (got " + baseValue + ")");
      }
    }
  }
}
