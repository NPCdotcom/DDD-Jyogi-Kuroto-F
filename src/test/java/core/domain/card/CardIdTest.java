package core.domain.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * CardId の不変条件テスト。
 *
 * <p>ADR-16 §E-1: CardId は null / 空文字列 / blank を拒否し、値ベースの等価性を保つ。
 */
class CardIdTest {

  // ハッピーパス

  @Test
  void validStringCreatesCardId() {
    // ADR-16: of(String) ファクトリが正常値で成功する
    CardId id = CardId.of("strike-001");
    assertEquals("strike-001", id.value());
  }

  @Test
  void sameValueEqualsEachOther() {
    // record の値ベース equals: 同じ文字列なら equals == true
    assertEquals(CardId.of("abc"), CardId.of("abc"));
  }

  // 境界値 / 例外パス

  @Test
  void nullValueThrowsNullPointerException() {
    // GAME_DESIGN §15-3: ID は必ず存在する識別子
    assertThrows(NullPointerException.class, () -> CardId.of(null));
  }

  @Test
  void emptyStringThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> CardId.of(""));
  }

  @Test
  void blankStringThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> CardId.of("  "));
  }
}
