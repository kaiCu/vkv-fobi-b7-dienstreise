package de.lgln.dienstreise.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import de.lgln.dienstreise.e2e.support.E2eTestBase;
import de.lgln.dienstreise.support.TripSeeder;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EditDeleteTripE2eTest extends E2eTestBase {

  @Test
  void editWithKeyboardThenDeleteAfterCancelling() {
    String owner = issuerUri() + "|00000000-0000-0000-0000-00000000a001";
    UUID first =
        TripSeeder.insert(jdbc, owner, 1, "2026-10-02T09:00", "2026-10-02T12:00", "Hannover");
    UUID second =
        TripSeeder.insert(jdbc, owner, 2, "2026-10-03T09:00", "2026-10-03T12:00", "Celle");
    loginAs("anna");
    assertThat(page.locator("tbody tr").first()).containsText("Hannover");

    // Edit trip 1: move it to 2026-10-05 and switch to car 3, keyboard only
    tabTo("a[href='/trips/" + first + "/edit']");
    page.keyboard().press("Enter");
    page.waitForSelector("#startDate");
    assertNoAccessibilityViolations(page);
    tabTo("#startDate");
    typeDate("05102026");
    tabTo("#endDate");
    typeDate("05102026");
    tabTo("main button[type=submit]:not([value=back])");
    page.keyboard().press("Enter");
    page.waitForSelector("input[name=carId]");
    assertNoAccessibilityViolations(page);
    // Tab reaches the checked radio of the group; arrow keys move the selection
    tabTo("input[name=carId]:checked");
    page.keyboard().press("ArrowDown");
    page.keyboard().press("ArrowDown");
    assertThat(page.locator("#car-3")).isChecked();
    tabTo("main button[type=submit]:not([value=back])");
    page.keyboard().press("Enter");
    page.waitForSelector("#destination");
    assertNoAccessibilityViolations(page);
    tabTo("main button[type=submit]:not([value=back])");
    page.keyboard().press("Enter");

    page.waitForURL(TRIPS_URL);
    assertThat(page.locator("[role=status]")).hasText("Die Änderungen wurden gespeichert.");
    assertThat(page.locator("tbody tr").first()).containsText("Celle");
    assertThat(page.locator("tbody tr").nth(1)).containsText("Hannover");
    assertThat(page.locator("tbody tr").nth(1)).containsText("Dienstwagen 3");

    // Delete trip 2: cancel first, then confirm
    page.locator("a[href='/trips/" + second + "/delete']").click();
    assertThat(page.locator("h1")).hasText("Dienstreise löschen?");
    assertNoAccessibilityViolations(page);
    page.locator("main a[href='/trips']").click();
    assertThat(page.locator("tbody tr")).hasCount(2);

    page.locator("a[href='/trips/" + second + "/delete']").click();
    page.locator("main button[type=submit]").click();
    assertThat(page.locator("[role=status]")).hasText("Die Dienstreise wurde gelöscht.");
    assertThat(page.locator("tbody tr")).hasCount(1);
  }
}
