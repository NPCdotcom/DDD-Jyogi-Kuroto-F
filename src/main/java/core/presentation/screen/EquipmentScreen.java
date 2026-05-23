package core.presentation.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import core.domain.card.CardId;
import core.domain.equipment.Equipment;
import core.domain.equipment.EquipmentSlot;
import core.domain.equipment.StatsBonus;
import core.infrastructure.bootstrap.InitialStateFactory;
import core.presentation.render.RenderLayout;
import core.presentation.render.Strings;
import java.util.List;
import java.util.Map;

/**
 * 装備変更画面 (§15-9)。{@code equipment.json} の全装備を一覧表示し、クリックで装備 / 解除する。タイトル画面から E キーで開く。
 *
 * <p>変更したロードアウトは {@link DddGame#loadout()} に保持され、次のラン開始時に Player へ反映される (ADR-26:
 * 戦闘中の装備変更は行わず、ラン開始時にデッキを再構築する)。固定カメラ 1 本 + スクロールオフセットの縦スクロール リスト ({@code CardCollectionScreen}
 * と同型) + クリックヒットテストで構成する。
 */
public final class EquipmentScreen extends ScreenAdapter {

  private static final float ROW_HEIGHT = 56f;
  private static final float LIST_X = 90f;
  private static final float LIST_TOP_Y = 900f;
  private static final float LIST_BOTTOM_Y = 110f;
  private static final float SCROLL_SPEED = 1100f;

  private static final Color EQUIPPED_COLOR = new Color(0.55f, 1f, 0.6f, 1f);
  private static final Color AVAILABLE_COLOR = new Color(0.82f, 0.82f, 0.88f, 1f);

  private final DddGame game;
  private final List<Equipment> allEquipment = InitialStateFactory.equipmentCatalog().all();

  private OrthographicCamera camera;
  private Viewport viewport;
  private SpriteBatch batch;
  private float scrollOffset;
  private float maxScroll;

  public EquipmentScreen(DddGame game) {
    this.game = game;
  }

  @Override
  public void show() {
    camera = new OrthographicCamera();
    viewport = new FitViewport(RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT, camera);
    batch = new SpriteBatch();
    maxScroll = Math.max(0f, allEquipment.size() * ROW_HEIGHT - (LIST_TOP_Y - LIST_BOTTOM_Y));
  }

  @Override
  public void render(float delta) {
    if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
      game.changeScreen(new TitleScreen(game));
      return;
    }
    handleScroll(delta);
    handleClick();

    ScreenUtils.clear(0.06f, 0.06f, 0.09f, 1f);
    viewport.apply();
    batch.setProjectionMatrix(camera.combined);
    batch.begin();
    drawList(batch);
    drawHeaderAndFooter(batch);
    batch.end();
  }

  private void handleScroll(float delta) {
    float dy = 0f;
    if (Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.S)) {
      dy += SCROLL_SPEED * delta;
    }
    if (Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W)) {
      dy -= SCROLL_SPEED * delta;
    }
    scrollOffset = Math.max(0f, Math.min(maxScroll, scrollOffset + dy));
  }

  private void handleClick() {
    if (!Gdx.input.justTouched()) {
      return;
    }
    Vector3 w = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
    viewport.unproject(w);
    if (w.y < LIST_BOTTOM_Y || w.y > LIST_TOP_Y + 20f) {
      return;
    }
    int i = Math.round((LIST_TOP_Y + scrollOffset - w.y) / ROW_HEIGHT);
    if (i < 0 || i >= allEquipment.size()) {
      return;
    }
    Equipment eq = allEquipment.get(i);
    if (eq.equals(game.loadout().get(eq.slot()))) {
      game.unequipSlot(eq.slot()); // 既に装着中 → 解除
    } else {
      game.equipInLoadout(eq); // 未装着 → 装着 (同スロットの既存装備は置き換わる)
    }
  }

  private void drawList(SpriteBatch batch) {
    boolean jp = game.fonts().isJapaneseAvailable();
    BitmapFont font = game.fonts().large();
    Map<EquipmentSlot, Equipment> loadout = game.loadout();
    for (int i = 0; i < allEquipment.size(); i++) {
      float y = LIST_TOP_Y - i * ROW_HEIGHT + scrollOffset;
      if (y < LIST_BOTTOM_Y || y > LIST_TOP_Y + ROW_HEIGHT) {
        continue;
      }
      Equipment eq = allEquipment.get(i);
      boolean equipped = eq.equals(loadout.get(eq.slot()));
      font.setColor(equipped ? EQUIPPED_COLOR : AVAILABLE_COLOR);
      font.draw(batch, rowText(eq, equipped, jp), LIST_X, y);
    }
    font.setColor(Color.WHITE);
  }

  private static String rowText(Equipment eq, boolean equipped, boolean jp) {
    String mark =
        equipped
            ? (jp ? Strings.Ja.EQUIP_EQUIPPED_MARK : Strings.En.EQUIP_EQUIPPED_MARK) + " "
            : "";
    return "%s%s [%s]  %s  + %s"
        .formatted(
            mark,
            eq.displayName(),
            slotLabel(eq.slot(), jp),
            bonusText(eq.statsBonus(), jp),
            grantedCardText(eq));
  }

  private static String slotLabel(EquipmentSlot slot, boolean jp) {
    return switch (slot) {
      case FEET -> jp ? Strings.Ja.EQUIP_SLOT_FEET : Strings.En.EQUIP_SLOT_FEET;
      case HAND -> jp ? Strings.Ja.EQUIP_SLOT_HAND : Strings.En.EQUIP_SLOT_HAND;
      case MAIN -> jp ? Strings.Ja.EQUIP_SLOT_MAIN : Strings.En.EQUIP_SLOT_MAIN;
      case HEAD -> jp ? Strings.Ja.EQUIP_SLOT_HEAD : Strings.En.EQUIP_SLOT_HEAD;
      case BODY -> jp ? Strings.Ja.EQUIP_SLOT_BODY : Strings.En.EQUIP_SLOT_BODY;
      case ACCESSORY -> jp ? Strings.Ja.EQUIP_SLOT_ACCESSORY : Strings.En.EQUIP_SLOT_ACCESSORY;
    };
  }

  private static String bonusText(StatsBonus b, boolean jp) {
    StringBuilder sb = new StringBuilder();
    appendStat(sb, jp ? "HP" : "HP", b.maxHp());
    appendStat(sb, jp ? "速" : "SPD", b.speed());
    appendStat(sb, jp ? "物攻" : "PAtk", b.physicalAttack());
    appendStat(sb, jp ? "魔攻" : "MAtk", b.magicalAttack());
    appendStat(sb, jp ? "物防" : "PDef", b.physicalDefense());
    appendStat(sb, jp ? "魔防" : "MDef", b.magicalDefense());
    return sb.isEmpty() ? "-" : sb.toString();
  }

  private static void appendStat(StringBuilder sb, String label, int value) {
    if (value == 0) {
      return;
    }
    if (!sb.isEmpty()) {
      sb.append(' ');
    }
    sb.append(label).append(value > 0 ? "+" : "").append(value);
  }

  private static String grantedCardText(Equipment eq) {
    if (eq.grantedCards().isEmpty()) {
      return "-";
    }
    StringBuilder sb = new StringBuilder();
    for (CardId cid : eq.grantedCards()) {
      if (!sb.isEmpty()) {
        sb.append(", ");
      }
      sb.append(InitialStateFactory.cardCatalog().get(cid).displayName());
    }
    return sb.toString();
  }

  private void drawHeaderAndFooter(SpriteBatch batch) {
    boolean jp = game.fonts().isJapaneseAvailable();
    BitmapFont font = game.fonts().large();
    font.setColor(0.95f, 0.85f, 0.3f, 1f);
    font.draw(
        batch,
        (jp ? Strings.Ja.EQUIP_SCREEN_TITLE : Strings.En.EQUIP_SCREEN_TITLE)
            + "    "
            + game.loadout().size()
            + " / "
            + EquipmentSlot.values().length,
        LIST_X,
        RenderLayout.SCREEN_HEIGHT - 28f);
    font.setColor(0.7f, 0.7f, 0.78f, 1f);
    font.draw(batch, jp ? Strings.Ja.EQUIP_SCREEN_HINT : Strings.En.EQUIP_SCREEN_HINT, LIST_X, 58f);
    font.setColor(Color.WHITE);
  }

  @Override
  public void resize(int width, int height) {
    viewport.update(width, height, true);
  }

  @Override
  public void dispose() {
    if (batch != null) {
      batch.dispose();
    }
  }
}
