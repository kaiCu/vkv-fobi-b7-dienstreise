package de.lgln.dienstreise.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.lgln.dienstreise.support.AbstractIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * SC-003: 500 trips render in under 2 s. Runs only with {@code ./mvnw verify -Pperf} on a dedicated
 * machine before each release; wall-clock assertions are not deterministic on shared CI runners.
 */
class TripListPerfIT extends AbstractIntegrationTest {

  static final int TRIPS = 500;

  @Test
  void fiveHundredTripsRenderInUnderTwoSeconds() throws Exception {
    Instant base = Instant.parse("2026-11-01T06:00:00Z");
    List<Object[]> rows = new ArrayList<>();
    for (int i = 0; i < TRIPS; i++) {
      Instant start = base.plus(Duration.ofHours(4L * (i / 3)));
      rows.add(
          new Object[] {
            UUID.randomUUID(),
            ANNA_SUBJECT,
            (i % 3) + 1,
            Timestamp.from(start),
            Timestamp.from(start.plus(Duration.ofHours(3))),
            "Ziel " + i
          });
    }
    jdbc.batchUpdate(
        "INSERT INTO business_trip (id, owner_subject, car_id, start_at, end_at, destination,"
            + " updated_at) VALUES (?, ?, ?, ?, ?, ?, now())",
        rows);

    mockMvc.perform(get("/trips").with(anna())).andExpect(status().isOk());
    for (int run = 0; run < 5; run++) {
      long started = System.nanoTime();
      String body =
          mockMvc
              .perform(get("/trips").with(anna()))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString(StandardCharsets.UTF_8);
      long millis = Duration.ofNanos(System.nanoTime() - started).toMillis();
      assertThat(millis).as("run %d", run).isLessThan(2_000);
      assertThat(org.jsoup.Jsoup.parse(body).select("tbody tr")).hasSize(TRIPS);
    }
  }
}
