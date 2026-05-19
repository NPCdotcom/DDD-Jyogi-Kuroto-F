package core.domain.equipment;

import core.domain.card.CardId;
import java.util.List;
import java.util.Objects;

/**
 * 装備品 (§15-9 / ADR-20 B 案 / ADR-25)。1 部位スタート (ADR-08 維持) で、ステ補正 + 装備固有カード自動付与を表現。
 *
 * <p>{@code grantedCards} は空リスト可。装備変更時は層末ショップノードのみで行い、戦闘中の装備変更は禁止 (ADR-26)。
 * 装備変更時は {@link core.domain.card.CardPileState} を再生成する (山札・手札・捨て札はリセット)。
 *
 * <p>耐久・特殊能力なし (§15-9)、装備テーマ変動 (§7-2) は M2 送り。
 */
public record Equipment(
    EquipmentId id,
    String displayName,
    EquipmentSlot slot,
    StatsBonus statsBonus,
    List<CardId> grantedCards) {

  public Equipment {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(displayName, "displayName");
    if (displayName.isBlank()) {
      throw new IllegalArgumentException("displayName must not be blank");
    }
    Objects.requireNonNull(slot, "slot");
    Objects.requireNonNull(statsBonus, "statsBonus");
    Objects.requireNonNull(grantedCards, "grantedCards");
    grantedCards = List.copyOf(grantedCards);
  }
}
