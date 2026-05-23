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
        Map.of("HAND", "tattered_dagger"));
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
  }

  @Test
  void roundTrip_emptyCollections() throws Exception {
    SaveData original =
        new SaveData(
            1, 1, 30, 30, 3, 1, 2, 1, 1, List.of(), 0, 0, List.of("root"), List.of(), Map.of());
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
                Map.of()));
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
                Map.of()));
  }
}
