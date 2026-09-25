package de.lgln.dienstreise.integration;

import static de.lgln.dienstreise.support.WizardSupport.period;
import static de.lgln.dienstreise.support.WizardSupport.postForm;
import static org.assertj.core.api.Assertions.assertThat;

import de.lgln.dienstreise.support.AbstractIntegrationTest;
import de.lgln.dienstreise.support.TripSeeder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** FR-019 / US4 AS6: nothing about anna's trip reaches ben. */
class NoDisclosureIT extends AbstractIntegrationTest {

  static final List<String> SECRETS =
      List.of("Geheimziel-XYZ-4711", "anna-sub", "Anna Beispiel", "09:00", "12:00");

  @Test
  void conflictResponsesDoNotRevealOtherTrips() throws Exception {
    TripSeeder.insert(
        jdbc, ANNA_SUBJECT, 1, "2026-10-02T09:00", "2026-10-02T12:00", "Geheimziel-XYZ-4711");

    assertNoSecrets(post("/trips/new/period", period("10:00", "11:00")));
    assertNoSecrets(post("/trips/new/car", period("10:00", "11:00", "carId", "1")));
    assertNoSecrets(post("/trips", period("10:00", "11:00", "carId", "1", "destination", "Celle")));
  }

  private MockHttpServletResponse post(String url, Map<String, String> values) throws Exception {
    return mockMvc.perform(postForm(url, ben(), values)).andReturn().getResponse();
  }

  private static void assertNoSecrets(MockHttpServletResponse response) throws Exception {
    StringBuilder all = new StringBuilder(response.getContentAsString(StandardCharsets.UTF_8));
    for (String name : response.getHeaderNames()) {
      all.append('\n').append(name).append(": ").append(response.getHeaders(name));
    }
    for (String secret : SECRETS) {
      assertThat(all.toString()).doesNotContain(secret);
    }
  }
}
