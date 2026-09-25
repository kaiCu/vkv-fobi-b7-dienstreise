package de.lgln.dienstreise.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Guards against XSS and CSP violations in Thymeleaf templates. */
class TemplateSafetyTest {

  private static final Path TEMPLATES = Path.of("src/main/resources/templates");
  private static final Pattern INLINE_SCRIPT = Pattern.compile("<script(?![^>]*\\bsrc=)[^>]*>");
  private static final Pattern STYLE_ATTR = Pattern.compile("\\sstyle=\"");
  private static final Pattern EXTERNAL_URL =
      Pattern.compile("\\s(?:th:)?(?:src|href|action)=\"https?://");

  @Test
  void templatesAreSafe() throws IOException {
    assertThat(TEMPLATES).isDirectory();
    List<String> violations = new ArrayList<>();
    try (Stream<Path> files = Files.walk(TEMPLATES)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".html")).toList()) {
        String content = Files.readString(file);
        check(violations, file, content, Pattern.compile("th:utext"), "th:utext");
        check(violations, file, content, INLINE_SCRIPT, "inline <script>");
        check(violations, file, content, STYLE_ATTR, "style attribute");
        check(violations, file, content, EXTERNAL_URL, "external URL");
      }
    }
    assertThat(violations).isEmpty();
  }

  private static void check(
      List<String> violations, Path file, String content, Pattern pattern, String what) {
    Matcher m = pattern.matcher(content);
    while (m.find()) {
      violations.add(file + ": " + what + " at offset " + m.start());
    }
  }
}
