package core.domain.skill;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * プレイヤー・敵が戦闘に持ち込めるスキル装着スロット。
 *
 * <p>MVP 最大数は 4 (GAME_DESIGN §5-2)。コンパクトコンストラクタで defensive copy する。
 */
public record SkillSlot(List<Skill> skills, int maxSize) {

  public SkillSlot {
    Objects.requireNonNull(skills, "skills");
    if (maxSize <= 0) {
      throw new IllegalArgumentException("maxSize must be positive: " + maxSize);
    }
    if (skills.size() > maxSize) {
      throw new IllegalArgumentException(
          "too many skills: %d > maxSize %d".formatted(skills.size(), maxSize));
    }
    skills = List.copyOf(skills);
  }

  public static SkillSlot empty(int maxSize) {
    return new SkillSlot(List.of(), maxSize);
  }

  public int size() {
    return skills.size();
  }

  public Optional<Skill> at(int index) {
    if (index < 0 || index >= skills.size()) {
      return Optional.empty();
    }
    return Optional.of(skills.get(index));
  }
}
