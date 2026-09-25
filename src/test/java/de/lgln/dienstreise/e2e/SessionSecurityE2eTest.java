package de.lgln.dienstreise.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.Cookie;
import de.lgln.dienstreise.e2e.support.E2eTestBase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class SessionSecurityE2eTest extends E2eTestBase {

  @Test
  void sessionIdChangesOnLoginAndIsInvalidatedOnLogout() throws Exception {
    page.navigate(tripsBaseUrl());
    page.locator("#username").waitFor();
    String beforeLogin = sessionCookie();

    page.locator("#username").fill("anna");
    page.locator("#password").fill("anna");
    page.locator("#kc-login").click();
    page.waitForURL(TRIPS_URL);
    String afterLogin = sessionCookie();

    assertThat(afterLogin).isNotNull().isNotEqualTo(beforeLogin);
    assertThat(fetchTrips(afterLogin).statusCode()).isEqualTo(200);

    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Abmelden")).click();
    page.waitForLoadState();

    HttpResponse<Void> afterLogout = fetchTrips(afterLogin);
    assertThat(afterLogout.statusCode()).isEqualTo(302);
    assertThat(afterLogout.headers().firstValue("Location").orElseThrow())
        .endsWith("/oauth2/authorization/dienstreise");
    String sessionId = new String(Base64.getDecoder().decode(afterLogin), StandardCharsets.UTF_8);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM spring_session WHERE session_id = ?",
                Integer.class,
                sessionId))
        .isZero();
  }

  private String sessionCookie() {
    return context.cookies().stream()
        .filter(c -> c.name.equals("SESSION") && c.domain.equals("localhost"))
        .map((Cookie c) -> c.value)
        .findFirst()
        .orElse(null);
  }

  private HttpResponse<Void> fetchTrips(String session) throws Exception {
    return HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()
        .send(
            HttpRequest.newBuilder(URI.create(tripsBaseUrl()))
                .header("Cookie", "SESSION=" + session)
                .build(),
            HttpResponse.BodyHandlers.discarding());
  }
}
