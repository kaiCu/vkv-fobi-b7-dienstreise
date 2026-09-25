package de.lgln.dienstreise.integration;

import static org.assertj.core.api.Assertions.assertThat;

import de.lgln.dienstreise.support.TestClockConfig;
import de.lgln.dienstreise.support.TestOidcConfig;
import de.lgln.dienstreise.support.TestcontainersConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "management.server.port=0")
@Import({TestcontainersConfig.class, TestClockConfig.class, TestOidcConfig.class})
@ActiveProfiles("test")
class ActuatorIT {

  @LocalManagementPort int managementPort;

  private final HttpClient client = HttpClient.newHttpClient();

  @Test
  void livenessIsUp() throws Exception {
    HttpResponse<String> response = get("/actuator/health/liveness");
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo("{\"status\":\"UP\"}");
  }

  @Test
  void readinessIsUp() throws Exception {
    HttpResponse<String> response = get("/actuator/health/readiness");
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo("{\"status\":\"UP\"}");
  }

  @Test
  void envEndpointIsNotExposed() throws Exception {
    assertThat(get("/actuator/env").statusCode()).isEqualTo(404);
  }

  private HttpResponse<String> get(String path) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + managementPort + path)).build(),
        HttpResponse.BodyHandlers.ofString());
  }
}
