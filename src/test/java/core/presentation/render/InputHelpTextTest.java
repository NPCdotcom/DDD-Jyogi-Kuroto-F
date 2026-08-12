package core.presentation.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import core.presentation.input.InputAction;
import org.junit.jupiter.api.Test;

/**
 * 操作説明の表示文が {@link InputAction} の割当と一致することを検証する (INPUT-01)。
 *
 * <p>レビュー P1-1 の再発防止。SPACE をターン終了と説明する誤記が 3 箇所にあり、正しい記述が 2 箇所にあるという自己矛盾が起きていた。
 *
 * <p>{@code PlayerInputs} は {@code Gdx.input} の静的参照を持ち LibGDX headless 基盤も無いため直接駆動しない。 代わりに、両者が参照する
 * {@link InputAction} と表示文を突き合わせる。既存の {@code CardDescriberTest} 等と 同じく純粋クラスのみを対象とする。
 */
class InputHelpTextTest {

  private static final String WAIT_KEY = InputAction.WAIT.displayToken();
  private static final String END_TURN_KEY = InputAction.END_TURN.displayToken();

  // ---------------- 割当そのもの ----------------

  @Test
  void waitAndEndTurnUseDifferentKeys() {
    assertFalse(WAIT_KEY.equals(END_TURN_KEY));
    assertFalse(InputAction.WAIT.keyCode() == InputAction.END_TURN.keyCode());
  }

  @Test
  void keyCodesMatchLibGdxInputKeys() {
    // com.badlogic.gdx.Input.Keys.SPACE / ENTER。純粋性のため直値で保持しているので固定する。
    assertEquals(62, InputAction.WAIT.keyCode());
    assertEquals(66, InputAction.END_TURN.keyCode());
  }

  // ---------------- 操作ヒント (元から正しかった箇所を壊さない) ----------------

  @Test
  void japaneseControlsHintKeepsWaitOnWaitKey() {
    assertTrue(Strings.Ja.CONTROLS_WAIT.contains(WAIT_KEY));
    assertTrue(Strings.Ja.CONTROLS_WAIT.contains("待機"));
  }

  @Test
  void japaneseControlsHintKeepsEndTurnOnEndTurnKey() {
    assertTrue(Strings.Ja.CONTROLS_END.contains(END_TURN_KEY));
    assertTrue(Strings.Ja.CONTROLS_END.contains("ターン終了"));
  }

  @Test
  void englishControlsHintKeepsWaitOnWaitKey() {
    assertTrue(Strings.En.CONTROLS_WAIT.contains(WAIT_KEY));
    assertTrue(Strings.En.CONTROLS_WAIT.toLowerCase().contains("wait"));
  }

  @Test
  void englishControlsHintKeepsEndTurnOnEndTurnKey() {
    assertTrue(Strings.En.CONTROLS_END.contains(END_TURN_KEY));
    assertTrue(Strings.En.CONTROLS_END.toLowerCase().contains("end"));
  }

  // ---------------- チュートリアル (誤記があった箇所) ----------------

  @Test
  void japaneseTutorialDoesNotListWaitKeyAsEndTurn() {
    String endTurnLine = lineContaining(Strings.Ja.TUTORIAL_BODY, "【ターン終了】");
    assertFalse(endTurnLine.contains(WAIT_KEY), "チュートリアルのターン終了行が待機キーを挙げている: " + endTurnLine);
    assertTrue(endTurnLine.contains(END_TURN_KEY));
  }

  @Test
  void englishTutorialDoesNotListWaitKeyAsEndTurn() {
    String endTurnLine = lineContaining(Strings.En.TUTORIAL_BODY, "[End Turn]");
    assertFalse(
        endTurnLine.contains(WAIT_KEY),
        "English tutorial lists the wait key as end turn: " + endTurnLine);
    assertTrue(endTurnLine.contains(END_TURN_KEY));
  }

  @Test
  void japaneseTutorialExplainsWaitKey() {
    // 待機は説明から消さない。誤記の除去で操作が説明されなくなるのを防ぐ。
    assertTrue(Strings.Ja.TUTORIAL_BODY.contains(WAIT_KEY));
  }

  @Test
  void englishTutorialExplainsWaitKey() {
    assertTrue(Strings.En.TUTORIAL_BODY.contains(WAIT_KEY));
  }

  // ---------------- HUD ヒント ----------------

  @Test
  void hudHintUsesEndTurnKey() {
    assertTrue(Strings.Ja.HUD_HINT.contains(END_TURN_KEY));
    assertTrue(Strings.En.HUD_HINT.contains(END_TURN_KEY));
  }

  /** 複数行テキストから、指定した目印を含む最初の行を返す。 */
  private static String lineContaining(String body, String marker) {
    for (String line : body.split("\n")) {
      if (line.contains(marker)) {
        return line;
      }
    }
    throw new AssertionError("目印 '" + marker + "' を含む行が見つかりません");
  }
}
