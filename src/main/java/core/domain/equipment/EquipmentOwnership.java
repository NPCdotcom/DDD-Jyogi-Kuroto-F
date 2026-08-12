package core.domain.equipment;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 恒久的な装備の所有状態 (EQUIP-00 / §15-9)。Profile 側に保存する値オブジェクト。
 *
 * <p>保持するのは「所有している装備 ID」「死亡時に保護する装備 ID」「保護枠の容量」の 3 つ。 ラン 1 回ぶんの持込・未確定・現在装備は {@link RunInventory}
 * が持ち、本型は跨がない。
 *
 * <p>不変条件:
 *
 * <ul>
 *   <li>初期装備 ({@value #STARTER_EQUIPMENT_VALUE}) は常に所有一覧に含まれる (§15-9 常に再支給)
 *   <li>初期装備は保護対象にできない (喪失しないため保護枠を消費させない)
 *   <li>保護 ID は所有 ID の部分集合
 *   <li>保護 ID の数は保護枠の容量以下
 * </ul>
 *
 * <p>ドメイン層のため {@code java.*} のみに依存する。保存形式への変換は {@code core.infrastructure.save.SaveDataConverter}
 * が担当し、本型に Jackson 注釈を持ち込まない。
 */
public record EquipmentOwnership(
    Set<EquipmentId> ownedIds, Set<EquipmentId> protectedIds, RetentionCapacity capacity) {

  /** 初期装備「ぼろい短剣」の ID 値 (ADR-30、`equipment.json` の {@code tattered_dagger} 準拠)。 */
  public static final String STARTER_EQUIPMENT_VALUE = "tattered_dagger";

  /** 初期装備「ぼろい短剣」の ID。喪失・ソウル変換・保護枠消費のいずれの対象にもならない (§15-9)。 */
  public static final EquipmentId STARTER_EQUIPMENT_ID = EquipmentId.of(STARTER_EQUIPMENT_VALUE);

  public EquipmentOwnership {
    Objects.requireNonNull(ownedIds, "ownedIds");
    Objects.requireNonNull(protectedIds, "protectedIds");
    Objects.requireNonNull(capacity, "capacity");

    // 初期装備は常に再支給されるため、欠けていても補う (§15-9)。
    Set<EquipmentId> owned = new LinkedHashSet<>(ownedIds);
    owned.add(STARTER_EQUIPMENT_ID);
    ownedIds = Set.copyOf(owned);

    protectedIds = Set.copyOf(protectedIds);
    if (protectedIds.contains(STARTER_EQUIPMENT_ID)) {
      throw new IllegalArgumentException(
          "starter equipment must not be protected: " + STARTER_EQUIPMENT_VALUE);
    }
    if (!ownedIds.containsAll(protectedIds)) {
      throw new IllegalArgumentException("protectedIds must be a subset of ownedIds");
    }
    if (!capacity.canHold(protectedIds.size())) {
      throw new IllegalArgumentException(
          "protectedIds size " + protectedIds.size() + " exceeds capacity " + capacity.value());
    }
  }

  /** 新規 Profile の初期状態。初期装備のみ所有し、保護枠は 0。 */
  public static EquipmentOwnership initial() {
    return new EquipmentOwnership(Set.of(STARTER_EQUIPMENT_ID), Set.of(), RetentionCapacity.none());
  }

  /** 装備を所有一覧に加えた新しい状態を返す (クリア時の未確定品の確定、ショップ購入の反映)。 */
  public EquipmentOwnership withOwned(EquipmentId id) {
    Objects.requireNonNull(id, "id");
    Set<EquipmentId> owned = new LinkedHashSet<>(ownedIds);
    owned.add(id);
    return new EquipmentOwnership(owned, protectedIds, capacity);
  }

  /**
   * 装備を所有一覧から除いた新しい状態を返す (死亡時の未保護品の喪失)。
   *
   * <p>保護指定も同時に外す。初期装備は常に再支給されるため除去できない (§15-9)。
   */
  public EquipmentOwnership withoutOwned(EquipmentId id) {
    Objects.requireNonNull(id, "id");
    if (STARTER_EQUIPMENT_ID.equals(id)) {
      return this;
    }
    Set<EquipmentId> owned = new LinkedHashSet<>(ownedIds);
    owned.remove(id);
    Set<EquipmentId> stillProtected = new LinkedHashSet<>(protectedIds);
    stillProtected.remove(id);
    return new EquipmentOwnership(owned, stillProtected, capacity);
  }

  /** 保護枠の容量を差し替えた新しい状態を返す (ソウルツリーの解放・リセット反映、SOUL-03)。 */
  public EquipmentOwnership withCapacity(RetentionCapacity newCapacity) {
    Objects.requireNonNull(newCapacity, "newCapacity");
    Set<EquipmentId> stillProtected =
        newCapacity.canHold(protectedIds.size()) ? protectedIds : Set.of();
    return new EquipmentOwnership(ownedIds, stillProtected, newCapacity);
  }
}
