package core.domain.skill;

import core.domain.card.CardElement;
import java.util.Objects;

/**
 * スキルが対象に与える効果。
 *
 * <p>MVP では単純なダメージのみ。回復・状態異常などは MVP 完了後に追加する (YAGNI)。 sealed にしているのは将来追加時に網羅性チェック (switch 式)
 * を効かせるため。
 */
public sealed interface SkillEffect permits SkillEffect.Damage {

  /**
   * 単純ダメージ。{@code amount} を基準値とし、被弾側の {@code element} 対応防御 (物理 → 物防 / 魔法 → 魔防) で軽減される。
   *
   * <p>ADR-17 改訂: 旧仕様ではスキルダメージは固定値だったが、防御ステが敵の攻撃に対し無意味になる問題を受け、 スキルダメージも {@code max(1, amount −
   * 防御)} を通す。軽減計算は {@code TurnEngine.resolveSkillDamage} が担う。
   */
  record Damage(int amount, CardElement element) implements SkillEffect {
    public Damage {
      if (amount < 0) {
        throw new IllegalArgumentException("damage amount must be non-negative: " + amount);
      }
      Objects.requireNonNull(element, "element");
    }

    /** 物理スキル用の利便コンストラクタ (既存呼出互換、{@code element = PHYSICAL})。 */
    public Damage(int amount) {
      this(amount, CardElement.PHYSICAL);
    }
  }
}
