package de.lgln.dienstreise.integration;

import static de.lgln.dienstreise.support.TripSeeder.berlin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.lgln.dienstreise.support.AbstractIntegrationTest;
import de.lgln.dienstreise.support.TripSeeder;
import de.lgln.dienstreise.trip.TripCommand;
import de.lgln.dienstreise.trip.TripService;
import de.lgln.dienstreise.trip.domain.CarAlreadyBookedException;
import de.lgln.dienstreise.trip.domain.TripPeriod;
import de.lgln.dienstreise.web.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OverlapRulesIT extends AbstractIntegrationTest {

  static final CurrentUser ANNA = new CurrentUser(ANNA_SUBJECT, "Anna Beispiel");
  static final CurrentUser BEN = new CurrentUser(BEN_SUBJECT, "Ben Muster");

  /** Returns 0 when no car has two overlapping trips. */
  static final String OVERLAP_SQL =
      "SELECT count(*) FROM business_trip a JOIN business_trip b ON a.car_id = b.car_id"
          + " AND a.id < b.id"
          + " AND tstzrange(a.start_at, a.end_at, '[)') && tstzrange(b.start_at, b.end_at, '[)')";

  @Autowired TripService tripService;

  private static TripCommand command(int carId, String from, String to) {
    return new TripCommand(
        (short) carId,
        new TripPeriod(berlin("2026-10-02T" + from), berlin("2026-10-02T" + to)),
        "Celle");
  }

  @Test
  void independentTestOfUserStory4() {
    tripService.create(ANNA, command(1, "09:00", "12:00"));

    assertThatThrownBy(() -> tripService.create(BEN, command(1, "11:00", "13:00")))
        .isInstanceOf(CarAlreadyBookedException.class);
    tripService.create(BEN, command(1, "12:00", "14:00"));
    tripService.create(BEN, command(2, "10:00", "11:00"));

    assertThat(TripSeeder.count(jdbc)).isEqualTo(3);
    assertThat(jdbc.queryForObject(OVERLAP_SQL, Integer.class)).isZero();
  }
}
