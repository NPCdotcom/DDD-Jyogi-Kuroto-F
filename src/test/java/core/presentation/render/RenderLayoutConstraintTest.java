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
  private static final int SCREEN_EDGE_MARGIN_PX = 8;

  // =========================================================================
  // 手札詳細テキスト vs カード画像 (重なり回避)
  // =========================================================================

  @Test
  void twoLineHandDetailFitsBetweenScreenBottomAndHandCards() {
    int detailBottomEdge =
        RenderLayout.HAND_DETAIL_TEXT_Y
            - (RenderLayout.HAND_DETAIL_LINE_COUNT - 1) * RenderLayout.HAND_DETAIL_FONT_SIZE
            - RenderLayout.HAND_DETAIL_FONT_SIZE;

    assertTrue(
        detailBottomEdge >= SCREEN_EDGE_MARGIN_PX,
        "2 行の選択カード詳細下端 (Y=%d) は画面下端から %dpx 以上空ける"
            .formatted(detailBottomEdge, SCREEN_EDGE_MARGIN_PX));
    assertTrue(
        RenderLayout.HAND_DETAIL_TEXT_Y <= RenderLayout.HAND_CARD_BOTTOM_Y - SCREEN_EDGE_MARGIN_PX,
        "選択カード詳細上端 (Y=%d) はカード底辺 (Y=%d) から %dpx 以上離す"
            .formatted(
                RenderLayout.HAND_DETAIL_TEXT_Y,
                RenderLayout.HAND_CARD_BOTTOM_Y,
                SCREEN_EDGE_MARGIN_PX));
  }

  @Test
  void handDetailWrapRegionFitsWithinScreenWidth() {
    assertTrue(RenderLayout.HAND_DETAIL_TEXT_X >= 0, "選択カード詳細の左端は画面内");
    assertTrue(RenderLayout.HAND_DETAIL_TEXT_WIDTH > 0, "選択カード詳細の折返し幅は正");
    assertTrue(
        RenderLayout.HAND_DETAIL_TEXT_X + RenderLayout.HAND_DETAIL_TEXT_WIDTH
            <= RenderLayout.SCREEN_WIDTH,
        "選択カード詳細の右端 (X=%d) は画面幅 %d を超えない"
            .formatted(
                RenderLayout.HAND_DETAIL_TEXT_X + RenderLayout.HAND_DETAIL_TEXT_WIDTH,
                RenderLayout.SCREEN_WIDTH));
  }

  // =========================================================================
  // 手札・操作ヒント・ログ (重なり回避)
  // =========================================================================

  @Test
  void selectedHandCardsHintAndLogStayVerticallySeparated() {
    int selectedHandTop =
        RenderLayout.HAND_CARD_BOTTOM_Y
            + RenderLayout.HAND_CARD_HEIGHT
            + RenderLayout.HAND_CARD_SELECTED_LIFT;
    int hintBottom = RenderLayout.HUD_Y_HINT - LARGE_FONT_HEIGHT_PX;
    assertTrue(
        selectedHandTop + SCREEN_EDGE_MARGIN_PX <= hintBottom,
        "選択中手札上端 (Y=%d) と操作ヒント下端 (Y=%d) は %dpx 以上離す"
            .formatted(selectedHandTop, hintBottom, SCREEN_EDGE_MARGIN_PX));

    int lowestLogBaseline =
        RenderLayout.LOG_TOP_Y
            - (RenderLayout.LOG_LINES_VISIBLE - 1) * RenderLayout.LOG_LINE_HEIGHT;
    int logBottom = lowestLogBaseline - LARGE_FONT_HEIGHT_PX;
    assertTrue(
        RenderLayout.HUD_Y_HINT + SCREEN_EDGE_MARGIN_PX <= logBottom,
        "操作ヒント上端 (Y=%d) とログ下端 (Y=%d) は %dpx 以上離す"
            .formatted(RenderLayout.HUD_Y_HINT, logBottom, SCREEN_EDGE_MARGIN_PX));
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
