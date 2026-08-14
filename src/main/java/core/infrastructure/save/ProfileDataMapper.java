package core.infrastructure.save;

import core.domain.card.CardId;
import core.domain.entity.EnemyKind;
import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentOwnership;
import core.domain.equipment.EquipmentSlot;
import core.domain.equipment.RetentionCapacity;
import core.domain.equipment.RunInventory;
import core.domain.meta.PlayerProgress;
import core.domain.meta.Soul;
import core.domain.tree.NodeId;
import core.domain.tree.SoulTree;
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
   * 層境界セーブ用。恒久ソウルを {@code previous} から据え置く。
   *
   * <p>ラン中は {@code progress.playerSoul()} が 0 である (ラン開始時に持越しソウルを Player へ注入して {@code progress} 側を 0
   * にするため)。そのまま書くと profile.json の保有ソウルが 0 で潰れ、 精算前に終了したプレイヤーの貯金が全損する。
   *
   * <p>この経路には恒久ソウルを渡す引数を用意しない。最も踏みやすい取り違えを、API から 選択肢を消すことで防ぐ。
   */
  public static ProfileData forLayerBoundary(PlayerProgress progress, ProfileData previous) {
    Objects.requireNonNull(previous, "previous");
    return write(progress, previous, previous.soulTotal());
  }

  /**
   * 恒久ソウルを更新する経路用 (ラン開始・死亡/クリア精算・放棄精算)。
   *
   * @param soulTotal 書き込む恒久ソウル。ラン開始は注入前の持越し分、精算時は精算後の総量
   */
  public static ProfileData forSoulUpdate(
      PlayerProgress progress, ProfileData previous, int soulTotal) {
    return write(progress, previous, soulTotal);
  }

  private static ProfileData write(PlayerProgress progress, ProfileData previous, int soulTotal) {
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
    for (core.domain.equipment.EquipmentId id : progress.equipmentOwnership().ownedIds()) {
      owned.add(id.value());
    }
    // 反復順は EquipmentSlot の宣言順に固定する (Map の反復順は JVM 実行ごとに変わりうる)。
    for (EquipmentSlot slot : EquipmentSlot.values()) {
      Equipment equipment = progress.loadout().get(slot);
      if (equipment == null) {
        continue;
      }
      loadoutMap.put(slot.name(), equipment.id().value());
      owned.add(equipment.id().value());
    }

    int capacityValue = currentCapacity(progress).value();
    List<String> protectedIds = new ArrayList<>();
    for (core.domain.equipment.EquipmentId id : progress.equipmentOwnership().protectedIds()) {
      protectedIds.add(id.value());
    }
    if (protectedIds.isEmpty() && !previous.protectedEquipmentIds().isEmpty()) {
      for (String id : previous.protectedEquipmentIds()) {
        if (owned.contains(id) && !id.equals(EquipmentOwnership.STARTER_EQUIPMENT_VALUE)) {
          protectedIds.add(id);
        }
      }
    }
    if (protectedIds.size() > capacityValue) {
      protectedIds = protectedIds.subList(0, capacityValue);
    }

    return new ProfileData(
        ProfileData.CURRENT_SCHEMA_VERSION,
        soulTotal,
        progress.runCount(),
        unlockedNodeIds,
        List.copyOf(owned),
        loadoutMap,
        List.copyOf(protectedIds),
        capacityValue,
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

  /**
   * 恒久保存の形からラン外進捗を復元する (SAVE-03B)。
   *
   * <p>起動時にこれを呼ばないと {@code progress} が空のまま走り出し、次のラン開始で {@link #forSoulUpdate} が空の値で profile.json
   * を全上書きして恒久進捗を消す。書込系が {@code previous} から引き継ぐのはラン ID 群だけで、 他はすべて {@code progress} から再構築するためである。
   *
   * @param profile 読み込んだ恒久状態
   * @param fallbackLoadout profile に編成が無い場合に使う初期編成 (新規プレイヤー用)。 空の編成で開始すると空デッキになり詰むため必須
   */
  public static PlayerProgress toPlayerProgress(
      ProfileData profile, Map<EquipmentSlot, Equipment> fallbackLoadout) {
    Objects.requireNonNull(profile, "profile");
    Objects.requireNonNull(fallbackLoadout, "fallbackLoadout");

    // 既存の SaveDataConverter を再利用するため、Profile だけから SaveData 形を組む。
    // ラン側のフィールドは復元に使わないのでゼロ値で埋める。
    SaveData shaped =
        new SaveData(
            SaveData.CURRENT_SCHEMA_VERSION,
            1,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            List.of(),
            profile.soulTotal(),
            profile.runCount(),
            profile.unlockedNodeIds(),
            profile.obtainedCardIds(),
            profile.loadout(),
            profile.defeatedEnemyKinds(),
            profile.tutorialSeen(),
            0,
            0);

    Map<EquipmentSlot, Equipment> loadout = SaveDataConverter.toLoadout(shaped);
    if (loadout.isEmpty()) {
      loadout = fallbackLoadout;
    }
    SoulTree tree = SaveDataConverter.toSoulTree(shaped);
    Set<core.domain.equipment.EquipmentId> owned = new LinkedHashSet<>();
    owned.add(EquipmentOwnership.STARTER_EQUIPMENT_ID);
    for (String id : profile.ownedEquipmentIds()) {
      owned.add(core.domain.equipment.EquipmentId.of(id));
    }
    for (Equipment eq : loadout.values()) {
      owned.add(eq.id());
    }
    Set<core.domain.equipment.EquipmentId> protectedIds = new LinkedHashSet<>();
    for (String id : profile.protectedEquipmentIds()) {
      core.domain.equipment.EquipmentId eqId = core.domain.equipment.EquipmentId.of(id);
      if (owned.contains(eqId) && !eqId.equals(EquipmentOwnership.STARTER_EQUIPMENT_ID)) {
        protectedIds.add(eqId);
      }
    }
    EquipmentOwnership ownership =
        new EquipmentOwnership(owned, protectedIds, tree.retentionCapacity());

    return new PlayerProgress(
        new Soul(profile.soulTotal()),
        profile.runCount(),
        profile.tutorialSeen(),
        SaveDataConverter.toObtainedCards(shaped),
        SaveDataConverter.toBestiary(shaped),
        loadout,
        tree,
        ownership);
  }

  /** 現在の保護枠 (SOUL-03: ソウルツリーの解放状態から導出)。 */
  public static RetentionCapacity currentCapacity(PlayerProgress progress) {
    Objects.requireNonNull(progress, "progress");
    return progress.soulTree().retentionCapacity();
  }
}
