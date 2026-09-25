package de.lgln.dienstreise.integration;

import static org.assertj.core.api.Assertions.assertThat;

import de.lgln.dienstreise.support.TestClockConfig;
import de.lgln.dienstreise.support.TestOidcConfig;
import de.lgln.dienstreise.support.TestcontainersConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "DIENSTREISE_SESSION_TIMEOUT=17m")
@Import({TestcontainersConfig.class, TestClockConfig.class, TestOidcConfig.class})
@ActiveProfiles("test")
class SessionSecurityIT {

  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;

  @Test
  void sessionCookieIsHardenedAndStoredInDatabase() throws Exception {
    jdbc.update("DELETE FROM spring_session");
    HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    HttpResponse<Void> response =
        client.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/trips")).build(),
            HttpResponse.BodyHandlers.discarding());

    assertThat(response.statusCode()).isEqualTo(302);
    List<String> cookies = response.headers().allValues("Set-Cookie");
    assertThat(cookies).noneMatch(c -> c.startsWith("JSESSIONID="));
    String session =
        cookies.stream().filter(c -> c.startsWith("SESSION=")).findFirst().orElseThrow();
    assertThat(session).contains("Secure").contains("HttpOnly").contains("SameSite=Lax");
    assertThat(session).doesNotContainIgnoringCase("Domain=");

    assertThat(jdbc.queryForObject("SELECT count(*) FROM spring_session", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject("SELECT max_inactive_interval FROM spring_session", Integer.class))
        .isEqualTo(1020);
  }
}
