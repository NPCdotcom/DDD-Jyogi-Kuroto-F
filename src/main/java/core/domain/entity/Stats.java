package core.domain.entity;

/**
 * エンティティの能力値 (§15-4)。すべての変化は新インスタンスを返す。
 *
 * <p>AP に関する状態は ActionPoints で別途管理する (責務分離)。Stats が持つのは恒常的なスペック (HP 上限・速度・4 攻防ステ) と現在 HP のみ。
 *
 * <p>4 攻防ステ (物攻/魔攻/物防/魔防) は §15-4 のカード使用時ダメージ計算で参照される (実計算は {@link
 * core.domain.card.CardEffect.Damage#resolve} に委譲)。スキル (`SkillEffect`) は固定ダメージで、これらのステの影響を受けない (ADR-17)。
 *
 * <p>本 PR では `with*` メソッド (バフ用) は YAGNI のため追加しない。バフ適用 Issue で必要時に追加する。
 */
public record Stats(
    int currentHp,
    int maxHp,
    int speed,
    int physicalAttack,
    int magicalAttack,
    int physicalDefense,
    int magicalDefense) {

  public Stats {
    if (maxHp <= 0) {
      throw new IllegalArgumentException("maxHp must be positive: " + maxHp);
    }
    if (currentHp < 0 || currentHp > maxHp) {
      throw new IllegalArgumentException(
          "currentHp must be in [0, %d]: %d".formatted(maxHp, currentHp));
    }
    if (speed < 0) {
      throw new IllegalArgumentException("speed must be non-negative: " + speed);
    }
    if (physicalAttack < 0) {
      throw new IllegalArgumentException(
          "physicalAttack must be non-negative: " + physicalAttack);
    }
    if (magicalAttack < 0) {
      throw new IllegalArgumentException("magicalAttack must be non-negative: " + magicalAttack);
    }
    if (physicalDefense < 0) {
      throw new IllegalArgumentException(
          "physicalDefense must be non-negative: " + physicalDefense);
    }
    if (magicalDefense < 0) {
      throw new IllegalArgumentException(
          "magicalDefense must be non-negative: " + magicalDefense);
    }
  }

  public boolean isAlive() {
    return currentHp > 0;
  }

  public Stats damaged(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("damage amount must be non-negative: " + amount);
    }
    return new Stats(
        Math.max(0, currentHp - amount),
        maxHp,
        speed,
        physicalAttack,
        magicalAttack,
        physicalDefense,
        magicalDefense);
  }

  public Stats healed(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("heal amount must be non-negative: " + amount);
    }
    return new Stats(
        Math.min(maxHp, currentHp + amount),
        maxHp,
        speed,
        physicalAttack,
        magicalAttack,
        physicalDefense,
        magicalDefense);
  }
}
