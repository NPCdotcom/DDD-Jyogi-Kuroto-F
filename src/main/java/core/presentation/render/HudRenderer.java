package core.presentation.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import core.application.GameContext;
import core.domain.battle.BattleEvent;
import core.domain.battle.TurnPhase;
import core.domain.card.Card;
import core.domain.card.CardElement;
import core.domain.entity.Player;
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
   */
  public static void draw(
      SpriteBatch batch, Fonts fonts, GameContext context, int pendingCardIndex) {
    // §UI 拡大方針: HUD / 手札 / ログをまとめて large (32px) で描画 (視認性最優先)。
    BitmapFont font = fonts.large();
    boolean jp = fonts.isJapaneseAvailable();
    Player p = context.state().player();

    // §15-4 / ADR-25: HP / 速度 は effectiveStats() (素ステ + 装備 + Buff 合算) を使う。
    // 装備で maxHp が増える場合の表示整合性を確保。差分は素ステとの差で算出して "(+N)" 表記。
    core.domain.entity.Stats baseStats = p.stats();
    core.domain.entity.Stats effStats = p.effectiveStats();

    font.setColor(Color.WHITE);
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
    font.draw(
        batch,
        "%s: %d".formatted(jp ? Strings.Ja.HUD_SOUL : Strings.En.HUD_SOUL, p.soul().amount()),
        RenderLayout.HUD_X,
        RenderLayout.HUD_Y_SOUL);
    font.draw(
        batch,
        "%s: %s"
            .formatted(
                jp ? Strings.Ja.HUD_PHASE : Strings.En.HUD_PHASE,
                phaseLabel(jp, context.state().phase())),
        RenderLayout.HUD_X,
        RenderLayout.HUD_Y_PHASE);

    // 移動権保持中のみ CYAN で「移動権 残 N 歩」を表示 (ADR-21 §15-5)
    drawMoveToken(batch, font, jp, p.pendingMoveCount());

    drawControlsHint(
        batch, fonts, jp, pendingCardIndex, p.pendingMoveCount() > 0, context.state().phase());
    drawLog(batch, font, jp, context.latestEvents(RenderLayout.LOG_LINES_VISIBLE));
    drawHand(batch, font, jp, p, pendingCardIndex);
  }

  /**
   * 後方互換オーバーロード。{@code pendingCardIndex = -1} で呼ぶ (カード選択ハイライトなし)。
   *
   * <p>既存の static 呼び出しがある場合に備えて残す。
   */
  public static void draw(SpriteBatch batch, Fonts fonts, GameContext context) {
    draw(batch, fonts, context, -1);
  }

  /** 手札を画面下部に描画する。 */
  private static void drawHand(
      SpriteBatch batch, BitmapFont font, boolean jp, Player p, int pendingCardIndex) {
    List<Card> cards = p.cardPileState().hand().cards();
    if (cards.isEmpty()) {
      return;
    }

    // ラベル行 (手札の上に 1 行分)
    font.setColor(Color.LIGHT_GRAY);
    font.draw(
        batch,
        jp ? Strings.Ja.HAND_LABEL : Strings.En.HAND_LABEL,
        RenderLayout.HUD_X,
        RenderLayout.HAND_Y + RenderLayout.LARGE_LINE_HEIGHT);

    // カード一覧 (1 行に並べる、最大 9 枚)
    int x = RenderLayout.HUD_X;
    for (int i = 0; i < cards.size(); i++) {
      Card card = cards.get(i);
      boolean selected = (i == pendingCardIndex);

      if (selected) {
        // 選択中カードを黄色でハイライト
        font.setColor(Color.YELLOW);
      } else {
        font.setColor(Color.WHITE);
      }

      String prefix = selected ? ">" : " ";
      String elemLabel = elementLabel(jp, card.element());
      // フォーマット: [1] 斬撃 AP:1 物  or  >[1] 斬撃 AP:1 物
      String text =
          "%s[%d]%s AP:%d %s  "
              .formatted(prefix, i + 1, card.displayName(), card.apCost(), elemLabel);
      font.draw(batch, text, x, RenderLayout.HAND_Y);

      // 次のカードの X 座標を文字幅分ずらす (RenderLayout.HAND_CARD_GLYPH_WIDTH px/文字で概算)
      x += text.length() * RenderLayout.HAND_CARD_GLYPH_WIDTH;
    }

    // カード選択中ヒントは drawControlsHint() (画面左下) に統合済み。
    // 手札の下に再描画すると二重表示になり、画面下端で文字が切れる原因となるためここでは描かない。
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
   * <p>Plan の「物攻 1 (+1) = 2」形式の簡略版: 装備込み合計値の隣に差分のみ表示し、
   * "(+1)" が見えれば装備効果がかかっていることが分かる (§15-9 / ADR-25)。
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
      TurnPhase phase) {
    // 優先度: CLEARED (層踏破) > 移動権保持中 > カード選択中 > 通常
    // ヒントは large() (32px) で描画 (プロジェクタ視聴対応、§UI 拡大方針)
    BitmapFont font = fonts.large();
    String hint;
    if (phase == TurnPhase.CLEARED) {
      // CLEARED は強調のため YELLOW (§15-6 / ADR-23)
      font.setColor(0.95f, 0.85f, 0.3f, 1f);
      hint = jp ? Strings.Ja.CLEARED_HINT : Strings.En.CLEARED_HINT;
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
  }

  private static String phaseLabel(boolean jp, TurnPhase phase) {
    return switch (phase) {
      case PLAYER_TURN -> jp ? Strings.Ja.PHASE_PLAYER : Strings.En.PHASE_PLAYER;
      case ENEMY_TURN -> jp ? Strings.Ja.PHASE_ENEMY : Strings.En.PHASE_ENEMY;
      case GAME_OVER -> jp ? Strings.Ja.PHASE_GAMEOVER : Strings.En.PHASE_GAMEOVER;
      case CLEARED -> jp ? Strings.Ja.PHASE_CLEARED : Strings.En.PHASE_CLEARED;
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
              .formatted(ba.who().value(), buffKindLabel(jp, ba.kind()), ba.amount(), ba.remainingTurns());
    };
  }

  /** {@link core.domain.card.CardEffect.BuffKind} の日英表示ラベル (HUD ログ用)。 */
  private static String buffKindLabel(boolean jp, core.domain.card.CardEffect.BuffKind kind) {
    return switch (kind) {
      case PHYSICAL_ATTACK_UP ->
          jp ? Strings.Ja.BUFF_KIND_PHYSICAL_ATTACK : Strings.En.BUFF_KIND_PHYSICAL_ATTACK;
      case MAGICAL_ATTACK_UP ->
          jp ? Strings.Ja.BUFF_KIND_MAGICAL_ATTACK : Strings.En.BUFF_KIND_MAGICAL_ATTACK;
      case PHYSICAL_DEFENSE_UP ->
          jp ? Strings.Ja.BUFF_KIND_PHYSICAL_DEFENSE : Strings.En.BUFF_KIND_PHYSICAL_DEFENSE;
      case MAGICAL_DEFENSE_UP ->
          jp ? Strings.Ja.BUFF_KIND_MAGICAL_DEFENSE : Strings.En.BUFF_KIND_MAGICAL_DEFENSE;
      case SPEED_UP -> jp ? Strings.Ja.BUFF_KIND_SPEED : Strings.En.BUFF_KIND_SPEED;
    };
  }
}
