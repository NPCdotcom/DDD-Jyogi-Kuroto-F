package core.infrastructure.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Set;
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
  void resumeNeverDropsProtectionEvenWhenTreeCapacityDisagrees() {
    // Profile は保存時点で protectedIds.size() <= retentionCapacity を強制済み。
    // SoulTree 側の導出値 (ここでは 0) と食い違っても、保護指定を切り捨ててはならない。
    // 切り捨ては「クラッシュしない代わりにユーザーの保護指定を黙って失う」データ損失になる。
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

    assertTrue(merged.protectedIds().contains(EquipmentId.of("steel_axe")), "保護指定は再開後も維持される");
    assertEquals(1, merged.capacity().value(), "保護枠は Profile の保存値を正とする");
    assertFalse(
        merged.protectedIds().contains(EquipmentOwnership.STARTER_EQUIPMENT_ID),
        "初期装備は保護枠を消費しない (§15-9)");
  }

  @Test
  void resumeDoesNotPromoteUnconfirmedRunEquipmentToPermanentOwnership() {
    // ラン中に金貨で買った未確定品はクリア時にだけ所有化される (§15-9)。
    // 再開の loadout は profile.loadout() 由来なので未確定品は載らないが、
    // 「所有が消える」を直した反動で「未確定品が恒久化する」対称の欠陥を作らないよう固定する。
    EquipmentOwnership merged =
        ProfileDataMapper.mergeOwnershipForResume(
            profileOwningThree(),
            Map.of(EquipmentSlot.HAND, equipment("iron_sword", EquipmentSlot.HAND)),
            SoulTree.empty());

    assertFalse(
        merged.ownedIds().contains(EquipmentId.of("unconfirmed_blade")),
        "Profile にも loadout にも無い未確定品が恒久所有へ紛れ込んではならない");
    assertEquals(
        Set.of(
            EquipmentOwnership.STARTER_EQUIPMENT_ID,
            EquipmentId.of("iron_sword"),
            EquipmentId.of("steel_axe")),
        merged.ownedIds(),
        "恒久所有は Profile + 装着中のみ。ラン中 Inventory と混ざらない");
  }
}
