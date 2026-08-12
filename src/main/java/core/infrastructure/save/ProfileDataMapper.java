package core.infrastructure.save;

import core.domain.card.CardId;
import core.domain.entity.EnemyKind;
import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentOwnership;
import core.domain.equipment.EquipmentSlot;
import core.domain.equipment.RetentionCapacity;
import core.domain.equipment.RunInventory;
import core.domain.meta.PlayerProgress;
import core.domain.tree.NodeId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * {@link PlayerProgress} と {@link ProfileData} を相互変換する純関数 (SAVE-03B)。
 *
 * <p>{@code DddGame} が保持するラン外進捗を、恒久保存の形へ写す。所有装備モデル (EQUIP-01) が 入るまでは「編成に載っている装備 +
 * 初期短剣」を所有集合とみなす暫定実装で、保護指定と保護枠は 常に空・0 とする。EQUIP-01 で {@code PlayerProgress} が所有集合を持ったら、この暫定を外す。
 *
 * <p>{@code activeRunId} と {@code lastSettledRunId} は本クラスでは扱わない。ラン ID の付け外しは {@link RunLifecycle}
 * の責務で、そこを 1 箇所に集約することで「終了済みランが再開できる」 事故 (レビュー P0-1) の入口を狭める。
 */
public final class ProfileDataMapper {

  private ProfileDataMapper() {}

  /**
   * ラン外進捗を恒久保存の形へ写す。
   *
   * @param progress ラン外で持続する進捗
   * @param previous 直前の Profile (ラン ID 群を引き継ぐため)。無い場合は {@link ProfileData#initial()}
   */
  public static ProfileData toProfileData(PlayerProgress progress, ProfileData previous) {
    Objects.requireNonNull(progress, "progress");
    Objects.requireNonNull(previous, "previous");

    List<String> unlockedNodeIds = new ArrayList<>();
    for (NodeId nodeId : progress.soulTree().unlockedNodes()) {
      unlockedNodeIds.add(nodeId.value());
    }
    List<String> obtainedCardIds = new ArrayList<>();
    for (CardId cardId : progress.obtainedCards()) {
      obtainedCardIds.add(cardId.value());
    }
    List<String> defeatedEnemyKinds = new ArrayList<>();
    for (EnemyKind kind : progress.bestiary().defeatedKinds()) {
      defeatedEnemyKinds.add(kind.name());
    }

    Map<String, String> loadoutMap = new LinkedHashMap<>();
    Set<String> owned = new LinkedHashSet<>();
    owned.add(EquipmentOwnership.STARTER_EQUIPMENT_VALUE);
    // 反復順は EquipmentSlot の宣言順に固定する (Map の反復順は JVM 実行ごとに変わりうる)。
    for (EquipmentSlot slot : EquipmentSlot.values()) {
      Equipment equipment = progress.loadout().get(slot);
      if (equipment == null) {
        continue;
      }
      loadoutMap.put(slot.name(), equipment.id().value());
      owned.add(equipment.id().value());
    }

    return new ProfileData(
        ProfileData.CURRENT_SCHEMA_VERSION,
        progress.playerSoul().amount(),
        progress.runCount(),
        unlockedNodeIds,
        List.copyOf(owned),
        loadoutMap,
        List.of(), // 保護指定は EQUIP-02 で編成画面から設定する
        0, // 保護枠は SOUL-03 でソウルツリーから導出する
        defeatedEnemyKinds,
        obtainedCardIds,
        progress.tutorialSeen(),
        previous.activeRunId(),
        previous.lastSettledRunId());
  }

  /**
   * 編成から現在ランの装備所持品を組み立てる (暫定)。
   *
   * <p>持込品 = 編成に載っている装備 + 初期短剣、装着 = {@link EquipmentSlot} 宣言順の最初の 1 件、 未確定品と保護品は空。ラン中購入 (EQUIP-03)
   * と保護指定 (EQUIP-02) が入るまでの繋ぎ。
   */
  public static RunInventory toRunInventory(PlayerProgress progress) {
    Objects.requireNonNull(progress, "progress");
    Set<core.domain.equipment.EquipmentId> carriedIn = new LinkedHashSet<>();
    carriedIn.add(EquipmentOwnership.STARTER_EQUIPMENT_ID);
    core.domain.equipment.EquipmentId equipped = null;
    for (EquipmentSlot slot : EquipmentSlot.values()) {
      Equipment equipment = progress.loadout().get(slot);
      if (equipment == null) {
        continue;
      }
      carriedIn.add(equipment.id());
      if (equipped == null) {
        equipped = equipment.id();
      }
    }
    return new RunInventory(carriedIn, Set.of(), Set.of(), Optional.ofNullable(equipped));
  }

  /** 現在の保護枠 (SOUL-03 でソウルツリーから導出するまでは常に 0)。 */
  public static RetentionCapacity currentCapacity(PlayerProgress progress) {
    Objects.requireNonNull(progress, "progress");
    return RetentionCapacity.none();
  }
}
