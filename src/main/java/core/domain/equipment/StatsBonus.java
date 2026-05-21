package core.domain.equipment;

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
    return new StatsBonus(
        maxHp + other.maxHp,
        speed + other.speed,
        physicalAttack + other.physicalAttack,
        magicalAttack + other.magicalAttack,
        physicalDefense + other.physicalDefense,
        magicalDefense + other.magicalDefense);
  }
}
