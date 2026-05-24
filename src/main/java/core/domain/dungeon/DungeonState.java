package core.domain.dungeon;

import core.domain.battle.TurnPhase;
import core.domain.common.Position;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.Player;
import core.domain.layer.Layer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * ダンジョン 1 階層の現在状態。
 *
 * <p>すべての更新メソッドは新インスタンスを返す (不変)。enemies / placedTraps は内部で defensive copy する。
 *
 * <p>{@code placedTraps} は §15-3 / ADR-22 の罠カード設置先。設置時の同座標重複は呼出側 (TurnEngine) で「上書き」処理する (本 record
 * は単純なリストとして保持)。
 */
public record DungeonState(
    DungeonMap map,
    Player player,
    List<Enemy> enemies,
    TurnPhase phase,
    List<PlacedTrap> placedTraps,
    Layer layer) {

  public DungeonState {
    Objects.requireNonNull(map, "map");
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(enemies, "enemies");
    Objects.requireNonNull(phase, "phase");
    Objects.requireNonNull(placedTraps, "placedTraps");
    Objects.requireNonNull(layer, "layer");
    enemies = List.copyOf(enemies);
    placedTraps = List.copyOf(placedTraps);
  }

  /** 既存呼出 (5 引数) 互換のための便利コンストラクタ。layer は 1 層目で初期化。 */
  public DungeonState(
      DungeonMap map,
      Player player,
      List<Enemy> enemies,
      TurnPhase phase,
      List<PlacedTrap> placedTraps) {
    this(map, player, enemies, phase, placedTraps, Layer.first());
  }

  /** 既存呼出 (4 引数) 互換のための便利コンストラクタ。placedTraps は空、layer は 1 層目で初期化。 */
  public DungeonState(DungeonMap map, Player player, List<Enemy> enemies, TurnPhase phase) {
    this(map, player, enemies, phase, List.of(), Layer.first());
  }

  public DungeonState withPlayer(Player newPlayer) {
    return new DungeonState(map, newPlayer, enemies, phase, placedTraps, layer);
  }

  /** マップだけを差し替えた新状態を返す (Wave 11 W11-α: 壊れる壁の破壊 → 床化に使う)。 */
  public DungeonState withMap(DungeonMap newMap) {
    return new DungeonState(newMap, player, enemies, phase, placedTraps, layer);
  }

  public DungeonState withEnemies(List<Enemy> newEnemies) {
    return new DungeonState(map, player, newEnemies, phase, placedTraps, layer);
  }

  public DungeonState withPhase(TurnPhase newPhase) {
    return new DungeonState(map, player, enemies, newPhase, placedTraps, layer);
  }

  public DungeonState withPlacedTraps(List<PlacedTrap> newPlacedTraps) {
    return new DungeonState(map, player, enemies, phase, newPlacedTraps, layer);
  }

  public DungeonState withLayer(Layer newLayer) {
    return new DungeonState(map, player, enemies, phase, placedTraps, newLayer);
  }

  /** ID が一致する敵を 1 体置き換えた新状態を返す。一致が無いと {@link IllegalStateException}。 */
  public DungeonState withEnemyReplaced(Enemy replacement) {
    Objects.requireNonNull(replacement, "replacement");
    List<Enemy> out = new ArrayList<>(enemies.size());
    boolean replaced = false;
    for (Enemy e : enemies) {
      if (!replaced && e.id().equals(replacement.id())) {
        out.add(replacement);
        replaced = true;
      } else {
        out.add(e);
      }
    }
    if (!replaced) {
      throw new IllegalStateException("enemy not found: " + replacement.id());
    }
    return withEnemies(out);
  }

  /** ID が一致する敵を取り除いた新状態を返す。一致が無いと {@link IllegalStateException}。 */
  public DungeonState withEnemyRemoved(ActorId id) {
    Objects.requireNonNull(id, "id");
    List<Enemy> out = new ArrayList<>(enemies.size());
    boolean removed = false;
    for (Enemy e : enemies) {
      if (!removed && e.id().equals(id)) {
        removed = true;
      } else {
        out.add(e);
      }
    }
    if (!removed) {
      throw new IllegalStateException("enemy not found: " + id);
    }
    return withEnemies(out);
  }

  public Optional<Enemy> findEnemyAt(Position position) {
    Objects.requireNonNull(position, "position");
    for (Enemy e : enemies) {
      if (e.position().equals(position)) {
        return Optional.of(e);
      }
    }
    return Optional.empty();
  }

  public Optional<Enemy> findEnemy(ActorId id) {
    Objects.requireNonNull(id, "id");
    for (Enemy e : enemies) {
      if (e.id().equals(id)) {
        return Optional.of(e);
      }
    }
    return Optional.empty();
  }

  public boolean isPositionOccupied(Position position) {
    return player.position().equals(position) || findEnemyAt(position).isPresent();
  }

  /** 指定座標の罠を返す。同座標複数は仕様上不在 (TurnEngine 側で上書き済) のため最初の 1 件で十分。 */
  public Optional<PlacedTrap> findTrapAt(Position position) {
    Objects.requireNonNull(position, "position");
    for (PlacedTrap t : placedTraps) {
      if (t.position().equals(position)) {
        return Optional.of(t);
      }
    }
    return Optional.empty();
  }
}
