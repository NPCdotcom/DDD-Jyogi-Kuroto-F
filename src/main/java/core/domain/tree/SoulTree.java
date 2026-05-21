package core.domain.tree;

import core.domain.card.Card;
import core.domain.card.CardId;
import core.domain.entity.Player;
import core.domain.equipment.StatsBonus;
import core.domain.meta.Soul;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * ソウルツリー (§15-7 / E-2)。永続強化のメタ進行を表現する不変 record。
 *
 * <p>構成: 23 ノード (中央 root + 6 ステ軸 Lv1 + 6 ステ軸 Lv2 + 派生軸 1 カード獲得 5 ノード + 派生軸 2 枠拡張 5 ノード)。 段階的開示
 * ({@link #isVisible}): 前提ノードを解放すると次のノードが画面に現れる。
 *
 * <p>ノードコスト (§15-7 仕様):
 *
 * <ul>
 *   <li>root: 0 (初期解放、入口)
 *   <li>HP +5: 20、速度 +1: 35、物攻 +1: 25、魔攻 +1: 25、物防 +1: 20、魔防 +1: 20
 *   <li>カード獲得: 30〜50
 *   <li>枠拡張: 40
 * </ul>
 *
 * <p>{@code unlockedNodes} に root が常に含まれる (新規 {@link #empty()} で root のみ解放済み)。 {@link
 * #unlock(NodeId, Soul, Function)} で前提ノード解放済み + Soul 残量を検証して新インスタンスを返す (純関数)。{@link #reset()} で
 * root だけに戻し、{@link #totalSpentSoul()} を呼出側で全額返却する想定 (ADR-09)。
 *
 * <p>{@link #applyTo(Player, Function)} はラン開始時に呼ぶ: 解放済みノードの効果を順番に Player に適用し、 装備や Buff と並んで Player
 * の能力値に反映する (ADR-25 / §15-9 と整合)。
 */
public record SoulTree(Set<NodeId> unlockedNodes, int totalSpentSoul) {

  public static final NodeId ROOT = NodeId.of("root");

  public SoulTree {
    Objects.requireNonNull(unlockedNodes, "unlockedNodes");
    if (totalSpentSoul < 0) {
      throw new IllegalArgumentException("totalSpentSoul must be non-negative: " + totalSpentSoul);
    }
    if (!unlockedNodes.contains(ROOT)) {
      throw new IllegalArgumentException("unlockedNodes must contain ROOT");
    }
    unlockedNodes = Set.copyOf(unlockedNodes);
  }

  /** root だけ解放済み、累計消費 0 の初期状態を返す。 */
  public static SoulTree empty() {
    return new SoulTree(Set.of(ROOT), 0);
  }

  /** 全 23 ノードのマスタ定義 (登録順保持・不変)。起動時に 1 度だけ構築しキャッシュする。 */
  private static final Map<NodeId, TreeNode> ALL_NODES = buildAllNodes();

  /**
   * 全 23 ノードのマスタ定義を返す (id → TreeNode、登録順保持の不変マップ)。
   *
   * <p>UI 側 (SoulTreeScreen) もここから配置情報を引く。{@link #isVisible} 経由で毎フレーム多数回呼ばれるため {@link #ALL_NODES}
   * にキャッシュ済み (呼出のたびに再構築しない)。座標は presentation 層で static に定義する。
   */
  public static Map<NodeId, TreeNode> allNodes() {
    return ALL_NODES;
  }

  private static Map<NodeId, TreeNode> buildAllNodes() {
    Map<NodeId, TreeNode> nodes = new LinkedHashMap<>();
    // 中央 (入口、効果なし、コスト 0)
    nodes.put(ROOT, new TreeNode(ROOT, "中央", 0, NodeEffect.None.INSTANCE, Set.of()));

    // 6 ステ軸 (root から放射状、§15-7 のメイン軸)
    // §15-7 仕様値 (ADR-30 で修正): HP+5 = 6 / 速度+1 = 35 / 物攻+1 = 5 / 魔攻+1 = 5 /
    // 物防+1 = 4 / 魔防+1 = 4 ソウル。雑魚撃破 Soul 1 (ADR-30) と組み合わせて 1 ラン 10〜15 ソウル
    // で 1〜2 ノード解放できるバランスを目指す。
    nodes.put(
        NodeId.of("hp_up_1"),
        new TreeNode(
            NodeId.of("hp_up_1"),
            "HP +5",
            6,
            new NodeEffect.StatsBonusEffect(new StatsBonus(5, 0, 0, 0, 0, 0)),
            Set.of(ROOT)));
    nodes.put(
        NodeId.of("speed_up_1"),
        new TreeNode(
            NodeId.of("speed_up_1"),
            "速度 +1",
            35,
            new NodeEffect.StatsBonusEffect(new StatsBonus(0, 1, 0, 0, 0, 0)),
            Set.of(ROOT)));
    nodes.put(
        NodeId.of("phys_atk_up_1"),
        new TreeNode(
            NodeId.of("phys_atk_up_1"),
            "物攻 +1",
            5,
            new NodeEffect.StatsBonusEffect(new StatsBonus(0, 0, 1, 0, 0, 0)),
            Set.of(ROOT)));
    nodes.put(
        NodeId.of("mag_atk_up_1"),
        new TreeNode(
            NodeId.of("mag_atk_up_1"),
            "魔攻 +1",
            5,
            new NodeEffect.StatsBonusEffect(new StatsBonus(0, 0, 0, 1, 0, 0)),
            Set.of(ROOT)));
    nodes.put(
        NodeId.of("phys_def_up_1"),
        new TreeNode(
            NodeId.of("phys_def_up_1"),
            "物防 +1",
            4,
            new NodeEffect.StatsBonusEffect(new StatsBonus(0, 0, 0, 0, 1, 0)),
            Set.of(ROOT)));
    nodes.put(
        NodeId.of("mag_def_up_1"),
        new TreeNode(
            NodeId.of("mag_def_up_1"),
            "魔防 +1",
            4,
            new NodeEffect.StatsBonusEffect(new StatsBonus(0, 0, 0, 0, 0, 1)),
            Set.of(ROOT)));

    // 派生軸 1: カード獲得 5 ノード (ステ軸の先、§15-7 派生軸)
    nodes.put(
        NodeId.of("card_grant_strong_strike"),
        new TreeNode(
            NodeId.of("card_grant_strong_strike"),
            "強打 +1 枚",
            30,
            new NodeEffect.CardGrantEffect(CardId.of("strong_strike")),
            Set.of(NodeId.of("phys_atk_up_1"))));
    nodes.put(
        NodeId.of("card_grant_fireball"),
        new TreeNode(
            NodeId.of("card_grant_fireball"),
            "火球 +1 枚",
            40,
            new NodeEffect.CardGrantEffect(CardId.of("fireball")),
            Set.of(NodeId.of("mag_atk_up_1"))));
    nodes.put(
        NodeId.of("card_grant_magic_bolt"),
        new TreeNode(
            NodeId.of("card_grant_magic_bolt"),
            "魔法弾 +1 枚",
            30,
            new NodeEffect.CardGrantEffect(CardId.of("magic_bolt")),
            Set.of(NodeId.of("mag_atk_up_1"))));
    nodes.put(
        NodeId.of("card_grant_iron_skin"),
        new TreeNode(
            NodeId.of("card_grant_iron_skin"),
            "鉄の皮膚 +1 枚",
            50,
            new NodeEffect.CardGrantEffect(CardId.of("iron_skin")),
            Set.of(NodeId.of("phys_def_up_1"))));
    nodes.put(
        NodeId.of("card_grant_arcane_veil"),
        new TreeNode(
            NodeId.of("card_grant_arcane_veil"),
            "魔力の帳 +1 枚",
            50,
            new NodeEffect.CardGrantEffect(CardId.of("arcane_veil")),
            Set.of(NodeId.of("mag_def_up_1"))));

    // 派生軸 2: 枠拡張 5 ノード (派生軸 1 の先、§15-7 派生軸)
    nodes.put(
        NodeId.of("slot_expand_1"),
        new TreeNode(
            NodeId.of("slot_expand_1"),
            "スキル枠 +1",
            40,
            new NodeEffect.SlotExpandEffect(1),
            Set.of(NodeId.of("card_grant_strong_strike"))));
    nodes.put(
        NodeId.of("slot_expand_2"),
        new TreeNode(
            NodeId.of("slot_expand_2"),
            "スキル枠 +1",
            40,
            new NodeEffect.SlotExpandEffect(1),
            Set.of(NodeId.of("card_grant_fireball"))));
    nodes.put(
        NodeId.of("slot_expand_3"),
        new TreeNode(
            NodeId.of("slot_expand_3"),
            "スキル枠 +1",
            40,
            new NodeEffect.SlotExpandEffect(1),
            Set.of(NodeId.of("card_grant_magic_bolt"))));
    nodes.put(
        NodeId.of("slot_expand_4"),
        new TreeNode(
            NodeId.of("slot_expand_4"),
            "スキル枠 +1",
            40,
            new NodeEffect.SlotExpandEffect(1),
            Set.of(NodeId.of("card_grant_iron_skin"))));
    nodes.put(
        NodeId.of("slot_expand_5"),
        new TreeNode(
            NodeId.of("slot_expand_5"),
            "スキル枠 +1",
            40,
            new NodeEffect.SlotExpandEffect(1),
            Set.of(NodeId.of("card_grant_arcane_veil"))));

    // 派生軸 3: 各ステ軸の Lv2 強化 (Lv1 の先、§15-7 充実化)。Lv1 より強い補正・高コスト。
    nodes.put(
        NodeId.of("hp_up_2"),
        new TreeNode(
            NodeId.of("hp_up_2"),
            "HP +10",
            14,
            new NodeEffect.StatsBonusEffect(new StatsBonus(10, 0, 0, 0, 0, 0)),
            Set.of(NodeId.of("hp_up_1"))));
    nodes.put(
        NodeId.of("speed_up_2"),
        new TreeNode(
            NodeId.of("speed_up_2"),
            "速度 +1",
            60,
            new NodeEffect.StatsBonusEffect(new StatsBonus(0, 1, 0, 0, 0, 0)),
            Set.of(NodeId.of("speed_up_1"))));
    nodes.put(
        NodeId.of("phys_atk_up_2"),
        new TreeNode(
            NodeId.of("phys_atk_up_2"),
            "物攻 +2",
            12,
            new NodeEffect.StatsBonusEffect(new StatsBonus(0, 0, 2, 0, 0, 0)),
            Set.of(NodeId.of("phys_atk_up_1"))));
    nodes.put(
        NodeId.of("mag_atk_up_2"),
        new TreeNode(
            NodeId.of("mag_atk_up_2"),
            "魔攻 +2",
            12,
            new NodeEffect.StatsBonusEffect(new StatsBonus(0, 0, 0, 2, 0, 0)),
            Set.of(NodeId.of("mag_atk_up_1"))));
    nodes.put(
        NodeId.of("phys_def_up_2"),
        new TreeNode(
            NodeId.of("phys_def_up_2"),
            "物防 +2",
            10,
            new NodeEffect.StatsBonusEffect(new StatsBonus(0, 0, 0, 0, 2, 0)),
            Set.of(NodeId.of("phys_def_up_1"))));
    nodes.put(
        NodeId.of("mag_def_up_2"),
        new TreeNode(
            NodeId.of("mag_def_up_2"),
            "魔防 +2",
            10,
            new NodeEffect.StatsBonusEffect(new StatsBonus(0, 0, 0, 0, 0, 2)),
            Set.of(NodeId.of("mag_def_up_1"))));

    return java.util.Collections.unmodifiableMap(nodes);
  }

  /**
   * ノードを画面に表示してよいか (段階的開示、§15-7)。前提ノードがすべて解放済みなら可視。 ROOT は前提なしのため常に可視。未定義の NodeId は不可視。
   *
   * <p>「根元を取ると奥のノードが現れる」挙動を表現する: 前提を解放するまで子ノードは隠れる。 SoulTreeScreen はこの判定で描画を出し分ける。
   */
  public boolean isVisible(NodeId nodeId) {
    Objects.requireNonNull(nodeId, "nodeId");
    TreeNode node = allNodes().get(nodeId);
    if (node == null) {
      return false;
    }
    for (NodeId prereq : node.prerequisites()) {
      if (!unlockedNodes.contains(prereq)) {
        return false;
      }
    }
    return true;
  }

  /**
   * 指定ノードを解放する純関数。
   *
   * <ul>
   *   <li>未定義の NodeId → {@link IllegalArgumentException}
   *   <li>既に解放済み → {@link IllegalStateException}
   *   <li>前提ノードが未解放 → {@link IllegalStateException}
   *   <li>Soul 残量不足 → {@link IllegalStateException}
   * </ul>
   *
   * @return 新 SoulTree (unlockedNodes に nodeId を追加、totalSpentSoul に soulCost を加算) + 新 Soul (引数
   *     currentSoul - 消費分)
   */
  public UnlockResult unlock(NodeId nodeId, Soul currentSoul) {
    Objects.requireNonNull(nodeId, "nodeId");
    Objects.requireNonNull(currentSoul, "currentSoul");
    Map<NodeId, TreeNode> defs = allNodes();
    TreeNode target = defs.get(nodeId);
    if (target == null) {
      throw new IllegalArgumentException("Unknown NodeId: " + nodeId.value());
    }
    if (unlockedNodes.contains(nodeId)) {
      throw new IllegalStateException("Node already unlocked: " + nodeId.value());
    }
    for (NodeId prereq : target.prerequisites()) {
      if (!unlockedNodes.contains(prereq)) {
        throw new IllegalStateException(
            "Prerequisite not unlocked: "
                + prereq.value()
                + " (required for "
                + nodeId.value()
                + ")");
      }
    }
    if (currentSoul.amount() < target.soulCost()) {
      throw new IllegalStateException(
          "Insufficient soul: have %d, need %d".formatted(currentSoul.amount(), target.soulCost()));
    }
    Set<NodeId> newUnlocked = new HashSet<>(unlockedNodes);
    newUnlocked.add(nodeId);
    SoulTree newTree = new SoulTree(newUnlocked, totalSpentSoul + target.soulCost());
    Soul newSoul = currentSoul.subtract(new Soul(target.soulCost()));
    return new UnlockResult(newTree, newSoul);
  }

  /**
   * ツリーをリセットして root だけ解放済みの状態に戻す純関数 (ADR-09)。 累計消費ソウルを Soul として返す (呼出側がプレイヤーに加算する責務)。
   *
   * @return 新 SoulTree (root のみ、totalSpentSoul=0) + 返却 Soul (返却額)
   */
  public ResetResult reset() {
    return new ResetResult(SoulTree.empty(), new Soul(totalSpentSoul));
  }

  /**
   * ラン開始時にツリーの解放済み効果を Player に適用する純関数 (§15-7)。
   *
   * <p>root 以外の解放済みノードを {@link TreeNode#effect()} 経由で順次 apply する。順序は {@link #allNodes()} の挿入順 (ステ軸
   * → カード軸 → 枠拡張) に従う (順序による結果の一貫性確保)。
   *
   * @param player ラン開始時の素 Player (Equipment は装備済みで OK)
   * @param cardResolver CardId から Card への解決関数 (CardGrantEffect で使用)
   * @return ツリー解放済み効果を全て適用した新 Player
   */
  public Player applyTo(Player player, Function<CardId, Card> cardResolver) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(cardResolver, "cardResolver");
    Player current = player;
    for (Map.Entry<NodeId, TreeNode> entry : allNodes().entrySet()) {
      if (!entry.getKey().equals(ROOT) && unlockedNodes.contains(entry.getKey())) {
        current = entry.getValue().effect().apply(current, cardResolver);
      }
    }
    return current;
  }

  /** {@link #unlock} の戻り値 (新ツリー + 残ソウル)。 */
  public record UnlockResult(SoulTree newTree, Soul newSoul) {
    public UnlockResult {
      Objects.requireNonNull(newTree, "newTree");
      Objects.requireNonNull(newSoul, "newSoul");
    }
  }

  /** {@link #reset} の戻り値 (新ツリー + 返却 Soul 量)。 */
  public record ResetResult(SoulTree newTree, Soul refundedSoul) {
    public ResetResult {
      Objects.requireNonNull(newTree, "newTree");
      Objects.requireNonNull(refundedSoul, "refundedSoul");
    }
  }
}
