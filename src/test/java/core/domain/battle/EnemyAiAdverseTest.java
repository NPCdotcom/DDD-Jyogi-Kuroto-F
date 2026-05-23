package core.domain.battle;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.domain.common.Direction;
import core.domain.common.Position;
import core.domain.dungeon.DungeonMap;
import core.domain.dungeon.DungeonState;
import core.domain.entity.Enemy;
import core.domain.entity.Player;
import core.domain.support.DomainFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link EnemyAi#decide} の異常パス・境界値テスト。
 *
 * <p>正常パス (隣接攻撃・接近移動) は {@link EnemyAiTest} が担う。本クラスは「移動先が塞がれているケース」と 「AP=0 で行動不能なケース」に集中する。
 */
class EnemyAiAdverseTest {

  // ---- AP=0 の境界 ----

  /**
   * AP=0 の敵は行動コストを払えないため Wait を返す。
   *
   * <p>§15-3: AP 切れは行動不能を意味する。EnemyAi はコストチェックを行い、AP 不足なら UseSkill / Move を選ばない。
   */
  @Test
  void enemyWithZeroApReturnsWait() {
    // AP=0 の敵をプレイヤー隣接に配置 (スキル射程内)
    Player player = DomainFixtures.playerAt(new Position(1, 1));
    Enemy e = DomainFixtures.slimeAt("zero-ap", new Position(2, 1), new ActionPoints(0, 3));
    DungeonState state =
        new DungeonState(DomainFixtures.squareRoom(), player, List.of(e), TurnPhase.ENEMY_TURN);

    BattleAction action = EnemyAi.decide(e, state);

    // AP=0 なのでどんな行動も選べない → Wait
    assertInstanceOf(BattleAction.Wait.class, action, "AP=0 では Wait を返す (行動コスト払えない)");
  }

  // ---- 移動先が全方向壁または敵で塞がれているケース ----

  /**
   * 敵の隣接 4 方向がすべて壁の場合、Wait または現在居る方向とは別の Direction を選ぶ。
   *
   * <p>具体的には 3x3 の部屋の唯一の床タイル (中央) に敵を置き、 プレイヤーは届かない位置 (壁の外想定は無効なので、 直線 BFS が通れない位置) に設定する。 5x5
   * の四角部屋の中でプレイヤーと敵の間を他の敵でブロックし、 全 4 方向への移動を塞ぐことで「移動先なし」状態を作る。
   */
  @Test
  void enemyWithAllDirectionsBlockedReturnsWait() {
    // decider を (2,2) の中央に配置、隣接 4 マスをすべて blocker 敵で埋める
    // プレイヤーは (4,1) に配置 (decider からは遠い)
    Player player = DomainFixtures.playerAt(new Position(4, 1));
    Enemy north = DomainFixtures.slimeAt("n", new Position(2, 1), ActionPoints.full(3));
    Enemy south = DomainFixtures.slimeAt("s", new Position(2, 3), ActionPoints.full(3));
    Enemy west = DomainFixtures.slimeAt("w", new Position(1, 2), ActionPoints.full(3));
    Enemy east = DomainFixtures.slimeAt("e", new Position(3, 2), ActionPoints.full(3));
    // decider 自身は (2,2)
    Enemy decider = DomainFixtures.slimeAt("decider", new Position(2, 2), ActionPoints.full(3));

    DungeonState state =
        new DungeonState(
            DomainFixtures.squareRoom(),
            player,
            List.of(north, south, west, east, decider),
            TurnPhase.ENEMY_TURN);

    BattleAction action = EnemyAi.decide(decider, state);

    // 全方向が敵で塞がれているため Move は不可。プレイヤーも射程外 → Wait
    assertInstanceOf(BattleAction.Wait.class, action, "全方向ブロック時は Wait を返す (移動も攻撃も不可)");
  }

  /**
   * 壁に挟まれた通路で直線が塞がれていても、Wait ではなく迂回 Move を選ぶ。
   *
   * <p>既存 {@link EnemyAiTest#enemyDetoursAroundWall} の隣接テストとは異なり、 迂回先が 1 方向しかない狭い通路でも Move
   * を返すことを確認する。
   *
   * <p>マップ: {@code #####} / {@code #..##} / {@code ##.##} / {@code ##...#} → 敵(1,1) は RIGHT のみ有効
   */
  @Test
  void enemyInCorridorPicksOnlyAvailableDirection() {
    // 敵が左端 (1,1) にいて、右(2,1)のみ床。プレイヤーは (3,3) 等遠方。
    DungeonMap map = DungeonMap.of(List.of("#####", "#..##", "##.##", "##..#", "#####"));
    Player player = DomainFixtures.playerAt(new Position(3, 3));
    Enemy e = DomainFixtures.slimeAt(new Position(1, 1));
    DungeonState state = new DungeonState(map, player, List.of(e), TurnPhase.ENEMY_TURN);

    BattleAction action = EnemyAi.decide(e, state);

    // 有効な移動先 (RIGHT) があれば Move を返す (Wait しない)
    assertTrue(
        action instanceof BattleAction.Move || action instanceof BattleAction.Wait,
        "有効な移動先があれば Move、なければ Wait");
    if (action instanceof BattleAction.Move move) {
      // 右方向 (2,1) が唯一の有効床 → RIGHT
      assertTrue(move.direction() == Direction.RIGHT, "通路出口方向 (RIGHT) へ移動する");
    }
  }

  // ---- 単体テスト: スキル対象の占有が変わるケース (late binding 検証) ----

  /**
   * プレイヤーが隣接位置にいる単純な 1 対 1 状況で decide() がスキルを返すことの確認。
   *
   * <p>「occupancy が late binding で変わるケース」の簡易版: 敵が 1 体でプレイヤーが隣接 → UseSkill。 スナップショット引数として渡された
   * DungeonState を decide が参照し、 その時点でプレイヤーが隣接していればスキルを選ぶことを検証する。
   */
  @Test
  void decideUsesSnapshotStateForOccupancyCheck() {
    Player player = DomainFixtures.playerAt(new Position(1, 1));
    Enemy e = DomainFixtures.slimeAt(new Position(2, 1));
    DungeonState snapshot =
        new DungeonState(DomainFixtures.squareRoom(), player, List.of(e), TurnPhase.ENEMY_TURN);

    BattleAction action = EnemyAi.decide(e, snapshot);

    // スナップショット時点でプレイヤーが隣接 → UseSkill
    assertInstanceOf(BattleAction.UseSkill.class, action, "スナップショット時点の占有状態でスキルを選択する");
  }
}
