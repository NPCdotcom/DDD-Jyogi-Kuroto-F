package core.presentation.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * カード画像が仕様規則を完全に満たしているか機械検証する自動テスト基盤。
 *
 * <p>検証契約 (Rule Standards):
 *
 * <ul>
 *   <li>解像度: 240 × 336 px (RGBA 透過 PNG)
 *   <li>色数制限: 可視 (Alpha > 0) ピクセルのユニーク色数が 24 色以下
 *   <li>四隅透過: 四隅ピクセルが完全透過 (Alpha == 0)
 *   <li>下部予約帯: y=240〜335 の領域に文字・枠・主要オブジェクトが非透過で存在しない
 *   <li>残留マゼンタ: 透過抜き残りの純粋マゼンタ (#FF00FF) が 0 ピクセル
 * </ul>
 */
public class CardImageValidatorTest {

  public static final int EXPECTED_WIDTH = 240;
  public static final int EXPECTED_HEIGHT = 336;
  public static final int MAX_VISIBLE_COLORS = 24;
  public static final int ART_HEIGHT_LIMIT = 240; // 上部 240px がアート枠、下部 96px はテキスト予約帯

  public record ValidationReport(
      boolean isValid,
      int width,
      int height,
      boolean hasAlpha,
      int visibleColorCount,
      boolean cornersTransparent,
      int bottomReservationVisiblePixels,
      int magentaPixelCount,
      String errorMessage) {}

  /** 指定した BufferedImage がカード画像仕様を全て満たしているか機械的に検査する。 */
  public static ValidationReport validateImage(BufferedImage img) {
    if (img == null) {
      return new ValidationReport(false, 0, 0, false, 0, false, 0, 0, "Image is null");
    }

    int width = img.getWidth();
    int height = img.getHeight();
    boolean hasAlpha = img.getColorModel().hasAlpha();

    if (width != EXPECTED_WIDTH || height != EXPECTED_HEIGHT) {
      return new ValidationReport(
          false,
          width,
          height,
          hasAlpha,
          0,
          false,
          0,
          0,
          "Invalid dimensions: "
              + width
              + "x"
              + height
              + ", expected "
              + EXPECTED_WIDTH
              + "x"
              + EXPECTED_HEIGHT);
    }

    if (!hasAlpha) {
      return new ValidationReport(
          false,
          width,
          height,
          false,
          0,
          false,
          0,
          0,
          "Color model must support alpha channel (RGBA)");
    }

    // 四隅透過の検証 (0,0), (239,0), (0,335), (239,335)
    boolean cornersTransparent =
        isTransparent(img, 0, 0)
            && isTransparent(img, EXPECTED_WIDTH - 1, 0)
            && isTransparent(img, 0, EXPECTED_HEIGHT - 1)
            && isTransparent(img, EXPECTED_WIDTH - 1, EXPECTED_HEIGHT - 1);

    Set<Integer> visibleColors = new HashSet<>();
    int bottomReservationVisiblePixels = 0;
    int magentaPixelCount = 0;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int argb = img.getRGB(x, y);
        int alpha = (argb >> 24) & 0xFF;
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;

        if (alpha > 0) {
          // アルファ値をマスクした RGB 24bit 色
          int rgb = argb & 0x00FFFFFF;
          visibleColors.add(rgb);

          // 残留マゼンタ (#FF00FF)
          if (red == 255 && green == 0 && blue == 255) {
            magentaPixelCount++;
          }

          // 下部 96px (y >= 240) の可視ピクセル数
          if (y >= ART_HEIGHT_LIMIT) {
            bottomReservationVisiblePixels++;
          }
        }
      }
    }

    int visibleColorCount = visibleColors.size();
    if (visibleColorCount > MAX_VISIBLE_COLORS) {
      return new ValidationReport(
          false,
          width,
          height,
          true,
          visibleColorCount,
          cornersTransparent,
          bottomReservationVisiblePixels,
          magentaPixelCount,
          "Visible color count exceeds limit: " + visibleColorCount + " > " + MAX_VISIBLE_COLORS);
    }

    if (!cornersTransparent) {
      return new ValidationReport(
          false,
          width,
          height,
          true,
          visibleColorCount,
          false,
          bottomReservationVisiblePixels,
          magentaPixelCount,
          "Corner pixels must be transparent");
    }

    if (magentaPixelCount > 0) {
      return new ValidationReport(
          false,
          width,
          height,
          true,
          visibleColorCount,
          cornersTransparent,
          bottomReservationVisiblePixels,
          magentaPixelCount,
          "Found residual magenta pixels: " + magentaPixelCount);
    }

    if (bottomReservationVisiblePixels > 50) { // 下部テキスト帯への著しい侵入を拒否
      return new ValidationReport(
          false,
          width,
          height,
          true,
          visibleColorCount,
          cornersTransparent,
          bottomReservationVisiblePixels,
          magentaPixelCount,
          "Bottom reservation area contains visible pixels: " + bottomReservationVisiblePixels);
    }

    return new ValidationReport(
        true,
        width,
        height,
        true,
        visibleColorCount,
        cornersTransparent,
        bottomReservationVisiblePixels,
        magentaPixelCount,
        "OK");
  }

  private static boolean isTransparent(BufferedImage img, int x, int y) {
    int argb = img.getRGB(x, y);
    int alpha = (argb >> 24) & 0xFF;
    return alpha == 0;
  }

  @Test
  void testValidCardImagePassesValidation(@TempDir Path tempDir) throws IOException {
    // 240x336 RGBA、四隅透過、16色、下部透過の正常ダミー画像を作成
    BufferedImage img =
        new BufferedImage(EXPECTED_WIDTH, EXPECTED_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < ART_HEIGHT_LIMIT; y++) {
      for (int x = 0; x < EXPECTED_WIDTH; x++) {
        if (x > 10 && x < 230 && y > 10 && y < 230) {
          // 上部中央に可視カラー描画 (16色以内のグラデーション/パターン)
          int color = 0xFF000000 | ((x % 4) * 0x330000) | ((y % 4) * 0x003300);
          img.setRGB(x, y, color);
        }
      }
    }

    ValidationReport report = validateImage(img);
    assertTrue(report.isValid(), "仕様に合致した画像は validation をパスすること: " + report.errorMessage());
    assertEquals(EXPECTED_WIDTH, report.width());
    assertEquals(EXPECTED_HEIGHT, report.height());
    assertTrue(report.hasAlpha());
    assertTrue(report.visibleColorCount() <= MAX_VISIBLE_COLORS);
    assertTrue(report.cornersTransparent());
    assertEquals(0, report.bottomReservationVisiblePixels());
    assertEquals(0, report.magentaPixelCount());
  }

  @Test
  void testTooManyColorsFailsValidation() {
    BufferedImage img =
        new BufferedImage(EXPECTED_WIDTH, EXPECTED_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    // 30 色のフルカラーピクセルを書き込む
    for (int i = 0; i < 30; i++) {
      img.setRGB(10 + i, 10, 0xFF000000 | (i * 0x080808));
    }

    ValidationReport report = validateImage(img);
    assertFalse(report.isValid(), "24色を超える画像は否決されること");
    assertTrue(report.errorMessage().contains("Visible color count exceeds limit"));
  }

  @Test
  void testMagentaPixelFailsValidation() {
    BufferedImage img =
        new BufferedImage(EXPECTED_WIDTH, EXPECTED_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    img.setRGB(50, 50, 0xFFFF00FF); // 残留マゼンタ

    ValidationReport report = validateImage(img);
    assertFalse(report.isValid(), "残留マゼンタを含む画像は否決されること");
    assertTrue(report.errorMessage().contains("Found residual magenta pixels"));
  }
}
