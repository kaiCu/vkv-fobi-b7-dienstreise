package de.lgln.dienstreise.integration;

import static de.lgln.dienstreise.support.TripSeeder.berlin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import de.lgln.dienstreise.support.AbstractIntegrationTest;
import de.lgln.dienstreise.support.TripSeeder;
import de.lgln.dienstreise.trip.AvailabilityService;
import de.lgln.dienstreise.trip.TripCommand;
import de.lgln.dienstreise.trip.TripService;
import de.lgln.dienstreise.trip.domain.CarAlreadyBookedException;
import de.lgln.dienstreise.trip.domain.TripPeriod;
import de.lgln.dienstreise.web.CurrentUser;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** SC-007: under concurrency the database constraint alone prevents double booking. */
class ConcurrentBookingIT extends AbstractIntegrationTest {

  static final int THREADS = 10;

  @Autowired TripService tripService;
  @MockitoSpyBean AvailabilityService availability;

  @RepeatedTest(5)
  void exactlyOneOfTenConcurrentBookingsSucceeds() throws Exception {
    doReturn(true).when(availability).isAvailable(any(), any(), any());
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Object>> results = new ArrayList<>();
    try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
      for (int i = 0; i < THREADS; i++) {
        CurrentUser user = new CurrentUser("https://idp.test|user-" + i, "User " + i);
        TripCommand cmd =
            new TripCommand(
                (short) 1,
                new TripPeriod(
                    berlin("2026-10-02T09:00").plusSeconds(60L * i), berlin("2026-10-02T12:00")),
                "Celle");
        results.add(
            pool.submit(
                () -> {
                  start.await();
                  try {
                    return tripService.create(user, cmd);
                  } catch (RuntimeException e) {
                    return e;
                  }
                }));
      }
      start.countDown();
    }

    List<Object> outcomes = new ArrayList<>();
    for (Future<Object> f : results) {
      outcomes.add(f.get());
    }
    assertThat(outcomes).filteredOn(java.util.UUID.class::isInstance).hasSize(1);
    assertThat(outcomes)
        .filteredOn(o -> !(o instanceof java.util.UUID))
        .hasSize(THREADS - 1)
        .allMatch(CarAlreadyBookedException.class::isInstance);
    assertThat(TripSeeder.count(jdbc)).isEqualTo(1);
    assertThat(jdbc.queryForObject(OverlapRulesIT.OVERLAP_SQL, Integer.class)).isZero();
  }
}
