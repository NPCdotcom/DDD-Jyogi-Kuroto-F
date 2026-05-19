package core.infrastructure.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.card.CardElement;
import core.domain.card.TrapLifetime;
import core.domain.common.Position;
import core.domain.dungeon.DungeonState;
import core.domain.dungeon.PlacedTrap;
import java.util.List;
import org.junit.jupiter.api.Test;

class InitialStateFactoryAdvanceLayerTest {

  // ハッピーパス

  @Test
  void firstFloorStartsAtLayer1() {
    // §15-6 「1 層目から開始」: firstFloor() が生成する状態は layer.number() == 1
    DungeonState state = InitialStateFactory.firstFloor();
    assertEquals(1, state.layer().number());
  }

  @Test
  void advanceLayerIncrementsLayerNumber() {
    // §15-6 「層が進むほど敵 AP 増加」: advanceLayer 後の layer.number() == 2
    DungeonState first = InitialStateFactory.firstFloor();
    DungeonState second = InitialStateFactory.advanceLayer(first);

    assertEquals(2, second.layer().number());
  }

  @Test
  void advanceLayerSetsEnemyApEqualToLayerNumber() {
    // ADR-06 / §15-5 「敵 AP = 層番号 N」: 2 層の敵は AP 2 で初期化される
    DungeonState second = InitialStateFactory.advanceLayer(InitialStateFactory.firstFloor());

    int expectedAp = second.layer().number(); // == 2
    boolean allEnemiesHaveCorrectAp =
        second.enemies().stream()
            .allMatch(e -> e.actionPoints().current() == expectedAp);
    assertTrue(allEnemiesHaveCorrectAp, "2 層の全敵は current AP = 2 でなければならない");
  }

  // 境界値

  @Test
  void advanceLayerResetsPlayerPositionToSpawn() {
    // §15-6 「層遷移時にプレイヤー位置をリセット」: advanceLayer 後は (1, 1) にリセット
    DungeonState first = InitialStateFactory.firstFloor();
    DungeonState second = InitialStateFactory.advanceLayer(first);

    assertEquals(new Position(1, 1), second.player().position());
  }

  @Test
  void advanceLayerCarriesPlayerStats() {
    // §15-6 「Player は HP / ソウル / 手札を持ち越し」: HP の変化なし確認
    DungeonState first = InitialStateFactory.firstFloor();
    int hpBefore = first.player().stats().currentHp();
    int handSizeBefore = first.player().cardPileState().hand().size();

    DungeonState second = InitialStateFactory.advanceLayer(first);

    assertEquals(hpBefore, second.player().stats().currentHp(), "HP は持ち越される");
    assertEquals(handSizeBefore, second.player().cardPileState().hand().size(), "手札サイズは持ち越される");
  }

  @Test
  void advanceLayerClearsPlacedTraps() {
    // §15-6 「罠は層間で持ち越さない」: advanceLayer 後の placedTraps は空
    PlacedTrap trap =
        new PlacedTrap(
            new Position(3, 3), 3, TrapLifetime.UntilStepped.INSTANCE, CardElement.PHYSICAL);
    DungeonState withTrap = InitialStateFactory.firstFloor().withPlacedTraps(List.of(trap));
    assertEquals(1, withTrap.placedTraps().size(), "前提: 罠が 1 件存在する");

    DungeonState advanced = InitialStateFactory.advanceLayer(withTrap);

    assertTrue(advanced.placedTraps().isEmpty(), "層遷移後の罠は空であること");
  }

  @Test
  void advanceLayerThriceReachesLayer4() {
    // 連続 3 回の advanceLayer で layer.number() == 4 かつ敵 AP = 4 になる
    DungeonState state = InitialStateFactory.firstFloor();
    state = InitialStateFactory.advanceLayer(state); // → 2 層
    state = InitialStateFactory.advanceLayer(state); // → 3 層
    state = InitialStateFactory.advanceLayer(state); // → 4 層

    assertEquals(4, state.layer().number());

    int expectedAp = 4;
    boolean allEnemiesHaveLayer4Ap =
        state.enemies().stream()
            .allMatch(e -> e.actionPoints().current() == expectedAp);
    assertTrue(allEnemiesHaveLayer4Ap, "4 層の全敵は current AP = 4 でなければならない");
  }

  // 例外パス

  @Test
  void newSlimeForLayerWithLayerNumberZeroIsRejected() {
    // layerNumber=0 は「敵 AP = 0」という無効状態になる
    assertThrows(
        IllegalArgumentException.class,
        () -> InitialStateFactory.newSlimeForLayer("slime#test", new Position(2, 2), 0));
  }

  @Test
  void newSlimeForLayerWithLayerNumber1IsAccepted() {
    // 最小有効値 1 でスライムが生成でき、AP = 1 になる (ADR-06)
    var slime = InitialStateFactory.newSlimeForLayer("slime#l1", new Position(2, 2), 1);
    assertEquals(1, slime.actionPoints().current());
  }
}
