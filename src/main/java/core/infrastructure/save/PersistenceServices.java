package core.infrastructure.save;

import java.util.Objects;

/**
 * 永続化系 3 コンポーネント (SaveManager / SettingsManager / Settings) を集約する holder (Wave 9 W9-β)。
 *
 * <p>従来 {@code DddGame} に個別フィールドとして散在していた 3 コンポーネントを 1 つの集約クラスにまとめ、 起動時に一括初期化する。Wave 8 W8-β の
 * {@code GameResources} と同型 (final class、ただし Disposable は不要)。
 *
 * <p>責務純度 (CTO レビュー反映 #3): 本クラスは <b>純粋にデータ層</b> として振る舞い、LibGDX の副作用 (フルスクリーン切替 / SoundManager 音量反映)
 * には依存しない。Settings 更新時の副作用は呼出側 ({@code DddGame.applySettings}) が仲介する形を維持し、infrastructure 層の純粋性
 * (LibGDX 非依存) を保つ。
 *
 * <p>final class を採用する理由は {@code settings} が mutable update を受けるため (record なら不変、final class
 * なら可変フィールドを許容)。
 */
public final class PersistenceServices {

  private final SaveManager saveManager;
  private final SettingsManager settingsManager;
  private Settings settings;

  private PersistenceServices(
      SaveManager saveManager, SettingsManager settingsManager, Settings settings) {
    this.saveManager = saveManager;
    this.settingsManager = settingsManager;
    this.settings = settings;
  }

  /**
   * 永続化サービス 3 件を起動時に一括初期化する。
   *
   * <ul>
   *   <li>{@link SaveManager}: new で生成 (ファイルパスは デフォルト)
   *   <li>{@link SettingsManager}: new で生成
   *   <li>{@link Settings}: SettingsManager.load() で取得 (ファイル欠損時は {@link Settings#DEFAULT})
   * </ul>
   */
  public static PersistenceServices load() {
    SaveManager saveManager = new SaveManager();
    SettingsManager settingsManager = new SettingsManager();
    Settings settings = settingsManager.load();
    return new PersistenceServices(saveManager, settingsManager, settings);
  }

  public SaveManager saveManager() {
    return saveManager;
  }

  public SettingsManager settingsManager() {
    return settingsManager;
  }

  public Settings settings() {
    return settings;
  }

  /**
   * Settings を新しい値に差し替える純粋なデータ更新 (CTO レビュー反映 #3)。
   *
   * <p>本メソッドは <b>LibGDX 副作用を含まない</b>。フルスクリーン切替 / SoundManager 音量反映等の副作用は呼出側 ({@code
   * DddGame.applySettings}) で別途実行する。
   *
   * @param newSettings 新しい設定値
   */
  public void apply(Settings newSettings) {
    Objects.requireNonNull(newSettings, "newSettings");
    this.settings = newSettings;
  }

  /** 現在の settings をファイルに保存する (SettingsScreen の ESC 時、初回プリセット選択後等)。 */
  public void save() {
    settingsManager.save(settings);
  }
}
