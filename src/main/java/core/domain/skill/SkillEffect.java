package core.domain.skill;

/**
 * スキルが対象に与える効果。
 *
 * <p>MVP では単純なダメージのみ。回復・状態異常などは MVP 完了後に追加する (YAGNI)。 sealed にしているのは将来追加時に網羅性チェック (switch 式)
 * を効かせるため。
 */
public sealed interface SkillEffect permits SkillEffect.Damage {

  /** 単純ダメージ。amount のぶん HP を減らす。 */
  record Damage(int amount) implements SkillEffect {
    public Damage {
      if (amount < 0) {
        throw new IllegalArgumentException("damage amount must be non-negative: " + amount);
      }
    }
  }
}
