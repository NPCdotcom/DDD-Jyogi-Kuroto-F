package core.presentation.window;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import core.domain.entity.Player;
import core.domain.entity.Stats;
import core.presentation.render.RenderLayout;
import core.presentation.render.Strings;
import java.util.Objects;

/**
 * ステータス確認ポップアップ (§15-4)。プレイヤーの 6 ステ (HP / 速度 / 物攻 / 魔攻 / 物防 / 魔防) を 素値 + 装備/Buff 補正つきで表示する。
 *
 * <p>{@link NodeChoicePopup} と同じ Scene2D {@link Stage} + {@link Window} パターン。NodeChoicePopup が
 * 構築時固定の選択肢を出すのに対し、本ポップアップは値が毎ターン変わるため、値ラベルの参照を保持し {@link #updateValues(Player)} で {@code setText}
 * する。開閉 (Tab キー) は DungeonScreen が制御する。
 */
public final class StatusPopup implements Disposable {

  /** 表示するステ行数 (HP / 速度 / 物攻 / 魔攻 / 物防 / 魔防)。 */
  private static final int ROW_COUNT = 6;

  private final Stage stage;
  private final Skin skin;

  /** 各ステの値ラベル。index: 0=HP 1=速度 2=物攻 3=魔攻 4=物防 5=魔防。 */
  private final Label[] valueLabels = new Label[ROW_COUNT];

  /**
   * Stage / Window を構築する。
   *
   * @param font {@code Fonts.large()} (32px) を渡す想定。NodeChoicePopup と同じく等倍運用 (Scene2D Label の倍率描画は
   *     漢字が黒四角化するため)。
   * @param japanese 日本語フォントが利用可能か (ラベル文言の日英切替に使う)
   */
  public StatusPopup(BitmapFont font, boolean japanese) {
    Objects.requireNonNull(font, "font");

    Viewport viewport = new FitViewport(RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT);
    this.stage = new Stage(viewport);
    this.skin = new Skin();
    skin.add("default-font", font);

    Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
    skin.add("default", labelStyle);

    Window.WindowStyle windowStyle = new Window.WindowStyle();
    windowStyle.titleFont = font;
    windowStyle.titleFontColor = new Color(0.95f, 0.85f, 0.3f, 1f);
    windowStyle.background = solidDrawable("status-bg", new Color(0.08f, 0.08f, 0.12f, 0.95f));
    skin.add("default", windowStyle);

    String title = japanese ? Strings.Ja.STATUS_TITLE : Strings.En.STATUS_TITLE;
    Window window = new Window(title, windowStyle);
    window.setMovable(false);
    window.setResizable(false);
    window.padTop(64f);
    window.padLeft(48f).padRight(48f).padBottom(32f);

    String[] names = rowNames(japanese);
    for (int i = 0; i < ROW_COUNT; i++) {
      Label name = new Label(names[i], labelStyle);
      name.setColor(new Color(0.7f, 0.75f, 0.85f, 1f));
      Label value = new Label("-", labelStyle);
      value.setColor(Color.WHITE);
      valueLabels[i] = value;
      window.add(name).left().padRight(64f).padBottom(14f);
      window.add(value).left().minWidth(220f).padBottom(14f).row();
    }

    Label hint = new Label(japanese ? Strings.Ja.STATUS_HINT : Strings.En.STATUS_HINT, labelStyle);
    hint.setColor(new Color(0.6f, 0.6f, 0.68f, 1f));
    window.add(hint).left().colspan(2).padTop(20f).row();

    window.pack();
    window.setPosition(
        (RenderLayout.SCREEN_WIDTH - window.getWidth()) / 2f,
        (RenderLayout.SCREEN_HEIGHT - window.getHeight()) / 2f);
    stage.addActor(window);
  }

  /** 6 行のステ名ラベル (HP / 速度 は HUD と共通の文言定数を再利用)。 */
  private static String[] rowNames(boolean japanese) {
    if (japanese) {
      return new String[] {
        Strings.Ja.HUD_HP,
        Strings.Ja.HUD_SPEED,
        Strings.Ja.STATUS_PHYSICAL_ATTACK,
        Strings.Ja.STATUS_MAGICAL_ATTACK,
        Strings.Ja.STATUS_PHYSICAL_DEFENSE,
        Strings.Ja.STATUS_MAGICAL_DEFENSE,
      };
    }
    return new String[] {
      Strings.En.HUD_HP,
      Strings.En.HUD_SPEED,
      Strings.En.STATUS_PHYSICAL_ATTACK,
      Strings.En.STATUS_MAGICAL_ATTACK,
      Strings.En.STATUS_PHYSICAL_DEFENSE,
      Strings.En.STATUS_MAGICAL_DEFENSE,
    };
  }

  /**
   * プレイヤーの現在ステを値ラベルに反映する。DungeonScreen が表示中に毎フレーム呼ぶ。
   *
   * <p>HP は {@code 現在 HP / 実効最大 HP}、その他は {@code 素値 (+補正)} 形式。補正は {@code effectiveStats() − stats()}
   * で算出する (装備 + Buff のぶん)。
   */
  public void updateValues(Player player) {
    Objects.requireNonNull(player, "player");
    Stats base = player.stats();
    Stats eff = player.effectiveStats();
    valueLabels[0].setText(base.currentHp() + " / " + eff.maxHp());
    valueLabels[1].setText(statText(base.speed(), eff.speed()));
    valueLabels[2].setText(statText(base.physicalAttack(), eff.physicalAttack()));
    valueLabels[3].setText(statText(base.magicalAttack(), eff.magicalAttack()));
    valueLabels[4].setText(statText(base.physicalDefense(), eff.physicalDefense()));
    valueLabels[5].setText(statText(base.magicalDefense(), eff.magicalDefense()));
  }

  /** 補正なしなら素値のみ、補正ありなら "素値 (+N)" / "素値 (-N)"。 */
  private static String statText(int base, int effective) {
    int delta = effective - base;
    if (delta == 0) {
      return Integer.toString(base);
    }
    return "%d (%+d)".formatted(base, delta);
  }

  /** Stage.act + Stage.draw を 1 フレーム分実行する。DungeonScreen.render から呼ぶ。 */
  public void render(float delta) {
    stage.act(delta);
    stage.draw();
  }

  /** ウィンドウ解像度変更時に Viewport を更新する。 */
  public void resize(int width, int height) {
    stage.getViewport().update(width, height, true);
  }

  @Override
  public void dispose() {
    stage.dispose();
    skin.dispose();
  }

  private TextureRegionDrawable solidDrawable(String name, Color color) {
    Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pm.setColor(color);
    pm.fill();
    Texture tex = new Texture(pm);
    pm.dispose();
    skin.add(name, tex); // skin.dispose() で破棄される
    return new TextureRegionDrawable(new TextureRegion(tex));
  }
}
