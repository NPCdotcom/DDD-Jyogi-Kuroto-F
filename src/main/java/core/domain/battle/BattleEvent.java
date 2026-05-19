package core.domain.battle;

import core.domain.card.CardEffect;
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
        BattleEvent.GoldGained,
        BattleEvent.TurnPhaseChanged,
        BattleEvent.ActionRejected,
        BattleEvent.MovementGranted,
        BattleEvent.TrapPlaced,
        BattleEvent.TrapTriggered,
        BattleEvent.FloorAdvanced,
        BattleEvent.BuffApplied,
        BattleEvent.EliteDefeated {

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

  /**
   * 金貨獲得通知 (§15-2 / §15-9 Shop の前提)。敵撃破時に {@link SoulGained} と並列に発火する。
   * HUD ログでは「{プレイヤー} は {N} 金貨を獲得」のように表示。
   */
  record GoldGained(ActorId who, int amount) implements BattleEvent {
    public GoldGained {
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

  /**
   * 罠設置通知 (§15-3 / ADR-22)。プレイヤーが Trap カードを使った瞬間に発火、HUD で「{プレイヤー} が ({x},{y}) に罠を設置」を表示する。
   *
   * <p>position は設置先タイル、baseValue は最終ダメ計算の基礎値。element 情報は PlacedTrap (DungeonState.placedTraps) で保持しており、
   * 本イベントには含めない (HUD ログでは設置を通知するだけで、属性はマップ上の罠アイコンや描画で表現する想定)。
   */
  record TrapPlaced(ActorId placer, Position position, int baseValue) implements BattleEvent {
    public TrapPlaced {
      Objects.requireNonNull(placer, "placer");
      Objects.requireNonNull(position, "position");
      if (baseValue < 1) {
        throw new IllegalArgumentException("baseValue must be >= 1: " + baseValue);
      }
    }
  }

  /**
   * 罠発動通知 (§15-3 / ADR-22)。敵 or プレイヤーが罠タイルに進入した瞬間に発火、ダメージ後の HP も含めて HUD に通知する。
   *
   * <p>victim = 踏んだエンティティ、damage = 最終ダメージ、remainingHp = 適用後 HP。
   */
  record TrapTriggered(ActorId victim, Position position, int damage, int remainingHp)
      implements BattleEvent {
    public TrapTriggered {
      Objects.requireNonNull(victim, "victim");
      Objects.requireNonNull(position, "position");
      if (damage < 1) {
        throw new IllegalArgumentException("damage must be >= 1: " + damage);
      }
      if (remainingHp < 0) {
        throw new IllegalArgumentException("remainingHp must be non-negative: " + remainingHp);
      }
    }
  }

  /**
   * フロア進行通知 (§15-6 / ADR-23)。CLEARED 状態のプレイヤーが次フロアへ遷移した瞬間に発火。
   *
   * <p>newLayer は遷移後の層番号 (2 以上、1 → 2 が最小遷移)。HUD ログでは「2 層に到達」のように表示する。
   */
  record FloorAdvanced(int newLayer) implements BattleEvent {
    public FloorAdvanced {
      if (newLayer < 2) {
        throw new IllegalArgumentException(
            "newLayer must be >= 2 (advance from 1 to 2 minimum): " + newLayer);
      }
    }
  }

  /**
   * Buff カード適用通知 (§15-3 / ADR-27)。プレイヤーが Buff カードを使った瞬間に発火し、
   * {@code PlayerStatuses.activeBuffs} に新規追加された {@link core.domain.card.ActiveBuff} を伝える。
   *
   * <p>{@code who} = 適用先 (現状 Player のみ、Enemy Buff は M2 送り)、{@code kind} = どのステを増減するか、
   * {@code amount} = 増減量 (負値はデバフ)、{@code remainingTurns} = 適用直後の残ターン数。
   *
   * <p>HUD ログでは「{プレイヤー} が {物攻 +2} (残 3 ターン)」のように表示する。
   */
  record BuffApplied(ActorId who, CardEffect.BuffKind kind, int amount, int remainingTurns)
      implements BattleEvent {
    public BuffApplied {
      Objects.requireNonNull(who, "who");
      Objects.requireNonNull(kind, "kind");
      if (amount == 0) {
        throw new IllegalArgumentException("amount must not be 0");
      }
      if (remainingTurns < 0) {
        throw new IllegalArgumentException(
            "remainingTurns must be non-negative: " + remainingTurns);
      }
    }
  }

  /**
   * 強化個体 (ELITE) 撃破通知 (§15-3 / §15-6)。雑魚と区別して、プレゼン層でカード追加 UI
   * (3 提示から 1 選択) を発火するトリガとして使う。{@link ActorDied} と並列に発火される。
   */
  record EliteDefeated(ActorId who) implements BattleEvent {
    public EliteDefeated {
      Objects.requireNonNull(who, "who");
    }
  }
}
