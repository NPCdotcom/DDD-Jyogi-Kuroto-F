package core.infrastructure.save;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** {@link SaveData} の Jackson round-trip シリアライズを検証する (§15-11)。 */
class SaveDataRoundTripTest {

  private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  private SaveData sample() {
    return new SaveData(
        SaveData.CURRENT_SCHEMA_VERSION,
        2,
        25,
        30,
        3,
        2,
        1,
        1,
        1,
        List.of("zangeki", "dash", "strong_strike"),
        15,
        1,
        List.of("root", "hp_up_1"),
        List.of("zangeki", "dash"),
        Map.of("HAND", "tattered_dagger"),
        // Wave 6 W6-β: defeatedEnemyKinds + tutorialSeen
        List.of("SLIME", "SWIFT_SLIME"),
        true);
  }

  @Test
  void roundTrip_preservesAllFields() throws Exception {
    SaveData original = sample();
    String json = mapper.writeValueAsString(original);
    SaveData restored = mapper.readValue(json, SaveData.class);

    assertEquals(original.schemaVersion(), restored.schemaVersion());
    assertEquals(original.nextLayerNumber(), restored.nextLayerNumber());
    assertEquals(original.currentHp(), restored.currentHp());
    assertEquals(original.maxHp(), restored.maxHp());
    assertEquals(original.speed(), restored.speed());
    assertEquals(original.physicalAttack(), restored.physicalAttack());
    assertEquals(original.magicalAttack(), restored.magicalAttack());
    assertEquals(original.physicalDefense(), restored.physicalDefense());
    assertEquals(original.magicalDefense(), restored.magicalDefense());
    assertEquals(original.deck(), restored.deck());
    assertEquals(original.soulTotal(), restored.soulTotal());
    assertEquals(original.runCount(), restored.runCount());
    assertEquals(original.unlockedNodeIds(), restored.unlockedNodeIds());
    assertEquals(original.obtainedCardIds(), restored.obtainedCardIds());
    assertEquals(original.loadout(), restored.loadout());
    assertEquals(original.defeatedEnemyKinds(), restored.defeatedEnemyKinds());
    assertEquals(original.tutorialSeen(), restored.tutorialSeen());
  }

  @Test
  void roundTrip_emptyCollections() throws Exception {
    SaveData original =
        new SaveData(
            2,
            1,
            30,
            30,
            3,
            1,
            2,
            1,
            1,
            List.of(),
            0,
            0,
            List.of("root"),
            List.of(),
            Map.of(),
            List.of(),
            false);
    String json = mapper.writeValueAsString(original);
    SaveData restored = mapper.readValue(json, SaveData.class);

    assertTrue(restored.deck().isEmpty());
    assertTrue(restored.obtainedCardIds().isEmpty());
    assertTrue(restored.loadout().isEmpty());
    assertEquals(List.of("root"), restored.unlockedNodeIds());
  }

  @Test
  void saveData_immutableCollections() {
    SaveData data = sample();
    // List.copyOf で不変にされているため変更しようとすると UnsupportedOperationException
    assertThrows(UnsupportedOperationException.class, () -> data.deck().add("new_card"));
    assertThrows(UnsupportedOperationException.class, () -> data.unlockedNodeIds().add("new_node"));
    assertThrows(UnsupportedOperationException.class, () -> data.obtainedCardIds().add("new_card"));
  }

  @Test
  void saveData_validationRejectsInvalidSchemaVersion() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SaveData(
                0,
                1,
                30,
                30,
                3,
                1,
                2,
                1,
                1,
                List.of(),
                0,
                0,
                List.of("root"),
                List.of(),
                Map.of(),
                List.of(),
                false));
  }

  @Test
  void saveData_validationRejectsInvalidNextLayerNumber() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SaveData(
                1,
                0,
                30,
                30,
                3,
                1,
                2,
                1,
                1,
                List.of(),
                0,
                0,
                List.of("root"),
                List.of(),
                Map.of(),
                List.of(),
                false));
  }

  // Wave 6 W6-β: v1 graceful migration (旧 schema=1 の JSON を v2 構造で読込)
  @Test
  void v1SaveData_isGracefullyMigrated_defeatedEnemyKindsBecomeEmptyList() throws Exception {
    // v1 セーブの JSON 形式 (defeatedEnemyKinds / tutorialSeen フィールドが存在しない)
    String v1Json =
        """
        {
          "schemaVersion": 1,
          "nextLayerNumber": 3,
          "currentHp": 20,
          "maxHp": 30,
          "speed": 3,
          "physicalAttack": 2,
          "magicalAttack": 1,
          "physicalDefense": 1,
          "magicalDefense": 1,
          "deck": ["zangeki"],
          "soulTotal": 10,
          "runCount": 2,
          "unlockedNodeIds": ["root"],
          "obtainedCardIds": ["zangeki"],
          "loadout": {"HAND": "tattered_dagger"}
        }
        """;
    SaveData restored = mapper.readValue(v1Json, SaveData.class);

    assertEquals(1, restored.schemaVersion(), "v1 として読込はできる");
    assertNotNull(restored.defeatedEnemyKinds(), "compact constructor が null → 空リストに正規化");
    assertTrue(restored.defeatedEnemyKinds().isEmpty(), "v1 では bestiary は空");
    assertFalse(restored.tutorialSeen(), "v1 では tutorialSeen は false (boolean default)");
  }
}
