package core.domain.entity;

import core.domain.battle.ActionPoints;
import core.domain.card.ActiveBuff;
import core.domain.card.CardId;
import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentSlot;
import core.domain.equipment.StatsBonus;
import core.domain.skill.SkillSlot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Player の能力値関連の集約 (§15-4 / ADR-25 / ADR-26)。素ステ / AP / スキル枠 / 装備 / アクティブ Buff を 1 単位で扱う。
 *
 * <p>ADR-25 で Player の引数増加問題 (8 → 10 引数) を解消するために導入。Buff (§15-3) + E-5 装備 (§15-9) を 1
 * つのレイヤーに統合し、{@link #effectiveStats()} で「素ステ + 装備補正 + Buff 合算」を 1 箇所表現する (DRY)。
 *
 * <p>装備は {@code Map<EquipmentSlot, Equipment>} で複数部位を保持 (§15-9 ぼろ靴 + ぼろい短剣の 2 部位スタートに対応)。 各 entry の
 * {@code key} と {@code value.slot()} は一致しなければならない (compact constructor で検証)。装備変更は 層末ショップノードのみ
 * (ADR-26)、戦闘中の装備変更は禁止。
 *
 * <p>Player 側は本 record の各フィールドへの委譲アクセサ ({@code player.stats()} 等) を維持するため、戦闘ロジックは 大量改修不要 (ADR-25
 * Decision #5)。
 */
public record PlayerStatuses(
    Stats stats,
    ActionPoints actionPoints,
    SkillSlot skillSlot,
    Map<EquipmentSlot, Equipment> equipment,
    List<ActiveBuff> activeBuffs) {

  public PlayerStatuses {
    Objects.requireNonNull(stats, "stats");
    Objects.requireNonNull(actionPoints, "actionPoints");
    Objects.requireNonNull(skillSlot, "skillSlot");
    Objects.requireNonNull(equipment, "equipment");
    Objects.requireNonNull(activeBuffs, "activeBuffs");
    for (var entry : equipment.entrySet()) {
      if (entry.getValue().slot() != entry.getKey()) {
        throw new IllegalArgumentException(
            "Equipment slot mismatch: key=%s but value.slot=%s"
                .formatted(entry.getKey(), entry.getValue().slot()));
      }
    }
    equipment = Map.copyOf(equipment);
    activeBuffs = List.copyOf(activeBuffs);
  }

  /** 装備・Buff なしの最小構成を返す静的ファクトリ (テスト・初期化用)。 */
  public static PlayerStatuses of(Stats stats, ActionPoints actionPoints, SkillSlot skillSlot) {
    return new PlayerStatuses(stats, actionPoints, skillSlot, Map.of(), List.of());
  }

  /**
   * 素ステ + 全装備の {@code statsBonus} 合算 + アクティブ Buff の合算後の {@link Stats} を返す純関数 (§15-4 / ADR-25)。
   *
   * <p>currentHp は素ステの値を維持しつつ、新 maxHp でクランプ ({@link Stats#plus(StatsBonus)} 側で処理)。
   */
  public Stats effectiveStats() {
    StatsBonus equipmentBonus = equipmentSum();
    StatsBonus buffBonus = buffSum();
    StatsBonus total = equipmentBonus.plus(buffBonus);
    return stats.plus(total);
  }

  /**
   * 全装備の {@code grantedCards} を連結したリストを返す (§15-9 / ADR-26)。
   *
   * <p>装備固有デッキ生成 ({@code InitialStateFactory.newPlayer} のデッキ動的化) や装備変更時の {@link
   * core.domain.card.CardPileState} 再生成で使用。装備が無い slot はスキップ。
   */
  public List<CardId> equipmentCards() {
    List<CardId> all = new ArrayList<>();
    for (Equipment e : equipment.values()) {
      all.addAll(e.grantedCards());
    }
    return List.copyOf(all);
  }

  private StatsBonus equipmentSum() {
    StatsBonus sum = StatsBonus.zero();
    for (Equipment e : equipment.values()) {
      sum = sum.plus(e.statsBonus());
    }
    return sum;
  }

  /**
   * アクティブ Buff の合算を {@link StatsBonus} 形式で返す (内部使用)。{@code maxHp} は §15-3 のスコープ外
   * (CardEffect.BuffKind が HP バフを含まない) のため 0 固定。
   */
  private StatsBonus buffSum() {
    int phyAtk = 0;
    int magAtk = 0;
    int phyDef = 0;
    int magDef = 0;
    int spd = 0;
    for (ActiveBuff b : activeBuffs) {
      switch (b.kind()) {
        case PHYSICAL_ATTACK_UP -> phyAtk += b.amount();
        case MAGICAL_ATTACK_UP -> magAtk += b.amount();
        case PHYSICAL_DEFENSE_UP -> phyDef += b.amount();
        case MAGICAL_DEFENSE_UP -> magDef += b.amount();
        case SPEED_UP -> spd += b.amount();
      }
    }
    return new StatsBonus(0, spd, phyAtk, magAtk, phyDef, magDef);
  }

  public PlayerStatuses withStats(Stats newStats) {
    return new PlayerStatuses(newStats, actionPoints, skillSlot, equipment, activeBuffs);
  }

  public PlayerStatuses withActionPoints(ActionPoints newActionPoints) {
    return new PlayerStatuses(stats, newActionPoints, skillSlot, equipment, activeBuffs);
  }

  public PlayerStatuses withSkillSlot(SkillSlot newSkillSlot) {
    return new PlayerStatuses(stats, actionPoints, newSkillSlot, equipment, activeBuffs);
  }

  public PlayerStatuses withEquipment(Map<EquipmentSlot, Equipment> newEquipment) {
    return new PlayerStatuses(stats, actionPoints, skillSlot, newEquipment, activeBuffs);
  }

  public PlayerStatuses withActiveBuffs(List<ActiveBuff> newActiveBuffs) {
    return new PlayerStatuses(stats, actionPoints, skillSlot, equipment, newActiveBuffs);
  }
}
