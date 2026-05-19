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
import core.domain.card.TrapLifetime;
import core.domain.common.Position;
import core.domain.dungeon.DungeonMap;
import core.domain.dungeon.DungeonState;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.EnemyKind;
import core.domain.entity.Player;
import core.domain.entity.PlayerStatuses;
import core.domain.entity.Stats;
import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentId;
import core.domain.equipment.EquipmentSlot;
import core.domain.equipment.StatsBonus;
import core.domain.layer.Layer;
import core.domain.meta.Gold;
import core.domain.meta.Soul;
import core.domain.skill.Skill;
import core.domain.skill.SkillEffect;
import core.domain.skill.SkillId;
import core.domain.skill.SkillSlot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

  // ----- チームメイト提案カード (ADR-24、本 PR で Java マスター登録、初期デッキには含めず将来 -----
  // ----- ショップ販売・強化個体撃破時の選択肢・装備固有カード解放で参照する素材として確保) -----

  /** 炎弾カード (魔法 / ダメージ 2 ベース、AP 1)。軽量連射型、AP 効率 2.0。 */
  public static Card emberShotCard() {
    return new Card(
        CardId.of("ember_shot"),
        "炎弾",
        1,
        CardTag.ATTACK,
        CardElement.MAGICAL,
        new CardEffect.Damage(2));
  }

  /** 爆炎カード (魔法 / ダメージ 6 ベース、AP 3)。1 ターン全力フィニッシャー、AP 効率 2.0。 */
  public static Card blazeNovaCard() {
    return new Card(
        CardId.of("blaze_nova"),
        "爆炎",
        3,
        CardTag.ATTACK,
        CardElement.MAGICAL,
        new CardEffect.Damage(6));
  }

  /** 瞬歩カード (魔法移動 / 距離 3、AP 1)。長距離移動、ADR-21 移動 α 案で動作。 */
  public static Card blinkStepCard() {
    return new Card(
        CardId.of("blink_step"),
        "瞬歩",
        1,
        CardTag.MOVEMENT,
        CardElement.MAGICAL,
        new CardEffect.Move(3));
  }

  /** 炎の魔法陣カード (魔法罠 / ダメージ 3 / 4 ターン残、AP 2)。Turns 型ライフタイム、ADR-22 で動作。 */
  public static Card flameCircleCard() {
    return new Card(
        CardId.of("flame_circle"),
        "炎の魔法陣",
        2,
        CardTag.TRAP,
        CardElement.MAGICAL,
        new CardEffect.Trap(3, new TrapLifetime.Turns(4)));
  }

  /**
   * ダッシュカード (物理移動 / 距離 2、AP 1)。ぼろ靴の {@code grantedCards} 経由で初期デッキに自動追加される
   * (§15-9 / ADR-26)。{@code docs/templates/cards.json} の {@code dash} エントリ準拠。
   */
  public static Card dashCard() {
    return new Card(
        CardId.of("dash"),
        "ダッシュ",
        1,
        CardTag.MOVEMENT,
        CardElement.PHYSICAL,
        new CardEffect.Move(2));
  }

  /**
   * 鉄の皮膚カード (物理 Buff / 物防 +2 / 3 ターン残、AP 1)。{@code docs/templates/cards.json} の
   * {@code iron_skin} エントリ準拠 (§15-3 / ADR-27)。
   */
  public static Card ironSkinCard() {
    return new Card(
        CardId.of("iron_skin"),
        "鉄の皮膚",
        1,
        CardTag.BUFF,
        CardElement.PHYSICAL,
        new CardEffect.Buff(CardEffect.BuffKind.PHYSICAL_DEFENSE_UP, 2, 3));
  }

  /**
   * 魔力の帳カード (魔法 Buff / 魔防 +3 / 2 ターン残、AP 2)。{@code docs/templates/cards.json} の
   * {@code arcane_veil} エントリ準拠 (§15-3 / ADR-27)。
   */
  public static Card arcaneVeilCard() {
    return new Card(
        CardId.of("arcane_veil"),
        "魔力の帳",
        2,
        CardTag.BUFF,
        CardElement.MAGICAL,
        new CardEffect.Buff(CardEffect.BuffKind.MAGICAL_DEFENSE_UP, 3, 2));
  }

  // ----------------------------- 装備マスタ (§15-9 / ADR-26) -----------------------------

  /**
   * ぼろ靴 (初期装備、{@code docs/templates/equipments.json} の {@code tattered_boots} 準拠)。
   * speed +1、{@link #dashCard()} を {@code grantedCards} で初期デッキに自動追加。
   */
  public static Equipment tatteredBoots() {
    return new Equipment(
        EquipmentId.of("tattered_boots"),
        "ぼろ靴",
        EquipmentSlot.FEET,
        new StatsBonus(0, 1, 0, 0, 0, 0),
        List.of(CardId.of("dash")));
  }

  /**
   * ぼろい短剣 (初期装備、{@code docs/templates/equipments.json} の {@code tattered_dagger} 準拠)。
   * 物攻 +1、{@link #zangetuCard()} (id="zangeki") を {@code grantedCards} で初期デッキに自動追加。
   */
  public static Equipment tatteredDagger() {
    return new Equipment(
        EquipmentId.of("tattered_dagger"),
        "ぼろい短剣",
        EquipmentSlot.HAND,
        new StatsBonus(0, 0, 1, 0, 0, 0),
        List.of(CardId.of("zangeki")));
  }

  // ----------------------------- カード ID → Card 解決 (§15-9 / ADR-26) -----------------------------

  /**
   * カード ID から {@link Card} を解決する (装備固有カード自動追加用)。マスタ未登録の ID は
   * {@link IllegalArgumentException}。
   *
   * <p>本セッションの装備固有カードは {@code dash} (ぼろ靴) と {@code zangeki} (ぼろい短剣) の 2 種のみ。
   * 将来のショップ販売 / 強化個体撃破時の選択肢で他カードが装備される場合、本メソッドに分岐を追加する。
   */
  public static Card resolveCard(CardId id) {
    Objects.requireNonNull(id, "id");
    return switch (id.value()) {
      case "dash" -> dashCard();
      case "zangeki" -> zangetuCard();
      case "magic_bolt" -> magicBoltCard();
      case "strong_strike" -> strongStrikeCard();
      case "fireball" -> fireballCard();
      case "ember_shot" -> emberShotCard();
      case "blaze_nova" -> blazeNovaCard();
      case "blink_step" -> blinkStepCard();
      case "flame_circle" -> flameCircleCard();
      case "iron_skin" -> ironSkinCard();
      case "arcane_veil" -> arcaneVeilCard();
      default ->
          throw new IllegalArgumentException("Unknown CardId for resolution: " + id.value());
    };
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

  /**
   * 1 層目の DungeonState を組み立てる (ADR-19: Random は引数注入で再現性を呼出元に委ねる)。
   *
   * <p>本番では {@code core.presentation.screen.DddGame#startNewRun} が {@code new Random()} を渡し、
   * 毎ラン異なる初期手札シャッフルを得る。テストでは {@code new Random(42)} 等の固定シードを渡し、
   * 再現可能性を担保する。
   */
  public static DungeonState firstFloor(Random rng) {
    Objects.requireNonNull(rng, "rng");
    DungeonMap map = DungeonMap.of(FLOOR_01);
    Player player = newPlayer(new Position(1, 1), rng);
    // 階段 (8, 1) の前に 1 体、フロア中央付近にもう 1 体配置することで、
    // 戦闘を経ずに踏破するルートにも敵を絡みやすくする。
    Enemy slimeNearStairs = newSlime("slime#stairs", new Position(6, 1));
    Enemy slimeMidRoom = newSlime("slime#mid", new Position(4, 4));
    return new DungeonState(
        map, player, List.of(slimeNearStairs, slimeMidRoom), TurnPhase.PLAYER_TURN);
  }

  /**
   * 初期 Player を組み立てる (§15-9 完全実装 / ADR-25 / ADR-26 / ADR-19: Random は引数注入)。
   *
   * <p>初期装備として {@link #tatteredBoots()} (FEET) と {@link #tatteredDagger()} (HAND) を装着し、
   * その {@code grantedCards} ({@code dash} + {@code zangeki}) を初期デッキとする。素ステに装備の
   * {@code statsBonus} を加えた {@link Player#effectiveStats()} が「物攻 1 → 2 / 速度 3 → 4」になる。
   *
   * <p>{@code rng} は初期手札シャッフルと初期ドローで共有される。同一インスタンスを渡せばシャッフル後の
   * 山札順 → 初期ドロー結果が一意に定まる。
   */
  public static Player newPlayer(Position spawn, Random rng) {
    Objects.requireNonNull(spawn, "spawn");
    Objects.requireNonNull(rng, "rng");

    // §15-9 / ADR-26: 装備固有カードを動的にデッキ化。ハードコード 4 枚は ADR-18 で予告したとおり
    // E-5 で削除し、装備の grantedCards から動的生成 (装備外し = カード削除のメカニズム整合)。
    Equipment boots = tatteredBoots();
    Equipment dagger = tatteredDagger();
    Map<EquipmentSlot, Equipment> equipmentMap = new HashMap<>();
    equipmentMap.put(boots.slot(), boots);
    equipmentMap.put(dagger.slot(), dagger);

    List<Card> deckCards = new java.util.ArrayList<>();
    for (CardId cid : boots.grantedCards()) {
      deckCards.add(resolveCard(cid));
    }
    for (CardId cid : dagger.grantedCards()) {
      deckCards.add(resolveCard(cid));
    }

    DrawPile drawPile = DrawPile.shuffledFrom(deckCards, rng);
    CardPileState pileBase = new CardPileState(drawPile, Hand.empty(), DiscardPile.empty());
    // 初期ドロー枚数は §15-3 仕様の CardPileState.initialDrawCount でデッキ枚数から自動決定
    // (deck 1-2→deck サイズ、3-5→3、6 以上→5)。デッキ枚数を変えても仕様準拠を構造的に保証。
    int initialDraw = CardPileState.initialDrawCount(deckCards.size());
    CardPileState initialPile = pileBase.drawN(initialDraw, rng);

    return new Player(
        ActorId.of("player"),
        spawn,
        Soul.zero(),
        Gold.zero(),
        new PlayerStatuses(
            new Stats(30, 30, 3, 1, 2, 1, 1),
            ActionPoints.full(5),
            new SkillSlot(List.of(lightSlash(), heavySlash()), 4),
            equipmentMap,
            List.of()),
        initialPile,
        0);
  }

  /**
   * 指定層番号で敵 AP を強化したスライムを生成する (ADR-06 / ADR-23: 敵 AP = 層番号 N)。
   *
   * @param layerNumber 1 以上の階層番号。1 → AP 1、2 → AP 2、...
   */
  public static Enemy newSlimeForLayer(String id, Position spawn, int layerNumber) {
    if (layerNumber < 1) {
      throw new IllegalArgumentException("layerNumber must be >= 1: " + layerNumber);
    }
    return new Enemy(
        ActorId.of(id),
        spawn,
        new Stats(10, 10, 2, 2, 0, 0, 0),
        ActionPoints.full(layerNumber),
        new SkillSlot(List.of(slimeBite()), 4),
        EnemyKind.SLIME);
  }

  /**
   * 既存呼出互換 (1 層相当の AP = 1 のスライム)。新規コードは {@link #newSlimeForLayer} を使う。
   *
   * <p>※ 元 MVP では AP 3 だったが、ADR-06 で「敵 AP = 層番号」と確定したため、本ファクトリでも 1 層用は AP 1 とする。これにより firstFloor の
   * 初期難度が下がるが、ADR-06 / §15-5 の正しい挙動。
   */
  public static Enemy newSlime(String id, Position spawn) {
    return newSlimeForLayer(id, spawn, 1);
  }

  /**
   * 現在の DungeonState から次の階層を生成する (§15-6 / ADR-23)。
   *
   * <p>処理:
   *
   * <ol>
   *   <li>{@link Layer#next()} で層番号 +1
   *   <li>同じマップを流用 (将来 FLOOR_02 等を別途定義する余地)
   *   <li>Player は持ち越し (HP/AP/手札/装備/ソウル/pendingMoveCount すべて)、ただし位置を初期スポーン (1, 1) にリセット
   *   <li>敵を新規生成 (AP = 次層番号、ADR-06)
   *   <li>罠は層間で持ち越さない (placedTraps = 空)
   *   <li>TurnPhase = PLAYER_TURN
   * </ol>
   *
   * @param current 現在の状態 (CLEARED 直前 / 直後を想定)
   * @return 次層の DungeonState
   */
  public static DungeonState advanceLayer(DungeonState current) {
    Layer nextLayer = current.layer().next();
    int apForLayer = nextLayer.number();
    Enemy slimeNearStairs =
        newSlimeForLayer("slime_L" + nextLayer.number() + "_a", new Position(6, 1), apForLayer);
    Enemy slimeMidRoom =
        newSlimeForLayer("slime_L" + nextLayer.number() + "_b", new Position(4, 4), apForLayer);
    Player carriedPlayer = current.player().withPosition(new Position(1, 1));
    return new DungeonState(
        current.map(),
        carriedPlayer,
        List.of(slimeNearStairs, slimeMidRoom),
        TurnPhase.PLAYER_TURN,
        List.of(),
        nextLayer);
  }

}
