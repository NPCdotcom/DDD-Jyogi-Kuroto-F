package core.presentation.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import core.application.GameContext;
import core.domain.battle.BattleEvent;
import core.domain.battle.TurnPhase;
import core.domain.entity.Player;
import java.util.List;

/**
 * HUD (HP / AP / Soul / フェーズ / メッセージログ) を {@link SpriteBatch} で描画するユーティリティ。
 *
 * <p>{@link Fonts#isJapaneseAvailable()} に応じて {@link Strings.Ja} / {@link Strings.En} の文言を
 * 自動で切り替える。
 */
public final class HudRenderer {

  private static final String LOG_PREFIX = "> ";

  private HudRenderer() {}

  public static void draw(SpriteBatch batch, Fonts fonts, GameContext context) {
    BitmapFont font = fonts.hud();
    boolean jp = fonts.isJapaneseAvailable();
    Player p = context.state().player();

    font.setColor(Color.WHITE);
    font.draw(
        batch,
        "%s: %d / %d".formatted(label(jp, "HP"), p.stats().currentHp(), p.stats().maxHp()),
        RenderLayout.HUD_X,
        RenderLayout.HUD_Y_HP);
    font.draw(
        batch,
        "%s: %d / %d  (%s %d)"
            .formatted(
                label(jp, "AP"),
                p.actionPoints().current(),
                p.actionPoints().max(),
                jp ? Strings.Ja.HUD_SPEED : Strings.En.HUD_SPEED,
                p.stats().speed()),
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

    drawControlsHint(batch, font, jp);
    drawLog(batch, font, jp, context.latestEvents(RenderLayout.LOG_LINES_VISIBLE));
  }

  private static String label(boolean jp, String englishLabel) {
    // HP / AP は日英で同記号 (略称) でも問題ないので英語表記を共有する
    return switch (englishLabel) {
      case "HP" -> jp ? Strings.Ja.HUD_HP : Strings.En.HUD_HP;
      case "AP" -> jp ? Strings.Ja.HUD_AP : Strings.En.HUD_AP;
      default -> englishLabel;
    };
  }

  private static void drawControlsHint(SpriteBatch batch, BitmapFont font, boolean jp) {
    font.setColor(0.7f, 0.7f, 0.7f, 1f);
    String hint = jp ? Strings.Ja.HUD_HINT : Strings.En.HUD_HINT;
    font.draw(batch, hint, RenderLayout.HUD_X + 240, RenderLayout.HUD_Y_PHASE);
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
    };
  }
}
