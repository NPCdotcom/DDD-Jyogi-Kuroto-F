package core.domain.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.card.Card;
import core.domain.card.CardEffect;
import core.domain.card.CardElement;
import core.domain.card.CardId;
import core.domain.card.CardTag;
import core.domain.common.Position;
import core.domain.entity.Player;
import core.domain.meta.Soul;
import core.domain.support.DomainFixtures;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** {@link SoulTree} の単体テスト (§15-7 / E-2)。 */
class SoulTreeTest {

  private static final Function<CardId, Card> DUMMY_RESOLVER =
      id ->
          new Card(
              id,
              "解決済み:" + id.value(),
              1,
              CardTag.ATTACK,
              CardElement.PHYSICAL,
              new CardEffect.Damage(1));

  // ---------------- empty / compact constructor ----------------

  @Test
  void emptyHasRootUnlockedAndZeroSpent() {
    SoulTree tree = SoulTree.empty();
    assertTrue(tree.unlockedNodes().contains(SoulTree.ROOT), "root は初期解放済み");
    assertEquals(1, tree.unlockedNodes().size(), "root のみ");
    assertEquals(0, tree.totalSpentSoul());
  }

  @Test
  void compactConstructorRejectsNegativeSpent() {
    assertThrows(IllegalArgumentException.class, () -> new SoulTree(Set.of(SoulTree.ROOT), -1));
  }

  @Test
  void compactConstructorRequiresRoot() {
    assertThrows(IllegalArgumentException.class, () -> new SoulTree(Set.of(), 0));
    assertThrows(
        IllegalArgumentException.class, () -> new SoulTree(Set.of(NodeId.of("hp_up_1")), 20));
  }

  @Test
  void compactConstructorRejectsNullUnlockedNodes() {
    assertThrows(NullPointerException.class, () -> new SoulTree(null, 0));
  }

  // ---------------- allNodes ----------------

  @Test
  void allNodesContainsRequiredCount() {
    // §15-7: 中央 1 + ステ軸 Lv1 (6) + 派生軸 1 カード (5) + 派生軸 2 枠拡張 (5) + ステ軸 Lv2 (6) + 層数拡張 (2) = 25
    assertEquals(25, SoulTree.allNodes().size());
    assertTrue(SoulTree.allNodes().containsKey(SoulTree.ROOT));
  }

  @Test
  void rootHasZeroCostAndNoPrerequisites() {
    TreeNode root = SoulTree.allNodes().get(SoulTree.ROOT);
    assertEquals(0, root.soulCost());
    assertTrue(root.prerequisites().isEmpty());
  }

  // ---------------- unlock: 正常 ----------------

  @Test
  void unlockStatRootChildSucceedsWithSufficientSoul() {
    SoulTree tree = SoulTree.empty();
    SoulTree.UnlockResult result = tree.unlock(NodeId.of("hp_up_1"), new Soul(30));

    assertTrue(result.newTree().unlockedNodes().contains(NodeId.of("hp_up_1")));
    assertEquals(6, result.newTree().totalSpentSoul(), "§15-7 HP+5 のコスト 6 が累計に");
    assertEquals(24, result.newSoul().amount(), "30 - 6 = 24");
  }

  @Test
  void unlockChainStatThenDerivedSucceeds() {
    // 物攻 +1 → 強打 +1 枚 の連鎖解放 (ADR-30 で物攻+1 = 5 ソウル)
    SoulTree tree = SoulTree.empty();
    SoulTree afterStat = tree.unlock(NodeId.of("phys_atk_up_1"), new Soul(100)).newTree();
    SoulTree.UnlockResult afterCard =
        afterStat.unlock(NodeId.of("card_grant_strong_strike"), new Soul(100));

    assertTrue(afterCard.newTree().unlockedNodes().contains(NodeId.of("card_grant_strong_strike")));
    assertEquals(5 + 30, afterCard.newTree().totalSpentSoul(), "物攻 5 + 強打 30 = 35");
  }

  // ---------------- unlock: 異常 ----------------

  @Test
  void unlockUnknownNodeIdRejected() {
    SoulTree tree = SoulTree.empty();
    assertThrows(
        IllegalArgumentException.class,
        () -> tree.unlock(NodeId.of("nonexistent_node"), new Soul(100)));
  }

  @Test
  void unlockAlreadyUnlockedRejected() {
    // root は初期解放済み、もう一度解放しようとすると失敗
    SoulTree tree = SoulTree.empty();
    assertThrows(IllegalStateException.class, () -> tree.unlock(SoulTree.ROOT, new Soul(100)));
  }

  @Test
  void unlockWithMissingPrerequisiteRejected() {
    // root だけの状態で派生軸 (前提=phys_atk_up_1) を解放しようとすると失敗
    SoulTree tree = SoulTree.empty();
    assertThrows(
        IllegalStateException.class,
        () -> tree.unlock(NodeId.of("card_grant_strong_strike"), new Soul(100)));
  }

  @Test
  void unlockWithInsufficientSoulRejected() {
    SoulTree tree = SoulTree.empty();
    // §15-7 / ADR-30: HP+5 のコスト 6、5 ソウルでは足りない
    assertThrows(IllegalStateException.class, () -> tree.unlock(NodeId.of("hp_up_1"), new Soul(5)));
  }

  // ---------------- reset ----------------

  @Test
  void resetReturnsEmptyTreeAndRefundsTotalSpent() {
    SoulTree tree = SoulTree.empty();
    tree = tree.unlock(NodeId.of("hp_up_1"), new Soul(100)).newTree(); // -6 (§15-7)
    tree = tree.unlock(NodeId.of("phys_atk_up_1"), new Soul(100)).newTree(); // -5 (§15-7)

    SoulTree.ResetResult result = tree.reset();

    assertEquals(1, result.newTree().unlockedNodes().size(), "root のみに戻る");
    assertEquals(0, result.newTree().totalSpentSoul());
    assertEquals(11, result.refundedSoul().amount(), "6 + 5 = 11 ソウル返却");
  }

  @Test
  void resetOnEmptyTreeRefundsZero() {
    SoulTree.ResetResult result = SoulTree.empty().reset();
    assertEquals(0, result.refundedSoul().amount());
  }

  // ---------------- applyTo ----------------

  @Test
  void applyToOnEmptyTreeReturnsPlayerUnchanged() {
    Player base = DomainFixtures.playerAt(new Position(1, 1));
    Player applied = SoulTree.empty().applyTo(base, DUMMY_RESOLVER);

    assertEquals(base.stats(), applied.stats(), "root のみなら素ステは変化しない");
  }

  @Test
  void applyToWithStatNodesUnlockedAppliesBonuses() {
    Player base = DomainFixtures.playerAt(new Position(1, 1));
    int basePhyAtk = base.stats().physicalAttack();
    int baseSpeed = base.stats().speed();

    SoulTree tree = SoulTree.empty();
    tree = tree.unlock(NodeId.of("phys_atk_up_1"), new Soul(100)).newTree();
    tree = tree.unlock(NodeId.of("speed_up_1"), new Soul(100)).newTree();

    Player applied = tree.applyTo(base, DUMMY_RESOLVER);

    assertEquals(basePhyAtk + 1, applied.stats().physicalAttack(), "物攻 +1");
    assertEquals(baseSpeed + 1, applied.stats().speed(), "速度 +1");
  }

  @Test
  void applyToWithCardGrantNodeAddsCardToDeck() {
    Player base = DomainFixtures.playerAt(new Position(1, 1));
    int beforeDeckSize =
        base.cardPileState().drawPile().size()
            + base.cardPileState().hand().size()
            + base.cardPileState().discardPile().size();

    SoulTree tree = SoulTree.empty();
    tree = tree.unlock(NodeId.of("phys_atk_up_1"), new Soul(100)).newTree();
    tree = tree.unlock(NodeId.of("card_grant_strong_strike"), new Soul(100)).newTree();

    Player applied = tree.applyTo(base, DUMMY_RESOLVER);

    int afterDeckSize =
        applied.cardPileState().drawPile().size()
            + applied.cardPileState().hand().size()
            + applied.cardPileState().discardPile().size();
    assertEquals(beforeDeckSize + 1, afterDeckSize, "デッキ枚数 +1 (strong_strike)");
  }

  @Test
  void applyToWithSlotExpandNodeIncreasesSkillSlotMax() {
    Player base = DomainFixtures.playerAt(new Position(1, 1));
    int beforeMax = base.skillSlot().maxSize();

    SoulTree tree = SoulTree.empty();
    tree = tree.unlock(NodeId.of("phys_atk_up_1"), new Soul(100)).newTree();
    tree = tree.unlock(NodeId.of("card_grant_strong_strike"), new Soul(100)).newTree();
    tree = tree.unlock(NodeId.of("slot_expand_1"), new Soul(100)).newTree();

    Player applied = tree.applyTo(base, DUMMY_RESOLVER);

    assertEquals(beforeMax + 1, applied.skillSlot().maxSize(), "スキル枠 +1");
  }

  @Test
  void applyToRejectsNullArguments() {
    Player base = DomainFixtures.playerAt(new Position(1, 1));
    assertThrows(NullPointerException.class, () -> SoulTree.empty().applyTo(null, DUMMY_RESOLVER));
    assertThrows(NullPointerException.class, () -> SoulTree.empty().applyTo(base, null));
  }

  // ---------------- 防御コピー ----------------

  @Test
  void unlockedNodesIsDefensivelyCopied() {
    Set<NodeId> mutable = new HashSet<>();
    mutable.add(SoulTree.ROOT);
    SoulTree tree = new SoulTree(mutable, 0);

    mutable.add(NodeId.of("intruder"));
    assertEquals(1, tree.unlockedNodes().size(), "外部 Set 変更は防御コピーで遮断される");
    assertFalse(tree.unlockedNodes().contains(NodeId.of("intruder")));
  }

  // ---------------- root と equality ----------------

  @Test
  void emptyInstancesAreEqual() {
    // record 構造的等価性: SoulTree.empty() 同士は equals で等しい
    assertEquals(SoulTree.empty(), SoulTree.empty());
  }

  @Test
  void rootConstantIsConsistent() {
    assertSame(SoulTree.ROOT, SoulTree.ROOT);
    assertEquals("root", SoulTree.ROOT.value());
  }

  // ---------------- 段階的開示 (isVisible) ----------------

  @Test
  void isVisibleTrueForRootAndFirstTierFromStart() {
    // 初期状態 (root のみ解放): root と、root を前提とする 6 ステ軸 Lv1 が可視。
    SoulTree tree = SoulTree.empty();
    assertTrue(tree.isVisible(SoulTree.ROOT), "root は常に可視");
    assertTrue(tree.isVisible(NodeId.of("phys_atk_up_1")), "前提 root が解放済 → ステ軸 Lv1 は可視");
  }

  @Test
  void isVisibleFalseForDeepNodeUntilPrerequisiteUnlocked() {
    // card_grant_strong_strike は前提 phys_atk_up_1。未解放なら不可視、解放すると可視になる。
    SoulTree tree = SoulTree.empty();
    assertFalse(tree.isVisible(NodeId.of("card_grant_strong_strike")), "前提未解放 → 不可視");
    SoulTree afterUnlock = tree.unlock(NodeId.of("phys_atk_up_1"), new Soul(100)).newTree();
    assertTrue(afterUnlock.isVisible(NodeId.of("card_grant_strong_strike")), "前提解放 → 可視化");
  }

  @Test
  void lv2NodeBecomesVisibleAfterLv1Unlocked() {
    // §15-7 充実化: ステ軸 Lv2 は Lv1 解放で開示される。
    SoulTree tree = SoulTree.empty();
    assertFalse(tree.isVisible(NodeId.of("hp_up_2")), "hp_up_1 未解放 → hp_up_2 不可視");
    SoulTree afterUnlock = tree.unlock(NodeId.of("hp_up_1"), new Soul(100)).newTree();
    assertTrue(afterUnlock.isVisible(NodeId.of("hp_up_2")), "hp_up_1 解放 → hp_up_2 可視化");
  }

  @Test
  void isVisibleFalseForUnknownNode() {
    assertFalse(SoulTree.empty().isVisible(NodeId.of("no_such_node")), "未定義 ID は不可視");
  }

  // ---------------- Phase 1 回帰: speed_up_1 / speed_up_2 コスト確認 ----------------

  @Test
  void speedUp1HasCost22() {
    // §15-7 ADR-30: speed_up_1 のコストは 22 ソウル
    TreeNode node = SoulTree.allNodes().get(NodeId.of("speed_up_1"));
    assertEquals(22, node.soulCost(), "speed_up_1 のコストは 22");
  }

  @Test
  void speedUp1CannotBeUnlockedWith21Soul() {
    // §15-7: 21 ソウルでは speed_up_1 (コスト 22) を解放できない
    SoulTree tree = SoulTree.empty();
    assertThrows(
        IllegalStateException.class, () -> tree.unlock(NodeId.of("speed_up_1"), new Soul(21)));
  }

  @Test
  void speedUp1CanBeUnlockedWith22Soul() {
    // §15-7: ちょうど 22 ソウルで解放可能 (境界値)
    SoulTree tree = SoulTree.empty();
    SoulTree.UnlockResult result = tree.unlock(NodeId.of("speed_up_1"), new Soul(22));
    assertTrue(result.newTree().unlockedNodes().contains(NodeId.of("speed_up_1")));
    assertEquals(22, result.newTree().totalSpentSoul());
    assertEquals(0, result.newSoul().amount(), "22 - 22 = 0 ソウル残");
  }

  @Test
  void speedUp2HasCost30AndSpeedEffect2() {
    // §15-7 ADR-30: speed_up_2 のコストは 30 ソウル、効果は速度 +2
    TreeNode node = SoulTree.allNodes().get(NodeId.of("speed_up_2"));
    assertEquals(30, node.soulCost(), "speed_up_2 のコストは 30");
    // NodeEffect.StatsBonusEffect の speed フィールドが 2 であることを applyTo で検証
    Player base = DomainFixtures.playerAt(new Position(1, 1));
    int baseSpeed = base.stats().speed();
    SoulTree tree = SoulTree.empty();
    tree = tree.unlock(NodeId.of("speed_up_1"), new Soul(100)).newTree();
    tree = tree.unlock(NodeId.of("speed_up_2"), new Soul(100)).newTree();
    Player applied = tree.applyTo(base, DUMMY_RESOLVER);
    assertEquals(
        baseSpeed + 1 + 2, applied.stats().speed(), "speed_up_1(+1) + speed_up_2(+2) = +3 合計");
  }

  @Test
  void speedUp2CannotBeUnlockedWith29Soul() {
    // §15-7: 29 ソウルでは speed_up_2 (コスト 30) を解放できない
    SoulTree tree = SoulTree.empty();
    tree = tree.unlock(NodeId.of("speed_up_1"), new Soul(100)).newTree();
    final SoulTree finalTree = tree;
    assertThrows(
        IllegalStateException.class, () -> finalTree.unlock(NodeId.of("speed_up_2"), new Soul(29)));
  }

  // ---------------- ROOT 除外ガード回帰 (SoulTreeScreen.tryUnlockAt の前提) ----------------

  @Test
  void allNodesContainsRootKey() {
    // 回帰防止: SoulTreeScreen.tryUnlockAt は allNodes().containsKey(ROOT) を前提に
    // ROOT 除外ガードを掛ける。ROOT がマスタ定義から削除されると整合が崩れる。
    assertTrue(SoulTree.allNodes().containsKey(SoulTree.ROOT), "allNodes のマスタ定義に ROOT が含まれる");
  }

  @Test
  void allNodesReturnsSameInstanceOnRepeatedCalls() {
    // allNodes() は static キャッシュ (ALL_NODES) を返すため、複数回呼んでも同一オブジェクト。
    // 毎フレーム呼ばれる SoulTreeScreen の isVisible が再構築コストを払わないことを確認。
    assertSame(SoulTree.allNodes(), SoulTree.allNodes(), "allNodes() は同一 Map インスタンスを返す (キャッシュ)");
  }

  @Test
  void unlockRootThrowsIllegalStateException() {
    // 回帰防止: ROOT はコスト 0 で初期解放済みのため、unlock しようとすると
    // "already unlocked" として IllegalStateException を投げる。
    // SoulTreeScreen の tryUnlockAt が ROOT をガードする前提を裏付ける。
    SoulTree tree = SoulTree.empty();
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> tree.unlock(SoulTree.ROOT, new Soul(0)));
    assertTrue(
        ex.getMessage().contains("already unlocked"),
        "例外メッセージに 'already unlocked' が含まれる: " + ex.getMessage());
  }

  @Test
  void isVisibleRootAlwaysTrueRegardlessOfUnlockedSet() {
    // 前提明示: ROOT は prerequisites が空なので、どの SoulTree インスタンスでも
    // isVisible(ROOT) == true。SoulTreeScreen が ROOT を常に描画する前提を固定。
    SoulTree fresh = SoulTree.empty();
    assertTrue(fresh.isVisible(SoulTree.ROOT), "empty() で ROOT は可視");
    // ROOT 以外も解放された状態でも変わらない
    SoulTree afterUnlock = fresh.unlock(NodeId.of("hp_up_1"), new Soul(100)).newTree();
    assertTrue(afterUnlock.isVisible(SoulTree.ROOT), "ノード解放後も ROOT は可視のまま");
  }
}
