package core.infrastructure.save;

import com.fasterxml.jackson.annotation.JsonProperty;
import core.domain.equipment.RetentionCapacity;
import java.util.List;

/**
 * 進行中ランのチェックポイント (SAVE-01 / §15-11)。{@code run-checkpoint.json} へ保存する。
 *
 * <p>層と層の間だけで書き、死亡またはクリアの精算後に削除する。{@link ProfileData#activeRunId()} と {@link #runId()}
 * が一致する場合だけ再開・精算を許すことで、終了済みランの再開 (レビュー P0-1) を構造的に防ぐ。
 *
 * <p>装備は 3 つに分かれる。{@code carriedInEquipmentIds} は所有済みで持ち込んだもの、 {@code unconfirmedEquipmentIds}
 * はラン中に金貨で買った未確定品 (クリア時のみ所有化、死亡時は Soul へ 変換せず失う)、{@code protectedEquipmentIds} は持込品のうち死亡時に維持するもの。
 *
 * <p>セーブは戦闘外にのみ行うため DungeonState / DungeonMap は保持しない。ロード後は {@code InitialStateFactory} で次の層を通常生成する。
 */
public record RunCheckpoint(
    @JsonProperty("schemaVersion") int schemaVersion,
    @JsonProperty("runId") String runId,
    @JsonProperty("nextLayerNumber") int nextLayerNumber,
    // プレイヤー能力値 (7 ステ)
    @JsonProperty("currentHp") int currentHp,
    @JsonProperty("maxHp") int maxHp,
    @JsonProperty("speed") int speed,
    @JsonProperty("physicalAttack") int physicalAttack,
    @JsonProperty("magicalAttack") int magicalAttack,
    @JsonProperty("physicalDefense") int physicalDefense,
    @JsonProperty("magicalDefense") int magicalDefense,
    // デッキ (山札 + 手札 + 捨て札の全カード ID)
    @JsonProperty("deck") List<String> deck,
    @JsonProperty("currentRunGold") int currentRunGold,
    @JsonProperty("currentRunSoul") int currentRunSoul,
    // §15-9 装備の 3 分類
    @JsonProperty("carriedInEquipmentIds") List<String> carriedInEquipmentIds,
    @JsonProperty("unconfirmedEquipmentIds") List<String> unconfirmedEquipmentIds,
    @JsonProperty("equippedId") String equippedId,
    @JsonProperty("protectedEquipmentIds") List<String> protectedEquipmentIds,
    @JsonProperty("retentionCapacity") int retentionCapacity,
    @JsonProperty("initialRunSoul") int initialRunSoul) {

  /** 現在のスキーマバージョン。Checkpoint 分離の初版なので 1 から始める。 */
  public static final int CURRENT_SCHEMA_VERSION = 1;

  public RunCheckpoint(
      int schemaVersion,
      String runId,
      int nextLayerNumber,
      int currentHp,
      int maxHp,
      int speed,
      int physicalAttack,
      int magicalAttack,
      int physicalDefense,
      int magicalDefense,
      List<String> deck,
      int currentRunGold,
      int currentRunSoul,
      List<String> carriedInEquipmentIds,
      List<String> unconfirmedEquipmentIds,
      String equippedId,
      List<String> protectedEquipmentIds,
      int retentionCapacity) {
    this(
        schemaVersion,
        runId,
        nextLayerNumber,
        currentHp,
        maxHp,
        speed,
        physicalAttack,
        magicalAttack,
        physicalDefense,
        magicalDefense,
        deck,
        currentRunGold,
        currentRunSoul,
        carriedInEquipmentIds,
        unconfirmedEquipmentIds,
        equippedId,
        protectedEquipmentIds,
        retentionCapacity,
        0);
  }

  public RunCheckpoint {
    if (schemaVersion < 1) {
      throw new IllegalArgumentException("schemaVersion must be >= 1: " + schemaVersion);
    }
    if (runId == null || runId.isBlank()) {
      throw new IllegalArgumentException("runId must not be blank");
    }
    if (nextLayerNumber < 1) {
      throw new IllegalArgumentException("nextLayerNumber must be >= 1: " + nextLayerNumber);
    }
    deck = copyOrEmpty(deck);
    carriedInEquipmentIds = copyOrEmpty(carriedInEquipmentIds);
    unconfirmedEquipmentIds = copyOrEmpty(unconfirmedEquipmentIds);
    protectedEquipmentIds = copyOrEmpty(protectedEquipmentIds);

    if (currentRunGold < 0) {
      currentRunGold = 0;
    }
    if (currentRunSoul < 0) {
      currentRunSoul = 0;
    }

    RetentionCapacity capacity = RetentionCapacity.of(retentionCapacity);
    if (!capacity.canHold(protectedEquipmentIds.size())) {
      throw new IllegalArgumentException(
          "protectedEquipmentIds size "
              + protectedEquipmentIds.size()
              + " exceeds retentionCapacity "
              + retentionCapacity);
    }
    // 保護は持込品に限る。未確定品は死亡時に保護の有無を問わず失う (§15-9)。
    if (!carriedInEquipmentIds.containsAll(protectedEquipmentIds)) {
      throw new IllegalArgumentException(
          "protectedEquipmentIds must be a subset of carriedInEquipmentIds");
    }
    for (String id : unconfirmedEquipmentIds) {
      if (carriedInEquipmentIds.contains(id)) {
        throw new IllegalArgumentException("carriedIn and unconfirmed must be disjoint: " + id);
      }
    }
    // RunInventory が課す不変条件。数だけ検査して読み込むと、ドメイン型へ変換する瞬間に
    // 例外が飛んで「つづき」で落ちるため、読込時点で拒否する。
    if (equippedId != null
        && !carriedInEquipmentIds.contains(equippedId)
        && !unconfirmedEquipmentIds.contains(equippedId)) {
      throw new IllegalArgumentException(
          "equippedId must be carried in or unconfirmed: " + equippedId);
    }
  }

  private static List<String> copyOrEmpty(List<String> values) {
    return values != null ? List.copyOf(values) : List.of();
  }
}
