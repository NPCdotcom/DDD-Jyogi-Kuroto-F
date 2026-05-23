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

  /** 装備アイコンの 1 辺サイズ (px、ROW_HEIGHT=56f より小さく、行頭に左寄せ配置)。 */
  private static final float ICON_SIZE = 40f;

  /** 装備アイコンの X 座標 (LIST_X=90f の左、行頭マージン)。 */
  private static final float ICON_X = 36f;

  private final DddGame game;
  private final List<Equipment> allEquipment;

  private OrthographicCamera camera;
  private Viewport viewport;
  private SpriteBatch batch;
  private float scrollOffset;
  private float maxScroll;

  /**
   * 装備アイコン Texture のキャッシュ (Wave 10 W10-γ)。show() で iconPath を持つ装備全件を 1 度だけ Texture 化、 dispose()
   * で全件解放 (VRAM リーク完全防止、Wave 8 W8-β GameResources パターン継承)。
   */
  private java.util.Map<core.domain.equipment.EquipmentId, com.badlogic.gdx.graphics.Texture>
      equipmentIcons;

  public EquipmentScreen(DddGame game) {
    this.game = game;
    // presentation → infrastructure 依存方向違反を解消するため、DddGame 経由で間接化。
    // game.cardCatalog() と同型の game.equipmentCatalog() がまだ無いので InitialStateFactory 直経由
    // (M2 で equipmentCatalog 窓口メソッドを DddGame に追加する候補、本 Task 1 では cardCatalog のみ統一)。
    this.allEquipment = core.infrastructure.bootstrap.InitialStateFactory.equipmentCatalog().all();
  }

  @Override
  public void show() {
    camera = new OrthographicCamera();
    viewport = new FitViewport(RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT, camera);
    batch = new SpriteBatch();
    maxScroll = Math.max(0f, allEquipment.size() * ROW_HEIGHT - (LIST_TOP_Y - LIST_BOTTOM_Y));
    // Wave 10 W10-γ: iconPath を持つ装備全件を 1 度だけ Texture 化してキャッシュ。
    equipmentIcons = new java.util.HashMap<>();
    for (Equipment eq : allEquipment) {
      eq.iconPath()
          .ifPresent(
              path -> {
                try {
                  com.badlogic.gdx.graphics.Texture tex =
                      new com.badlogic.gdx.graphics.Texture(Gdx.files.internal(path));
                  tex.setFilter(
                      com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest,
                      com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest);
                  equipmentIcons.put(eq.id(), tex);
                } catch (Exception e) {
                  Gdx.app.log(
                      "EquipmentScreen", "Icon load failed for " + eq.id().value() + ": " + path);
                }
              });
    }
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
    if (eq.equals(game.progress().loadout().get(eq.slot()))) {
      game.unequipSlot(eq.slot()); // 既に装着中 → 解除
    } else {
      game.equipInLoadout(eq); // 未装着 → 装着 (同スロットの既存装備は置き換わる)
    }
    // Wave 10 W10-β-2: 装着/解除クリック確定時に BUTTON_DECISION SE
    // (項目移動 SE は scrollOffset 連続値のためカウンタ境界が定義できず、連続爆音回避で省略)
    game.soundManager().playSe(core.infrastructure.audio.SeKind.BUTTON_DECISION);
  }

  private void drawList(SpriteBatch batch) {
    boolean jp = game.fonts().isJapaneseAvailable();
    BitmapFont font = game.fonts().large();
    Map<EquipmentSlot, Equipment> loadout = game.progress().loadout();
    for (int i = 0; i < allEquipment.size(); i++) {
      float y = LIST_TOP_Y - i * ROW_HEIGHT + scrollOffset;
      if (y < LIST_BOTTOM_Y || y > LIST_TOP_Y + ROW_HEIGHT) {
        continue;
      }
      Equipment eq = allEquipment.get(i);
      boolean equipped = eq.equals(loadout.get(eq.slot()));
      // Wave 10 W10-γ: 行頭にアイコン描画 (iconPath ありの装備のみ、欠落は描画スキップ)
      com.badlogic.gdx.graphics.Texture icon = equipmentIcons.get(eq.id());
      if (icon != null) {
        // テキストベースライン (y) より上にアイコン底辺を合わせる: y - ICON_SIZE + フォント高さ調整
        batch.draw(icon, ICON_X, y - ICON_SIZE + 8f, ICON_SIZE, ICON_SIZE);
      }
      font.setColor(equipped ? EQUIPPED_COLOR : AVAILABLE_COLOR);
      font.draw(batch, rowText(eq, equipped, jp), LIST_X, y);
    }
    font.setColor(Color.WHITE);
  }

  private String rowText(Equipment eq, boolean equipped, boolean jp) {
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
    // HP は Ja / En 共通の "HP" を Strings.Ja.HUD_HP 経由で取る (キー重複の defensive 化)。
    appendStat(sb, jp ? Strings.Ja.HUD_HP : Strings.En.HUD_HP, b.maxHp());
    appendStat(
        sb, jp ? Strings.Ja.BONUS_STAT_SPEED_SHORT : Strings.En.BONUS_STAT_SPEED_SHORT, b.speed());
    appendStat(
        sb,
        jp
            ? Strings.Ja.BONUS_STAT_PHYSICAL_ATTACK_SHORT
            : Strings.En.BONUS_STAT_PHYSICAL_ATTACK_SHORT,
        b.physicalAttack());
    appendStat(
        sb,
        jp
            ? Strings.Ja.BONUS_STAT_MAGICAL_ATTACK_SHORT
            : Strings.En.BONUS_STAT_MAGICAL_ATTACK_SHORT,
        b.magicalAttack());
    appendStat(
        sb,
        jp
            ? Strings.Ja.BONUS_STAT_PHYSICAL_DEFENSE_SHORT
            : Strings.En.BONUS_STAT_PHYSICAL_DEFENSE_SHORT,
        b.physicalDefense());
    appendStat(
        sb,
        jp
            ? Strings.Ja.BONUS_STAT_MAGICAL_DEFENSE_SHORT
            : Strings.En.BONUS_STAT_MAGICAL_DEFENSE_SHORT,
        b.magicalDefense());
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

  /**
   * 装備の grantedCards を「カード表示名」文字列に変換する。 {@code game.cardCatalog()} 経由で presentation→infrastructure
   * 依存方向違反を解消 (Wave 1 P4-8 残り対応)。 非 static 化したのでスコープ解決は {@code this.rowText} 経由 (drawList 内
   * instance method からの呼出)。
   */
  private String grantedCardText(Equipment eq) {
    if (eq.grantedCards().isEmpty()) {
      return "-";
    }
    StringBuilder sb = new StringBuilder();
    for (CardId cid : eq.grantedCards()) {
      if (!sb.isEmpty()) {
        sb.append(", ");
      }
      sb.append(game.cardCatalog().get(cid).displayName());
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
            + game.progress().loadout().size()
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
    // Wave 10 W10-γ: 装備アイコン Texture 群をループで全件 dispose (VRAM リーク絶対防衛、
    // Wave 8 W8-β GameResources パターン継承で個別 try-catch)
    if (equipmentIcons != null) {
      for (com.badlogic.gdx.graphics.Texture tex : equipmentIcons.values()) {
        if (tex != null) {
          try {
            tex.dispose();
          } catch (Exception e) {
            Gdx.app.log("EquipmentScreen", "Icon dispose failed: " + e.getMessage());
          }
        }
      }
      equipmentIcons.clear();
      equipmentIcons = null;
    }
  }
}
