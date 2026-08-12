package core.infrastructure.save;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 旧セーブ移行の journal (SAVE-02B)。{@code migration-state.json} へ保存する。
 *
 * <p>Profile、RunCheckpoint、完了マーカーを別々に書く途中で停止しても、どこまで進んだかを 記録しておくことで再起動後に不足側だけを再生成できる。
 *
 * <p>{@code migrationId} は旧ファイルの SHA-256。内容が同じ旧ファイルを二度処理しないための
 * 同一性判定に使う。旧ファイルが差し替わればハッシュが変わり、新しい移行として扱う。
 *
 * @param migrationId 旧 {@code save.json} の SHA-256 (16 進小文字)
 * @param profileWritten Profile を書き終えたか
 * @param checkpointWritten RunCheckpoint を書き終えたか
 * @param completed 検証まで終えたか。true の間は同じ migrationId を再処理しない
 */
public record LegacyMigrationState(
    @JsonProperty("migrationId") String migrationId,
    @JsonProperty("profileWritten") boolean profileWritten,
    @JsonProperty("checkpointWritten") boolean checkpointWritten,
    @JsonProperty("completed") boolean completed) {

  public LegacyMigrationState {
    if (migrationId == null || migrationId.isBlank()) {
      throw new IllegalArgumentException("migrationId must not be blank");
    }
  }

  /** 未着手の journal。 */
  public static LegacyMigrationState started(String migrationId) {
    return new LegacyMigrationState(migrationId, false, false, false);
  }

  public LegacyMigrationState withProfileWritten() {
    return new LegacyMigrationState(migrationId, true, checkpointWritten, completed);
  }

  public LegacyMigrationState withCheckpointWritten() {
    return new LegacyMigrationState(migrationId, profileWritten, true, completed);
  }

  public LegacyMigrationState withCompleted() {
    return new LegacyMigrationState(migrationId, profileWritten, checkpointWritten, true);
  }

  /** この journal が指定の旧ファイルの処理済み記録か。 */
  public boolean isCompletedFor(String otherMigrationId) {
    return completed && migrationId.equals(otherMigrationId);
  }
}
