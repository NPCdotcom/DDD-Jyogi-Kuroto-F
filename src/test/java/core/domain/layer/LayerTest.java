package core.domain.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LayerTest {

  // ハッピーパス

  @Test
  void firstIsLayer1Room1() {
    // §15-6「1 層目から開始」: first() は number=1 / roomIndex=1 / displayName="1 層"
    Layer first = Layer.first();
    assertEquals(1, first.number());
    assertEquals(1, first.roomIndex());
    assertEquals("1 層", first.displayName());
  }

  @Test
  void nextIncrementsNumberAndResetsRoomIndex() {
    // §15-6「層が進むほど敵が強化」: next() で number +1、roomIndex は 1 にリセット。
    // 部屋 2 まで進んだ層から next() しても部屋番号が 1 に戻ることを確認。
    Layer second = Layer.first().nextRoom().next();
    assertEquals(2, second.number());
    assertEquals(1, second.roomIndex(), "next() は部屋番号を 1 にリセットする");
    assertEquals("2 層", second.displayName());
  }

  @Test
  void nextChainsCorrectly() {
    // next() を連鎖して 3 層に到達できることを確認 (immutable chain)
    Layer third = Layer.first().next().next();
    assertEquals(3, third.number());
    assertEquals("3 層", third.displayName());
  }

  @Test
  void nextRoomIncrementsRoomIndexKeepingLayer() {
    // §15-6「N 層 = N 部屋」: nextRoom() は同じ層で部屋番号だけ +1
    Layer room2 = Layer.first().nextRoom();
    assertEquals(1, room2.number(), "層番号は据置");
    assertEquals(2, room2.roomIndex());
    assertEquals("1 層", room2.displayName(), "displayName は据置");
  }

  @Test
  void nextRoomChainsWithinLayer() {
    Layer room3 = Layer.first().nextRoom().nextRoom();
    assertEquals(1, room3.number());
    assertEquals(3, room3.roomIndex());
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

  @Test
  void nextRoomReturnsNewInstanceLeavingOriginalIntact() {
    Layer first = Layer.first();
    Layer room2 = first.nextRoom();
    assertEquals(1, first.roomIndex(), "元の Layer は変化してはならない");
    assertEquals(2, room2.roomIndex());
    assertNotSame(first, room2);
  }

  // 境界値

  @Test
  void numberOneRoomOneIsAccepted() {
    // 最小有効値 (1, 1) で構築できる
    Layer l = new Layer(1, 1, "1 層");
    assertEquals(1, l.number());
    assertEquals(1, l.roomIndex());
  }

  // 例外パス

  @Test
  void numberZeroIsRejected() {
    // §15-6「1 層始まり」: 0 は不正な層番号
    assertThrows(IllegalArgumentException.class, () -> new Layer(0, 1, "0 層"));
  }

  @Test
  void numberNegativeIsRejected() {
    // 負数の層番号は意味を持たない
    assertThrows(IllegalArgumentException.class, () -> new Layer(-1, 1, "負層"));
  }

  @Test
  void roomIndexZeroIsRejected() {
    // 部屋番号は 1 始まり、0 は不正
    assertThrows(IllegalArgumentException.class, () -> new Layer(1, 0, "1 層"));
  }

  @Test
  void roomIndexNegativeIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new Layer(1, -1, "1 層"));
  }

  @Test
  void displayNameNullIsRejected() {
    assertThrows(NullPointerException.class, () -> new Layer(1, 1, null));
  }

  @Test
  void displayNameEmptyIsRejected() {
    // 空文字は HUD 表示として無意味
    assertThrows(IllegalArgumentException.class, () -> new Layer(1, 1, ""));
  }

  @Test
  void displayNameBlankIsRejected() {
    // 空白のみも HUD 表示として無意味
    assertThrows(IllegalArgumentException.class, () -> new Layer(1, 1, "   "));
  }
}
