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

  /** InitialStateFactory の初期 SkillSlot (maxSize=4) と同じ構成を検証 (Wave2 Task C)。 */
  @Test
  void defaultMaxSizeIsFour() {
    SkillSlot slot = new SkillSlot(List.of(skill("light_slash", 2), skill("heavy_slash", 3)), 4);
    assertEquals(4, slot.maxSize(), "初期スキル枠は 4 であること");
    assertEquals(2, slot.size(), "初期装着スキル数は 2 であること");
  }

  /** expandedBy でmaxSize が正しく増加すること (SlotExpandEffect 連動)。 */
  @Test
  void expandedByIncreasesMaxSize() {
    SkillSlot slot = new SkillSlot(List.of(skill("a", 1)), 4);
    SkillSlot expanded = slot.expandedBy(2);
    assertEquals(6, expanded.maxSize(), "expandedBy(2) で maxSize が 4 -> 6 になること");
    assertEquals(1, expanded.size(), "スキルリストは変わらないこと");
  }

  /** expandedBy(0) / 負値はバリデーション例外を投げること。 */
  @Test
  void expandedByZeroOrNegativeIsRejected() {
    SkillSlot slot = SkillSlot.empty(4);
    assertThrows(IllegalArgumentException.class, () -> slot.expandedBy(0));
    assertThrows(IllegalArgumentException.class, () -> slot.expandedBy(-1));
  }
}
