package core.infrastructure.save;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 設定ファイルの永続化を担当する (§15-1)。
 *
 * <p>保存先: {@code <user.home>/.ddd-jyogi-kuroto-f/settings.json}。セーブデータ ({@code save.json})
 * とは独立したファイル。
 *
 * <p>破損 JSON・欠損ファイル・I/O エラーは {@link Settings#DEFAULT} を返して呼出元をクラッシュさせない (graceful)。 エラーは {@link
 * Logger} で WARN/SEVERE レベルに記録する。パターンは {@link SaveManager} と同一。
 */
public final class SettingsManager {

  private static final Logger LOG = Logger.getLogger(SettingsManager.class.getName());

  private static final String SETTINGS_FILE_NAME = "settings.json";

  private final File settingsFile;
  private final ObjectMapper mapper;

  /** デフォルトコンストラクタ。保存先は {@code <user.home>/.ddd-jyogi-kuroto-f/settings.json}。 */
  public SettingsManager() {
    this(
        new File(
            System.getProperty("user.home"),
            SaveManager.SAVE_DIR_NAME + File.separator + SETTINGS_FILE_NAME));
  }

  /**
   * テスト用コンストラクタ。任意の保存先を指定できる。
   *
   * @param settingsFile 設定ファイルの絶対パス
   */
  SettingsManager(File settingsFile) {
    this.settingsFile = settingsFile;
    this.mapper = new ObjectMapper();
    this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
  }

  /**
   * 設定をファイルに書き込む。
   *
   * <p>保存先ディレクトリが存在しない場合は自動生成する。I/O エラー時は SEVERE ログを出力して失敗を無視する。
   *
   * @param settings 保存する設定
   */
  public void save(Settings settings) {
    try {
      File dir = settingsFile.getParentFile();
      if (dir != null && !dir.exists()) {
        boolean created = dir.mkdirs();
        if (!created && !dir.exists()) {
          LOG.severe("Failed to create settings directory: " + dir.getAbsolutePath());
          return;
        }
      }
      mapper.writeValue(settingsFile, settings);
      LOG.info("Settings saved to " + settingsFile.getAbsolutePath());
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Failed to save settings: " + settingsFile.getAbsolutePath(), e);
    }
  }

  /**
   * 設定をファイルから読み込む。
   *
   * <p>ファイルが存在しない場合・破損 JSON・バリデーション違反は {@link Settings#DEFAULT} を返す (graceful)。
   *
   * @return 読み込んだ設定、ファイルなし / 破損時はデフォルト設定
   */
  public Settings load() {
    if (!settingsFile.exists()) {
      return Settings.DEFAULT;
    }
    try {
      Settings loaded = mapper.readValue(settingsFile, Settings.class);
      if (loaded == null) {
        LOG.warning("Settings deserialized to null, using DEFAULT");
        return Settings.DEFAULT;
      }
      return loaded;
    } catch (IOException e) {
      LOG.log(
          Level.WARNING,
          "Failed to load settings (using DEFAULT): " + settingsFile.getAbsolutePath(),
          e);
      return Settings.DEFAULT;
    } catch (IllegalArgumentException e) {
      // Settings compact constructor のバリデーション違反 (スキーマ不整合)
      LOG.log(
          Level.WARNING,
          "Invalid settings data (using DEFAULT): " + settingsFile.getAbsolutePath(),
          e);
      return Settings.DEFAULT;
    }
  }

  /**
   * 設定ファイルが存在するかを返す。
   *
   * <p>初回起動判定 (UI プリセット選択画面の表示有無) に使う。
   *
   * @return ファイルが存在すれば true
   */
  public boolean exists() {
    return settingsFile.exists() && settingsFile.isFile();
  }
}
