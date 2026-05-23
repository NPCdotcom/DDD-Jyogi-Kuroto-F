package core.infrastructure.audio;

/**
 * BGM 種別。{@link SoundManager#playBgm(BgmKind)} に渡す。
 *
 * <p>対応ファイルパスは {@link SoundManager} のコメントおよび {@code assets/audio/README.md} を参照。
 */
public enum BgmKind {
  /** タイトル / メニュー系画面で流れる BGM。 */
  TITLE("bgm/title.ogg"),

  /** ダンジョン画面で流れる BGM。 */
  DUNGEON("bgm/dungeon.ogg");

  /** {@code assets/audio/} からの相対パス。 */
  final String path;

  BgmKind(String path) {
    this.path = path;
  }
}
