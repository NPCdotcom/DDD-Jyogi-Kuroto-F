package core.infrastructure.desktop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 現行版のプラットフォーム表明が実装と一致することを検証する (PLATFORM-01)。
 *
 * <p>Android backend、タッチ導線、端末間セーブ同期はいずれも未実装であり、これらを現行機能として 説明する記述をリポジトリへ再混入させない。レビュー P0-4 の再発防止。
 *
 * <p>履歴資料は対象外とする。過去の判断を記録した文書を書き換えると、なぜ訴求を変えたのかが 追えなくなるため ({@link #HISTORICAL_PATHS})。
 */
class PlatformClaimTest {

  /** 検査対象の拡張子。 */
  private static final List<String> TARGET_EXTENSIONS =
      List.of(".md", ".java", ".gradle", ".yml", ".yaml");

  /** 履歴資料。過去の判断の記録なので改変せず、検査からも外す。 */
  private static final List<String> HISTORICAL_PATHS = List.of("docs/20260812_", "tasks/ai_log/");

  /** 走査から除外するディレクトリ。 */
  private static final List<String> EXCLUDED_DIRS =
      List.of(".git", "build", ".gradle", ".claude", "gradle");

  /**
   * 未実装機能を現行のものとして説明する表現。
   *
   * <p>「Android」単体は将来計画として言及されうるため対象にしない。対象外である旨を明記した 記述まで落とすと、逆に判断の記録が消えるため。
   */
  private static final List<String> FORBIDDEN_CLAIMS =
      List.of(
          "PC・スマホで動く",
          "Doko-demo Rogue",
          "どこでも動くローグ",
          "Desktop / Android 両動作",
          "Desktop / Android を 1 ソースで動かす",
          "スマホ側で同じセーブ",
          "スマホで **タイトル画面を同時表示**");

  @Test
  void currentDocsAndCodeDoNotClaimUnimplementedPlatforms() throws IOException {
    Path root = repositoryRoot();
    List<String> violations = new ArrayList<>();

    try (Stream<Path> paths = Files.walk(root)) {
      paths
          .filter(Files::isRegularFile)
          .filter(p -> hasTargetExtension(root.relativize(p)))
          .filter(p -> !isExcluded(root.relativize(p)))
          .filter(p -> !isHistorical(root.relativize(p)))
          .forEach(p -> collectViolations(root, p, violations));
    }

    assertTrue(
        violations.isEmpty(), "未実装のプラットフォーム表明が現行ファイルに残っています:\n" + String.join("\n", violations));
  }

  private static void collectViolations(Path root, Path file, List<String> violations) {
    List<String> lines;
    try {
      lines = Files.readAllLines(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      return; // UTF-8 で読めないファイルは対象外 (バイナリ等)。
    }
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      for (String claim : FORBIDDEN_CLAIMS) {
        if (line.contains(claim)) {
          violations.add(root.relativize(file) + ":" + (i + 1) + " -> " + claim);
        }
      }
    }
  }

  private static boolean hasTargetExtension(Path relative) {
    String name = relative.getFileName().toString().toLowerCase(Locale.ROOT);
    return TARGET_EXTENSIONS.stream().anyMatch(name::endsWith);
  }

  private static boolean isExcluded(Path relative) {
    String normalized = normalize(relative);
    return EXCLUDED_DIRS.stream().anyMatch(dir -> normalized.startsWith(dir + "/"));
  }

  private static boolean isHistorical(Path relative) {
    String normalized = normalize(relative);
    if (normalized.endsWith("PlatformClaimTest.java")) {
      return true; // 禁止語の一覧そのものを持つため自分自身は対象外。
    }
    return HISTORICAL_PATHS.stream().anyMatch(normalized::startsWith);
  }

  private static String normalize(Path relative) {
    return relative.toString().replace('\\', '/');
  }

  /**
   * リポジトリルートを解決する。
   *
   * <p>テストの作業ディレクトリはプロジェクトルートだが、worktree 実行でも壊れないよう {@code build.gradle} の存在で確認する。
   */
  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null && !Files.exists(current.resolve("build.gradle"))) {
      current = current.getParent();
    }
    if (current == null) {
      throw new IllegalStateException("build.gradle を含むリポジトリルートが見つかりません");
    }
    return current;
  }
}
