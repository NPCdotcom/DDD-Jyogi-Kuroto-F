package core.infrastructure.save;

import core.domain.equipment.EquipmentOwnership;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 旧 {@code save.json} ({@link SaveData} v1〜v3) を {@link ProfileData} と {@link RunCheckpoint} へ写す純関数
 * (SAVE-02A)。
 *
 * <p>I/O を持たず戻り値だけで結果を返すため、版ごとの写像規則を単体試験で固定できる。 実際のファイル操作と障害復旧は SAVE-02B の移行 journal が担当する。
 *
 * <p>版ごとの通貨の扱い:
 *
 * <ul>
 *   <li>v1 / v2: ラン中 Soul と Gold のフィールドを持たないため復元できない。0 で開始し、 {@link Result#warnings()} で復元不能を通知する
 *   <li>v3: {@code currentRunSoul} と {@code currentRunGold} を Checkpoint へ移す
 * </ul>
 *
 * <p>いずれの版でも {@code soulTotal} は Profile へだけ移し、ラン中 Soul を足して二重計上しない。
 */
public final class LegacySaveMapper {

  /** 廃止した装備枠拡張ノードの ID 接頭辞 (§15-7、SOUL-02 で「魂の刻印」へ置換)。 */
  static final String SLOT_EXPAND_PREFIX = "slot_expand_";

  /** 旧 SlotExpand ノード 1 件あたりの返金額。`tree.json` の当時の価格。 */
  static final int SLOT_EXPAND_REFUND = 40;

  /** 移行で作るランに割り当てる ID。Profile の activeRunId と一致させる。 */
  static final String MIGRATED_RUN_ID = "legacy-run-1";

  private LegacySaveMapper() {}

  /**
   * 写像の結果。
   *
   * @param profile 恒久状態
   * @param checkpoint 進行中ラン。旧セーブは層境界で書かれるため通常は存在する
   * @param warnings 呼出側がユーザーへ提示すべき通知 (復元不能な値、カタログ外 ID など)
   */
  public record Result(
      ProfileData profile, Optional<RunCheckpoint> checkpoint, List<String> warnings) {

    public Result {
      Objects.requireNonNull(profile, "profile");
      Objects.requireNonNull(checkpoint, "checkpoint");
      warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
    }
  }

  /**
   * 旧セーブを新しい 2 ファイル構成へ写す。
   *
   * @param legacy 旧 {@code save.json} の内容
   * @return Profile、RunCheckpoint、通知
   */
  public static Result map(SaveData legacy) {
    Objects.requireNonNull(legacy, "legacy");
    List<String> warnings = new ArrayList<>();

    // 1. 廃止ノードを取り除き、取り除いた分だけ返金する (同じ ID が重複していても 1 回だけ)。
    Set<String> remainingNodes = new LinkedHashSet<>();
    Set<String> refundedNodes = new LinkedHashSet<>();
    for (String nodeId : legacy.unlockedNodeIds()) {
      if (nodeId != null && nodeId.startsWith(SLOT_EXPAND_PREFIX)) {
        refundedNodes.add(nodeId);
      } else if (nodeId != null) {
        remainingNodes.add(nodeId);
      }
    }
    int refund = refundedNodes.size() * SLOT_EXPAND_REFUND;
    if (refund > 0) {
      warnings.add("廃止された装備枠拡張ノード " + refundedNodes.size() + " 件を削除し、" + refund + " ソウルを返金しました。");
    }

    // 2. 旧 loadout の装備を所有・持込・現在装備へ写す。初期短剣は常に所有させる。
    Set<String> owned = new LinkedHashSet<>();
    owned.add(EquipmentOwnership.STARTER_EQUIPMENT_VALUE);
    Set<String> carriedIn = new LinkedHashSet<>();
    carriedIn.add(EquipmentOwnership.STARTER_EQUIPMENT_VALUE);
    String equipped = null;
    for (String equipmentId : legacy.loadout().values()) {
      if (equipmentId == null || equipmentId.isBlank()) {
        continue;
      }
      owned.add(equipmentId);
      carriedIn.add(equipmentId);
      equipped = equipmentId; // 1 部位スタート (ADR-30) なので最後の 1 件が現在装備。
    }

    // 3. ラン中通貨。v3 だけが値を持つ。
    boolean hasRunCurrency = legacy.schemaVersion() >= 3;
    int runSoul = hasRunCurrency ? legacy.currentRunSoul() : 0;
    int runGold = hasRunCurrency ? legacy.currentRunGold() : 0;
    if (!hasRunCurrency) {
      warnings.add("v" + legacy.schemaVersion() + " のセーブはラン中のソウルと金貨を記録していないため復元できません。0 から再開します。");
    }

    ProfileData profile =
        new ProfileData(
            ProfileData.CURRENT_SCHEMA_VERSION,
            legacy.soulTotal() + refund,
            legacy.runCount(),
            List.copyOf(remainingNodes),
            List.copyOf(owned),
            legacy.loadout(),
            List.of(), // 保護指定は移行時点では空 (保護枠 0 なので指定できない)
            0, // 保護枠は「魂の刻印」未解放なので 0
            legacy.defeatedEnemyKinds(),
            legacy.obtainedCardIds(),
            legacy.tutorialSeen(),
            MIGRATED_RUN_ID,
            null); // 未精算。移行したランは通常どおり精算できる

    RunCheckpoint checkpoint =
        new RunCheckpoint(
            RunCheckpoint.CURRENT_SCHEMA_VERSION,
            MIGRATED_RUN_ID,
            legacy.nextLayerNumber(),
            legacy.currentHp(),
            legacy.maxHp(),
            legacy.speed(),
            legacy.physicalAttack(),
            legacy.magicalAttack(),
            legacy.physicalDefense(),
            legacy.magicalDefense(),
            legacy.deck(),
            runGold,
            runSoul,
            List.copyOf(carriedIn),
            List.of(), // 未確定品は移行時点では空
            equipped,
            List.of(), // 保護品も空
            0);

    return new Result(profile, Optional.of(checkpoint), warnings);
  }
}
