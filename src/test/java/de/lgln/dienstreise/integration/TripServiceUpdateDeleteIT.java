package de.lgln.dienstreise.integration;

import static de.lgln.dienstreise.support.TripSeeder.berlin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.lgln.dienstreise.support.AbstractIntegrationTest;
import de.lgln.dienstreise.support.TripSeeder;
import de.lgln.dienstreise.trip.TripCommand;
import de.lgln.dienstreise.trip.TripService;
import de.lgln.dienstreise.trip.domain.CarAlreadyBookedException;
import de.lgln.dienstreise.trip.domain.TripNotFoundException;
import de.lgln.dienstreise.trip.domain.TripPeriod;
import de.lgln.dienstreise.trip.domain.TripReadOnlyException;
import de.lgln.dienstreise.trip.domain.TripStaleException;
import de.lgln.dienstreise.trip.domain.TripValidationException;
import de.lgln.dienstreise.web.CurrentUser;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TripServiceUpdateDeleteIT extends AbstractIntegrationTest {

  static final CurrentUser ANNA = new CurrentUser(ANNA_SUBJECT, "Anna Beispiel");
  static final CurrentUser BEN = new CurrentUser(BEN_SUBJECT, "Ben Muster");

  @Autowired TripService tripService;

  UUID annas;

  @BeforeEach
  void seed() {
    annas =
        TripSeeder.insert(
            jdbc, ANNA_SUBJECT, 1, "2026-10-02T09:00", "2026-10-02T12:00", "Hannover");
  }

  private static TripCommand command(int carId, String day, String from, String to) {
    return new TripCommand(
        (short) carId,
        new TripPeriod(berlin(day + "T" + from), berlin(day + "T" + to)),
        "Braunschweig");
  }

  private Timestamp startOf(UUID id) {
    return jdbc.queryForObject(
        "SELECT start_at FROM business_trip WHERE id = ?", Timestamp.class, id);
  }

  @Test
  void unchangedOrWithinOwnSlotDoesNotConflictWithItself() {
    tripService.update(ANNA, annas, 0, command(1, "2026-10-02", "09:00", "12:00"));
    tripService.update(ANNA, annas, 1, command(1, "2026-10-02", "10:00", "11:30"));

    assertThat(startOf(annas).toInstant()).isEqualTo(berlin("2026-10-02T10:00"));
    assertThat(
            jdbc.queryForObject(
                "SELECT destination FROM business_trip WHERE id = ?", String.class, annas))
        .isEqualTo("Braunschweig");
  }

  @Test
  void movingIntoOtherTripConflicts() {
    TripSeeder.insert(jdbc, BEN_SUBJECT, 2, "2026-10-03T09:00", "2026-10-03T12:00", "Celle");

    assertThatThrownBy(
            () -> tripService.update(ANNA, annas, 0, command(2, "2026-10-03", "11:00", "13:00")))
        .isInstanceOf(CarAlreadyBookedException.class);
  }

  @Test
  void movingStartIntoThePastIsRejected() {
    assertThatThrownBy(
            () -> tripService.update(ANNA, annas, 0, command(1, "2026-09-30", "09:00", "12:00")))
        .isInstanceOf(TripValidationException.class)
        .satisfies(
            e ->
                assertThat(((TripValidationException) e).codes())
                    .containsExactly("trip.start.notInFuture"));
  }

  @Test
  void startedTripIsReadOnly() {
    clock.setInstant(berlin("2026-10-02T09:00"));

    assertThatThrownBy(
            () -> tripService.update(ANNA, annas, 0, command(1, "2026-10-05", "09:00", "12:00")))
        .isInstanceOf(TripReadOnlyException.class);
    assertThatThrownBy(() -> tripService.delete(ANNA, annas))
        .isInstanceOf(TripReadOnlyException.class);
    assertThat(TripSeeder.count(jdbc)).isEqualTo(1);
  }

  @Test
  void staleVersionIsRejected() {
    tripService.update(ANNA, annas, 0, command(1, "2026-10-02", "10:00", "12:00"));

    assertThatThrownBy(
            () -> tripService.update(ANNA, annas, 0, command(1, "2026-10-02", "09:00", "12:00")))
        .isInstanceOf(TripStaleException.class);
  }

  @Test
  void otherUsersTripIsNotFound() {
    assertThatThrownBy(
            () -> tripService.update(BEN, annas, 0, command(1, "2026-10-05", "09:00", "12:00")))
        .isInstanceOf(TripNotFoundException.class);
    assertThatThrownBy(() -> tripService.delete(BEN, annas))
        .isInstanceOf(TripNotFoundException.class);
    assertThat(startOf(annas).toInstant()).isEqualTo(berlin("2026-10-02T09:00"));
  }

  @Test
  void deleteRemovesTrip() {
    tripService.delete(ANNA, annas);

    assertThat(TripSeeder.count(jdbc)).isZero();
  }
}
