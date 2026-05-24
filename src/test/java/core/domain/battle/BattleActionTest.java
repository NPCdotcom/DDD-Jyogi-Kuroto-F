package core.domain.battle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import core.domain.common.Direction;
import org.junit.jupiter.api.Test;

/**
 * {@link BattleAction} の各 record の compact constructor バリデーション検証。
 *
 * <p>sealed interface の網羅確認: Move / UseSkill / UseCard / Wait / EndTurn。 各 record の compact
 * constructor が正当な入力を受け入れ、不当な入力を適切な例外で弾くことを検証する。
 */
class BattleActionTest {

  // ---- Move ----

  @Test
  void moveWithValidDirectionConstructsSuccessfully() {
    // ハッピーパス: 有効な Direction で Move を生成できる
    assertDoesNotThrow(() -> new BattleAction.Move(Direction.RIGHT));
  }

  @Test
  void moveRejectsNullDirection() {
    // Move の compact constructor は direction が null なら NullPointerException
    assertThrows(NullPointerException.class, () -> new BattleAction.Move(null));
  }

  @Test
  void moveExposesDirection() {
    // Move から取り出した direction が一致する (record accessor 確認)
    BattleAction.Move move = new BattleAction.Move(Direction.DOWN);
    assertEquals(Direction.DOWN, move.direction());
  }

  // ---- UseSkill ----

  @Test
  void useSkillWithIndexZeroConstructsSuccessfully() {
    // slotIndex=0 は最小有効値 (境界値)
    assertDoesNotThrow(() -> new BattleAction.UseSkill(0));
  }

  @Test
  void useSkillRejectsNegativeSlotIndex() {
    // slotIndex < 0 は IllegalArgumentException (compact constructor 検証)
    assertThrows(IllegalArgumentException.class, () -> new BattleAction.UseSkill(-1));
  }

  @Test
  void useSkillExposesSlotIndex() {
    BattleAction.UseSkill action = new BattleAction.UseSkill(2);
    assertEquals(2, action.slotIndex());
  }

  // ---- UseCard ----

  @Test
  void useCardWithHandIndexZeroConstructsSuccessfully() {
    // handIndex=0 は最小有効値 (境界値)
    BattleAction.UseCard action =
        assertDoesNotThrow(() -> new BattleAction.UseCard(0, Direction.RIGHT));
    assertEquals(0, action.handIndex());
  }

  @Test
  void useCardRejectsNegativeHandIndex() {
    // §15-3: 手札インデックスは 0 以上でなければならない
    assertThrows(
        IllegalArgumentException.class, () -> new BattleAction.UseCard(-1, Direction.RIGHT));
  }

  @Test
  void useCardRejectsNullDirection() {
    // direction は必須フィールド → null は NullPointerException
    assertThrows(NullPointerException.class, () -> new BattleAction.UseCard(0, null));
  }

  @Test
  void useCardExposesHandIndexAndDirection() {
    BattleAction.UseCard action = new BattleAction.UseCard(3, Direction.LEFT);
    assertEquals(3, action.handIndex());
    assertEquals(Direction.LEFT, action.direction());
  }

  // ---- Wait ----

  @Test
  void waitConstructsSuccessfully() {
    // Wait は引数なし、常に生成可能
    assertInstanceOf(BattleAction.Wait.class, assertDoesNotThrow(BattleAction.Wait::new));
  }

  // ---- EndTurn ----

  @Test
  void endTurnConstructsSuccessfully() {
    // EndTurn は引数なし、常に生成可能
    assertInstanceOf(BattleAction.EndTurn.class, assertDoesNotThrow(BattleAction.EndTurn::new));
  }

  // ---- sealed interface のパターンマッチング網羅確認 ----

  @Test
  void sealedVariantsAreDistinguishable() {
    // sealed permit 5 種すべてが instanceof で区別できる
    BattleAction move = new BattleAction.Move(Direction.UP);
    BattleAction useSkill = new BattleAction.UseSkill(0);
    BattleAction useCard = new BattleAction.UseCard(0, Direction.UP);
    BattleAction wait = new BattleAction.Wait();
    BattleAction endTurn = new BattleAction.EndTurn();

    assertInstanceOf(BattleAction.Move.class, move);
    assertInstanceOf(BattleAction.UseSkill.class, useSkill);
    assertInstanceOf(BattleAction.UseCard.class, useCard);
    assertInstanceOf(BattleAction.Wait.class, wait);
    assertInstanceOf(BattleAction.EndTurn.class, endTurn);
  }
}
