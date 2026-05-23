package core.presentation.render;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link RenderLayout} の定数が設計意図 (コメント) と整合していることを検証する。
 *
 * <p>描画座標を数値で直接変更した際に、重なり・配置逆転を即座に検出するための回帰テスト。 LibGDX に依存しない純粋な定数 assertion のみ。
 */
class RenderLayoutConstraintTest {

  // --- フォント高さ前提 ---
  // large フォント (32px) のベースライン高さを仮定。描画範囲は Y∈[Y-32, Y] を占有する。
  private static final int LARGE_FONT_HEIGHT_PX = 32;

  // =========================================================================
  // 手札詳細テキスト vs カード画像 (重なり回避)
  // =========================================================================

  @Test
  void handDetailTextYIsStrictlyBelowHandCardBottomY() {
    // HAND_DETAIL_TEXT_Y=80 < HAND_CARD_BOTTOM_Y=110。
    // BitmapFont は Y がベースライン (下端) を指すため、
    // 詳細テキストのベースラインがカード底辺より下であれば画像と重ならない。
    assertTrue(
        RenderLayout.HAND_DETAIL_TEXT_Y < RenderLayout.HAND_CARD_BOTTOM_Y,
        "選択カード詳細テキスト (Y=%d) はカード底辺 (Y=%d) より下に来る"
            .formatted(RenderLayout.HAND_DETAIL_TEXT_Y, RenderLayout.HAND_CARD_BOTTOM_Y));
  }

  // =========================================================================
  // ログ vs 手札詳細テキスト (重なり回避)
  // =========================================================================

  @Test
  void logTopYIsStrictlyAboveHandDetailTextWithFontMargin() {
    // LOG_TOP_Y=520、HAND_DETAIL_TEXT_Y=80。
    // ログの最下端 = LOG_TOP_Y - LOG_LINE_HEIGHT*(LOG_LINES_VISIBLE-1) ≈ 472、
    // 詳細テキスト描画範囲上端 = HAND_DETAIL_TEXT_Y - LARGE_FONT_HEIGHT_PX + 1 ≈ 49。
    // 要するに LOG_TOP_Y が詳細テキスト上端 + フォント高さを大きく上回ることを確認。
    int detailTopEdge = RenderLayout.HAND_DETAIL_TEXT_Y; // ベースライン ≒ 描画上端 (保守的)
    assertTrue(
        RenderLayout.LOG_TOP_Y > detailTopEdge + LARGE_FONT_HEIGHT_PX,
        "ログ上端 (Y=%d) は詳細テキスト + フォント高 (Y=%d) より上にある (重ならない)"
            .formatted(RenderLayout.LOG_TOP_Y, detailTopEdge + LARGE_FONT_HEIGHT_PX));
  }

  // =========================================================================
  // 9 枚センター配置の整合 (HAND_FIRST_X)
  // =========================================================================

  @Test
  void handFirstXMatchesCenterAlignment() {
    // §15-3: 9 枚センター配置。HAND_FIRST_X = (SCREEN_WIDTH - 9*CARD_WIDTH - 8*MARGIN) / 2
    // 端数は切り捨て (int 除算)。コメント記載値 356 と一致するかを式で検証する。
    int maxCards = 9;
    int expected =
        (RenderLayout.SCREEN_WIDTH
                - maxCards * RenderLayout.HAND_CARD_WIDTH
                - (maxCards - 1) * RenderLayout.HAND_CARD_MARGIN)
            / 2;
    assertTrue(
        RenderLayout.HAND_FIRST_X == expected,
        "HAND_FIRST_X=%d は 9 枚センター計算値=%d と一致する".formatted(RenderLayout.HAND_FIRST_X, expected));
  }

  @Test
  void handNineCardsWidthFitsWithinScreenWidth() {
    // 9 枚並べた右端 X がスクリーン幅を超えないこと。
    int maxCards = 9;
    int rightEdgeX =
        RenderLayout.HAND_FIRST_X
            + maxCards * RenderLayout.HAND_CARD_WIDTH
            + (maxCards - 1) * RenderLayout.HAND_CARD_MARGIN;
    assertTrue(
        rightEdgeX <= RenderLayout.SCREEN_WIDTH,
        "9 枚手札の右端 X=%d がスクリーン幅 %d を超えない".formatted(rightEdgeX, RenderLayout.SCREEN_WIDTH));
  }

  // =========================================================================
  // スクリーン内制約 (各 HUD Y 座標が有効範囲内)
  // =========================================================================

  @Test
  void allHudYCoordinatesAreWithinScreen() {
    // HUD の各行が画面内 (0 < Y <= SCREEN_HEIGHT) に収まること。
    int[] hudYs = {
      RenderLayout.HUD_Y_HP,
      RenderLayout.HUD_Y_AP,
      RenderLayout.HUD_Y_SOUL,
      RenderLayout.HUD_Y_GOLD,
      RenderLayout.HUD_Y_PHASE,
      RenderLayout.HUD_Y_MOVE_TOKEN,
      RenderLayout.HUD_Y_HINT,
    };
    for (int y : hudYs) {
      assertTrue(
          y > 0 && y <= RenderLayout.SCREEN_HEIGHT,
          "HUD Y=%d は画面内 (1..%d)".formatted(y, RenderLayout.SCREEN_HEIGHT));
    }
  }

  @Test
  void hudYValuesAreStrictlyDescendingFromTopToBottom() {
    // HUD 行は HP → AP → Soul → Gold → Phase → MoveToken の順で Y 値が下がる (上から下へ配置)。
    assertTrue(
        RenderLayout.HUD_Y_HP > RenderLayout.HUD_Y_AP,
        "HP 行 (Y=%d) は AP 行 (Y=%d) より上".formatted(RenderLayout.HUD_Y_HP, RenderLayout.HUD_Y_AP));
    assertTrue(
        RenderLayout.HUD_Y_AP > RenderLayout.HUD_Y_SOUL,
        "AP 行 (Y=%d) は Soul 行 (Y=%d) より上"
            .formatted(RenderLayout.HUD_Y_AP, RenderLayout.HUD_Y_SOUL));
    assertTrue(
        RenderLayout.HUD_Y_SOUL > RenderLayout.HUD_Y_GOLD,
        "Soul 行 (Y=%d) は Gold 行 (Y=%d) より上"
            .formatted(RenderLayout.HUD_Y_SOUL, RenderLayout.HUD_Y_GOLD));
    assertTrue(
        RenderLayout.HUD_Y_GOLD > RenderLayout.HUD_Y_PHASE,
        "Gold 行 (Y=%d) は Phase 行 (Y=%d) より上"
            .formatted(RenderLayout.HUD_Y_GOLD, RenderLayout.HUD_Y_PHASE));
    assertTrue(
        RenderLayout.HUD_Y_PHASE > RenderLayout.HUD_Y_MOVE_TOKEN,
        "Phase 行 (Y=%d) は MoveToken 行 (Y=%d) より上"
            .formatted(RenderLayout.HUD_Y_PHASE, RenderLayout.HUD_Y_MOVE_TOKEN));
  }
}
