package core.domain.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.infrastructure.bootstrap.InitialStateFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * リセットで装備保護枠ノード (魂の刻印) が保持され、その分が返金されないことを固定する (§15-9)。
 *
 * <p>刻印は装備の永続保護という「取り消せない約束」を買う枠なので、リセットで剥がすと保護していた装備の 扱いが宙に浮く。よって刻印だけは解放したまま残し、その消費分は返金しない。
 *
 * <p>非返金額はハードコードせず {@code tree.json} の実コストから導出する。刻印 I=30 / II=60 なので I のみ保持なら 30、I+II 保持なら 90
 * が非返金となる。
 */
class SoulTreeResetRetentionTest {

  private static final NodeId SEAL_1 = NodeId.of("seal_of_soul_1");
  private static final NodeId SEAL_2 = NodeId.of("seal_of_soul_2");
  private static final NodeId HP_1 = NodeId.of("hp_up_1");

  @BeforeAll
  static void loadCatalog() {
    SoulTree.setNodeProvider(InitialStateFactory::soulTreeNodes);
  }

  private static int costOf(NodeId id) {
    return SoulTree.allNodes().get(id).soulCost();
  }

  private static SoulTree unlock(SoulTree tree, NodeId id) {
    return tree.unlock(id, new core.domain.meta.Soul(10_000)).newTree();
  }

  @Test
  void resetWithoutSealsRefundsEverything() {
    SoulTree tree = unlock(SoulTree.empty(), HP_1);
    SoulTree.ResetResult result = tree.reset();

    assertEquals(costOf(HP_1), result.refundedSoul().amount(), "刻印が無ければ全額返金");
    assertEquals(SoulTree.empty().unlockedNodes(), result.newTree().unlockedNodes(), "root のみへ戻る");
  }

  @Test
  void resetKeepsFirstSealAndWithholdsItsCost() {
    SoulTree tree = unlock(unlock(SoulTree.empty(), HP_1), SEAL_1);
    SoulTree.ResetResult result = tree.reset();

    assertTrue(result.newTree().unlockedNodes().contains(SEAL_1), "刻印 I はリセット後も保持される");
    assertFalse(result.newTree().unlockedNodes().contains(HP_1), "刻印以外は剥がれる");
    assertEquals(costOf(HP_1), result.refundedSoul().amount(), "刻印 I の消費分 (30) は返金されない");
    assertEquals(costOf(SEAL_1), result.newTree().totalSpentSoul(), "保持した刻印の消費は累計に残す");
  }

  @Test
  void resetKeepsBothSealsAndWithholdsNinety() {
    SoulTree tree = unlock(unlock(unlock(SoulTree.empty(), HP_1), SEAL_1), SEAL_2);
    SoulTree.ResetResult result = tree.reset();

    assertTrue(result.newTree().unlockedNodes().contains(SEAL_1), "刻印 I を保持");
    assertTrue(result.newTree().unlockedNodes().contains(SEAL_2), "刻印 II を保持");
    assertEquals(costOf(HP_1), result.refundedSoul().amount(), "刻印 I+II の消費分 (90) は返金されない");
    assertEquals(
        costOf(SEAL_1) + costOf(SEAL_2), result.newTree().totalSpentSoul(), "非返金額は 30 + 60 = 90");
    assertEquals(2, result.newTree().retentionCapacity().value(), "保護枠 2 を維持する");
  }

  @Test
  void resetRejectsProfileWhereSpentSoulIsLessThanRetainedCost() {
    // 破損 Profile: 刻印 I+II (実コスト 90) を解放済みなのに totalSpentSoul が 50。
    // 差し引くと返金が負になる。黙って 0 に丸めると「返金されるはずの分」が消えるため、
    // 不整合として拒否する (fail-closed)。
    SoulTree broken = new SoulTree(java.util.Set.of(SoulTree.ROOT, SEAL_1, SEAL_2), 50);

    assertThrows(
        IllegalStateException.class,
        broken::reset,
        "解放済みノードの実コストが totalSpentSoul を超える Profile はリセットを拒否する");
  }

  @Test
  void resetRepairsProfileHoldingSecondSealWithoutFirst() {
    // 破損 Profile: 消費額は I+II ぶん (90) あるのに、解放集合から刻印 I が落ちている。
    // 剥がすと保護していた装備の扱いが宙に浮くため、前提の刻印 I を補完して保持する。
    // (消費額まで足りない場合は別途 fail-closed。補完で購入を捏造しないための境界)
    SoulTree broken =
        new SoulTree(java.util.Set.of(SoulTree.ROOT, SEAL_2), costOf(SEAL_1) + costOf(SEAL_2));
    SoulTree.ResetResult result = broken.reset();

    assertTrue(result.newTree().unlockedNodes().contains(SEAL_2), "刻印 II は保持する");
    assertTrue(result.newTree().unlockedNodes().contains(SEAL_1), "前提の刻印 I を補完する");
    assertEquals(2, result.newTree().retentionCapacity().value(), "補完後の保護枠は 2");
  }
}
