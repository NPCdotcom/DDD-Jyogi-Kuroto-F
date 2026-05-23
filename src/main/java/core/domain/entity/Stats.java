package core.domain.entity;

import core.domain.equipment.StatsBonus;
import java.util.Objects;

/**
 * エンティティの能力値 (§15-4)。すべての変化は新インスタンスを返す。
 *
 * <p>AP に関する状態は ActionPoints で別途管理する (責務分離)。Stats が持つのは恒常的なスペック (HP 上限・速度・4 攻防ステ) と現在 HP のみ。
 *
 * <p>4 攻防ステ (物攻/魔攻/物防/魔防) はダメージ計算で参照される。カード使用時は {@link core.domain.card.CardEffect.Damage#resolve}
 * が、スキル使用時は {@code TurnEngine.resolveSkillDamage} が 被弾側の防御 (物防/魔防) を差し引く (ADR-17 改訂:
 * スキルも固定ダメージをやめ防御を通す)。
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
      throw new IllegalArgumentException("physicalAttack must be non-negative: " + physicalAttack);
    }
    if (magicalAttack < 0) {
      throw new IllegalArgumentException("magicalAttack must be non-negative: " + magicalAttack);
    }
    if (physicalDefense < 0) {
      throw new IllegalArgumentException(
          "physicalDefense must be non-negative: " + physicalDefense);
    }
    if (magicalDefense < 0) {
      throw new IllegalArgumentException("magicalDefense must be non-negative: " + magicalDefense);
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

  /**
   * 最大 HP を {@code amount} 引き上げ、現在 HP も同量加算した新 Stats を返す純関数 (§15-8 層末ノード HP 強化用)。
   *
   * <p>「上限上昇分が即座に空きスロットを埋める」恒常強化の体験を表現する。引数順ミスをコンパイラが防げるよう、 呼出側の手組み {@code new Stats(...)} を置き換える。
   */
  public Stats withMaxHpRaised(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("amount must be non-negative: " + amount);
    }
    return new Stats(
        currentHp + amount,
        maxHp + amount,
        speed,
        physicalAttack,
        magicalAttack,
        physicalDefense,
        magicalDefense);
  }

  /**
   * 速度を {@code amount} 引き上げた新 Stats を返す純関数 (§15-8 層末ノード 速度強化用)。
   *
   * <p>速度 = 1 ターンの最大 AP (§15-4)。引数順ミスをコンパイラが防げるよう、呼出側の手組み {@code new Stats(...)} を置き換える。
   */
  public Stats withSpeedRaised(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("amount must be non-negative: " + amount);
    }
    return new Stats(
        currentHp,
        maxHp,
        speed + amount,
        physicalAttack,
        magicalAttack,
        physicalDefense,
        magicalDefense);
  }

  /**
   * 装備 / Buff の {@link StatsBonus} を加算した新 Stats を返す純関数 (§15-4 / ADR-25)。
   *
   * <p>{@code maxHp} は最低 1 を保証 (装備の負補正で 0 以下にならない)。{@code currentHp} は新 maxHp でクランプ (max
   * が下がった時に張り付かない)。speed / 4 攻防ステは 0 でクランプ (負値を弾く、コンストラクタの非負制約を満たす)。
   */
  public Stats plus(StatsBonus bonus) {
    Objects.requireNonNull(bonus, "bonus");
    int newMaxHp = Math.max(1, maxHp + bonus.maxHp());
    return new Stats(
        Math.min(newMaxHp, currentHp),
        newMaxHp,
        Math.max(0, speed + bonus.speed()),
        Math.max(0, physicalAttack + bonus.physicalAttack()),
        Math.max(0, magicalAttack + bonus.magicalAttack()),
        Math.max(0, physicalDefense + bonus.physicalDefense()),
        Math.max(0, magicalDefense + bonus.magicalDefense()));
  }
}
