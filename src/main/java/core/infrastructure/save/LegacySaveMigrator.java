package core.infrastructure.save;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 旧 {@code save.json} を Profile / RunCheckpoint へ移行する (SAVE-02B)。
 *
 * <p>書込の途中で停止しても、再起動して同じ旧ファイルを与えれば全体を <b>決定的に再生成する</b> ({@link LegacySaveMapper} は純関数)。途中まで書いた
 * Profile に差分を足すのではなく常に上書き するため、何度中断・再実行しても返金が累積しない。
 *
 * <p>{@link LegacyMigrationState} が持つのは「どの旧ファイルを」「完了したか」の 2 点だけ。 部分再開のための進捗フラグは持たない —
 * 全体を作り直す方が、書込地点ごとの分岐を正しく 保つより壊れにくい。journal の役割は (a) 完了済みの再処理を防ぐ、(b) 中断中の再実行を 既存 Profile
 * のクロバーと区別する、の 2 つに絞る。
 *
 * <p>完了マーカーを書くまで旧 {@code save.json} を削除しない。移行が中断しても原本が残る。
 */
public final class LegacySaveMigrator {

  private static final Logger LOG = Logger.getLogger(LegacySaveMigrator.class.getName());

  /** 移行後に原本を退避する先のファイル名。削除しないのは撤回時の復旧手段を残すため。 */
  static final String LEGACY_BACKUP_FILE_NAME = "save.json.legacy-backup";

  private static void deleteQuietly(File file) {
    if (file.exists() && !file.delete()) {
      LOG.warning("Failed to delete: " + file.getAbsolutePath());
    }
  }

  private final SaveManager saveManager;

  public LegacySaveMigrator(SaveManager saveManager) {
    this.saveManager = Objects.requireNonNull(saveManager, "saveManager");
  }

  /**
   * 移行の結果。
   *
   * @param migrated 今回移行を実行したか (不要だった場合は false)
   * @param warnings ユーザーへ提示すべき通知
   */
  public record MigrationResult(boolean migrated, List<String> warnings) {

    public MigrationResult {
      warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
    }

    static MigrationResult skipped() {
      return new MigrationResult(false, List.of());
    }
  }

  /**
   * 必要なら旧セーブを移行する。
   *
   * <p>旧ファイルが無い、または同じ内容を移行済みの場合は何もしない。
   */
  public MigrationResult migrateIfNeeded() {
    File legacyFile = saveManager.legacySaveFile();
    if (!legacyFile.isFile()) {
      return MigrationResult.skipped();
    }

    Optional<String> migrationId = sha256(legacyFile);
    if (migrationId.isEmpty()) {
      return MigrationResult.skipped();
    }
    String id = migrationId.get();

    Optional<LegacyMigrationState> journal = saveManager.loadMigrationState();
    if (journal.isPresent() && journal.get().isCompletedFor(id)) {
      // 完了済み。旧ファイルが残っていても再処理しない。
      return MigrationResult.skipped();
    }

    Optional<SaveData> legacy = saveManager.load();
    if (legacy.isEmpty()) {
      LOG.warning("Legacy save is unreadable; skipping migration: " + legacyFile.getAbsolutePath());
      return MigrationResult.skipped();
    }

    // 同じ旧ファイルの移行が中断している場合だけ、書きかけの Profile を上書きしてよい。
    boolean resuming =
        journal.isPresent() && journal.get().migrationId().equals(id) && !journal.get().completed();

    // それ以外で既存 Profile がある状態の移行は恒久進捗の全損に直結するため実行しない。
    // 旧 save.json はクラウド同期の復元や旧ビルドの 1 回起動で再出現しうる。
    if (saveManager.profileExists() && !resuming) {
      String message =
          "旧 save.json が見つかりましたが、既に新形式の profile.json があるため移行しません。"
              + "旧ファイルは削除せず残します: "
              + legacyFile.getAbsolutePath();
      LOG.warning(message);
      return new MigrationResult(false, List.of(message));
    }

    // 値は毎回ここで再計算する。中断後の再実行でも同じ結果になり、返金が累積しない。
    LegacySaveMapper.Result mapped = LegacySaveMapper.map(legacy.get());

    // 着手を記録してから書く。中断してもこの journal があるので、同じ旧ファイルの再実行を
    // 「既存 Profile のクロバー」ではなく「再開」と判別できる。
    LegacyMigrationState state = LegacyMigrationState.started(id);
    if (!saveManager.saveMigrationState(state).isSuccess()) {
      LOG.severe("Migration aborted: failed to write journal");
      return new MigrationResult(false, mapped.warnings());
    }

    if (!saveManager.saveProfile(mapped.profile()).isSuccess()) {
      LOG.severe("Migration aborted: failed to write profile");
      return new MigrationResult(false, mapped.warnings());
    }

    if (mapped.checkpoint().isPresent()
        && !saveManager.saveCheckpoint(mapped.checkpoint().get()).isSuccess()) {
      LOG.severe("Migration aborted: failed to write checkpoint");
      return new MigrationResult(false, mapped.warnings());
    }

    // 読み直して一致を確認できた場合だけ完了とする。
    if (!verify(mapped)) {
      LOG.severe("Migration aborted: verification failed");
      return new MigrationResult(false, mapped.warnings());
    }

    // 完了マーカーの書込に失敗したら旧ファイルを消さない。原本を残して次回やり直す。
    if (!saveManager.saveMigrationState(state.withCompleted()).isSuccess()) {
      LOG.severe("Migration incomplete: failed to write completion marker; keeping legacy save");
      return new MigrationResult(false, mapped.warnings());
    }
    // 完了マーカーを書けた後に原本を退避する。削除ではなくリネームにするのは、移行後に
    // 不具合が見つかってこの版を撤回したくなったとき、プレイヤーの手元に旧ビルドが読める
    // ファイルを 1 つも残さない状態を避けるため。次のマイナー版で削除に切り替える。
    File backup = new File(legacyFile.getParentFile(), LEGACY_BACKUP_FILE_NAME);
    deleteQuietly(backup);
    if (legacyFile.renameTo(backup)) {
      LOG.info("Legacy save migrated and archived as " + LEGACY_BACKUP_FILE_NAME + ": " + id);
    } else {
      // 退避できなくても移行自体は成立している。原本を残したままにすると次回起動で
      // 「既に profile.json がある」として移行を拒否するので、実害はない。
      LOG.warning("Migrated but failed to archive legacy save: " + legacyFile.getAbsolutePath());
    }
    return new MigrationResult(true, mapped.warnings());
  }

  /** 書き込んだ内容を読み直し、写像結果と一致するか確認する。 */
  private boolean verify(LegacySaveMapper.Result mapped) {
    Optional<ProfileData> profile = saveManager.loadProfile();
    if (profile.isEmpty() || !profile.get().equals(mapped.profile())) {
      return false;
    }
    if (mapped.checkpoint().isEmpty()) {
      return true;
    }
    Optional<RunCheckpoint> checkpoint = saveManager.loadCheckpoint();
    return checkpoint.isPresent() && checkpoint.get().equals(mapped.checkpoint().get());
  }

  /** 旧ファイルの SHA-256 を 16 進小文字で返す。読めない場合は empty。 */
  static Optional<String> sha256(File file) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(Files.readAllBytes(file.toPath()));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(Character.forDigit((b >> 4) & 0xF, 16));
        hex.append(Character.forDigit(b & 0xF, 16));
      }
      return Optional.of(hex.toString());
    } catch (IOException | NoSuchAlgorithmException e) {
      LOG.log(Level.WARNING, "Failed to hash legacy save: " + file.getAbsolutePath(), e);
      return Optional.empty();
    }
  }
}
