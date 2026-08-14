package core.domain.meta;

import core.domain.card.CardId;
import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentId;
import core.domain.equipment.EquipmentOwnership;
import core.domain.equipment.EquipmentSlot;
import core.domain.tree.SoulTree;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ラン外で持続するプレイヤーの永続進捗を 1 つの不変 record に集約する (Wave 5 W5-β / EQUIP-01)。
 *
 * <p>従来は {@code DddGame} に個別フィールド (playerSoul / runCount / tutorialSeen / obtainedCards / bestiary
 * / loadout / soulTree / equipmentOwnership) として散在していたものを 1 つの値オブジェクトに統合し、God Object 化を防ぐ。
 *
 * <p>不変性: コンパクトコンストラクタで {@link Set#copyOf} / {@link Map#copyOf} を適用、外部の変更から守る。 状態遷移は {@code with*}
 * メソッド経由の新インスタンス生成で行う (record の慣習)。
 */
public record PlayerProgress(
    Soul playerSoul,
    int runCount,
    boolean tutorialSeen,
    Set<CardId> obtainedCards,
    Bestiary bestiary,
    Map<EquipmentSlot, Equipment> loadout,
    SoulTree soulTree,
    EquipmentOwnership equipmentOwnership) {

  public PlayerProgress {
    Objects.requireNonNull(playerSoul, "playerSoul");
    if (runCount < 0) {
      throw new IllegalArgumentException("runCount must be non-negative: " + runCount);
    }
    Objects.requireNonNull(obtainedCards, "obtainedCards");
    Objects.requireNonNull(bestiary, "bestiary");
    Objects.requireNonNull(loadout, "loadout");
    Objects.requireNonNull(soulTree, "soulTree");
    Objects.requireNonNull(equipmentOwnership, "equipmentOwnership");
    obtainedCards = Set.copyOf(obtainedCards);
    loadout = Map.copyOf(loadout);

    // 不変条件: loadout に含まれる全装備は equipmentOwnership.ownedIds() の部分集合でなければならない (EQUIP-01)
    for (Equipment eq : loadout.values()) {
      if (!equipmentOwnership.ownedIds().contains(eq.id())) {
        throw new IllegalArgumentException("loadout equipment not owned: " + eq.id().value());
      }
    }
  }

  /** 旧 7 引数コンストラクタ (下位互換 / 移行用)。loadout から所有集合を自動補完する。 */
  public PlayerProgress(
      Soul playerSoul,
      int runCount,
      boolean tutorialSeen,
      Set<CardId> obtainedCards,
      Bestiary bestiary,
      Map<EquipmentSlot, Equipment> loadout,
      SoulTree soulTree) {
    this(
        playerSoul,
        runCount,
        tutorialSeen,
        obtainedCards,
        bestiary,
        loadout,
        soulTree,
        deriveOwnership(loadout, soulTree));
  }

  private static EquipmentOwnership deriveOwnership(
      Map<EquipmentSlot, Equipment> loadout, SoulTree soulTree) {
    Set<EquipmentId> owned = new LinkedHashSet<>();
    owned.add(EquipmentOwnership.STARTER_EQUIPMENT_ID);
    if (loadout != null) {
      for (Equipment eq : loadout.values()) {
        owned.add(eq.id());
      }
    }
    return new EquipmentOwnership(
        owned,
        Set.of(),
        soulTree != null
            ? soulTree.retentionCapacity()
            : core.domain.equipment.RetentionCapacity.none());
  }

  /** 全フィールドが初期値の初期状態 ({@code DddGame} 起動時のデフォルト)。 */
  public static PlayerProgress initial(Map<EquipmentSlot, Equipment> defaultLoadout) {
    SoulTree tree = SoulTree.empty();
    return new PlayerProgress(
        Soul.zero(),
        0,
        false,
        Set.of(),
        Bestiary.empty(),
        defaultLoadout,
        tree,
        deriveOwnership(defaultLoadout, tree));
  }

  /** 所有している全装備の ID 文字列集合を返す (EQUIP-02 画面表示用)。 */
  public Set<String> ownedEquipmentIds() {
    return equipmentOwnership.ownedIds().stream()
        .map(EquipmentId::value)
        .collect(Collectors.toSet());
  }

  public PlayerProgress withPlayerSoul(Soul soul) {
    return new PlayerProgress(
        soul,
        runCount,
        tutorialSeen,
        obtainedCards,
        bestiary,
        loadout,
        soulTree,
        equipmentOwnership);
  }

  public PlayerProgress withRunCount(int newRunCount) {
    return new PlayerProgress(
        playerSoul,
        newRunCount,
        tutorialSeen,
        obtainedCards,
        bestiary,
        loadout,
        soulTree,
        equipmentOwnership);
  }

  public PlayerProgress withTutorialSeen(boolean seen) {
    return new PlayerProgress(
        playerSoul, runCount, seen, obtainedCards, bestiary, loadout, soulTree, equipmentOwnership);
  }

  public PlayerProgress withObtainedCards(Set<CardId> newCards) {
    return new PlayerProgress(
        playerSoul,
        runCount,
        tutorialSeen,
        newCards,
        bestiary,
        loadout,
        soulTree,
        equipmentOwnership);
  }

  public PlayerProgress withBestiary(Bestiary newBestiary) {
    return new PlayerProgress(
        playerSoul,
        runCount,
        tutorialSeen,
        obtainedCards,
        newBestiary,
        loadout,
        soulTree,
        equipmentOwnership);
  }

  public PlayerProgress withLoadout(Map<EquipmentSlot, Equipment> newLoadout) {
    return new PlayerProgress(
        playerSoul,
        runCount,
        tutorialSeen,
        obtainedCards,
        bestiary,
        newLoadout,
        soulTree,
        equipmentOwnership);
  }

  public PlayerProgress withSoulTree(SoulTree newTree) {
    // ソウルツリー更新時に保護枠容量を同期
    EquipmentOwnership nextOwnership =
        new EquipmentOwnership(
            equipmentOwnership.ownedIds(),
            equipmentOwnership.protectedIds(),
            newTree.retentionCapacity());
    return new PlayerProgress(
        playerSoul,
        runCount,
        tutorialSeen,
        obtainedCards,
        bestiary,
        loadout,
        newTree,
        nextOwnership);
  }

  public PlayerProgress withEquipmentOwnership(EquipmentOwnership newOwnership) {
    return new PlayerProgress(
        playerSoul,
        runCount,
        tutorialSeen,
        obtainedCards,
        bestiary,
        loadout,
        soulTree,
        newOwnership);
  }
}
