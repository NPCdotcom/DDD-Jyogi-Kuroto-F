package core.domain.meta;

import core.domain.card.CardId;
import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentSlot;
import core.domain.tree.SoulTree;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * ラン外で持続するプレイヤーの永続進捗を 1 つの不変 record に集約する (Wave 5 W5-β)。
 *
 * <p>従来は {@code DddGame} に個別フィールド (playerSoul / runCount / tutorialSeen / obtainedCards / bestiary
 * / loadout / soulTree) として散在していたものを 1 つの値オブジェクトに統合し、God Object 化を防ぐ。
 *
 * <p>不変性: コンパクトコンストラクタで {@link Set#copyOf} / {@link Map#copyOf} を適用、外部の変更から守る。 状態遷移は {@code with*}
 * メソッド経由の新インスタンス生成で行う (record の慣習)。
 *
 * <p>Screen 公開 API は当面 {@code DddGame} の個別 getter / setter を残す (段階的アプローチ)。 集約を進めるリファクタは Wave 6 以降で
 * breaking change として実施予定。
 */
public record PlayerProgress(
    Soul playerSoul,
    int runCount,
    boolean tutorialSeen,
    Set<CardId> obtainedCards,
    Bestiary bestiary,
    Map<EquipmentSlot, Equipment> loadout,
    SoulTree soulTree) {

  public PlayerProgress {
    Objects.requireNonNull(playerSoul, "playerSoul");
    if (runCount < 0) {
      throw new IllegalArgumentException("runCount must be non-negative: " + runCount);
    }
    Objects.requireNonNull(obtainedCards, "obtainedCards");
    Objects.requireNonNull(bestiary, "bestiary");
    Objects.requireNonNull(loadout, "loadout");
    Objects.requireNonNull(soulTree, "soulTree");
    obtainedCards = Set.copyOf(obtainedCards);
    loadout = Map.copyOf(loadout);
  }

  /** 全フィールドが初期値の初期状態 ({@code DddGame} 起動時のデフォルト)。 */
  public static PlayerProgress initial(Map<EquipmentSlot, Equipment> defaultLoadout) {
    return new PlayerProgress(
        Soul.zero(), 0, false, Set.of(), Bestiary.empty(), defaultLoadout, SoulTree.empty());
  }

  public PlayerProgress withPlayerSoul(Soul soul) {
    return new PlayerProgress(
        soul, runCount, tutorialSeen, obtainedCards, bestiary, loadout, soulTree);
  }

  public PlayerProgress withRunCount(int newRunCount) {
    return new PlayerProgress(
        playerSoul, newRunCount, tutorialSeen, obtainedCards, bestiary, loadout, soulTree);
  }

  public PlayerProgress withTutorialSeen(boolean seen) {
    return new PlayerProgress(
        playerSoul, runCount, seen, obtainedCards, bestiary, loadout, soulTree);
  }

  public PlayerProgress withObtainedCards(Set<CardId> newCards) {
    return new PlayerProgress(
        playerSoul, runCount, tutorialSeen, newCards, bestiary, loadout, soulTree);
  }

  public PlayerProgress withBestiary(Bestiary newBestiary) {
    return new PlayerProgress(
        playerSoul, runCount, tutorialSeen, obtainedCards, newBestiary, loadout, soulTree);
  }

  public PlayerProgress withLoadout(Map<EquipmentSlot, Equipment> newLoadout) {
    return new PlayerProgress(
        playerSoul, runCount, tutorialSeen, obtainedCards, bestiary, newLoadout, soulTree);
  }

  public PlayerProgress withSoulTree(SoulTree newTree) {
    return new PlayerProgress(
        playerSoul, runCount, tutorialSeen, obtainedCards, bestiary, loadout, newTree);
  }
}
