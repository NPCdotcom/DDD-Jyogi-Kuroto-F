package core.domain.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LayerTest {

  // ハッピーパス

  @Test
  void firstIsLayer1() {
    // §15-6 「1 層目から開始」: first() は number=1 / displayName="1 層" を返す
    Layer first = Layer.first();
    assertEquals(1, first.number());
    assertEquals("1 層", first.displayName());
  }

  @Test
  void nextIncrementsNumber() {
    // §15-6 「層が進むほど敵が強化」: next() で number が +1 かつ displayName が更新される
    Layer second = Layer.first().next();
    assertEquals(2, second.number());
    assertEquals("2 層", second.displayName());
  }

  @Test
  void nextChainsCorrectly() {
    // next() を連鎖して 3 層に到達できることを確認 (immutable chain)
    Layer third = Layer.first().next().next();
    assertEquals(3, third.number());
    assertEquals("3 層", third.displayName());
  }

  // 不変性確認

  @Test
  void nextReturnsNewInstanceLeavingOriginalIntact() {
    // record の next() が新インスタンスを返し、元の Layer は変化しない
    Layer first = Layer.first();
    Layer second = first.next();
    assertEquals(1, first.number(), "元の Layer は変化してはならない");
    assertEquals(2, second.number());
    assertNotSame(first, second);
  }

  // 境界値

  @Test
  void numberOneIsAccepted() {
    // 最小有効値 1 で構築できる
    Layer l = new Layer(1, "1 層");
    assertEquals(1, l.number());
  }

  // 例外パス

  @Test
  void numberZeroIsRejected() {
    // §15-6 「1 層始まり」: 0 は不正な層番号
    assertThrows(IllegalArgumentException.class, () -> new Layer(0, "0 層"));
  }

  @Test
  void numberNegativeIsRejected() {
    // 負数の層番号は意味を持たない
    assertThrows(IllegalArgumentException.class, () -> new Layer(-1, "負層"));
  }

  @Test
  void displayNameNullIsRejected() {
    assertThrows(NullPointerException.class, () -> new Layer(1, null));
  }

  @Test
  void displayNameEmptyIsRejected() {
    // 空文字は HUD 表示として無意味
    assertThrows(IllegalArgumentException.class, () -> new Layer(1, ""));
  }

  @Test
  void displayNameBlankIsRejected() {
    // 空白のみも HUD 表示として無意味
    assertThrows(IllegalArgumentException.class, () -> new Layer(1, "   "));
  }
}
