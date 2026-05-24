package core.infrastructure.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.entity.EnemyKind;
import core.domain.meta.Bestiary;
import core.domain.tree.SoulTree;
import core.infrastructure.bootstrap.InitialStateFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** {@link SaveDataConverter} の Wave 6 W6-β 追加分 (toBestiary / v1 graceful migration) を検証する。 */
class SaveDataConverterTest {

  /** Wave 5 W5-γ: toSoulTree 等で SoulTree.allNodes() を呼ぶため Supplier 注入が必要。 */
  @BeforeAll
  static void initSoulTreeProvider() {
    SoulTree.setNodeProvider(InitialStateFactory::soulTreeNodes);
  }

  private static SaveData buildSampleData(List<String> defeatedEnemyKinds, boolean tutorialSeen) {
    return new SaveData(
        SaveData.CURRENT_SCHEMA_VERSION,
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
        Map.of("HAND", "tattered_dagger"),
        defeatedEnemyKinds,
        tutorialSeen,
        // Wave 15 W15-α: currentRunGold + currentRunSoul
        0,
        0);
  }

  @Test
  void toBestiary_restoresKindsFromNames() {
    SaveData data = buildSampleData(List.of("SLIME", "SWIFT_SLIME"), false);
    Bestiary restored = SaveDataConverter.toBestiary(data);

    assertEquals(2, restored.defeatedKinds().size());
    assertTrue(restored.defeatedKinds().contains(EnemyKind.SLIME));
    assertTrue(restored.defeatedKinds().contains(EnemyKind.SWIFT_SLIME));
  }

  @Test
  void toBestiary_returnsEmptyForV1SaveData() {
    // v1 セーブ (defeatedEnemyKinds 欠落) は compact constructor で空リスト化される
    SaveData data = buildSampleData(List.of(), false);
    Bestiary restored = SaveDataConverter.toBestiary(data);
    assertTrue(restored.defeatedKinds().isEmpty(), "v1 セーブからは空 Bestiary が返る");
  }

  @Test
  void toBestiary_gracefullySkipsUnknownEnemyKind() {
    // 未知名 (将来の Enum 改変で削除された敵種 / typo) は WARN + skip
    SaveData data = buildSampleData(List.of("SLIME", "WAS_DELETED_KIND", "SWIFT_SLIME"), false);
    Bestiary restored = SaveDataConverter.toBestiary(data);

    assertEquals(2, restored.defeatedKinds().size(), "未知名は graceful skip、既知名のみ復元");
    assertTrue(restored.defeatedKinds().contains(EnemyKind.SLIME));
    assertTrue(restored.defeatedKinds().contains(EnemyKind.SWIFT_SLIME));
  }

  @Test
  void toBestiary_handlesAllUnknownNamesAsEmpty() {
    SaveData data = buildSampleData(List.of("FAKE_1", "FAKE_2"), false);
    Bestiary restored = SaveDataConverter.toBestiary(data);
    assertTrue(restored.defeatedKinds().isEmpty(), "全 unknown でも空 Bestiary、例外を投げない");
  }

  @Test
  void tutorialSeen_isReadbackFromSaveData() {
    // tutorialSeen は単純な boolean なので変換は data.tutorialSeen() だが、true / false 双方で readback できることを保証
    SaveData seen = buildSampleData(List.of(), true);
    assertTrue(seen.tutorialSeen());

    SaveData notSeen = buildSampleData(List.of(), false);
    assertFalse(notSeen.tutorialSeen());
  }
}
