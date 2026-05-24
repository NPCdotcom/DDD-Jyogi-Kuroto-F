package core.domain.equipment;

import core.domain.entity.StatKind;
import java.util.Objects;

/**
 * ステ補正の差分値 (§15-9 / ADR-25)。装備の {@code statsBonus} と Buff 合算結果を表現する共通型。
 *
 * <p>{@link core.domain.entity.Stats} 本体の 7 フィールドのうち、装備で増えると整合性が崩れる {@code currentHp} を除く 6
 * フィールドを保持する。 全フィールドは 0 を許容 (装備によって特定ステだけ +1 する単発バフを表現可能)。負値も許容 (将来「呪い装備」「デバフ」を同型で扱う余地)。
 *
 * <p>{@link #zero()} で「補正なし」を表現する不変インスタンスを返す。
 */
public record StatsBonus(
    int maxHp,
    int speed,
    int physicalAttack,
    int magicalAttack,
    int physicalDefense,
    int magicalDefense) {

  private static final StatsBonus ZERO = new StatsBonus(0, 0, 0, 0, 0, 0);

  /** 全フィールド 0 の補正なしインスタンス。 */
  public static StatsBonus zero() {
    return ZERO;
  }

  /** 別の {@link StatsBonus} を加算した新インスタンスを返す純関数。 */
  public StatsBonus plus(StatsBonus other) {
    Objects.requireNonNull(other, "other");
    return new StatsBonus(
        maxHp + other.maxHp,
        speed + other.speed,
        physicalAttack + other.physicalAttack,
        magicalAttack + other.magicalAttack,
        physicalDefense + other.physicalDefense,
        magicalDefense + other.magicalDefense);
  }

  /**
   * 最大補正値を持つステ種別を返す。同値時の優先順は HP → SPEED → PHYS_ATK → MAG_ATK → PHYS_DEF → MAG_DEF。 全フィールド 0 の場合は HP
   * を返す (装備テーマ判定で「最も特徴のあるステ」を取り出す用途、文字列マッチ回避)。
   */
  public StatKind dominantStat() {
    StatKind best = StatKind.HP;
    int bestValue = maxHp;
    if (speed > bestValue) {
      best = StatKind.SPEED;
      bestValue = speed;
    }
    if (physicalAttack > bestValue) {
      best = StatKind.PHYSICAL_ATTACK;
      bestValue = physicalAttack;
    }
    if (magicalAttack > bestValue) {
      best = StatKind.MAGICAL_ATTACK;
      bestValue = magicalAttack;
    }
    if (physicalDefense > bestValue) {
      best = StatKind.PHYSICAL_DEFENSE;
      bestValue = physicalDefense;
    }
    if (magicalDefense > bestValue) {
      best = StatKind.MAGICAL_DEFENSE;
    }
    return best;
  }
}
