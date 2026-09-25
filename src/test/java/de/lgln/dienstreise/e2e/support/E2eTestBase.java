package de.lgln.dienstreise.e2e.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.deque.html.axecore.playwright.AxeBuilder;
import com.deque.html.axecore.results.AxeResults;
import com.deque.html.axecore.results.Rule;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.lgln.dienstreise.support.MutableClock;
import de.lgln.dienstreise.support.TestClockConfig;
import de.lgln.dienstreise.support.TestcontainersConfig;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Real browser (Chromium) against the app with a real Keycloak login. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfig.class, TestClockConfig.class})
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class E2eTestBase {

  public static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.7.4";

  static final KeycloakContainer KEYCLOAK =
      new KeycloakContainer(KEYCLOAK_IMAGE).withRealmImportFile("keycloak/realm-dienstreise.json");

  static {
    KEYCLOAK.start();
  }

  @DynamicPropertySource
  static void oidc(DynamicPropertyRegistry registry) {
    registry.add("DIENSTREISE_OIDC_ISSUER_URI", E2eTestBase::issuerUri);
    registry.add("DIENSTREISE_OIDC_CLIENT_SECRET", () -> "dev-secret");
  }

  protected static String issuerUri() {
    return KEYCLOAK.getAuthServerUrl() + "/realms/dienstreise";
  }

  /** The list, also with Spring Security's {@code ?continue} marker after login. */
  protected static final Pattern TRIPS_URL = Pattern.compile(".*/trips(\\?continue)?$");

  @LocalServerPort protected int port;
  @Autowired protected JdbcTemplate jdbc;
  @Autowired protected MutableClock clock;

  private Playwright playwright;
  private Browser browser;
  protected BrowserContext context;
  protected Page page;

  @BeforeAll
  void launchBrowser() {
    playwright = Playwright.create();
    browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
  }

  @AfterAll
  void closeBrowser() {
    if (playwright != null) {
      playwright.close();
    }
  }

  @BeforeEach
  void newContext() {
    jdbc.update("DELETE FROM business_trip");
    clock.setInstant(TestClockConfig.DEFAULT_NOW);
    context = newBrowserContext();
    page = context.newPage();
  }

  @AfterEach
  void closeContext() {
    if (context != null) {
      context.close();
    }
  }

  protected BrowserContext newBrowserContext() {
    return browser.newContext(
        new Browser.NewContextOptions().setLocale("de-DE").setTimezoneId("Europe/Berlin"));
  }

  protected String tripsBaseUrl() {
    return "http://localhost:" + port + "/trips";
  }

  /** Opens the list and signs in on the Keycloak login page. */
  protected void loginAs(String user) {
    loginAs(page, user);
  }

  protected void loginAs(Page page, String user) {
    page.navigate(tripsBaseUrl());
    page.locator("#username").fill(user);
    page.locator("#password").fill(user);
    page.locator("#kc-login").click();
    page.waitForURL(TRIPS_URL);
  }

  /** Automated WCAG 2.1 A/AA check with axe-core. */
  protected void assertNoAccessibilityViolations(Page page) {
    AxeResults results =
        new AxeBuilder(page)
            .withTags(List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa"))
            .analyze();
    List<Rule> violations = results.getViolations();
    String report =
        violations.stream()
            .map(
                v ->
                    v.getId()
                        + " ("
                        + v.getImpact()
                        + "): "
                        + v.getHelp()
                        + " -> "
                        + v.getNodes().stream()
                            .map(n -> String.valueOf(n.getTarget()))
                            .collect(Collectors.joining(", ")))
            .collect(Collectors.joining("\n"));
    assertThat(violations).as("axe violations on %s:%n%s", page.url(), report).isEmpty();
  }

  /** Presses Tab until the element matching the selector has focus. */
  protected void tabTo(String selector) {
    for (int i = 0; i < 40; i++) {
      page.keyboard().press("Tab");
      if ((Boolean) page.evaluate("s => document.activeElement.matches(s)", selector)) {
        return;
      }
    }
    throw new AssertionError("Could not reach " + selector + " with the keyboard");
  }

  /**
   * Types a date into the focused native date input. The segment order depends on the browser UI
   * language (headless Chromium ignores the page locale), so it is probed once with a scratch
   * input.
   */
  protected void typeDate(String ddmmyyyy) {
    boolean dayFirst;
    try (Page probe = context.newPage()) {
      probe.setContent("<input type=date id=d>");
      probe.locator("#d").focus();
      probe.keyboard().type("01022026");
      dayFirst = probe.locator("#d").inputValue().equals("2026-02-01");
    }
    page.bringToFront();
    String day = ddmmyyyy.substring(0, 2);
    String month = ddmmyyyy.substring(2, 4);
    page.keyboard().type((dayFirst ? day + month : month + day) + ddmmyyyy.substring(4));
  }

  /** Types a time into the input; adds AM/PM when the browser UI uses a 12-hour clock. */
  protected void typeTime(String selector, String hhmm) {
    page.keyboard().type(hhmm);
    if (page.locator(selector).inputValue().isEmpty()) {
      page.keyboard().type(Integer.parseInt(hhmm.substring(0, 2)) >= 12 ? "P" : "A");
    }
  }
}
