package core.infrastructure.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;
import core.infrastructure.save.Settings;
import java.util.EnumMap;
import java.util.Map;

/**
 * BGM ({@link Music}) と SE ({@link Sound}) を一元管理するサウンドマネージャ。
 *
 * <h2>音声ファイル配置規約 (チームメイト向け)</h2>
 *
 * <pre>
 * assets/audio/
 *   bgm/
 *     title.ogg      … タイトル / メニュー系画面 BGM
 *     dungeon.ogg    … ダンジョン戦闘 BGM
 *   se/
 *     enemy_defeated.ogg … 敵撃破 SE
 *     player_damaged.ogg … 被ダメ SE
 *     deal_damage.ogg    … 与ダメ SE
 *     card_used.ogg      … カード使用 SE
 *     button.ogg         … ボタン操作 / 画面遷移 SE
 *     floor_advance.ogg  … 層遷移 SE
 * </pre>
 *
 * <p>ファイルが存在しない場合は起動時に {@code Gdx.app.log} で警告を出力し、当該音の再生を no-op にする。 1 つもファイルがなくてもゲームは正常動作する
 * (graceful degradation)。
 *
 * <h2>音量</h2>
 *
 * <p>BGM 音量・SE 音量は {@link Settings} の値を参照する。{@link #applySettings(Settings)} で即時反映できる。
 */
public final class SoundManager implements Disposable {

  private static final String LOG_TAG = "SoundManager";
  private static final String AUDIO_BASE = "audio/";

  /** 現在再生中の BGM 種別 (停止中は null)。 */
  private BgmKind currentBgm = null;

  /** BGM インスタンスキャッシュ (ロード済みのもののみ格納)。 */
  private final Map<BgmKind, Music> bgmMap = new EnumMap<>(BgmKind.class);

  /** SE インスタンスキャッシュ (ロード済みのもののみ格納)。 */
  private final Map<SeKind, Sound> seMap = new EnumMap<>(SeKind.class);

  private float bgmVolume;
  private float seVolume;

  /**
   * {@link Settings} を受け取って初期化し、音声ファイルを非同期ロードする。
   *
   * @param settings 初期音量設定
   */
  public SoundManager(Settings settings) {
    this.bgmVolume = clamp(settings.bgmVolume());
    this.seVolume = clamp(settings.seVolume());
    loadAll();
  }

  // =========================================================================
  // 公開 API
  // =========================================================================

  /**
   * SE を再生する。ファイルが存在しない SE 種別は no-op。
   *
   * @param kind SE 種別
   */
  public void playSe(SeKind kind) {
    Sound sound = seMap.get(kind);
    if (sound == null) {
      return;
    }
    sound.play(seVolume);
  }

  /**
   * BGM を再生する。既に同じ種別が再生中なら何もしない。別の種別が再生中なら停止してから開始。 ファイルが存在しない BGM 種別は no-op。
   *
   * @param kind BGM 種別
   */
  public void playBgm(BgmKind kind) {
    if (kind == currentBgm) {
      return;
    }
    stopBgm();
    Music music = bgmMap.get(kind);
    if (music == null) {
      return;
    }
    music.setVolume(bgmVolume);
    music.setLooping(true);
    music.play();
    currentBgm = kind;
  }

  /** 現在再生中の BGM を停止する。再生中でなければ no-op。 */
  public void stopBgm() {
    if (currentBgm == null) {
      return;
    }
    Music music = bgmMap.get(currentBgm);
    if (music != null) {
      music.stop();
    }
    currentBgm = null;
  }

  /**
   * {@link Settings} を受け取って音量を即時更新する。再生中の BGM にも適用する。
   *
   * @param settings 新しい設定
   */
  public void applySettings(Settings settings) {
    this.bgmVolume = clamp(settings.bgmVolume());
    this.seVolume = clamp(settings.seVolume());
    // 再生中 BGM の音量を即時反映
    if (currentBgm != null) {
      Music music = bgmMap.get(currentBgm);
      if (music != null) {
        music.setVolume(bgmVolume);
      }
    }
  }

  /**
   * 現在の BGM 音量 (0.0〜1.0)。
   *
   * @return BGM 音量
   */
  public float bgmVolume() {
    return bgmVolume;
  }

  /**
   * 現在の SE 音量 (0.0〜1.0)。
   *
   * @return SE 音量
   */
  public float seVolume() {
    return seVolume;
  }

  @Override
  public void dispose() {
    for (Music m : bgmMap.values()) {
      m.dispose();
    }
    bgmMap.clear();
    for (Sound s : seMap.values()) {
      s.dispose();
    }
    seMap.clear();
    currentBgm = null;
  }

  // =========================================================================
  // 内部処理
  // =========================================================================

  /** 起動時に全音声ファイルのロードを試みる。ファイル欠損は警告ログのみ (クラッシュしない)。 */
  private void loadAll() {
    for (BgmKind kind : BgmKind.values()) {
      tryLoadBgm(kind);
    }
    for (SeKind kind : SeKind.values()) {
      tryLoadSe(kind);
    }
  }

  private void tryLoadBgm(BgmKind kind) {
    String path = AUDIO_BASE + kind.path;
    try {
      FileHandle fh = Gdx.files.internal(path);
      if (!fh.exists()) {
        Gdx.app.log(LOG_TAG, "BGM file not found (will be silent): " + path);
        return;
      }
      Music music = Gdx.audio.newMusic(fh);
      bgmMap.put(kind, music);
    } catch (Exception e) {
      Gdx.app.log(LOG_TAG, "Failed to load BGM " + path + ": " + e.getMessage());
    }
  }

  private void tryLoadSe(SeKind kind) {
    String path = AUDIO_BASE + kind.path;
    try {
      FileHandle fh = Gdx.files.internal(path);
      if (!fh.exists()) {
        Gdx.app.log(LOG_TAG, "SE file not found (will be silent): " + path);
        return;
      }
      Sound sound = Gdx.audio.newSound(fh);
      seMap.put(kind, sound);
    } catch (Exception e) {
      Gdx.app.log(LOG_TAG, "Failed to load SE " + path + ": " + e.getMessage());
    }
  }

  /** 音量値を 0.0〜1.0 にクランプする。 */
  static float clamp(float v) {
    return Math.max(Settings.VOLUME_MIN, Math.min(Settings.VOLUME_MAX, v));
  }
}
