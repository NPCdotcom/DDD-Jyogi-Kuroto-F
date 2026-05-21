package core.infrastructure.bootstrap;

import core.domain.battle.ActionPoints;
import core.domain.battle.TurnPhase;
import core.domain.card.Card;
import core.domain.card.CardElement;
import core.domain.card.CardId;
import core.domain.card.CardPileState;
import core.domain.card.DiscardPile;
import core.domain.card.DrawPile;
import core.domain.card.Hand;
import core.domain.common.Position;
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

  /**
   * ボス専用スキル (§15-6)。8 ダメージ (魔法属性) / AP 3。最終層ボス戦で魔防が意味を持つよう 魔法属性とする (物理スキルだけだと魔防が常に無効になるため)。実ダメージは
   * {@code max(1, 8 − 魔防)}。
   */
  public static Skill bossSlam() {
    return new Skill(
        SkillId.of("boss_slam"), "Boss Slam", 3, new SkillEffect.Damage(8, CardElement.MAGICAL));
  }

  // ----------------------------- カードマスタ (§15-3、cards.json) -----------------------------

  /**
   * カードマスタ。クラスパスの {@code cards.json} から起動時に 1 度ロードする。カード定義はハードコードせず JSON に集約し、{@link #resolveCard}
   * / {@link #cardCatalog()} 経由で参照する (チームがカードを増やす際は JSON 編集のみで済む)。
   */
  private static final CardCatalog CARD_CATALOG = CardCatalog.load();

  /** カードマスタを返す (ショップ抽選・強化個体報酬・カード図鑑が全カードを参照するのに使う)。 */
  public static CardCatalog cardCatalog() {
    return CARD_CATALOG;
  }

  // ----------------------------- 装備マスタ (§15-9 / ADR-26) -----------------------------

  /**
   * ぼろ靴 (初期装備、{@code docs/templates/equipments.json} の {@code tattered_boots} 準拠)。 speed +1、{@code
   * dash} カードを {@code grantedCards} で初期デッキに自動追加。
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
   * ぼろい短剣 (初期装備、{@code docs/templates/equipments.json} の {@code tattered_dagger} 準拠)。 物攻 +1、{@code
   * zangeki} カードを {@code grantedCards} で初期デッキに自動追加。
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
   * カード ID から {@link Card} を解決する (装備固有カード自動追加・ソウルツリーのカード解放用)。{@link #CARD_CATALOG} (cards.json)
   * に委譲する。マスタ未登録の ID は {@link IllegalArgumentException}。
   */
  public static Card resolveCard(CardId id) {
    Objects.requireNonNull(id, "id");
    return CARD_CATALOG.get(id);
  }

  // ----------------------------- ダンジョン構成 -----------------------------

  /** ラン全体の層数 (§15-6「初期 3 層」)。最終層 (= MAX_LAYER) にボスが出現する。 層数拡張 (ソウルツリー) は M2 送り、本セッションは固定 3 層。 */
  public static final int MAX_LAYER = 3;

  /** 指定の層が最終層か (§15-6 クリア条件 = 最終層ボス撃破)。最終層 (MAX_LAYER) にボスを配置する。 */
  public static boolean isFinalLayer(Layer layer) {
    return layer.number() == MAX_LAYER;
  }

  // ----------------------------- ファクトリ -----------------------------

  /**
   * 1 層目の DungeonState を組み立てる (ADR-19: Random は引数注入で再現性を呼出元に委ねる)。
   *
   * <p>本番では {@code core.presentation.screen.DddGame#startNewRun} が {@code new Random()} を渡し、
   * 毎ラン異なる初期手札シャッフルを得る。テストでは {@code new Random(42)} 等の固定シードを渡し、 再現可能性を担保する。
   */
  public static DungeonState firstFloor(Random rng) {
    Objects.requireNonNull(rng, "rng");
    Player player = newPlayer(DungeonGenerator.SPAWN, rng);
    return generateLayerState(Layer.first(), player, rng);
  }

  /**
   * 初期 Player を組み立てる (§15-9 完全実装 / ADR-25 / ADR-26 / ADR-19: Random は引数注入)。
   *
   * <p>初期装備として {@link #tatteredBoots()} (FEET) と {@link #tatteredDagger()} (HAND) を装着し、 その {@code
   * grantedCards} ({@code dash} + {@code zangeki}) を初期デッキとする。素ステに装備の {@code statsBonus} を加えた {@link
   * Player#effectiveStats()} が「物攻 1 → 2 / 速度 3 → 4」になる。
   *
   * <p>{@code rng} は初期手札シャッフルと初期ドローで共有される。同一インスタンスを渡せばシャッフル後の 山札順 → 初期ドロー結果が一意に定まる。
   */
  public static Player newPlayer(Position spawn, Random rng) {
    Objects.requireNonNull(spawn, "spawn");
    Objects.requireNonNull(rng, "rng");

    // §15-9 仕様 (1 部位スタート、ADR-30 修正): ぼろい短剣のみ装着。
    // GAME_DESIGN.md:705 「装備 1 部位スタート」準拠。
    // ぼろ靴 (dash カード付き) は Shop / Event で獲得可能、初期デッキには含めない。
    Equipment dagger = tatteredDagger();
    Map<EquipmentSlot, Equipment> equipmentMap = new HashMap<>();
    equipmentMap.put(dagger.slot(), dagger);

    List<Card> deckCards = new java.util.ArrayList<>();
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
   * 強化個体スライム生成 (§15-6 強化個体、5 層ごとに 1 体出現)。
   *
   * <p>雑魚スライムより強い: HP 20 (倍)、物攻 3 (1.5 倍)、物防 1、AP は層番号 +2 で雑魚を凌駕。 撃破時に {@link
   * core.domain.battle.BattleEvent.EliteDefeated} 発火 → プレゼン層でカード追加 UI を表示する (§15-3 強化個体撃破時のカード追加)。
   */
  public static Enemy newEliteSlimeForLayer(String id, Position spawn, int layerNumber) {
    if (layerNumber < 1) {
      throw new IllegalArgumentException("layerNumber must be >= 1: " + layerNumber);
    }
    return new Enemy(
        ActorId.of(id),
        spawn,
        new Stats(20, 20, 3, 3, 0, 1, 0),
        ActionPoints.full(layerNumber + 2),
        new SkillSlot(List.of(slimeBite()), 4),
        EnemyKind.ELITE_SLIME);
  }

  /**
   * 現在の DungeonState から次の層を生成する (§15-6)。
   *
   * <p>{@link Layer#next()} で層番号 +1。マップは {@link DungeonGenerator} で手続き生成、敵は AP = 層番号で 新規生成 (層 2
   * に強化個体、最終層にボス)。Player は持ち越し (HP/AP/手札/装備/ソウル) し位置を spawn に リセット、罠は持ち越さない。
   *
   * @param current 現在の状態 (層末ノード選択後を想定)
   * @param rng マップ生成の乱数源 (ADR-19: 引数注入)
   */
  public static DungeonState advanceLayer(DungeonState current, Random rng) {
    Objects.requireNonNull(current, "current");
    Objects.requireNonNull(rng, "rng");
    return generateLayerState(current.layer().next(), current.player(), rng);
  }

  /**
   * 指定の層の DungeonState を組み立てる (firstFloor / advanceLayer 共通)。
   *
   * <p>{@link DungeonGenerator} で BSP マップ + 配置座標を生成する。層ごとにグリッドが拡大し ({@link #gridWidthFor} / {@link
   * #gridHeightFor})、敵数も増える ({@link #enemyCountFor})。最終層は先頭 1 体をボスに、層 2 は先頭 1
   * 体を強化個体に、残りは雑魚スライムにする。Player は位置を spawn にリセットして持ち越し、罠は持ち越さない (placedTraps 空)。
   */
  private static DungeonState generateLayerState(Layer layer, Player player, Random rng) {
    int n = layer.number();
    boolean boss = isFinalLayer(layer);
    // §15-3 / §15-6: 強化個体は層 2 に出現させる (撃破で EliteDefeated → カード追加 UI のデモ用)。
    boolean hasElite = !boss && n == 2;
    DungeonGenerator.GeneratedDungeon dungeon =
        DungeonGenerator.generate(gridWidthFor(n), gridHeightFor(n), enemyCountFor(n), !boss, rng);

    List<Enemy> enemies = new java.util.ArrayList<>();
    List<Position> spawns = dungeon.enemySpawns();
    for (int i = 0; i < spawns.size(); i++) {
      Position pos = spawns.get(i);
      if (boss && i == 0) {
        // ボス層: 先頭 1 体 (spawn から最遠) をボスに。撃破で RUN_CLEARED。
        enemies.add(newBossForLayer("boss_L" + n, pos, n));
      } else if (hasElite && i == 0) {
        // 層 2: 先頭 1 体を強化個体に。撃破で EliteDefeated → カード追加 UI 発火。
        enemies.add(newEliteSlimeForLayer("elite_L" + n, pos, n));
      } else {
        enemies.add(newSlimeForLayer("slime_L" + n + "_" + i, pos, n));
      }
    }

    Player atSpawn = player.withPosition(dungeon.spawn());
    return new DungeonState(
        dungeon.map(), atSpawn, List.copyOf(enemies), TurnPhase.PLAYER_TURN, List.of(), layer);
  }

  /** 層ごとの敵数 (§15-6 難易度: 層 1=3 / 層 2=6 [雑魚 5+強化個体 1] / 層 3=8 [雑魚 7+ボス 1])。 */
  private static int enemyCountFor(int layerNumber) {
    return switch (layerNumber) {
      case 1 -> 3;
      case 2 -> 6;
      default -> 8;
    };
  }

  /** 層ごとの BSP グリッド幅 (層が深いほど床面積が拡大、§15-6)。 */
  private static int gridWidthFor(int layerNumber) {
    return switch (layerNumber) {
      case 1 -> 14;
      case 2 -> 19;
      default -> 26;
    };
  }

  /** 層ごとの BSP グリッド高さ。 */
  private static int gridHeightFor(int layerNumber) {
    return switch (layerNumber) {
      case 1 -> 12;
      case 2 -> 15;
      default -> 20;
    };
  }

  /**
   * 最終層のボスを生成する (§15-6 / §15-2)。雑魚 (HP 10) ・強化個体 (HP 20) を凌ぐ HP 45、速度 / AP = 層番号 +1、専用スキル {@link
   * #bossSlam} (魔法 8 ダメージ / AP 3、プレイヤー魔防で軽減) を持つ。撃破で {@code RUN_CLEARED} (ラン勝利)。
   *
   * <p>バランス根拠: プレイヤー (HP 30 前後・heavySlash で 15/ターン相当) が約 3 ターンで HP 45 を 削り切る一方、ボスは魔法 8 (プレイヤー魔防 1
   * なら実 7) /ターンで 4〜6 ターンかけて削る — プレイヤー先制で勝ち筋が残る値。{@code bossSlam} を魔法属性にしたため、ソウルツリー等で 魔防を上げると被ダメが減る
   * (ADR-17 改訂)。HP 60 / 15 ダメージ案は隣接戦が数学的に成立しなかったため下方修正。
   *
   * @param layerNumber 1 以上の階層番号 (速度 / AP の算出に使う)
   */
  public static Enemy newBossForLayer(String id, Position spawn, int layerNumber) {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(spawn, "spawn");
    if (layerNumber < 1) {
      throw new IllegalArgumentException("layerNumber must be >= 1: " + layerNumber);
    }
    return new Enemy(
        ActorId.of(id),
        spawn,
        new Stats(45, 45, layerNumber + 1, 5, 0, 1, 0),
        ActionPoints.full(layerNumber + 1),
        new SkillSlot(List.of(bossSlam()), 4),
        EnemyKind.BOSS);
  }
}
