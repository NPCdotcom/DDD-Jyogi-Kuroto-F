package core.domain.card;

import java.util.Objects;
import java.util.Optional;

/**
 * カード 1 枚を表す不変値。
 *
 * <p>カードの多態性 (攻撃 / 移動 / バフ / 罠) は {@link CardEffect} (sealed) に集約しており、 {@code Card}
 * 自体はサブクラスを持たない。タグ ({@link CardTag}) と効果 ({@link CardEffect}) は独立した次元として扱う (例: 同じ ATTACK タグでも、効果は
 * Damage だったり Move 込みのこともある)。
 *
 * <p>仕様: §15-3。AP コストは必ず 1 以上 (0 コストカードは「ターン終了条件 = AP 枯渇」と相性が悪いため禁止)。
 *
 * <p>Wave 11 W11-β: {@link CardRarity} を {@link Optional} で保持し、cards.json で未指定なら {@code
 * Optional.empty()} → {@link #rarityOrDefault()} で {@link CardRarity#COMMON} にフォールバックする (graceful)。
 */
public record Card(
    CardId id,
    String displayName,
    int apCost,
    CardTag tag,
    CardElement element,
    CardEffect effect,
    Optional<CardRarity> rarity) {

  public Card {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(displayName, "displayName");
    Objects.requireNonNull(tag, "tag");
    Objects.requireNonNull(element, "element");
    Objects.requireNonNull(effect, "effect");
    // rarity が null なら Optional.empty() に正規化 (Equipment.themeName / iconPath と同パターン)
    rarity = (rarity == null) ? Optional.empty() : rarity;
    if (displayName.isBlank()) {
      throw new IllegalArgumentException("displayName must not be blank");
    }
    if (apCost < 1) {
      throw new IllegalArgumentException("apCost must be >= 1 (got " + apCost + ")");
    }
  }

  /**
   * 後方互換コンストラクタ (rarity 未指定 = Optional.empty())。Wave 11 W11-β 以前の呼出 (テスト fixture / DomainFixtures /
   * Wave 10 までの本番コード) はこちらを通る。
   */
  public Card(
      CardId id,
      String displayName,
      int apCost,
      CardTag tag,
      CardElement element,
      CardEffect effect) {
    this(id, displayName, apCost, tag, element, effect, Optional.empty());
  }

  /**
   * レアリティを「未設定 = COMMON」として返す型安全な公開窓口 (CTO チェックポイント #3、Wave 11 W11-β)。
   *
   * <p>呼出側 ({@link core.presentation.screen.DungeonScreen} の drawSeFor 等) は {@code .orElse(COMMON)}
   * を毎回書く必要がない。図鑑等で「明示 COMMON」と「未設定」を区別したい場合は {@link #rarity()} (Optional) を直接使う。
   */
  public CardRarity rarityOrDefault() {
    return rarity.orElse(CardRarity.COMMON);
  }
}
