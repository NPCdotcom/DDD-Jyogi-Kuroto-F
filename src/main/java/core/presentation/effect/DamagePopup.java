package core.presentation.effect;

import com.badlogic.gdx.graphics.Color;
import java.util.Objects;

/**
 * ダメージポップアップ (§15-5 / E-8)。タイル中央から上方向にフェードアウトしながら浮かぶダメージ数字を表現する。
 *
 * <p>不変 record で「副作用なし、advance / cleanup は呼出側 (DungeonScreen) が新インスタンスへ差替え」する設計。
 *
 * <ul>
 *   <li>{@link #DURATION} = 1.0 秒で完全消滅 ({@link #isExpired} が true になる)
 *   <li>{@link #RISE_VELOCITY} = 60 px/s で上昇 ({@link #currentY} は age に比例)
 *   <li>{@link #alpha} は 1.0 → 0.0 へ線形フェード
 *   <li>{@link #baseColor} は被弾 (赤) / 与ダメ (白) / 暴力的 (黄) の色分けに使用
 * </ul>
 */
public record DamagePopup(
    float worldX, float worldY, int amount, float ageSeconds, Color baseColor) {

  /** ポップアップが完全に消えるまでの秒数。 */
  public static final float DURATION = 1.0f;

  /** 上昇速度 (px/秒)。 */
  public static final float RISE_VELOCITY = 60f;

  public DamagePopup {
    Objects.requireNonNull(baseColor, "baseColor");
    if (ageSeconds < 0f) {
      throw new IllegalArgumentException("ageSeconds must be non-negative: " + ageSeconds);
    }
  }

  /** delta 秒だけ時間を進めた新インスタンスを返す。 */
  public DamagePopup advanced(float delta) {
    if (delta < 0f) {
      throw new IllegalArgumentException("delta must be non-negative: " + delta);
    }
    return new DamagePopup(worldX, worldY, amount, ageSeconds + delta, baseColor);
  }

  /** {@link #DURATION} を超えていれば期限切れ。呼出側はこれを true にした popup を List から除去する。 */
  public boolean isExpired() {
    return ageSeconds >= DURATION;
  }

  /** 透明度 (1.0 → 0.0 への線形フェード、{@link #DURATION} 超過後は 0)。 */
  public float alpha() {
    return Math.max(0f, 1f - ageSeconds / DURATION);
  }

  /** 現在の Y 座標 ({@link #worldY} + age × {@link #RISE_VELOCITY} で上方向に移動)。 */
  public float currentY() {
    return worldY + ageSeconds * RISE_VELOCITY;
  }
}
