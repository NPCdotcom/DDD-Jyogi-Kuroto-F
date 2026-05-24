package core.domain.skill;

import java.util.Objects;

/** スキルを一意に識別する不変 ID。 */
public record SkillId(String value) {

  public SkillId {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("SkillId must not be blank");
    }
  }

  public static SkillId of(String value) {
    return new SkillId(value);
  }
}
