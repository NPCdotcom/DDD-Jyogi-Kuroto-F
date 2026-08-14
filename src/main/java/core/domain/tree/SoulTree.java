package core.domain.tree;

import core.domain.card.Card;
import core.domain.card.CardId;
import core.domain.entity.Player;
import core.domain.meta.Soul;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * ソウルツリー (§15-7 / E-2)。永続強化のメタ進行を表現する不変 record。
 *
 * <p>構成: 23 ノード (中央 root + 6 ステ軸 Lv1 + 6 ステ軸 Lv2 + 派生軸 1 カード獲得 5 ノード + 派生軸 2 枠拡張 5 ノード)。 段階的開示
 * ({@link #isVisible}): 前提ノードを解放すると次のノードが画面に現れる。
 *
 * <p>ノードコスト (tree.json と同期):
 *
 * <ul>
 *   <li>root: 0 (初期解放、入口)
 *   <li>Lv1 ステ軸: HP +5: 6、速度 +1: 22、物攻 +1: 5、魔攻 +1: 5、物防 +1: 4、魔防 +1: 4
 *   <li>Lv2 ステ軸: HP +10: 14、速度 +2: 30、物攻 +2: 12、魔攻 +2: 12、物防 +2: 10、魔防 +2: 10
 *   <li>カード獲得: 30〜50
 *   <li>枠拡張: 40
 * </ul>
 *
 * <p>{@code unlockedNodes} に root が常に含まれる (新規 {@link #empty()} で root のみ解放済み)。 {@link
 * #unlock(NodeId, Soul)} で前提ノード解放済み + Soul 残量を検証して新インスタンスを返す (純関数)。{@link #reset()} で root
 * だけに戻し、{@link #totalSpentSoul()} を呼出側で全額返却する想定 (ADR-09)。
 *
 * <p>{@link #applyTo(Player, Function)} はラン開始時に呼ぶ: 解放済みノードの効果を順番に Player に適用し、 装備や Buff と並んで Player
 * の能力値に反映する (ADR-25 / §15-9 と整合)。
 */
public record SoulTree(Set<NodeId> unlockedNodes, int totalSpentSoul) {

  public static final NodeId ROOT = NodeId.of("root");

  /**
   * §設計原則 / Wave 5 W5-γ: domain → infrastructure 依存方向違反 解消のための Supplier 注入。
   *
   * <p>本フィールドは「Logger / Properties 級の単発 initialization setter」として扱う (lessons.md 2026-05-23 記録)。
   * 起動時に DddGame.create() から 1 度だけ {@link #setNodeProvider} で設定し、以降は read-only。 mutable static
   * state 禁止ルールの 例外として許容: (1) 初期化フェーズのみ set (2) read 以降の state 変更なし (3) テストでも初期化可能。
   */
  private static Supplier<Map<NodeId, TreeNode>> nodeProvider;

  /**
   * ノードマスタの Supplier を注入する。DddGame.create() で {@code
   * SoulTree.setNodeProvider(InitialStateFactory::soulTreeNodes)} を呼ぶ。
   *
   * <p>テスト時はカタログモック等で setProvider を上書き可能だが、 {@code @AfterEach} or {@code @AfterAll} で前 provider
   * に復元する (並列テスト時の NPE 防止)。
   */
  public static void setNodeProvider(Supplier<Map<NodeId, TreeNode>> provider) {
    nodeProvider = Objects.requireNonNull(provider, "provider");
  }

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

  /**
   * 全ノードのマスタ定義を返す (id → TreeNode、登録順保持の不変マップ)。
   *
   * <p>UI 側 (SoulTreeScreen) もここから配置情報を引く。{@link #isVisible} 経由で毎フレーム多数回呼ばれるため infrastructure 層 の
   * {@code SoulTreeCatalog} で 1 度ロードしキャッシュ済みのマップを委譲して返す (呼出のたびに再構築しない)。
   *
   * <p>Wave 5 W5-γ: 旧実装は {@code InitialStateFactory.soulTreeNodes()} を直接呼んでいたが、 domain →
   * infrastructure 依存方向違反だったため Supplier 注入パターンに変更。{@link #setNodeProvider} で 起動時に注入された Supplier
   * から取得する。
   *
   * @throws IllegalStateException nodeProvider が未注入の場合 (起動初期化漏れ)
   */
  public static Map<NodeId, TreeNode> allNodes() {
    if (nodeProvider == null) {
      throw new IllegalStateException(
          "SoulTree.nodeProvider not initialized; call SoulTree.setNodeProvider(...) at startup");
    }
    return nodeProvider.get();
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

  /**
   * 解放済みノードから保護枠容量を導出する (SOUL-03)。
   *
   * <p>seal_of_soul_1 で +1、seal_of_soul_2 で +1 (最大 2)。
   */
  public core.domain.equipment.RetentionCapacity retentionCapacity() {
    int cap = 0;
    if (unlockedNodes.contains(NodeId.of("seal_of_soul_1"))) {
      cap++;
    }
    if (unlockedNodes.contains(NodeId.of("seal_of_soul_2"))) {
      cap++;
    }
    return core.domain.equipment.RetentionCapacity.of(cap);
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
