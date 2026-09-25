package de.lgln.dienstreise.integration;

import static de.lgln.dienstreise.support.TripSeeder.berlin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.lgln.dienstreise.support.AbstractIntegrationTest;
import de.lgln.dienstreise.support.TestClockConfig;
import de.lgln.dienstreise.support.TripSeeder;
import de.lgln.dienstreise.trip.TripCommand;
import de.lgln.dienstreise.trip.TripService;
import de.lgln.dienstreise.trip.domain.CarAlreadyBookedException;
import de.lgln.dienstreise.trip.domain.TripPeriod;
import de.lgln.dienstreise.trip.domain.TripValidationException;
import de.lgln.dienstreise.web.CurrentUser;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TripServiceCreateIT extends AbstractIntegrationTest {

  static final CurrentUser ANNA = new CurrentUser(ANNA_SUBJECT, "Anna Beispiel");

  @Autowired TripService tripService;

  private static TripCommand command(int carId, String from, String to, String destination) {
    return new TripCommand(
        (short) carId,
        new TripPeriod(berlin("2026-10-02T" + from), berlin("2026-10-02T" + to)),
        destination);
  }

  @Test
  void createStoresTripForOwner() {
    UUID id = tripService.create(ANNA, command(1, "09:00", "12:00", "  Hannover "));

    Map<String, Object> row = jdbc.queryForMap("SELECT * FROM business_trip WHERE id = ?", id);
    assertThat(row.get("owner_subject")).isEqualTo(ANNA_SUBJECT);
    assertThat(row.get("destination")).isEqualTo("Hannover");
    assertThat(((Number) row.get("car_id")).intValue()).isEqualTo(1);
    assertThat(((Timestamp) row.get("created_at")).toInstant())
        .isEqualTo(TestClockConfig.DEFAULT_NOW);
    assertThat(((Timestamp) row.get("start_at")).toInstant()).isEqualTo(berlin("2026-10-02T09:00"));
  }

  @Test
  void startInThePastAtSaveIsRejected() {
    clock.advance(Duration.ofDays(2));

    assertThatThrownBy(() -> tripService.create(ANNA, command(1, "09:00", "12:00", "Hannover")))
        .isInstanceOf(TripValidationException.class)
        .satisfies(
            e ->
                assertThat(((TripValidationException) e).codes())
                    .contains("trip.start.notInFuture"));
    assertThat(TripSeeder.count(jdbc)).isZero();
  }

  @Test
  void bookedCarIsRejectedBeforeInsert() {
    TripSeeder.insert(jdbc, BEN_SUBJECT, 1, "2026-10-02T10:00", "2026-10-02T11:00", "Celle");

    assertThatThrownBy(() -> tripService.create(ANNA, command(1, "09:00", "12:00", "Hannover")))
        .isInstanceOf(CarAlreadyBookedException.class);
    assertThat(TripSeeder.count(jdbc)).isEqualTo(1);
  }
}
