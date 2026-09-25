package de.lgln.dienstreise.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import de.lgln.dienstreise.e2e.support.E2eTestBase;
import org.junit.jupiter.api.Test;

/** US4 AS2 with two real users in two browsers. */
class DoubleBookingE2eTest extends E2eTestBase {

  private void reachDestinationStep(Page page, String from, String to) {
    page.navigate(tripsBaseUrl() + "/new");
    page.locator("#startDate").fill("2026-10-02");
    page.locator("#startTime").fill(from);
    page.locator("#endDate").fill("2026-10-02");
    page.locator("#endTime").fill(to);
    page.locator("main button[value=next]").click();
    page.locator("#car-2").check();
    page.locator("main button[value=next]").click();
    page.locator("#destination").fill("Ziel");
  }

  @Test
  void secondSaverIsSentBackToCarSelection() {
    loginAs(page, "anna");
    try (BrowserContext bensContext = newBrowserContext()) {
      Page bensPage = bensContext.newPage();
      loginAs(bensPage, "ben");

      reachDestinationStep(page, "09:00", "12:00");
      reachDestinationStep(bensPage, "11:00", "13:00");

      page.locator("main button[value=save]").click();
      assertThat(page.locator("[role=status]")).hasText("Die Dienstreise wurde gespeichert.");

      bensPage.locator("main button[value=save]").click();
      assertThat(bensPage.locator("#car-2")).isDisabled();
      assertThat(bensPage.locator("label[for=car-2]")).containsText("nicht verfügbar");
      assertThat(bensPage.locator("#error-summary"))
          .containsText("Dieses Fahrzeug ist im gewählten Zeitraum bereits gebucht.");
      assertNoAccessibilityViolations(bensPage);
    }
  }
}
