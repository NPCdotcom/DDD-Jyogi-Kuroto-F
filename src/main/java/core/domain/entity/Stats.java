package core.domain.entity;

/**
 * エンティティの能力値。すべての変化は新インスタンスを返す。
 *
 * <p>AP に関する状態は ActionPoints で別途管理する (責務分離)。Stats が持つのは恒常的なスペック (HP 上限・速度) と現在 HP のみ。
 *
 * <p>攻撃力 (power 等) は MVP のスキルが固定ダメージのため不要 (YAGNI)。追加が必要になった段階で このレコードに新フィールドを足す。
 */
public record Stats(int currentHp, int maxHp, int speed) {

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
  }

  public boolean isAlive() {
    return currentHp > 0;
  }

  public Stats damaged(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("damage amount must be non-negative: " + amount);
    }
    return new Stats(Math.max(0, currentHp - amount), maxHp, speed);
  }

  public Stats healed(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("heal amount must be non-negative: " + amount);
    }
    return new Stats(Math.min(maxHp, currentHp + amount), maxHp, speed);
  }
}
