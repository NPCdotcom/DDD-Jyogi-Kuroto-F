package core.infrastructure.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentSlot;
import core.domain.meta.PlayerProgress;
import core.domain.tree.SoulTree;
import core.infrastructure.bootstrap.InitialStateFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * {@link ProfileDataMapper} の往復を検証する (SAVE-03B)。
 *
 * <p>敵対的検証で見つかった P0 の回帰防止。起動時に {@code progress} を profile.json から復元しないまま ラン開始で書込系を通すと、空の値で
 * profile.json が全上書きされ、 ソウル・ツリー解放・図鑑・周回数が消える。書込系が {@code previous} から引き継ぐのはラン ID 群だけで、 他はすべて {@code
 * progress} から再構築するためである。
 */
class ProfileDataMapperTest {

  @BeforeAll
  static void initNodeProvider() {
    // SoulTree は起動時に注入される nodeProvider を必要とする (Wave 5 W5-γ)。
    SoulTree.setNodeProvider(InitialStateFactory::soulTreeNodes);
  }

  private static Map<EquipmentSlot, Equipment> defaultLoadout() {
    Equipment dagger = InitialStateFactory.tatteredDagger();
    return Map.of(dagger.slot(), dagger);
  }

  private static ProfileData richProfile() {
    return new ProfileData(
        ProfileData.CURRENT_SCHEMA_VERSION,
        200,
        3,
        List.of("root", "hp_up_1"),
        List.of("tattered_dagger"),
        Map.of(),
        List.of(),
        0,
        List.of("SLIME"),
        List.of("zangeki"),
        true,
        null,
        null);
  }

  // ---------------- 空 progress による上書きの検出 ----------------

  @Test
  void emptyProgressWouldWipePersistedFieldsWhenWrittenDirectly() {
    // これが P0 の再現。復元を挟まず初期 progress を書くと恒久進捗が空になる。
    ProfileData written =
        ProfileDataMapper.forSoulUpdate(PlayerProgress.initial(defaultLoadout()), richProfile(), 0);

    assertEquals(0, written.runCount(), "空 progress からは周回数が復元されない");
    assertFalse(written.unlockedNodeIds().contains("hp_up_1"), "解放したノードが失われる");
    assertFalse(written.tutorialSeen(), "チュートリアル既読も失われる");
  }

  @Test
  void restoringProgressFirstPreservesPersistedFields() {
    // 起動時に復元してから書けば、恒久進捗は保たれる。
    PlayerProgress restored = ProfileDataMapper.toPlayerProgress(richProfile(), defaultLoadout());
    ProfileData written =
        ProfileDataMapper.forSoulUpdate(restored, richProfile(), richProfile().soulTotal());

    assertEquals(200, written.soulTotal());
    assertEquals(3, written.runCount());
    assertTrue(written.unlockedNodeIds().contains("hp_up_1"), "解放したノードが残る");
    assertTrue(written.tutorialSeen());
    assertTrue(written.defeatedEnemyKinds().contains("SLIME"));
    assertTrue(written.obtainedCardIds().contains("zangeki"));
  }

  // ---------------- 復元そのもの ----------------

  @Test
  void toPlayerProgressRestoresScalarFields() {
    PlayerProgress restored = ProfileDataMapper.toPlayerProgress(richProfile(), defaultLoadout());
    assertEquals(200, restored.playerSoul().amount());
    assertEquals(3, restored.runCount());
    assertTrue(restored.tutorialSeen());
  }

  @Test
  void emptyLoadoutFallsBackToDefault() {
    // 編成が空のまま開始すると空デッキで詰む。新規プレイヤーは初期短剣で始める。
    PlayerProgress restored =
        ProfileDataMapper.toPlayerProgress(ProfileData.initial(), defaultLoadout());
    assertFalse(restored.loadout().isEmpty(), "空編成で開始させない");
  }

  @Test
  void freshProfileRestoresToEmptyProgress() {
    PlayerProgress restored =
        ProfileDataMapper.toPlayerProgress(ProfileData.initial(), defaultLoadout());
    assertEquals(0, restored.playerSoul().amount());
    assertEquals(0, restored.runCount());
    assertFalse(restored.tutorialSeen());
  }

  // ---------------- ラン ID の引き継ぎ ----------------

  @Test
  void runIdsAreCarriedFromPreviousNotFromProgress() {
    ProfileData previous = richProfile().withActiveRunId("run-active");
    ProfileData written =
        ProfileDataMapper.forSoulUpdate(
            ProfileDataMapper.toPlayerProgress(previous, defaultLoadout()), previous, 200);
    assertEquals("run-active", written.activeRunId(), "ラン ID は previous から引き継ぐ");
  }
}
