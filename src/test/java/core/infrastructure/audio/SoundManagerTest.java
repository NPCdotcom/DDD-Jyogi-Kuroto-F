package core.infrastructure.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link SoundManager} の LibGDX 非依存な純粋ロジック (音量クランプ) と、音声種別 enum の不変条件を検証する。
 *
 * <p>{@code Gdx.audio} に依存する再生処理はヘッドレス環境で検証できないためテスト対象外。
 */
class SoundManagerTest {

  @Test
  void clampReturnsValueUnchangedWithinRange() {
    assertEquals(0.5f, SoundManager.clamp(0.5f));
  }

  @Test
  void clampFloorsValueBelowMinimumToZero() {
    assertEquals(0.0f, SoundManager.clamp(-0.3f));
  }

  @Test
  void clampCeilsValueAboveMaximumToOne() {
    assertEquals(1.0f, SoundManager.clamp(1.7f));
  }

  @Test
  void clampAcceptsExactBoundaries() {
    assertEquals(0.0f, SoundManager.clamp(0.0f));
    assertEquals(1.0f, SoundManager.clamp(1.0f));
  }

  @Test
  void everyBgmKindHasDistinctPathUnderBgmDir() {
    Set<String> paths = new HashSet<>();
    for (BgmKind kind : BgmKind.values()) {
      assertTrue(
          kind.path != null && kind.path.startsWith("bgm/"),
          "BGM path must be under bgm/: " + kind);
      assertTrue(paths.add(kind.path), "duplicate BGM path: " + kind.path);
    }
  }

  @Test
  void everySeKindHasDistinctPathUnderSeDir() {
    Set<String> paths = new HashSet<>();
    for (SeKind kind : SeKind.values()) {
      assertTrue(
          kind.path != null && kind.path.startsWith("se/"), "SE path must be under se/: " + kind);
      assertTrue(paths.add(kind.path), "duplicate SE path: " + kind.path);
    }
  }
}
