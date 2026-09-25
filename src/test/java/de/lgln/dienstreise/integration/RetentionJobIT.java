package de.lgln.dienstreise.integration;

import static org.assertj.core.api.Assertions.assertThat;

import de.lgln.dienstreise.retention.RetentionJob;
import de.lgln.dienstreise.support.AbstractIntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** FR-024: trips are deleted strictly after end + retention period (P90D in tests). */
class RetentionJobIT extends AbstractIntegrationTest {

  @Autowired RetentionJob job;
  @Autowired MeterRegistry meters;

  private void insertEndingAt(int carId, String end, String destination) {
    Instant endAt = Instant.parse(end);
    jdbc.update(
        "INSERT INTO business_trip (id, owner_subject, car_id, start_at, end_at, destination,"
            + " updated_at) VALUES (?, ?, ?, ?, ?, ?, now())",
        UUID.randomUUID(),
        ANNA_SUBJECT,
        carId,
        Timestamp.from(endAt.minusSeconds(3600)),
        Timestamp.from(endAt),
        destination);
  }

  private double deletedCounter() {
    var counter = meters.find("dienstreise.retention.deleted").counter();
    return counter == null ? 0 : counter.count();
  }

  @Test
  void deletesTripsOlderThanRetentionPeriod() {
    clock.setInstant(Instant.parse("2027-01-10T00:00:00Z"));
    insertEndingAt(1, "2026-10-11T23:59:00Z", "zu alt");
    insertEndingAt(2, "2026-10-12T00:00:00Z", "genau an der Grenze");
    insertEndingAt(3, "2026-12-01T10:00:00Z", "jünger");
    double before = deletedCounter();

    job.run();

    List<String> remaining =
        jdbc.queryForList("SELECT destination FROM business_trip ORDER BY end_at", String.class);
    assertThat(remaining).containsExactly("genau an der Grenze", "jünger");
    assertThat(deletedCounter() - before).isEqualTo(1.0);

    job.run();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM business_trip", Integer.class))
        .isEqualTo(2);
    assertThat(deletedCounter() - before).isEqualTo(1.0);
  }
}
