package core.infrastructure.save;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentId;
import core.domain.equipment.EquipmentOwnership;
import core.domain.equipment.EquipmentSlot;
import core.domain.equipment.StatsBonus;
import core.domain.tree.SoulTree;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 「つづき」で所有装備が失われないことを固定する。
 *
 * <p>再開経路は Checkpoint 側の loadout しか持たないため、所有集合を loadout から再生成すると
 * 「所有しているが装着していない装備」が消え、次の保存で永久に失われる。所有の正は常に Profile 側であり、 Checkpoint の loadout は所有へ合流させるだけでよい。
 */
class ResumeOwnershipTest {

  private static Equipment equipment(String id, EquipmentSlot slot) {
    return new Equipment(
        EquipmentId.of(id),
        id,
        slot,
        StatsBonus.zero(),
        List.of(),
        Optional.empty(),
        Optional.empty());
  }

  /** 所有 3 件 / 装着 1 件の Profile。装着していない 2 件が再開で消えてはならない。 */
  private static ProfileData profileOwningThree() {
    return new ProfileData(
        ProfileData.CURRENT_SCHEMA_VERSION,
        500,
        3,
        List.of("root"),
        List.of("tattered_dagger", "iron_sword", "steel_axe"),
        Map.of(),
        List.of(),
        0,
        List.of(),
        List.of(),
        true,
        "run-1",
        null);
  }

  @Test
  void resumeKeepsEquipmentThatIsOwnedButNotEquipped() {
    // Checkpoint 側は「今装備しているもの」しか知らない。
    Map<EquipmentSlot, Equipment> checkpointLoadout =
        Map.of(EquipmentSlot.HAND, equipment("iron_sword", EquipmentSlot.HAND));

    EquipmentOwnership merged =
        ProfileDataMapper.mergeOwnershipForResume(
            profileOwningThree(), checkpointLoadout, SoulTree.empty());

    assertTrue(merged.ownedIds().contains(EquipmentId.of("iron_sword")), "装着中の装備は所有に残る");
    assertTrue(
        merged.ownedIds().contains(EquipmentId.of("steel_axe")), "所有しているが装着していない装備が再開で消えてはならない");
    assertTrue(merged.ownedIds().contains(EquipmentOwnership.STARTER_EQUIPMENT_ID), "初期装備は常に所有される");
  }

  @Test
  void resumeAdoptsEquipmentPresentOnlyInCheckpointLoadout() {
    // ラン中に購入し Profile へ未反映の装備を装着していても、不変条件 (loadout ⊆ owned) を壊さない。
    Map<EquipmentSlot, Equipment> checkpointLoadout =
        Map.of(EquipmentSlot.HAND, equipment("run_bought_blade", EquipmentSlot.HAND));

    EquipmentOwnership merged =
        ProfileDataMapper.mergeOwnershipForResume(
            profileOwningThree(), checkpointLoadout, SoulTree.empty());

    assertTrue(
        merged.ownedIds().contains(EquipmentId.of("run_bought_blade")),
        "Checkpoint の装着品は所有へ合流させる (compact constructor の不変条件を満たすため)");
    assertTrue(merged.ownedIds().contains(EquipmentId.of("steel_axe")), "Profile 側の所有も維持する");
  }

  @Test
  void resumeDropsProtectionBeyondCapacityInsteadOfThrowing() {
    // ツリーをリセットして保護枠が減った Profile では、保護指定が現在の枠を超えうる。
    // 超過分をそのまま渡すと EquipmentOwnership が例外を投げ、「つづき」自体が落ちる。
    // 落とすのではなく、超過分を捨てて再開できることを固定する。
    ProfileData withProtection =
        new ProfileData(
            ProfileData.CURRENT_SCHEMA_VERSION,
            0,
            0,
            List.of("root"),
            List.of("tattered_dagger", "iron_sword", "steel_axe"),
            Map.of(),
            List.of("steel_axe"),
            1,
            List.of(),
            List.of(),
            false,
            null,
            null);

    EquipmentOwnership merged =
        ProfileDataMapper.mergeOwnershipForResume(
            withProtection,
            Map.of(EquipmentSlot.HAND, equipment("iron_sword", EquipmentSlot.HAND)),
            SoulTree.empty());

    // SoulTree.empty() の保護枠は 0。保護指定 1 件は枠を超えるので落ちる。
    assertTrue(merged.protectedIds().isEmpty(), "保護枠 0 なら保護指定は残らない");
    assertTrue(merged.ownedIds().contains(EquipmentId.of("steel_axe")), "保護が外れても所有そのものは維持される");
    assertFalse(
        merged.protectedIds().contains(EquipmentOwnership.STARTER_EQUIPMENT_ID),
        "初期装備は保護枠を消費しない (§15-9)");
  }
}
