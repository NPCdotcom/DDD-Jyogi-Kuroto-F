package core.infrastructure.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import core.presentation.screen.DddGame;

/**
 * Desktop (Windows / macOS / Linux) 起動エントリ。
 *
 * <p>infrastructure 層に置く理由: LibGDX (Lwjgl3) という具体技術への依存を application 以下から 切り離すため。{@link DddGame} は
 * LibGDX の {@code Game} を継承する純粋な ApplicationListener で、 このクラスがそれをホスティングする。
 */
public final class DesktopLauncher {

  private DesktopLauncher() {}

  public static void main(String[] args) {
    Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
    config.setTitle("DDD-Jyogi-Kuroto-F (Doko-demo Rogue) - MVP");
    config.setWindowedMode(800, 600);
    config.setResizable(false);
    config.setForegroundFPS(60);
    config.useVsync(true);
    new Lwjgl3Application(new DddGame(), config);
  }
}
