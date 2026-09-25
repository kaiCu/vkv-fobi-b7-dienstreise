package de.lgln.dienstreise.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Page;
import de.lgln.dienstreise.e2e.support.E2eTestBase;
import org.junit.jupiter.api.Test;

class RecordTripE2eTest extends E2eTestBase {

  @Test
  void recordTripWithKeyboardOnly() {
    loginAs("anna");
    assertNoAccessibilityViolations(page);

    tabTo("a[href='/trips/new']");
    page.keyboard().press("Enter");
    page.waitForURL("**/trips/new");
    assertNoAccessibilityViolations(page);

    // Invalid submit: error summary gets focus
    tabTo("main button[type=submit]:not([value=back])");
    page.evaluate(
        "() => document.querySelectorAll('[required]').forEach(e => e.removeAttribute('required'))");
    page.keyboard().press("Enter");
    page.waitForSelector("#error-summary");
    assertThat(page.title()).startsWith("Fehler: ");
    assertThat((Boolean) page.evaluate("() => document.activeElement.id === 'error-summary'"))
        .isTrue();
    assertNoAccessibilityViolations(page);

    // Period tomorrow 09:00-12:00 (server clock: 2026-10-01 08:00)
    tabTo("#startDate");
    typeDate("02102026");
    tabTo("#startTime");
    typeTime("#startTime", "0900");
    tabTo("#endDate");
    typeDate("02102026");
    tabTo("#endTime");
    typeTime("#endTime", "1200");
    assertThat(page.locator("#startDate").inputValue()).isEqualTo("2026-10-02");
    assertThat(page.locator("#endTime").inputValue()).isEqualTo("12:00");
    tabTo("main button[type=submit]:not([value=back])");
    page.keyboard().press("Enter");

    page.waitForSelector("input[name=carId]");
    assertNoAccessibilityViolations(page);
    tabTo("#car-1");
    page.keyboard().press("Space");
    tabTo("main button[type=submit]:not([value=back])");
    page.keyboard().press("Enter");

    page.waitForSelector("#destination");
    assertNoAccessibilityViolations(page);
    tabTo("#destination");
    page.keyboard().type("Hannover");
    tabTo("main button[type=submit]:not([value=back])");
    page.keyboard().press("Enter");

    page.waitForURL(TRIPS_URL);
    assertThat(page.locator("[role=status]")).hasText("Die Dienstreise wurde gespeichert.");

    page.reload();
    assertThat(page.locator("main")).containsText("Hannover");
    assertThat(jdbc.queryForObject("SELECT destination FROM business_trip", String.class))
        .isEqualTo("Hannover");
  }

  @Test
  void notFoundPageIsAccessible() {
    loginAs("anna");
    Page.NavigateOptions options = new Page.NavigateOptions();
    page.navigate(tripsBaseUrl() + "/00000000-0000-0000-0000-000000000000/edit", options);
    assertThat(page.locator("h1")).hasText("Dienstreise nicht gefunden.");
    assertNoAccessibilityViolations(page);
  }
}
