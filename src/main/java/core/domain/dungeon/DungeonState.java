package core.domain.dungeon;

import core.domain.battle.TurnPhase;
import core.domain.common.Position;
import core.domain.entity.ActorId;
import core.domain.entity.Enemy;
import core.domain.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * ダンジョン 1 階層の現在状態。
 *
 * <p>すべての更新メソッドは新インスタンスを返す (不変)。enemies は内部で defensive copy する。
 */
public record DungeonState(DungeonMap map, Player player, List<Enemy> enemies, TurnPhase phase) {

  public DungeonState {
    Objects.requireNonNull(map, "map");
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(enemies, "enemies");
    Objects.requireNonNull(phase, "phase");
    enemies = List.copyOf(enemies);
  }

  public DungeonState withPlayer(Player newPlayer) {
    return new DungeonState(map, newPlayer, enemies, phase);
  }

  public DungeonState withEnemies(List<Enemy> newEnemies) {
    return new DungeonState(map, player, newEnemies, phase);
  }

  public DungeonState withPhase(TurnPhase newPhase) {
    return new DungeonState(map, player, enemies, newPhase);
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
}
