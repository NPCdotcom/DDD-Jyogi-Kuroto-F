package core.domain.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SkillSlotTest {

  private static Skill skill(String id, int apCost) {
    return new Skill(SkillId.of(id), id, apCost, new SkillEffect.Damage(1));
  }

  @Test
  void atReturnsEmptyForOutOfRange() {
    SkillSlot slot = new SkillSlot(List.of(skill("a", 1)), 4);
    assertTrue(slot.at(-1).isEmpty());
    assertTrue(slot.at(1).isEmpty());
    assertTrue(slot.at(slot.size()).isEmpty());
  }

  @Test
  void atReturnsSkillForValidIndex() {
    Skill a = skill("a", 1);
    SkillSlot slot = new SkillSlot(List.of(a), 4);
    assertEquals(a, slot.at(0).orElseThrow());
  }

  @Test
  void exceedingMaxSizeRejected() {
    Skill a = skill("a", 1);
    Skill b = skill("b", 1);
    Skill c = skill("c", 1);
    assertThrows(IllegalArgumentException.class, () -> new SkillSlot(List.of(a, b, c), 2));
  }

  @Test
  void emptyFactoryReturnsEmptyWithMax() {
    SkillSlot slot = SkillSlot.empty(4);
    assertEquals(0, slot.size());
    assertEquals(4, slot.maxSize());
  }

  @Test
  void compactConstructorPerformsDefensiveCopy() {
    Skill a = skill("a", 1);
    List<Skill> mutable = new ArrayList<>();
    mutable.add(a);
    SkillSlot slot = new SkillSlot(mutable, 4);
    mutable.clear();
    assertEquals(1, slot.size(), "外部の List 操作が内部に伝播してはならない");
  }
}
