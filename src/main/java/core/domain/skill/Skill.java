package core.domain.skill;

import java.util.Objects;

/** スキルの不変定義。スロットに装着して戦闘で発動する。 */
public record Skill(SkillId id, String displayName, int apCost, SkillEffect effect) {

  public Skill {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(displayName, "displayName");
    Objects.requireNonNull(effect, "effect");
    if (displayName.isBlank()) {
      throw new IllegalArgumentException("displayName must not be blank");
    }
    if (apCost <= 0) {
      throw new IllegalArgumentException("apCost must be positive: " + apCost);
    }
  }
}
