package core.presentation.window;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import core.domain.layer.LayerEndNode;
import core.domain.layer.NodeResolveContext;
import core.presentation.render.LayerEndNodeLabels;
import core.presentation.render.RenderLayout;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 層末ノード選択ポップアップ (§15-8 / E-6)。
 *
 * <p>Scene2D の {@link Stage} + {@link Window} を最小構成で利用する初の UI ポップアップ。Skin は JSON 経由ではなく Java
 * コードから直接組み立てる (`uiskin.json` をリポジトリに置かないハッカソンスコープ)。背景の単色 Drawable は 1x1 Pixmap から Texture を作り
 * {@link Skin} に管理させる (skin.dispose() で破棄される)。
 *
 * <p>入力ハンドリングは Scene2D の Actor input 機構を使わず、外部 (DungeonScreen) が {@code Gdx.input.isKeyJustPressed}
 * を読み取り {@link #select(int)} を呼ぶ方針。マウスクリックは YAGNI (M2 で本格 Scene2D 化する時に追加)。
 *
 * <p>選択結果は {@link #consume()} 経由で取得され、取得時点で内部状態がリセットされる (同じ選択が二重発火しない)。
 */
public final class NodeChoicePopup implements Disposable {

  /** 選択肢の数 (§15-8 「4 種から 3 提示」、本 PR は固定 3 提示)。 */
  public static final int CHOICE_COUNT = 3;

  private final Stage stage;
  private final Skin skin;
  private final List<LayerEndNode> choices;
  private LayerEndNode pendingResolution;

  /**
   * 3 種の {@link LayerEndNode} を受け取り、Stage / Window を構築する。
   *
   * @param font プロジェクタ視認性確保のため {@link core.presentation.render.Fonts#large()} (32px) を渡す想定。 旧実装は
   *     hud(16px) + {@code setFontScale(2f)} だったが、Scene2D Label の倍率描画 + multi-page atlas で漢字が黒四角化する
   *     LibGDX バグに遭遇したため、 等倍 32px で運用する。
   * @param title Window タイトル (Strings.Ja/En.LAYER_END_TITLE を呼出側で日英解決して渡す)
   * @param choices サイズ 3 必須、それ以外は IAE
   * @param context Wave 3 Task A: 各 LayerEndNode のラベル解決 ({@link LayerEndNodeLabels#labelOf}) に渡す
   *     Card / Equipment 解決コンテキスト
   * @param jp Wave 8 W8-α: true なら日本語ラベル、false なら英語ラベルで描画
   */
  public NodeChoicePopup(
      BitmapFont font,
      String title,
      List<LayerEndNode> choices,
      NodeResolveContext context,
      boolean jp) {
    Objects.requireNonNull(font, "font");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(choices, "choices");
    Objects.requireNonNull(context, "context");
    if (choices.size() != CHOICE_COUNT) {
      throw new IllegalArgumentException(
          "exactly " + CHOICE_COUNT + " choices required, got " + choices.size());
    }
    this.choices = List.copyOf(choices);

    Viewport viewport = new FitViewport(RenderLayout.SCREEN_WIDTH, RenderLayout.SCREEN_HEIGHT);
    this.stage = new Stage(viewport);
    this.skin = new Skin();
    skin.add("default-font", font);

    Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
    skin.add("default", labelStyle);

    Window.WindowStyle windowStyle = new Window.WindowStyle();
    windowStyle.titleFont = font;
    windowStyle.titleFontColor = new Color(0.95f, 0.85f, 0.3f, 1f);
    windowStyle.background = solidDrawable("popup-bg", new Color(0.08f, 0.08f, 0.12f, 0.95f));
    skin.add("default", windowStyle);

    Window window = new Window(title, windowStyle);
    window.setMovable(false);
    window.setResizable(false);
    // 32px 直接フォント前提でタイトル上余白は 64px に縮小 (旧 96px は scale=2 + 16px ベースの遷移コスト)
    window.padTop(64f);
    window.padLeft(32f).padRight(32f).padBottom(32f);
    // setFontScale は使わない: pixel フォントを Scene2D Label で倍率描画すると multi-page atlas
    // 参照がずれて漢字が黒四角化する (LibGDX 1.14 既知挙動)。large(32px) を等倍で運用する。

    for (int i = 0; i < CHOICE_COUNT; i++) {
      // Wave 8 W8-α: ラベル解決は presentation 層の LayerEndNodeLabels に委譲 (domain 純度回復)
      final int idx = i;
      Label line =
          new Label(
              "  [%d]  %s"
                  .formatted(i + 1, LayerEndNodeLabels.labelOf(choices.get(i), context, jp)),
              labelStyle);
      line.setColor(Color.WHITE);
      // Wave 14 W14-β: Scene2D ClickListener で各選択肢をクリック可能化 (キーボード数字キーと等価)
      line.setTouchable(Touchable.enabled);
      line.addListener(
          new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
              select(idx);
            }
          });
      window.add(line).left().padBottom(16f).row();
    }

    Label hint = new Label("数字キー 1 〜 3 / クリックで選択", labelStyle);
    hint.setColor(new Color(0.65f, 0.65f, 0.7f, 1f));
    window.add(hint).left().padTop(20f).row();

    window.pack();
    window.setPosition(
        (RenderLayout.SCREEN_WIDTH - window.getWidth()) / 2f,
        (RenderLayout.SCREEN_HEIGHT - window.getHeight()) / 2f);
    stage.addActor(window);
  }

  /**
   * 1〜{@link #CHOICE_COUNT} 番目の選択肢を pending にセットする (0-indexed)。
   *
   * <p>範囲外は no-op (誤入力に強い設計)。{@link #consume()} を呼ぶまで pending は保持される。
   */
  public void select(int index) {
    if (index < 0 || index >= choices.size()) {
      return;
    }
    pendingResolution = choices.get(index);
  }

  /**
   * 選択結果を取得し、内部状態をリセットする。
   *
   * <p>未選択 (or 取得済) の場合は {@link Optional#empty()} を返す。
   */
  public Optional<LayerEndNode> consume() {
    LayerEndNode result = pendingResolution;
    pendingResolution = null;
    return Optional.ofNullable(result);
  }

  /** 表示中の選択肢 (構築時に渡された choices そのまま)。 */
  public List<LayerEndNode> choices() {
    return choices;
  }

  /**
   * Stage.act + Stage.draw を 1 フレーム分実行する。DungeonScreen.render から呼ぶ。
   *
   * <p>Wave 14 W14-β: 表示中は Stage を InputProcessor として登録し、Scene2D ClickListener (各選択肢の クリック発火)
   * を有効化する。{@code Gdx.input.justTouched}/{@code isKeyJustPressed} は polling API のため InputProcessor
   * 登録と独立で動く (DungeonScreen 側の数字キー判定は両立する)。
   */
  public void render(float delta) {
    com.badlogic.gdx.Gdx.input.setInputProcessor(stage);
    stage.act(delta);
    stage.draw();
  }

  /** ウィンドウ解像度変更時に Viewport を更新する。 */
  public void resize(int width, int height) {
    stage.getViewport().update(width, height, true);
  }

  @Override
  public void dispose() {
    // Wave 14 W14-β: 登録した InputProcessor を解除して他画面に干渉しないようにする
    if (com.badlogic.gdx.Gdx.input.getInputProcessor() == stage) {
      com.badlogic.gdx.Gdx.input.setInputProcessor(null);
    }
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
