package core.domain.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import core.domain.card.ActiveBuff;
import core.domain.card.CardEffect;
import core.domain.card.CardElement;
import core.domain.card.TrapLifetime;
import core.domain.common.Direction;
import core.domain.common.Position;
import core.domain.dungeon.DungeonState;
import core.domain.dungeon.PlacedTrap;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.EnemyKind;
import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.domain.skill.Skill;
import core.domain.skill.SkillEffect;
import core.domain.skill.SkillId;
import core.domain.skill.SkillSlot;
import core.domain.support.DomainFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * ADR-17 改訂の証跡テスト: 敵スキルダメージ・罠ダメージがプレイヤーの防御 (物防/魔防) を通すこと。
 *
 * <p>修正前は敵スキルが固定ダメージで、プレイヤーの物防/魔防が一切機能していなかった。
 */
class TurnEngineDefenseTest {

  private static final Position PLAYER_POS = new Position(1, 1);
  private static final Position ADJACENT_POS = new Position(2, 1); // PLAYER_POS の右隣 (床)

  // ---------------- fixture ----------------

  /** 素ステの物防/魔防だけ差し替えたプレイヤー (装備/Buff なし → effectiveStats == stats)。 */
  private static Player playerWithDefense(int physicalDefense, int magicalDefense) {
    return DomainFixtures.playerAt(PLAYER_POS)
        .withStats(new Stats(30, 30, 3, 0, 0, physicalDefense, magicalDefense));
  }

  /** 素 物防 0 + 物防 Buff を持つプレイヤー (effectiveStats().physicalDefense() == amount)。 */
  private static Player playerWithPhysicalDefenseBuff(int amount) {
    Player base = DomainFixtures.playerAt(PLAYER_POS);
    return base.withStatuses(
        base.statuses()
            .withActiveBuffs(
                List.of(new ActiveBuff(CardEffect.BuffKind.PHYSICAL_DEFENSE_UP, amount, 5))));
  }

  private static Skill physicalSkill(int amount) {
    return new Skill(SkillId.of("phys"), "物理攻撃", 1, new SkillEffect.Damage(amount));
  }

  private static Skill magicalSkill(int amount) {
    return new Skill(
        SkillId.of("mag"), "魔法攻撃", 1, new SkillEffect.Damage(amount, CardElement.MAGICAL));
  }

  /** ADJACENT_POS に居て指定スキルを持つ敵 (AP 3、スキル AP コスト 1)。 */
  private static Enemy enemyWithSkill(Skill skill) {
    return new Enemy(
        ActorId.of("attacker"),
        ADJACENT_POS,
        new Stats(10, 10, 2, 0, 0, 0, 0),
        ActionPoints.full(3),
        new SkillSlot(List.of(skill), 4),
        EnemyKind.SLIME);
  }

  private static DungeonState enemyTurnState(Player player, Enemy enemy) {
    return new DungeonState(
        DomainFixtures.squareRoom(), player, List.of(enemy), TurnPhase.ENEMY_TURN);
  }

  /** 結果イベントから最初の DamageDealt のダメージ量を取り出す。 */
  private static int playerDamage(TurnEngine.StepResult result) {
    return result.events().stream()
        .filter(e -> e instanceof BattleEvent.DamageDealt)
        .map(e -> ((BattleEvent.DamageDealt) e).damage())
        .findFirst()
        .orElseThrow(() -> new AssertionError("DamageDealt が発火していない: " + result.events()));
  }

  // ---------------- 敵スキル × 防御 ----------------

  @Test
  void enemyPhysicalSkillReducedByPlayerPhysicalDefense() {
    Player player = playerWithDefense(2, 0);
    Enemy enemy = enemyWithSkill(physicalSkill(5));
    DungeonState s = enemyTurnState(player, enemy);

    TurnEngine.StepResult result =
        TurnEngine.resolveEnemyAction(s, enemy.id(), new BattleAction.UseSkill(0));

    assertEquals(3, playerDamage(result), "物理 5 − 物防 2 = 3");
  }

  @Test
  void enemyMagicalSkillReducedByPlayerMagicalDefense() {
    Player player = playerWithDefense(0, 3);
    Enemy enemy = enemyWithSkill(magicalSkill(8));
    DungeonState s = enemyTurnState(player, enemy);

    TurnEngine.StepResult result =
        TurnEngine.resolveEnemyAction(s, enemy.id(), new BattleAction.UseSkill(0));

    assertEquals(5, playerDamage(result), "魔法 8 − 魔防 3 = 5");
  }

  @Test
  void physicalDefenseDoesNotReduceMagicalAttack() {
    // 物防を極端に高くしても、魔法属性の攻撃には魔防のみが効く。
    Player player = playerWithDefense(100, 2);
    Enemy enemy = enemyWithSkill(magicalSkill(8));
    DungeonState s = enemyTurnState(player, enemy);

    TurnEngine.StepResult result =
        TurnEngine.resolveEnemyAction(s, enemy.id(), new BattleAction.UseSkill(0));

    assertEquals(6, playerDamage(result), "魔法 8 − 魔防 2 = 6 (物防 100 は無関係)");
  }

  @Test
  void defenseFloorsDamageAtOne() {
    // 防御がダメージを上回っても最低 1 ダメージは保証される。
    Player player = playerWithDefense(100, 0);
    Enemy enemy = enemyWithSkill(physicalSkill(5));
    DungeonState s = enemyTurnState(player, enemy);

    TurnEngine.StepResult result =
        TurnEngine.resolveEnemyAction(s, enemy.id(), new BattleAction.UseSkill(0));

    assertEquals(1, playerDamage(result), "max(1, 5 − 100) = 1");
  }

  @Test
  void enemyAttackUsesPlayerEffectiveDefenseIncludingBuff() {
    // 修正 2: 敵スキルダメージはプレイヤーの effectiveStats() (装備/Buff 込み) の防御で軽減される。
    Player buffed = playerWithPhysicalDefenseBuff(4);
    assertEquals(4, buffed.effectiveStats().physicalDefense(), "前提: Buff で実効物防 4");
    assertEquals(0, buffed.stats().physicalDefense(), "前提: 素の物防は 0");
    Enemy enemy = enemyWithSkill(physicalSkill(5));
    DungeonState s = enemyTurnState(buffed, enemy);

    TurnEngine.StepResult result =
        TurnEngine.resolveEnemyAction(s, enemy.id(), new BattleAction.UseSkill(0));

    assertEquals(1, playerDamage(result), "5 − 実効物防 4 = 1 (素ステ 0 を見ていたら 5 になる)");
  }

  // ---------------- 罠 × 実効防御 (修正 4) ----------------

  @Test
  void trapDamageUsesPlayerEffectiveDefense() {
    // 修正 4: 罠ダメージもプレイヤーの実効防御 (装備/Buff 込み) で軽減される。
    Player buffed = playerWithPhysicalDefenseBuff(4);
    PlacedTrap trap =
        new PlacedTrap(ADJACENT_POS, 5, TrapLifetime.UntilStepped.INSTANCE, CardElement.PHYSICAL);
    DungeonState s =
        new DungeonState(
            DomainFixtures.squareRoom(), buffed, List.of(), TurnPhase.PLAYER_TURN, List.of(trap));

    TurnEngine.StepResult result =
        TurnEngine.resolvePlayerAction(s, new BattleAction.Move(Direction.RIGHT));

    int trapDamage =
        result.events().stream()
            .filter(e -> e instanceof BattleEvent.TrapTriggered)
            .map(e -> ((BattleEvent.TrapTriggered) e).damage())
            .findFirst()
            .orElseThrow(() -> new AssertionError("TrapTriggered 未発火: " + result.events()));
    assertEquals(1, trapDamage, "罠 5 − 実効物防 4 = max(1, 1) = 1");
  }
}
