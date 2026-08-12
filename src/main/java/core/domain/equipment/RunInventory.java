package core.domain.equipment;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * ラン 1 回ぶんの装備所持品 (EQUIP-00 / §15-9)。RunCheckpoint 側に保存する値オブジェクト。
 *
 * <p>死亡時の精算規則が「何を持ち込んだか」と「ラン中に買ったか」で変わるため、両者を型で分ける。
 *
 * <ul>
 *   <li>{@code carriedIn} 持込品: 所有済み装備のうちこのランへ持ち込んだもの。死亡時は保護品のみ残り、 未保護品は所有一覧から外れて Soul へ変換される
 *   <li>{@code unconfirmed} 未確定品: ラン中に金貨で購入した装備。クリア時のみ所有化され、 死亡時は Soul へ変換せず失う (金貨から Soul
 *       への交換経路を遮断)
 *   <li>{@code protectedIds} 保護品: 持込品のうち死亡時に維持するもの。未確定品は対象外
 *   <li>{@code equipped} 現在装備: 持込品と未確定品の和集合に含まれる
 * </ul>
 *
 * <p>ドメイン層のため {@code java.*} のみに依存する。保存形式への変換は {@code core.infrastructure.save.SaveDataConverter}
 * が担当する。
 */
public record RunInventory(
    Set<EquipmentId> carriedIn,
    Set<EquipmentId> unconfirmed,
    Set<EquipmentId> protectedIds,
    Optional<EquipmentId> equipped) {

  public RunInventory {
    Objects.requireNonNull(carriedIn, "carriedIn");
    Objects.requireNonNull(unconfirmed, "unconfirmed");
    Objects.requireNonNull(protectedIds, "protectedIds");
    Objects.requireNonNull(equipped, "equipped");

    carriedIn = Set.copyOf(carriedIn);
    unconfirmed = Set.copyOf(unconfirmed);
    protectedIds = Set.copyOf(protectedIds);

    for (EquipmentId id : unconfirmed) {
      if (carriedIn.contains(id)) {
        throw new IllegalArgumentException(
            "carriedIn and unconfirmed must be disjoint: " + id.value());
      }
    }
    if (protectedIds.contains(EquipmentOwnership.STARTER_EQUIPMENT_ID)) {
      throw new IllegalArgumentException(
          "starter equipment must not be protected: " + EquipmentOwnership.STARTER_EQUIPMENT_VALUE);
    }
    if (!carriedIn.containsAll(protectedIds)) {
      throw new IllegalArgumentException("protectedIds must be a subset of carriedIn");
    }
    if (equipped.isPresent()) {
      EquipmentId id = equipped.get();
      if (!carriedIn.contains(id) && !unconfirmed.contains(id)) {
        throw new IllegalArgumentException(
            "equipped must be carried in or unconfirmed: " + id.value());
      }
    }
  }

  /**
   * 所有状態と突き合わせてラン開始時の所持品を作る。
   *
   * <p>持込品が所有済みであること、保護指定が保護枠の容量に収まることを検証する。 コンストラクタ単体では所有集合を参照できないため、この入口で両者を突き合わせる。
   *
   * @param ownership ラン開始時点の所有状態
   * @param carriedIn 持ち込む装備 (すべて所有済みである必要がある)
   * @param protectedIds 死亡時に保護する装備 (持込品の部分集合、容量以下)
   * @param equipped 開始時の装着装備
   */
  public static RunInventory startRun(
      EquipmentOwnership ownership,
      Set<EquipmentId> carriedIn,
      Set<EquipmentId> protectedIds,
      Optional<EquipmentId> equipped) {
    Objects.requireNonNull(ownership, "ownership");
    Objects.requireNonNull(carriedIn, "carriedIn");
    Objects.requireNonNull(protectedIds, "protectedIds");
    if (!ownership.ownedIds().containsAll(carriedIn)) {
      throw new IllegalArgumentException("carriedIn must be a subset of ownedIds");
    }
    if (!ownership.capacity().canHold(protectedIds.size())) {
      throw new IllegalArgumentException(
          "protectedIds size "
              + protectedIds.size()
              + " exceeds capacity "
              + ownership.capacity().value());
    }
    // ラン開始時の未確定品は空 (購入はラン中にのみ発生する)。
    return new RunInventory(carriedIn, Set.of(), protectedIds, equipped);
  }

  /**
   * 死亡時に喪失する持込品を返す (§15-9)。
   *
   * <p>保護品と初期装備を除いた持込品。未確定品はここに含めない — 未確定品は Soul へ変換せず 別経路で失うため、精算処理 (SETTLE-01) が個別に扱う。
   */
  public Set<EquipmentId> unprotectedCarriedIn() {
    Set<EquipmentId> lost = new LinkedHashSet<>(carriedIn);
    lost.removeAll(protectedIds);
    lost.remove(EquipmentOwnership.STARTER_EQUIPMENT_ID);
    return Set.copyOf(lost);
  }

  /** ラン中の購入で未確定品を加えた新しい所持品を返す。 */
  public RunInventory withPurchased(EquipmentId id) {
    Objects.requireNonNull(id, "id");
    Set<EquipmentId> bought = new LinkedHashSet<>(unconfirmed);
    bought.add(id);
    return new RunInventory(carriedIn, bought, protectedIds, equipped);
  }

  /** 装着装備を差し替えた新しい所持品を返す。 */
  public RunInventory withEquipped(Optional<EquipmentId> newEquipped) {
    Objects.requireNonNull(newEquipped, "newEquipped");
    return new RunInventory(carriedIn, unconfirmed, protectedIds, newEquipped);
  }
}
