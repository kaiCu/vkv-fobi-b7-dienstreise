package de.lgln.dienstreise.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import de.lgln.dienstreise.e2e.support.E2eTestBase;
import de.lgln.dienstreise.support.TripSeeder;
import org.junit.jupiter.api.Test;

class TripListE2eTest extends E2eTestBase {

  static final String ANNA = "%s|00000000-0000-0000-0000-00000000a001";

  @Test
  void listIsAccessibleAndReflows() {
    String owner = ANNA.formatted(issuerUri());
    TripSeeder.insert(jdbc, owner, 1, "2026-10-03T09:00", "2026-10-03T12:00", "Celle");
    TripSeeder.insert(jdbc, owner, 2, "2026-10-02T09:00", "2026-10-02T12:00", "Hannover");
    TripSeeder.insert(jdbc, owner, 3, "2026-09-30T09:00", "2026-09-30T12:00", "Emden");

    loginAs("anna");
    assertThat(page.locator("tbody tr")).hasCount(3);
    assertThat(page.locator("tbody tr").first()).containsText("Emden");
    assertNoAccessibilityViolations(page);

    page.setViewportSize(320, 800);
    page.reload();
    assertThat((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= 320"))
        .isTrue();
  }

  @Test
  void emptyStateIsAccessible() {
    loginAs("ben");
    assertThat(page.locator("main")).containsText("Sie haben noch keine Dienstreisen erfasst.");
    assertNoAccessibilityViolations(page);
  }
}
