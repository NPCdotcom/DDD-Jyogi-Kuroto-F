package core.infrastructure.save;

import com.fasterxml.jackson.annotation.JsonProperty;
import core.domain.equipment.EquipmentOwnership;
import core.domain.equipment.RetentionCapacity;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ラン外の恒久状態 (SAVE-01 / §15-11)。{@code profile.json} へ保存する。
 *
 * <p>レビュー P0-1 の是正。従来は恒久進捗と進行中ランを単一 {@code save.json} に混在させていたため、
 * ラン終了時に進捗が保存されず、逆に終了済みランを「つづき」から再開できた。 恒久側を本型、ラン側を {@link RunCheckpoint} に分ける。
 *
 * <p>{@code activeRunId} は現在進行中のランの ID。{@link RunCheckpoint#runId()} と一致する場合だけ 再開または精算を許す。{@code
 * lastSettledRunId} は最後に精算したランの ID で、同じランを二重に 精算して Soul と周回数を重複加算するのを防ぐ。いずれも該当なしの場合は {@code null}。
 *
 * <p>保存層の型なので Jackson に依存してよい。ドメイン型 ({@link core.domain.equipment.EquipmentOwnership} 等) への変換は
 * {@link SaveDataConverter} が担当する。
 */
public record ProfileData(
    @JsonProperty("schemaVersion") int schemaVersion,
    @JsonProperty("soulTotal") int soulTotal,
    @JsonProperty("runCount") int runCount,
    @JsonProperty("unlockedNodeIds") List<String> unlockedNodeIds,
    // §15-9: 所有装備。初期短剣は常に含まれる (EquipmentOwnership が補う)
    @JsonProperty("ownedEquipmentIds") List<String> ownedEquipmentIds,
    // EquipmentSlot.name() → EquipmentId.value()
    @JsonProperty("loadout") Map<String, String> loadout,
    // §15-9: 死亡時に保護する装備。所有装備の部分集合で、数は retentionCapacity 以下
    @JsonProperty("protectedEquipmentIds") List<String> protectedEquipmentIds,
    // §15-7: 「魂の刻印 I / II」で解放される保護枠 (0〜2)
    @JsonProperty("retentionCapacity") int retentionCapacity,
    @JsonProperty("defeatedEnemyKinds") List<String> defeatedEnemyKinds,
    @JsonProperty("obtainedCardIds") List<String> obtainedCardIds,
    @JsonProperty("tutorialSeen") boolean tutorialSeen,
    @JsonProperty("activeRunId") String activeRunId,
    @JsonProperty("lastSettledRunId") String lastSettledRunId) {

  /** 現在のスキーマバージョン。Profile 分離の初版なので 1 から始める。 */
  public static final int CURRENT_SCHEMA_VERSION = 1;

  public ProfileData {
    if (schemaVersion < 1) {
      throw new IllegalArgumentException("schemaVersion must be >= 1: " + schemaVersion);
    }
    if (soulTotal < 0) {
      soulTotal = 0;
    }
    if (runCount < 0) {
      runCount = 0;
    }
    unlockedNodeIds = copyOrEmpty(unlockedNodeIds);
    ownedEquipmentIds = copyOrEmpty(ownedEquipmentIds);
    protectedEquipmentIds = copyOrEmpty(protectedEquipmentIds);
    defeatedEnemyKinds = copyOrEmpty(defeatedEnemyKinds);
    obtainedCardIds = copyOrEmpty(obtainedCardIds);
    loadout = loadout != null ? Map.copyOf(loadout) : Map.of();

    // 保護枠の不変条件はドメイン側と同じ規則を使う (RetentionCapacity が 0〜2 を強制)。
    RetentionCapacity capacity = RetentionCapacity.of(retentionCapacity);
    if (!capacity.canHold(protectedEquipmentIds.size())) {
      throw new IllegalArgumentException(
          "protectedEquipmentIds size "
              + protectedEquipmentIds.size()
              + " exceeds retentionCapacity "
              + retentionCapacity);
    }
    // EquipmentOwnership が課す残りの不変条件もここで弾く。数だけ検査して読み込むと、
    // ドメイン型へ変換する瞬間に例外が飛び、「つづき」で落ちる。読込時点で拒否する方が graceful。
    if (protectedEquipmentIds.contains(EquipmentOwnership.STARTER_EQUIPMENT_VALUE)) {
      throw new IllegalArgumentException(
          "starter equipment must not be protected: " + EquipmentOwnership.STARTER_EQUIPMENT_VALUE);
    }
    if (!ownedEquipmentIds.containsAll(protectedEquipmentIds)) {
      throw new IllegalArgumentException(
          "protectedEquipmentIds must be a subset of ownedEquipmentIds");
    }
    if (!ownedEquipmentIds.containsAll(loadout.values())) {
      throw new IllegalArgumentException("loadout must reference owned equipment only");
    }
  }

  private static List<String> copyOrEmpty(List<String> values) {
    return values != null ? List.copyOf(values) : List.of();
  }

  /** 新規プレイヤーの初期 Profile。所有装備は初期短剣のみ、保護枠 0、進行中ランなし。 */
  public static ProfileData initial() {
    return new ProfileData(
        CURRENT_SCHEMA_VERSION,
        0,
        0,
        List.of(),
        List.of(core.domain.equipment.EquipmentOwnership.STARTER_EQUIPMENT_VALUE),
        Map.of(),
        List.of(),
        0,
        List.of(),
        List.of(),
        false,
        null,
        null);
  }

  /** 指定したランを進行中として記録した新しい Profile を返す。 */
  public ProfileData withActiveRunId(String runId) {
    return new ProfileData(
        schemaVersion,
        soulTotal,
        runCount,
        unlockedNodeIds,
        ownedEquipmentIds,
        loadout,
        protectedEquipmentIds,
        retentionCapacity,
        defeatedEnemyKinds,
        obtainedCardIds,
        tutorialSeen,
        runId,
        lastSettledRunId);
  }

  /**
   * 指定したランを精算済みとして記録した新しい Profile を返す。進行中ランは解除する。
   *
   * <p>{@code lastSettledRunId} を更新することで、同じ ID の再精算を呼出側が検出できる (SETTLE-02 が二重加算を防ぐために使う)。
   */
  public ProfileData withSettledRunId(String runId) {
    return new ProfileData(
        schemaVersion,
        soulTotal,
        runCount,
        unlockedNodeIds,
        ownedEquipmentIds,
        loadout,
        protectedEquipmentIds,
        retentionCapacity,
        defeatedEnemyKinds,
        obtainedCardIds,
        tutorialSeen,
        null,
        runId);
  }

  /** 指定したランが既に精算済みか。二重精算による Soul / 周回数の重複加算を防ぐ判定。 */
  public boolean isAlreadySettled(String runId) {
    return Objects.equals(lastSettledRunId, runId);
  }
}
