package core.domain.card;

import java.util.Objects;

/**
 * アクティブな Buff の戦闘中状態 (§15-3 / ADR-25 / ADR-27)。
 *
 * <p>{@link CardEffect.Buff} がカードのデータ定義 (kind / amount / durationTurns) なのに対し、本 record は戦闘中に Player
 * に 付与されたあとの「残ターン数」を含む動的状態。{@code PlayerStatuses.activeBuffs} に保持される (ADR-27)。
 *
 * <p>ターン経過: {@link #decrementedTurn()} で残ターン -1、0 になったら除去 ({@link #isExpired()})。 重複 {@link
 * CardEffect.BuffKind} は加算合成ではなく上書き (KISS、ADR-22 罠の上書きパターン踏襲、ADR-27)。
 */
public record ActiveBuff(CardEffect.BuffKind kind, int amount, int remainingTurns) {

  public ActiveBuff {
    Objects.requireNonNull(kind, "kind");
    if (amount == 0) {
      throw new IllegalArgumentException("amount must not be 0");
    }
    if (remainingTurns < 0) {
      throw new IllegalArgumentException("remainingTurns must be non-negative: " + remainingTurns);
    }
  }

  /** 残ターンを 1 減らした新インスタンスを返す (0 でクランプ)。 */
  public ActiveBuff decrementedTurn() {
    return new ActiveBuff(kind, amount, Math.max(0, remainingTurns - 1));
  }

  /** 残ターンが 0 なら期限切れ。 */
  public boolean isExpired() {
    return remainingTurns == 0;
  }
}
