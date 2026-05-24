package core.presentation.screen;

import core.domain.entity.ActorId;
import core.domain.entity.EnemyKind;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 「直近観測時の敵 ID → kind」キャッシュ (§15-5 / E-7、Wave 4 W4-α で DungeonScreen から切り出し)。
 *
 * <p>{@code BattleEvent.ActorDied} は ActorId のみ持ち kind を持たないため、生存中に観測した kind を 覚えておく必要がある。 {@code
 * DungeonScreen.processNewEvents} で毎フレーム生存敵から記録し、 死亡イベントで {@link #getEnemyKind} を引いて Bestiary
 * に記録する流れ。
 *
 * <p>SaveData 永続化は M2 送り、本セッションはラン内のみ有効 (純粋にメモリ上のマップ)。
 *
 * <p>純粋データクラスとしてテスト容易性を確保: LibGDX 依存なし。
 */
public final class EnemyKindMemory {

  private final Map<ActorId, EnemyKind> kindById = new HashMap<>();

  /**
   * 生存中の敵 ID → kind を記録する。同じ ID で既に記録されている場合は上書きする (敵が transmute する 仕様は無いので実質的に no-op だが、API は冪等)。
   */
  public void recordEnemy(ActorId id, EnemyKind kind) {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(kind, "kind");
    kindById.put(id, kind);
  }

  /** 記録された ID の kind を返す。未記録なら empty。 */
  public Optional<EnemyKind> getEnemyKind(ActorId id) {
    Objects.requireNonNull(id, "id");
    return Optional.ofNullable(kindById.get(id));
  }

  /** 記録を全消去する (新ラン開始時 / hide() / dispose() 時に呼ぶ)。 */
  public void clear() {
    kindById.clear();
  }

  /** 現在記録されている敵 ID 数 (テスト / デバッグ用)。 */
  public int size() {
    return kindById.size();
  }
}
