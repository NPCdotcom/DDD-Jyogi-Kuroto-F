package core.presentation.input;

/**
 * ターン中の基本操作とキー割当の単一ソース (INPUT-01)。
 *
 * <p>SPACE を「ターン終了」と説明する誤記が日本語チュートリアル・英語チュートリアル・HUD コメントの 3 箇所にあり、一方で操作ヒントは正しく「待機」と書いていた (レビュー P1-1
 * の自己矛盾)。 同じ対応を 2 箇所で書いていたことが原因なので、割当をここへ集約する。
 *
 * <p>本 enum は <b>LibGDX に依存しない</b>。{@link core.presentation.input.PlayerInputs} は {@code Gdx.input}
 * の静的参照を持つため単体試験から駆動できず、そのままでは表示文との対応を検証できない。 キーコードを int で保持して純粋な型に切り出すことで、表示文との突き合わせを {@code
 * InputHelpTextTest} から LibGDX なしで検証できるようにする。
 *
 * <p>{@link #keyCode()} は {@code com.badlogic.gdx.Input.Keys} の値と一致させる。値を直接書くのは この型を純粋に保つためで、対応は
 * {@code PlayerInputs} 側の利用箇所と本 javadoc で担保する。
 */
public enum InputAction {

  /** 待機。AP を 1 消費してその場に留まる ({@code Input.Keys.SPACE})。 */
  WAIT(62, "SPACE"),

  /** ターン終了。残り AP を放棄して敵ターンへ移る ({@code Input.Keys.ENTER})。 */
  END_TURN(66, "ENTER");

  private final int keyCode;
  private final String displayToken;

  InputAction(int keyCode, String displayToken) {
    this.keyCode = keyCode;
    this.displayToken = displayToken;
  }

  /** {@code com.badlogic.gdx.Input.Keys} と同じキーコード。 */
  public int keyCode() {
    return keyCode;
  }

  /** 操作説明へ表示するキー名。表示文はこの文字列を含まなければならない。 */
  public String displayToken() {
    return displayToken;
  }
}
