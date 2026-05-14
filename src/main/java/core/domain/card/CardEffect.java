package core.domain.card;

import java.util.Objects;

/**
 * カードの効果。
 *
 * <p>sealed で型を限定し、switch 式の網羅性チェックを利用する。新カードタイプ追加時は permits 一覧を更新するためコンパイラが全 switch 箇所を強制的に書き換えさせる
 * (驚き最小)。
 *
 * <p>仕様: §15-3。ダメージ計算式は CardEffect 自身は知らず、 application 層の解決器 (TurnEngine 等) が Stats と組み合わせて算出する
 * (副作用と純粋値の分離)。
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
