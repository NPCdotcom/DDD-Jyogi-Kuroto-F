package core.presentation.screen;

import com.badlogic.gdx.Game;
import core.application.GameContext;
import core.application.TurnDirector;
import core.infrastructure.bootstrap.InitialStateFactory;
import core.presentation.render.Fonts;
import java.util.Random;

/**
 * LibGDX の {@link Game} を継承したエントリポイント。
 *
 * <p>presentation 層に置く理由: LibGDX 依存 (Game 継承 + Screen 切替) を application 層から 切り離すため。application 層は
 * LibGDX に依存しない {@link GameContext} と {@link TurnDirector} のみで構成される。
 *
 * <p>{@link GameContext} / {@link TurnDirector} / {@link Fonts} を 1 つの所有者 (この Game) に集約する
 * ことで、Screen 側が古い参照を保持して新ラン後に状態を取りこぼす事故を構造的に防ぐ。Screen からは {@link #context()} / {@link #director()}
 * / {@link #fonts()} で都度取得する。
 */
public final class DddGame extends Game {

  private GameContext context;
  private TurnDirector director;
  private Fonts fonts;

  public GameContext context() {
    return context;
  }

  public TurnDirector director() {
    return director;
  }

  public Fonts fonts() {
    return fonts;
  }

  /** 新しいラン (= ダンジョン挑戦) を開始する。context と director を作り直す。 */
  public void startNewRun() {
    this.context = GameContext.startNewRun(InitialStateFactory.firstFloor());
    // ADR-19: TurnDirector に Random を注入 (毎ターンドロー + 山札再シャッフル用)。
    // ラン毎に new Random() で異なるシード = ゲームプレイは非再現的 (テストでは固定シード使用)。
    this.director = new TurnDirector(this.context, new Random());
  }

  @Override
  public void create() {
    fonts = new Fonts();
    startNewRun();
    setScreen(new TitleScreen(this));
  }

  @Override
  public void dispose() {
    super.dispose();
    if (fonts != null) {
      fonts.dispose();
    }
  }
}
