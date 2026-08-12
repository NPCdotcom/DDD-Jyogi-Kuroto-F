package core.infrastructure.save;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 旧セーブ移行の journal (SAVE-02B)。{@code migration-state.json} へ保存する。
 *
 * <p>保持するのは「どの旧ファイルを」「移行し終えたか」の 2 点だけ。移行の値は毎回旧ファイルから 決定的に再計算するため ({@link LegacySaveMapper}
 * は純関数)、途中経過を記録して部分再開する必要がない。
 *
 * <p>初版は Profile 書込済 / Checkpoint 書込済のフラグも持っていたが、分岐に一度も使われず 削除した
 * (2026-08-13)。使われない状態を残すと「意味がある」と誤読され、 将来の分岐が壊れた前提の上に載る。
 *
 * <p>{@code migrationId} は旧ファイルの SHA-256。内容が同じ旧ファイルを二度処理しないための
 * 同一性判定に使う。旧ファイルが差し替わればハッシュが変わり、新しい移行として扱う。
 *
 * @param migrationId 旧 {@code save.json} の SHA-256 (16 進小文字)
 * @param completed 移行を完了したか。true の間は同じ migrationId を再処理しない
 */
public record LegacyMigrationState(
    @JsonProperty("migrationId") String migrationId, @JsonProperty("completed") boolean completed) {

  public LegacyMigrationState {
    if (migrationId == null || migrationId.isBlank()) {
      throw new IllegalArgumentException("migrationId must not be blank");
    }
  }

  /** 着手済みだが未完了の journal。 */
  public static LegacyMigrationState started(String migrationId) {
    return new LegacyMigrationState(migrationId, false);
  }

  public LegacyMigrationState withCompleted() {
    return new LegacyMigrationState(migrationId, true);
  }

  /** この journal が指定の旧ファイルの処理済み記録か。 */
  public boolean isCompletedFor(String otherMigrationId) {
    return completed && migrationId.equals(otherMigrationId);
  }
}
