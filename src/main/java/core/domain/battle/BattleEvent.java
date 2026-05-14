package core.domain.battle;

import core.domain.common.Position;
import core.domain.entity.ActorId;
import java.util.Objects;

/**
 * 1 ステップで発生したイベント。プレゼン層がこれを順次描画 / ログ表示する。
 *
 * <p>イベントは "起きた事実" だけを表現する (副作用なし)。状態変化は DungeonState が表現する。
 */
public sealed interface BattleEvent
    permits BattleEvent.Moved,
        BattleEvent.SkillUsed,
        BattleEvent.DamageDealt,
        BattleEvent.ActorDied,
        BattleEvent.SoulGained,
        BattleEvent.TurnPhaseChanged,
        BattleEvent.ActionRejected,
        BattleEvent.MovementGranted {

  record Moved(ActorId who, Position from, Position to) implements BattleEvent {
    public Moved {
      Objects.requireNonNull(who, "who");
      Objects.requireNonNull(from, "from");
      Objects.requireNonNull(to, "to");
    }
  }

  record SkillUsed(ActorId who, String skillName) implements BattleEvent {
    public SkillUsed {
      Objects.requireNonNull(who, "who");
      Objects.requireNonNull(skillName, "skillName");
    }
  }

  record DamageDealt(ActorId from, ActorId to, int damage, int remainingHp) implements BattleEvent {
    public DamageDealt {
      Objects.requireNonNull(from, "from");
      Objects.requireNonNull(to, "to");
      if (damage < 0) {
        throw new IllegalArgumentException("damage must be non-negative: " + damage);
      }
      if (remainingHp < 0) {
        throw new IllegalArgumentException("remainingHp must be non-negative: " + remainingHp);
      }
    }
  }

  record ActorDied(ActorId who) implements BattleEvent {
    public ActorDied {
      Objects.requireNonNull(who, "who");
    }
  }

  record SoulGained(ActorId who, int amount) implements BattleEvent {
    public SoulGained {
      Objects.requireNonNull(who, "who");
      if (amount <= 0) {
        throw new IllegalArgumentException("amount must be positive: " + amount);
      }
    }
  }

  record TurnPhaseChanged(TurnPhase newPhase) implements BattleEvent {
    public TurnPhaseChanged {
      Objects.requireNonNull(newPhase, "newPhase");
    }
  }

  record ActionRejected(ActorId who, String reason) implements BattleEvent {
    public ActionRejected {
      Objects.requireNonNull(who, "who");
      Objects.requireNonNull(reason, "reason");
    }
  }

  /**
   * 移動権付与 (§15-5 / ADR-21)。移動カードを切ったときに発火し、distance 分の連続移動権を付与する。プレゼン層はこれを拾って HUD に「移動権 残 N 歩」を表示する。
   *
   * <p>残りステップ数 (remainingSteps) は付与直後の値、つまりカード使用時点では distance と同じ。AWSD で 1 マス移動するごとに Player.pendingMoveCount
   * がデクリメントされる (本イベントは再発火しない、状態は Player record 側から読む)。
   */
  record MovementGranted(ActorId who, int remainingSteps) implements BattleEvent {
    public MovementGranted {
      Objects.requireNonNull(who, "who");
      if (remainingSteps <= 0) {
        throw new IllegalArgumentException(
            "remainingSteps must be positive: " + remainingSteps);
      }
    }
  }
}
