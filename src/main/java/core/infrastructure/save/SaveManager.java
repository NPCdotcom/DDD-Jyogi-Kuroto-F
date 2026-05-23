package core.infrastructure.save;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 層単位セーブの永続化を担当する (§15-11)。
 *
 * <p>保存先: {@code <user.home>/.ddd-jyogi-kuroto-f/save.json}。スロットは 1 つ。
 *
 * <p>破損 JSON・欠損ファイル・I/O エラーは「セーブなし」として扱い、呼出元をクラッシュさせない (graceful)。 エラーは {@link Logger} で
 * WARN/SEVERE レベルに記録する。
 */
public final class SaveManager {

  private static final Logger LOG = Logger.getLogger(SaveManager.class.getName());

  /** 全セーブ系ファイルを格納するユーザーホーム配下のディレクトリ名。SettingsManager もこの定数を参照して書込先を一致させる。 */
  public static final String SAVE_DIR_NAME = ".ddd-jyogi-kuroto-f";

  private static final String SAVE_FILE_NAME = "save.json";

  private final File saveFile;
  private final ObjectMapper mapper;

  /** デフォルトコンストラクタ。保存先は {@code <user.home>/.ddd-jyogi-kuroto-f/save.json}。 */
  public SaveManager() {
    this(
        new File(System.getProperty("user.home"), SAVE_DIR_NAME + File.separator + SAVE_FILE_NAME));
  }

  /**
   * テスト用コンストラクタ。任意の保存先を指定できる。
   *
   * @param saveFile セーブファイルの絶対パス
   */
  SaveManager(File saveFile) {
    this.saveFile = saveFile;
    this.mapper = new ObjectMapper();
    this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
  }

  /**
   * セーブデータをファイルに書き込む。
   *
   * <p>保存先ディレクトリが存在しない場合は自動生成する。I/O エラー時は SEVERE ログを出力して失敗を無視する (セーブ失敗は警告だが即クラッシュしない設計)。
   *
   * @param data 保存するセーブデータ
   */
  public void save(SaveData data) {
    try {
      File dir = saveFile.getParentFile();
      if (dir != null && !dir.exists()) {
        boolean created = dir.mkdirs();
        if (!created && !dir.exists()) {
          LOG.severe("Failed to create save directory: " + dir.getAbsolutePath());
          return;
        }
      }
      mapper.writeValue(saveFile, data);
      LOG.info("Saved to " + saveFile.getAbsolutePath());
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Failed to save: " + saveFile.getAbsolutePath(), e);
    }
  }

  /**
   * セーブデータをファイルから読み込む。
   *
   * <p>ファイルが存在しない場合・破損 JSON・欠損フィールドは {@link Optional#empty()} を返す (graceful)。
   *
   * @return 読み込んだセーブデータ。ファイルなし / 破損時は empty
   */
  public Optional<SaveData> load() {
    if (!saveFile.exists()) {
      return Optional.empty();
    }
    try {
      SaveData data = mapper.readValue(saveFile, SaveData.class);
      return Optional.of(data);
    } catch (IOException e) {
      LOG.log(
          Level.WARNING, "Failed to load (treating as no save): " + saveFile.getAbsolutePath(), e);
      return Optional.empty();
    } catch (IllegalArgumentException e) {
      // SaveData compact constructor のバリデーション違反 (スキーマ不整合) も graceful に扱う
      LOG.log(
          Level.WARNING,
          "Invalid save data (treating as no save): " + saveFile.getAbsolutePath(),
          e);
      return Optional.empty();
    }
  }

  /**
   * セーブファイルが存在するかを返す。
   *
   * @return ファイルが存在すれば true
   */
  public boolean exists() {
    return saveFile.exists() && saveFile.isFile();
  }

  /**
   * セーブファイルを削除する。
   *
   * <p>存在しない場合は何もしない。削除失敗は WARN ログを出力するが例外は投げない。
   */
  public void delete() {
    if (!saveFile.exists()) {
      return;
    }
    boolean deleted = saveFile.delete();
    if (!deleted) {
      LOG.warning("Failed to delete save file: " + saveFile.getAbsolutePath());
    } else {
      LOG.info("Deleted save file: " + saveFile.getAbsolutePath());
    }
  }
}
