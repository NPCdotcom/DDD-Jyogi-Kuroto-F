package core.infrastructure.save;

import core.application.RunId;
import core.domain.card.Card;
import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.domain.equipment.EquipmentId;
import core.domain.equipment.RetentionCapacity;
import core.domain.equipment.RunInventory;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * {@link RunCheckpoint} とラン中状態を相互変換する純関数 (SAVE-03A)。
 *
 * <p>{@link SaveDataConverter} が旧 {@code save.json} 向けだったのに対し、本クラスは分離後の Checkpoint 専用。
 * 保存すべきラン状態は「次に入る層」「能力値」「デッキ」「通貨」「装備の 3 分類」「保護枠」に限られる。
 *
 * <p>ラン中に獲得した Soul は Checkpoint 側に保持し、Profile の保有 Soul へは精算時にだけ移す (SETTLE-01/02)。
 * これにより、層境界セーブを繰り返しても Soul が二重計上されない。
 */
public final class RunCheckpointMapper {

  private RunCheckpointMapper() {}

  /**
   * ラン中状態を Checkpoint へ変換する。
   *
   * @param runId このランの ID。Profile の activeRunId と一致させる
   * @param player 現在のプレイヤー (能力値・デッキ・通貨の出所)
   * @param nextLayerNumber ロード後に進入する層番号
   * @param inventory 装備の 3 分類と保護指定
   * @param capacity 保護枠 (ソウルツリー由来)
   */
  public static RunCheckpoint toCheckpoint(
      RunId runId,
      Player player,
      int nextLayerNumber,
      RunInventory inventory,
      RetentionCapacity capacity) {
    Objects.requireNonNull(runId, "runId");
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(inventory, "inventory");
    Objects.requireNonNull(capacity, "capacity");

    // RunInventory は保護枠を持たないため「保護数 ≦ 容量」を型で防げない。ここで前置検証しないと
    // RunCheckpoint の compact constructor が層境界セーブの最中に IAE を投げ、オートセーブで落ちる。
    if (!capacity.canHold(inventory.protectedIds().size())) {
      throw new IllegalArgumentException(
          "protectedIds size "
              + inventory.protectedIds().size()
              + " exceeds capacity "
              + capacity.value()
              + " (runId="
              + runId.value()
              + ")");
    }

    Stats stats = player.stats();
    return new RunCheckpoint(
        RunCheckpoint.CURRENT_SCHEMA_VERSION,
        runId.value(),
        nextLayerNumber,
        stats.currentHp(),
        stats.maxHp(),
        stats.speed(),
        stats.physicalAttack(),
        stats.magicalAttack(),
        stats.physicalDefense(),
        stats.magicalDefense(),
        deckIds(player),
        player.gold().amount(),
        player.soul().amount(),
        toIdValues(inventory.carriedIn()),
        toIdValues(inventory.unconfirmed()),
        inventory.equipped().map(EquipmentId::value).orElse(null),
        toIdValues(inventory.protectedIds()),
        capacity.value());
  }

  /** Checkpoint のラン ID を返す。 */
  public static RunId toRunId(RunCheckpoint checkpoint) {
    Objects.requireNonNull(checkpoint, "checkpoint");
    return RunId.of(checkpoint.runId());
  }

  /** Checkpoint から装備の 3 分類を復元する。 */
  public static RunInventory toRunInventory(RunCheckpoint checkpoint) {
    Objects.requireNonNull(checkpoint, "checkpoint");
    return new RunInventory(
        toEquipmentIds(checkpoint.carriedInEquipmentIds()),
        toEquipmentIds(checkpoint.unconfirmedEquipmentIds()),
        toEquipmentIds(checkpoint.protectedEquipmentIds()),
        Optional.ofNullable(checkpoint.equippedId()).map(EquipmentId::of));
  }

  /** Checkpoint から保護枠を復元する。 */
  public static RetentionCapacity toRetentionCapacity(RunCheckpoint checkpoint) {
    Objects.requireNonNull(checkpoint, "checkpoint");
    return RetentionCapacity.of(checkpoint.retentionCapacity());
  }

  /** Checkpoint から能力値を復元する。 */
  public static Stats toStats(RunCheckpoint checkpoint) {
    Objects.requireNonNull(checkpoint, "checkpoint");
    return new Stats(
        checkpoint.currentHp(),
        checkpoint.maxHp(),
        checkpoint.speed(),
        checkpoint.physicalAttack(),
        checkpoint.magicalAttack(),
        checkpoint.physicalDefense(),
        checkpoint.magicalDefense());
  }

  /** 山札・手札・捨て札を 1 本のリストへ並べる (復元時は山札として積み直す)。 */
  private static List<String> deckIds(Player player) {
    List<String> deck = new ArrayList<>();
    for (Card c : player.cardPileState().drawPile().cards()) {
      deck.add(c.id().value());
    }
    for (Card c : player.cardPileState().hand().cards()) {
      deck.add(c.id().value());
    }
    for (Card c : player.cardPileState().discardPile().cards()) {
      deck.add(c.id().value());
    }
    return deck;
  }

  private static List<String> toIdValues(Set<EquipmentId> ids) {
    List<String> values = new ArrayList<>(ids.size());
    for (EquipmentId id : ids) {
      values.add(id.value());
    }
    return values;
  }

  private static Set<EquipmentId> toEquipmentIds(List<String> values) {
    Set<EquipmentId> ids = new LinkedHashSet<>();
    for (String value : values) {
      ids.add(EquipmentId.of(value));
    }
    return ids;
  }
}
