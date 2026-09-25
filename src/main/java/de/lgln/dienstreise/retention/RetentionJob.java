package de.lgln.dienstreise.retention;

import de.lgln.dienstreise.config.DienstreiseProperties;
import de.lgln.dienstreise.trip.TripRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes trips whose end lies more than the retention period in the past (FR-024, DSGVO storage
 * limitation). The statement is idempotent, so running in several pods at once is harmless.
 */
@Component
public class RetentionJob {

  private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);

  private final TripRepository trips;
  private final DienstreiseProperties properties;
  private final Clock clock;
  private final Counter deleted;

  public RetentionJob(
      TripRepository trips, DienstreiseProperties properties, Clock clock, MeterRegistry meters) {
    this.trips = trips;
    this.properties = properties;
    this.clock = clock;
    this.deleted =
        Counter.builder("dienstreise.retention.deleted")
            .description("Trips deleted after the retention period")
            .register(meters);
  }

  @Scheduled(cron = "${dienstreise.retention.cron}")
  @Transactional
  public void run() {
    Duration period = properties.retention().period();
    if (period == null) {
      log.atWarn().log("retention period not configured, no trips deleted");
      return;
    }
    Instant cutoff = clock.instant().minus(period);
    int n = trips.deleteByEndAtBefore(cutoff);
    deleted.increment(n);
    log.atInfo()
        .addKeyValue("event", "retention.run")
        .addKeyValue("deleted", n)
        .addKeyValue("cutoff", cutoff)
        .log("retention run");
  }
}
