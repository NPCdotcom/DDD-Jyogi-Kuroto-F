package core.domain.card;

/**
 * カードのレアリティ (Wave 11 W11-β)。CARD_DRAW 系 SE のレアリティ別音分けに使う (将来はショップ抽選率にも反映予定)。
 *
 * <p>{@link Card#rarity()} は {@link java.util.Optional} で保持し、JSON で未指定なら {@link
 * Card#rarityOrDefault()} で {@link #COMMON} にフォールバックする「未設定」と「明示 COMMON」を区別したい場合 (図鑑表示等) のために
 * Optional のまま公開する API も併存する (デュアル API)。
 */
public enum CardRarity {
  COMMON,
  UNCOMMON,
  RARE
}
