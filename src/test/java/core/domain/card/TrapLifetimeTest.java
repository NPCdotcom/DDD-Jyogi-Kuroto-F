package core.domain.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * TrapLifetime sealed 型の各バリアントテスト。
 *
 * <p>ADR-16 検証ポイント 6: TrapLifetime.Turns(remaining=0) で例外。
 * ADR-16 検証ポイント 14: UntilStepped.INSTANCE が equals で同じ (値ベース)。
 * GAME_DESIGN §15-3: 物理罠は踏まれるまで永続、魔法罠は N ターンで消滅。
 */
class TrapLifetimeTest {

  // =========================================================
  // 検証ポイント 14: UntilStepped.INSTANCE の値ベース等価性
  // =========================================================

  @Test
  void untilSteppedInstanceEqualsNewInstance() {
    // 検証ポイント 14: record の値ベース equals (参照同一性に依存しない)
    // INSTANCE と new UntilStepped() は equals で等価でなければならない
    TrapLifetime.UntilStepped other = new TrapLifetime.UntilStepped();
    assertEquals(TrapLifetime.UntilStepped.INSTANCE, other,
        "record は値ベース equals なので INSTANCE と new 生成は等価");
  }

  @Test
  void untilSteppedInstanceIsSameReference() {
    // INSTANCE 定数は static final なので同じ参照を再利用できる
    assertSame(TrapLifetime.UntilStepped.INSTANCE, TrapLifetime.UntilStepped.INSTANCE);
  }

  // =========================================================
  // 検証ポイント 6: Turns の境界値
  // =========================================================

  @Test
  void turnsRemainingOneIsMinimumValid() {
    // remaining=1 は最小有効値
    // GAME_DESIGN §15-3: 魔法罠は 1 ターン以上残る
    TrapLifetime.Turns t = new TrapLifetime.Turns(1);
    assertEquals(1, t.remaining());
  }

  @Test
  void turnsRemainingAboveOneIsAccepted() {
    TrapLifetime.Turns t = new TrapLifetime.Turns(3);
    assertEquals(3, t.remaining());
  }

  @Test
  void turnsRemainingZeroThrowsIllegalArgumentException() {
    // 検証ポイント 6: remaining=0 で例外
    // 設置直後に消える罠は意味を成さないため禁止
    assertThrows(IllegalArgumentException.class, () -> new TrapLifetime.Turns(0));
  }

  @Test
  void turnsRemainingNegativeThrowsIllegalArgumentException() {
    // 検証ポイント 6: remaining=-1 も例外
    assertThrows(IllegalArgumentException.class, () -> new TrapLifetime.Turns(-1));
  }

  // sealed switch の網羅性 (コンパイル確認)

  @Test
  void sealedSwitchCoversUntilSteppedAndTurns() {
    // sealed switch で default なしにより、将来バリアント追加時にコンパイルエラーで検知
    TrapLifetime phys = TrapLifetime.UntilStepped.INSTANCE;
    TrapLifetime magic = new TrapLifetime.Turns(2);

    assertEquals("physical", describeLifetime(phys));
    assertEquals("magic:2",  describeLifetime(magic));
  }

  private String describeLifetime(TrapLifetime lifetime) {
    return switch (lifetime) {
      case TrapLifetime.UntilStepped u -> "physical";
      case TrapLifetime.Turns        t -> "magic:" + t.remaining();
    };
  }
}
