package core.domain.support;

import core.domain.battle.ActionPoints;
import core.domain.battle.TurnPhase;
import core.domain.card.Card;
import core.domain.card.CardEffect;
import core.domain.card.CardElement;
import core.domain.card.CardId;
import core.domain.card.CardPileState;
import core.domain.card.CardTag;
import core.domain.card.DiscardPile;
import core.domain.card.DrawPile;
import core.domain.card.Hand;
import core.domain.card.TrapLifetime;
import core.domain.common.Position;
import core.domain.dungeon.DungeonMap;
import core.domain.dungeon.DungeonState;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.EnemyKind;
import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.domain.meta.Soul;
import core.domain.skill.Skill;
import core.domain.skill.SkillEffect;
import core.domain.skill.SkillId;
import core.domain.skill.SkillSlot;
import java.util.ArrayList;
import java.util.List;

/** テスト用のドメインオブジェクトファクトリ。プロダクションコードから参照してはならない。 */
public final class DomainFixtures {

  private DomainFixtures() {}

  /** 5x5 の四角部屋。外周壁、内部すべて床。 */
  public static final List<String> SQUARE_ROOM_5X5 =
      List.of("#####", "#...#", "#...#", "#...#", "#####");

  /** 5x5 の四角部屋。中央 (2, 2) に階段。 */
  public static final List<String> ROOM_WITH_STAIRS_5X5 =
      List.of("#####", "#...#", "#.>.#", "#...#", "#####");

  public static DungeonMap squareRoom() {
    return DungeonMap.of(SQUARE_ROOM_5X5);
  }

  public static DungeonMap roomWithStairs() {
    return DungeonMap.of(ROOM_WITH_STAIRS_5X5);
  }

  public static Skill lightAttack() {
    return new Skill(SkillId.of("light"), "軽攻撃", 1, new SkillEffect.Damage(5));
  }

  public static Skill heavyAttack() {
    return new Skill(SkillId.of("heavy"), "強攻撃", 3, new SkillEffect.Damage(15));
  }

  public static Player playerAt(Position position) {
    return new Player(
        ActorId.of("p1"),
        position,
        // ADR-17: 物攻/魔攻/物防/魔防 は暫定 0 埋め。
        new Stats(30, 30, 3, 0, 0, 0, 0),
        ActionPoints.full(5),
        new SkillSlot(List.of(lightAttack(), heavyAttack()), 4),
        Soul.zero(),
        // ADR-18: 空 CardPileState で初期化 (Deck 接続は別 Issue)。
        CardPileState.empty());
  }

  /**
   * 既存 {@link #playerAt(Position)} に手札 N 枚をセットしたバリアント (ADR-18 / Issue #18 UseCard テスト用)。
   *
   * <p>手札に詰めるのは {@link #attackCard} 系の物理攻撃カード ({@code "hand-1"} 〜 {@code "hand-N"})。
   */
  public static Player playerAtWithHand(Position position, int handSize) {
    Player base = playerAt(position);
    return base.withCardPileState(
        new core.domain.card.CardPileState(
            core.domain.card.DrawPile.empty(),
            handOfSize(handSize),
            core.domain.card.DiscardPile.empty()));
  }

  public static Enemy slimeAt(Position position) {
    return slimeAt("slime#" + position.x() + "_" + position.y(), position, ActionPoints.full(3));
  }

  /** id / AP を明示的に渡せるスライム生成。複数体を区別したり AP 不足状態を作りたいときに使う。 */
  public static Enemy slimeAt(String id, Position position, ActionPoints actionPoints) {
    return new Enemy(
        ActorId.of(id),
        position,
        // ADR-17: 物攻/魔攻/物防/魔防 は暫定 0 埋め。
        new Stats(10, 10, 2, 0, 0, 0, 0),
        actionPoints,
        new SkillSlot(List.of(lightAttack()), 4),
        EnemyKind.SLIME);
  }

  public static DungeonState newStateWith(
      DungeonMap map, Player player, List<Enemy> enemies, TurnPhase phase) {
    return new DungeonState(map, player, enemies, phase);
  }

  // =========================================================
  // カードドメイン fixture (ADR-16, §15-3)
  // =========================================================

  /** AP コスト 1 の最小限の物理攻撃カード。 */
  public static Card attackCard(String id) {
    return new Card(
        CardId.of(id),
        "斬撃",
        1,
        CardTag.ATTACK,
        CardElement.PHYSICAL,
        new CardEffect.Damage(5));
  }

  /** AP コスト 2 の魔法攻撃カード。 */
  public static Card magicCard(String id) {
    return new Card(
        CardId.of(id),
        "魔法弾",
        2,
        CardTag.ATTACK,
        CardElement.MAGICAL,
        new CardEffect.Damage(8));
  }

  /** 移動カード (distance=2)。 */
  public static Card moveCard(String id) {
    return new Card(
        CardId.of(id),
        "ダッシュ",
        1,
        CardTag.MOVEMENT,
        CardElement.PHYSICAL,
        new CardEffect.Move(2));
  }

  /** 物理罠カード。 */
  public static Card trapCard(String id) {
    return new Card(
        CardId.of(id),
        "落とし穴",
        1,
        CardTag.TRAP,
        CardElement.PHYSICAL,
        new CardEffect.Trap(3, TrapLifetime.UntilStepped.INSTANCE));
  }

  /** n 枚の攻撃カードからなる DrawPile。各カード ID は "card-1" ～ "card-n"。 */
  public static DrawPile drawPileOfSize(int n) {
    List<Card> cards = new ArrayList<>();
    for (int i = 1; i <= n; i++) {
      cards.add(attackCard("card-" + i));
    }
    return new DrawPile(cards);
  }

  /** n 枚の攻撃カードからなる DiscardPile。各カード ID は "disc-1" ～ "disc-n"。 */
  public static DiscardPile discardPileOfSize(int n) {
    DiscardPile pile = DiscardPile.empty();
    for (int i = 1; i <= n; i++) {
      pile = pile.add(attackCard("disc-" + i));
    }
    return pile;
  }

  /** n 枚の攻撃カードからなる Hand。n <= Hand.MAX_SIZE であること。 */
  public static Hand handOfSize(int n) {
    Hand h = Hand.empty();
    for (int i = 1; i <= n; i++) {
      h = h.add(attackCard("hand-" + i));
    }
    return h;
  }
}
