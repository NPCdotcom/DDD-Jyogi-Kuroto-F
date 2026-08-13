package core.presentation.window;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import org.junit.jupiter.api.Test;

class BorrowedFontSkinTest {

  @Test
  void disposeKeepsBorrowedFontAliveAndDisposesOwnedResources() {
    TrackingFont borrowedFont = new TrackingFont();
    TrackingResource ownedResource = new TrackingResource();
    BorrowedFontSkin skin = new BorrowedFontSkin(borrowedFont);
    skin.add("owned-resource", ownedResource, TrackingResource.class);

    skin.dispose();

    assertFalse(borrowedFont.disposed);
    assertTrue(ownedResource.disposed);
  }

  private static final class TrackingFont extends BitmapFont {
    private boolean disposed;

    private TrackingFont() {
      super(new BitmapFontData(), new TextureRegion(), false);
    }

    @Override
    public void dispose() {
      disposed = true;
    }
  }

  private static final class TrackingResource implements Disposable {
    private boolean disposed;

    @Override
    public void dispose() {
      disposed = true;
    }
  }
}
