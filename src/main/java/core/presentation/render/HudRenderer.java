package core.presentation.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import core.application.GameContext;
import core.domain.battle.BattleEvent;
import core.domain.battle.TurnPhase;
import core.domain.card.Card;
import core.domain.card.CardElement;
import core.domain.entity.Player;
import core.infrastructure.bootstrap.CardImageRegistry;
import core.infrastructure.save.UiPreset;
import java.util.List;

/**
 * HUD (HP / AP / Soul / フェーズ / メッセージログ / 手札) を {@link SpriteBatch} で描画するユーティリティ。
 *
 * <p>{@link Fonts#isJapaneseAvailable()} に応じて {@link Strings.Ja} / {@link Strings.En} の文言を
 * 自動で切り替える。
 */
public final class HudRenderer {

  private static final String LOG_PREFIX = "> ";

  private HudRenderer() {}

  /**
   * HUD 全体を描画する。
   *
   * @param batch 描画用 SpriteBatch (begin 済みであること)
   * @param fonts フォント群
   * @param context ゲームコンテキスト
   * @param pendingCardIndex PlayerInputs から取得したカード選択中インデックス (-1 = 未選択)
   * @param theme 現在の UI テーマ (§7-2 / W4-ε、null の場合は defaultTheme() を使う)
   */
  public static void draw(
      SpriteBatch batch,
      Fonts fonts,
      GameContext context,
      int pendingCardIndex,
      CardImageRegistry images,
      UiPreset preset,
      UiTheme theme) {
    // §UI 拡大方針: HUD / 手札 / ログをまとめて large (32px) で描画 (視認性最優先)。
    BitmapFont font = fonts.large();
    boolean jp = fonts.isJapaneseAvailable();
    Player p = context.state().player();
    UiTheme t = (theme != null) ? theme : UiTheme.defaultTheme();

    // §15-4 / ADR-25: HP / 速度 は effectiveStats() (素ステ + 装備 + Buff 合算) を使う。
    // 装備で maxHp が増える場合の表示整合性を確保。差分は素ステとの差で算出して "(+N)" 表記。
    core.domain.entity.Stats baseStats = p.stats();
    core.domain.entity.Stats effStats = p.effectiveStats();

    font.setColor(t.textColor());
    font.draw(
        batch,
        "%s: %d / %d%s"
            .formatted(
                label(jp, "HP"),
                baseStats.currentHp(),
                effStats.maxHp(),
                bonusSuffix(effStats.maxHp() - baseStats.maxHp())),
        RenderLayout.HUD_X,
        RenderLayout.HUD_Y_HP);
    font.draw(
        batch,
        "%s: %d / %d  (%s %d%s)"
            .formatted(
                label(jp, "AP"),
                p.actionPoints().current(),
                p.actionPoints().max(),
                jp ? Strings.Ja.HUD_SPEED : Strings.En.HUD_SPEED,
                baseStats.speed(),
                bonusSuffix(effStats.speed() - baseStats.speed())),
        RenderLayout.HUD_X,
        RenderLayout.HUD_Y_AP);

    // §15-1 / §15-8: 拡張 HUD (Soul / Gold / Phase) は MINIMAL プリセットで抑制する。
    // HP / AP は essential のため preset に関わらず常時表示。
    if (RenderLayout.showExtendedHud(preset)) {
      font.draw(
          batch,
          "%s: %d".formatted(jp ? Strings.Ja.HUD_SOUL : Strings.En.HUD_SOUL, p.soul().amount()),
          RenderLayout.HUD_X,
          RenderLayout.HUD_Y_SOUL);
      font.draw(
          batch,
          "%s: %d".formatted(jp ? Strings.Ja.HUD_GOLD : Strings.En.HUD_GOLD, p.gold().amount()),
          RenderLayout.HUD_X,
          RenderLayout.HUD_Y_GOLD);
      font.draw(
          batch,
          "%s: %s"
              .formatted(
                  jp ? Strings.Ja.HUD_PHASE : Strings.En.HUD_PHASE,
                  phaseLabel(jp, context.state().phase())),
          RenderLayout.HUD_X,
          RenderLayout.HUD_Y_PHASE);
    }

    // 移動権保持中のみ CYAN で「移動権 残 N 歩」を表示 (ADR-21 §15-5)
    drawMoveToken(batch, font, jp, p.pendingMoveCount());

    drawControlsHint(
        batch,
        fonts,
        jp,
        pendingCardIndex,
        p.pendingMoveCount() > 0,
        context.state().phase(),
        preset);
    drawLog(batch, font, jp, context.latestEvents(RenderLayout.LOG_LINES_VISIBLE));
    drawHand(batch, font, jp, p, pendingCardIndex, images);
    // Wave 15 W15-β / #3 #6: SkillSlot 完全廃止 + HUD 重なり整理。
    // drawSkillSlots(...) を廃止 (Player.skillSlot は空 4 枠固定なので何も描かれていなかったが念のため削除、
    // 画面下部のスキル枠領域も廃止で HUD が整理される)
    drawEndTurnButton(batch, font, jp, t);
  }

  /**
   * 後方互換オーバーロード (theme なし)。{@code theme = null} → {@link UiTheme#defaultTheme()} が使われる。
   *
   * @see #draw(SpriteBatch, Fonts, GameContext, int, CardImageRegistry, UiPreset, UiTheme)
   */
  public static void draw(
      SpriteBatch batch,
      Fonts fonts,
      GameContext context,
      int pendingCardIndex,
      CardImageRegistry images,
      UiPreset preset) {
    draw(batch, fonts, context, pendingCardIndex, images, preset, null);
  }

  /**
   * 後方互換オーバーロード。{@code pendingCardIndex = -1} で呼ぶ (カード選択ハイライトなし)、 サムネイル無し、プリセットは STANDARD。
   *
   * <p>既存の static 呼び出しがある場合に備えて残す。
   */
  public static void draw(SpriteBatch batch, Fonts fonts, GameContext context) {
    draw(batch, fonts, context, -1, null, UiPreset.STANDARD, null);
  }

  /**
   * 指定 index の手札カードが描画される矩形を返す (§15-3 UI/UX 改善、マウスクリック判定用)。
   *
   * <p>{@code DungeonScreen} のクリック判定と {@link #drawHand} の描画位置が同一式を共有する DRY 原則。 X 位置は固定 (9 枚センター配置の
   * {@code HAND_FIRST_X} 基準)、選択中の Y 持ち上げは {@link #drawHand} 側で処理。
   *
   * @param index 0〜8 の手札位置
   * @return カード矩形 (HUD カメラ座標、左下原点)
   */
  public static Rectangle handCardBounds(int index) {
    float x =
        RenderLayout.HAND_FIRST_X
            + index * (RenderLayout.HAND_CARD_WIDTH + RenderLayout.HAND_CARD_MARGIN);
    return new Rectangle(
        x,
        RenderLayout.HAND_CARD_BOTTOM_Y,
        RenderLayout.HAND_CARD_WIDTH,
        RenderLayout.HAND_CARD_HEIGHT);
  }

  // Wave 14 W14-β: ターン終了ボタン (画面右下に常時表示、ENTER と同等)。
  // INPUT-01: SPACE は BattleAction.Wait (AP 1 消費) でありターン終了ではない。
  // 割当の正は core.presentation.input.InputAction。
  private static final int END_TURN_BUTTON_X = 1750;
  private static final int END_TURN_BUTTON_Y = 30;
  private static final int END_TURN_BUTTON_WIDTH = 150;
  private static final int END_TURN_BUTTON_HEIGHT = 60;

  /**
   * ターン終了ボタンの矩形を返す (Wave 14 W14-β、{@link ButtonBounds} 形式で {@code DungeonScreen} のクリック判定に再利用)。
   *
   * <p>位置は手札 (左下〜中央) と被らない画面右下 (x=1750+、Y=30)。クリックで {@code BattleAction.EndTurn} を発行する。
   */
  public static ButtonBounds endTurnButtonBounds() {
    return new ButtonBounds(
        END_TURN_BUTTON_X, END_TURN_BUTTON_Y, END_TURN_BUTTON_WIDTH, END_TURN_BUTTON_HEIGHT);
  }

  /**
   * ターン終了ボタンのラベルを HUD 右下に描画する (Wave 14 W14-β)。背景塗りは YAGNI (SpriteBatch のみで完結)、 テキストだけで「ターン終了
   * ENTER」のようにキーバインドも併記する。クリック判定は {@link #endTurnButtonBounds} の矩形で行うため、文字位置と判定矩形が ButtonBounds
   * に集約される (DRY)。
   */
  private static void drawEndTurnButton(
      SpriteBatch batch, BitmapFont font, boolean jp, UiTheme theme) {
    font.setColor(theme.textColor());
    String label = jp ? "[ターン終了]" : "[End Turn]";
    com.badlogic.gdx.graphics.g2d.GlyphLayout layout =
        new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, label);
    float textX = END_TURN_BUTTON_X + (END_TURN_BUTTON_WIDTH - layout.width) / 2f;
    float textY = END_TURN_BUTTON_Y + (END_TURN_BUTTON_HEIGHT + layout.height) / 2f;
    font.draw(batch, label, textX, textY);
  }

  // Wave 15 W15-β / Wave 16 W16-α #6: drawSkillSlots を完全削除 (SkillSlot 廃止に伴い HUD 整理)。
  // 関連: RenderLayout.SKILL_SLOT_* 定数も削除済。Skill / SkillSlot クラスは敵側 (Boss bossSlam 等) で使用継続。

  /** 手札を画面下部にカード画像 120×168 で描画する (§15-3 / UI 改善)。 */
  private static void drawHand(
      SpriteBatch batch,
      BitmapFont font,
      boolean jp,
      Player p,
      int pendingCardIndex,
      CardImageRegistry images) {
    List<Card> cards = p.cardPileState().hand().cards();
    if (cards.isEmpty()) {
      return;
    }

    // 描画ステート初期化 (前段の描画から色が引き継がれないよう保険、§UI ブレンドモード明示)
    batch.setColor(Color.WHITE);

    // カード画像列 — 各カードは固定 X 位置 (handCardBounds と DRY)。選択中は Y を持ち上げ + 黄色ティント。
    // 2026-05-24: card_frame.png 合成描画を廃止し、card_XX.png (完成形カード絵) を 1 枚描画するのみ。
    for (int i = 0; i < cards.size(); i++) {
      Card card = cards.get(i);
      boolean selected = (i == pendingCardIndex);
      Rectangle bounds = handCardBounds(i);
      float y = bounds.y + (selected ? RenderLayout.HAND_CARD_SELECTED_LIFT : 0f);
      Texture cardImage = (images != null) ? images.get(card.id()) : null;
      Color tint = selected ? new Color(1f, 1f, 0.6f, 1f) : Color.WHITE;

      if (cardImage != null) {
        CardRenderer.drawCard(batch, cardImage, bounds.x, y, bounds.width, bounds.height, tint);
      } else {
        // CardImageRegistry 未注入 or テクスチャ欠落時はテキストフォールバック (テスト・後方互換)
        font.setColor(selected ? Color.YELLOW : Color.WHITE);
        font.draw(
            batch, "[%d]%s".formatted(i + 1, card.displayName()), bounds.x, y + bounds.height / 2f);
        font.setColor(Color.WHITE);
        font.draw(batch, "AP%d".formatted(card.apCost()), bounds.x + bounds.width - 60f, y + 28f);
      }
    }

    // §15-3: 選択中カードのみ詳細テキストを画像群の下 (HAND_DETAIL_TEXT_Y) に表示。
    if (pendingCardIndex >= 0 && pendingCardIndex < cards.size()) {
      Card sel = cards.get(pendingCardIndex);
      font.setColor(Color.YELLOW);
      String elemLabel = elementLabel(jp, sel.element());
      String full =
          "%s  AP:%d  %s  —  %s"
              .formatted(sel.displayName(), sel.apCost(), elemLabel, CardDescriber.describe(sel));
      // §UI 改善 (A9 攻撃 5 対応): HAND_X〜HUD_X 領域 (約 1024px) を超える長文は末尾「…」省略。
      // GlyphLayout で実描画幅を計測し、HUD_X の手前 80px (右余白) を超える場合は文字列を縮める。
      float maxWidth = RenderLayout.HUD_X - RenderLayout.HAND_FIRST_X - 80f;
      com.badlogic.gdx.graphics.g2d.GlyphLayout layout =
          new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, full);
      String drawn = full;
      if (layout.width > maxWidth) {
        // 1 文字ずつ削って「…」を付け、収まるまで縮める (BitmapFont.draw に直接渡せる String)。
        for (int len = full.length() - 1; len > 0; len--) {
          drawn = full.substring(0, len) + "…";
          layout.setText(font, drawn);
          if (layout.width <= maxWidth) break;
        }
      }
      font.draw(batch, drawn, RenderLayout.HAND_FIRST_X, RenderLayout.HAND_DETAIL_TEXT_Y);
      font.setColor(Color.WHITE);
    }
  }

  /** 自動ターン終了の理由ラベル (HUD ログ表示用)。 */
  private static String autoTurnReasonLabel(boolean jp, BattleEvent.AutoTurnEnded.Reason reason) {
    return switch (reason) {
      case STUCK ->
          jp ? Strings.Ja.AUTO_TURN_END_REASON_STUCK : Strings.En.AUTO_TURN_END_REASON_STUCK;
    };
  }

  private static String elementLabel(boolean jp, CardElement element) {
    return switch (element) {
      case PHYSICAL -> jp ? Strings.Ja.CARD_ELEMENT_PHYSICAL : Strings.En.CARD_ELEMENT_PHYSICAL;
      case MAGICAL -> jp ? Strings.Ja.CARD_ELEMENT_MAGICAL : Strings.En.CARD_ELEMENT_MAGICAL;
    };
  }

  private static String label(boolean jp, String englishLabel) {
    // HP / AP は日英で同記号 (略称) でも問題ないので英語表記を共有する
    return switch (englishLabel) {
      case "HP" -> jp ? Strings.Ja.HUD_HP : Strings.En.HUD_HP;
      case "AP" -> jp ? Strings.Ja.HUD_AP : Strings.En.HUD_AP;
      default -> englishLabel;
    };
  }

  /**
   * 装備 / Buff の差分ぶん " (+N)" もしくは " (-N)" 表記。0 ならは空文字 (素ステのみ)。
   *
   * <p>Plan の「物攻 1 (+1) = 2」形式の簡略版: 装備込み合計値の隣に差分のみ表示し、 "(+1)" が見えれば装備効果がかかっていることが分かる (§15-9 /
   * ADR-25)。
   */
  private static String bonusSuffix(int delta) {
    if (delta == 0) {
      return "";
    }
    return delta > 0 ? " (+%d)".formatted(delta) : " (%d)".formatted(delta);
  }

  /**
   * 移動権残量表示。pendingMoveCount > 0 のときのみ CYAN で描画する (ADR-21 §15-5)。
   *
   * <p>HUD 右側の HUD_Y_PHASE より 1 行下に配置し、移動モード中であることをプレイヤーに通知する。
   */
  private static void drawMoveToken(
      SpriteBatch batch, BitmapFont font, boolean jp, int pendingMoveCount) {
    if (pendingMoveCount <= 0) {
      return;
    }
    font.setColor(Color.CYAN);
    String text =
        (jp ? Strings.Ja.MOVE_TOKEN_REMAINING_FORMAT : Strings.En.MOVE_TOKEN_REMAINING_FORMAT)
            .formatted(pendingMoveCount);
    font.draw(batch, text, RenderLayout.HUD_X, RenderLayout.HUD_Y_MOVE_TOKEN);
  }

  private static void drawControlsHint(
      SpriteBatch batch,
      Fonts fonts,
      boolean jp,
      int pendingCardIndex,
      boolean inMovementTokenMode,
      TurnPhase phase,
      UiPreset preset) {
    // §15-1 / §15-8 プリセット連動: CLEARED / ENEMY_TURN 状態は最重要のため常時表示。
    // それ以外で MINIMAL プリセット (showExtendedHud=false) ならヒントを抑制する。
    boolean criticalPhase = phase == TurnPhase.CLEARED || phase == TurnPhase.ENEMY_TURN;
    if (!criticalPhase && !RenderLayout.showExtendedHud(preset)) {
      return;
    }
    // 優先度: CLEARED (層踏破) > ENEMY_TURN (敵ターン進行中) > 移動権保持中 > カード選択中 > 通常
    // ヒントは large() (32px) で描画 (プロジェクタ視聴対応、§UI 拡大方針)
    BitmapFont font = fonts.large();
    String hint;
    if (phase == TurnPhase.CLEARED) {
      // CLEARED は強調のため YELLOW (§15-6 / ADR-23)
      font.setColor(0.95f, 0.85f, 0.3f, 1f);
      hint = jp ? Strings.Ja.CLEARED_HINT : Strings.En.CLEARED_HINT;
    } else if (phase == TurnPhase.ENEMY_TURN) {
      // §UI 改善: 敵ターン中は「敵のターン進行中…」をオレンジで常時表示
      // (敵が画面外で動いていてもプレイヤーが状況を把握できる)
      font.setColor(1f, 0.6f, 0.3f, 1f);
      hint = jp ? Strings.Ja.HINT_ENEMY_TURN_IN_PROGRESS : Strings.En.HINT_ENEMY_TURN_IN_PROGRESS;
    } else {
      font.setColor(0.7f, 0.7f, 0.7f, 1f);
      if (inMovementTokenMode) {
        hint = jp ? Strings.Ja.MOVE_TOKEN_HINT : Strings.En.MOVE_TOKEN_HINT;
      } else if (pendingCardIndex >= 0) {
        hint = jp ? Strings.Ja.HAND_HINT : Strings.En.HAND_HINT;
      } else {
        hint = jp ? Strings.Ja.HUD_HINT : Strings.En.HUD_HINT;
      }
    }
    // 画面左下に左寄せで配置 (HUD_HINT 等の長文 large 32px が HUD 列右側に並ぶと画面外にはみ出るため)。
    font.draw(batch, hint, RenderLayout.LOG_X, RenderLayout.HUD_Y_HINT);
  }

  private static void drawLog(
      SpriteBatch batch, BitmapFont font, boolean jp, List<BattleEvent> events) {
    font.setColor(Color.LIGHT_GRAY);
    int y = RenderLayout.LOG_TOP_Y;
    for (BattleEvent e : events) {
      font.draw(batch, LOG_PREFIX + describe(jp, e), RenderLayout.LOG_X, y);
      y -= RenderLayout.LOG_LINE_HEIGHT;
    }
    font.setColor(Color.WHITE); // 後続描画への影響をリセット
  }

  private static String phaseLabel(boolean jp, TurnPhase phase) {
    return switch (phase) {
      case PLAYER_TURN -> jp ? Strings.Ja.PHASE_PLAYER : Strings.En.PHASE_PLAYER;
      case ENEMY_TURN -> jp ? Strings.Ja.PHASE_ENEMY : Strings.En.PHASE_ENEMY;
      case GAME_OVER -> jp ? Strings.Ja.PHASE_GAMEOVER : Strings.En.PHASE_GAMEOVER;
      case CLEARED -> jp ? Strings.Ja.PHASE_CLEARED : Strings.En.PHASE_CLEARED;
      case RUN_CLEARED -> jp ? Strings.Ja.PHASE_RUN_CLEARED : Strings.En.PHASE_RUN_CLEARED;
    };
  }

  /** BattleEvent を 1 行に整形。日英で書式テンプレートを切り替える。 */
  private static String describe(boolean jp, BattleEvent event) {
    return switch (event) {
      case BattleEvent.Moved m ->
          (jp ? Strings.Ja.EV_MOVED_FORMAT : Strings.En.EV_MOVED_FORMAT)
              .formatted(m.who().value(), m.to().x(), m.to().y());
      case BattleEvent.SkillUsed s ->
          (jp ? Strings.Ja.EV_SKILL_USED_FORMAT : Strings.En.EV_SKILL_USED_FORMAT)
              .formatted(s.who().value(), s.skillName());
      case BattleEvent.DamageDealt d ->
          (jp ? Strings.Ja.EV_DAMAGE_FORMAT : Strings.En.EV_DAMAGE_FORMAT)
              .formatted(d.from().value(), d.to().value(), d.damage(), d.remainingHp());
      case BattleEvent.ActorDied a ->
          (jp ? Strings.Ja.EV_DIED_FORMAT : Strings.En.EV_DIED_FORMAT).formatted(a.who().value());
      case BattleEvent.SoulGained sg ->
          (jp ? Strings.Ja.EV_SOUL_GAINED_FORMAT : Strings.En.EV_SOUL_GAINED_FORMAT)
              .formatted(sg.who().value(), sg.amount());
      case BattleEvent.GoldGained gg ->
          (jp ? Strings.Ja.EV_GOLD_GAINED_FORMAT : Strings.En.EV_GOLD_GAINED_FORMAT)
              .formatted(gg.who().value(), gg.amount());
      case BattleEvent.TurnPhaseChanged tp ->
          (jp ? Strings.Ja.EV_PHASE_FORMAT : Strings.En.EV_PHASE_FORMAT)
              .formatted(phaseLabel(jp, tp.newPhase()));
      case BattleEvent.ActionRejected ar ->
          (jp ? Strings.Ja.EV_REJECTED_FORMAT : Strings.En.EV_REJECTED_FORMAT)
              .formatted(ar.who().value(), ar.reason());
      case BattleEvent.MovementGranted mg ->
          (jp ? Strings.Ja.EV_MOVEMENT_GRANTED_FORMAT : Strings.En.EV_MOVEMENT_GRANTED_FORMAT)
              .formatted(mg.who().value(), mg.remainingSteps());
      case BattleEvent.TrapPlaced tp ->
          (jp ? Strings.Ja.EV_TRAP_PLACED_FORMAT : Strings.En.EV_TRAP_PLACED_FORMAT)
              .formatted(tp.placer().value(), tp.position().x(), tp.position().y());
      case BattleEvent.TrapTriggered tt ->
          (jp ? Strings.Ja.EV_TRAP_TRIGGERED_FORMAT : Strings.En.EV_TRAP_TRIGGERED_FORMAT)
              .formatted(tt.victim().value(), tt.damage(), tt.remainingHp());
      case BattleEvent.FloorAdvanced fa ->
          (jp ? Strings.Ja.EV_FLOOR_ADVANCED_FORMAT : Strings.En.EV_FLOOR_ADVANCED_FORMAT)
              .formatted(fa.newLayer());
      case BattleEvent.BuffApplied ba ->
          (jp ? Strings.Ja.EV_BUFF_APPLIED_FORMAT : Strings.En.EV_BUFF_APPLIED_FORMAT)
              .formatted(
                  ba.who().value(),
                  BuffKindLabels.labelOf(ba.kind(), jp),
                  ba.amount(),
                  ba.remainingTurns());
      case BattleEvent.EliteDefeated ed ->
          (jp ? Strings.Ja.EV_ELITE_DEFEATED_FORMAT : Strings.En.EV_ELITE_DEFEATED_FORMAT)
              .formatted(ed.who().value());
      case BattleEvent.AutoTurnEnded ate ->
          (jp ? Strings.Ja.EV_AUTO_TURN_END_FORMAT : Strings.En.EV_AUTO_TURN_END_FORMAT)
              .formatted(autoTurnReasonLabel(jp, ate.reason()));
      case BattleEvent.WallBroken wb ->
          (jp ? Strings.Ja.EV_WALL_BROKEN_FORMAT : Strings.En.EV_WALL_BROKEN_FORMAT)
              .formatted(wb.position().x(), wb.position().y());
    };
  }
}
