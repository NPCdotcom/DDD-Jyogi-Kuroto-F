package core.infrastructure.bootstrap;

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
import java.util.List;
import java.util.Random;

/**
 * ダンジョン初期状態 (1 ラン分の DungeonState) を組み立てるファクトリ。
 *
 * <p>MVP では JSON ロードを行わず固定値で構築する (YAGNI)。マップ・敵・スキル定義を 1 か所に集めて 「驚き最小」: 何がどこに置かれるかが 1
 * ファイルで読める。動的化が必要になった段階で、ここを Repository 経由のロードに差し替える。
 */
public final class InitialStateFactory {

  private InitialStateFactory() {}

  // ----------------------------- スキルマスタ -----------------------------

  public static Skill lightSlash() {
    return new Skill(SkillId.of("light_slash"), "Light Slash", 1, new SkillEffect.Damage(5));
  }

  public static Skill heavySlash() {
    return new Skill(SkillId.of("heavy_slash"), "Heavy Slash", 3, new SkillEffect.Damage(15));
  }

  public static Skill slimeBite() {
    return new Skill(SkillId.of("slime_bite"), "Bite", 1, new SkillEffect.Damage(4));
  }

  // ----------------------------- カードマスタ -----------------------------

  /**
   * 斬撃カード (物理 / ダメージ 5 ベース)。
   *
   * <p>物攻ステに連動するため、{@link Stats#physicalAttack()} が高いほど最終ダメージが伸びる。
   */
  public static Card zangetuCard() {
    return new Card(
        CardId.of("zangeki"),
        "斬撃",
        1,
        CardTag.ATTACK,
        CardElement.PHYSICAL,
        new CardEffect.Damage(5));
  }

  /**
   * 魔法弾カード (魔法 / ダメージ 3 ベース)。
   *
   * <p>魔攻ステに連動する。
   */
  public static Card magicBoltCard() {
    return new Card(
        CardId.of("magic_bolt"),
        "魔法弾",
        2,
        CardTag.ATTACK,
        CardElement.MAGICAL,
        new CardEffect.Damage(3));
  }

  /**
   * 強打カード (物理 / ダメージ 4 ベース)。
   *
   * <p>斬撃より基礎値は低いが AP コストも 1 で使いやすい。
   */
  public static Card strongStrikeCard() {
    return new Card(
        CardId.of("strong_strike"),
        "強打",
        1,
        CardTag.ATTACK,
        CardElement.PHYSICAL,
        new CardEffect.Damage(4));
  }

  /**
   * 火球カード (魔法 / ダメージ 3 ベース)。
   *
   * <p>docs/templates/cards.json のテンプレに合わせて追加。魔法弾と同性能だが別 id で扱う (将来別効果に拡張する余地)。
   */
  public static Card fireballCard() {
    return new Card(
        CardId.of("fireball"),
        "火球",
        2,
        CardTag.ATTACK,
        CardElement.MAGICAL,
        new CardEffect.Damage(3));
  }

  // ----------------------------- マップ -----------------------------

  /** 10x10 の固定マップ。外周は壁、内部は床。右下に階段。 */
  public static final List<String> FLOOR_01 =
      List.of(
          "##########",
          "#........#",
          "#........#",
          "#........#",
          "#........#",
          "#........#",
          "#........#",
          "#........#",
          "#.......>#",
          "##########");

  // ----------------------------- ファクトリ -----------------------------

  public static DungeonState firstFloor() {
    DungeonMap map = DungeonMap.of(FLOOR_01);
    Player player = newPlayer(new Position(1, 1));
    // 階段 (8, 1) の前に 1 体、フロア中央付近にもう 1 体配置することで、
    // 戦闘を経ずに踏破するルートにも敵を絡みやすくする。
    Enemy slimeNearStairs = newSlime("slime#stairs", new Position(6, 1));
    Enemy slimeMidRoom = newSlime("slime#mid", new Position(4, 4));
    return new DungeonState(
        map, player, List.of(slimeNearStairs, slimeMidRoom), TurnPhase.PLAYER_TURN);
  }

  public static Player newPlayer(Position spawn) {
    // テスト用初期デッキ (4 枚、ADR-18 通り E-5 Equipment で動的化されるまでのハードコード)
    // docs/templates/cards.json のテンプレに対応する Damage 系カードを揃える。
    // Move/Buff/Trap カードは TurnEngine で reject されるため、ゲーム内で動作可能な
    // Damage 系のみを初期デッキに含める (テンプレに記載はあるが実装は 5/15 以降)。
    List<Card> deckCards =
        List.of(zangetuCard(), magicBoltCard(), strongStrikeCard(), fireballCard());
    // Random を 1 個に統一 (シャッフルと初期ドローで同じシードを共有、再現性確保)
    Random rng = new Random(42);
    DrawPile drawPile = DrawPile.shuffledFrom(deckCards, rng);
    CardPileState pileBase = new CardPileState(drawPile, Hand.empty(), DiscardPile.empty());
    // 初期ドロー枚数は §15-3 仕様の CardPileState.initialDrawCount でデッキ枚数から自動決定
    // (deck 1-2→deck サイズ、3-5→3、6 以上→5)。デッキ枚数を変えても仕様準拠を構造的に保証。
    int initialDraw = CardPileState.initialDrawCount(deckCards.size());
    CardPileState initialPile = pileBase.drawN(initialDraw, rng);

    return new Player(
        ActorId.of("player"),
        spawn,
        new Stats(30, 30, 3, 1, 2, 1, 1),
        ActionPoints.full(5),
        new SkillSlot(List.of(lightSlash(), heavySlash()), 4),
        Soul.zero(),
        initialPile);
  }

  public static Enemy newSlime(String id, Position spawn) {
    return new Enemy(
        ActorId.of(id),
        spawn,
        new Stats(10, 10, 2, 2, 0, 0, 0),
        ActionPoints.full(3),
        new SkillSlot(List.of(slimeBite()), 4),
        EnemyKind.SLIME);
  }
}
