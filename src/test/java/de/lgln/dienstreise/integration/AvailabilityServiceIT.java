package de.lgln.dienstreise.integration;

import static de.lgln.dienstreise.support.TripSeeder.berlin;
import static org.assertj.core.api.Assertions.assertThat;

import de.lgln.dienstreise.support.AbstractIntegrationTest;
import de.lgln.dienstreise.support.TripSeeder;
import de.lgln.dienstreise.trip.AvailabilityService;
import de.lgln.dienstreise.trip.CarAvailability;
import de.lgln.dienstreise.trip.domain.TripPeriod;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AvailabilityServiceIT extends AbstractIntegrationTest {

  @Autowired AvailabilityService availability;

  private static TripPeriod period(String from, String to) {
    return new TripPeriod(berlin("2026-10-02T" + from), berlin("2026-10-02T" + to));
  }

  @Test
  void overlappingTripMakesCarUnavailable() {
    TripSeeder.insert(jdbc, ANNA_SUBJECT, 1, "2026-10-02T09:00", "2026-10-02T12:00", "Hannover");

    List<CarAvailability> result = availability.availabilityFor(period("10:00", "11:00"), null);

    assertThat(result)
        .containsExactly(
            new CarAvailability((short) 1, "Dienstwagen 1 (H-LG 101)", false),
            new CarAvailability((short) 2, "Dienstwagen 2 (H-LG 102)", true),
            new CarAvailability((short) 3, "Dienstwagen 3 (H-LG 103)", true));
    assertThat(AvailabilityService.noneAvailable(result)).isFalse();
  }

  @Test
  void backToBackPeriodIsAvailable() {
    TripSeeder.insert(jdbc, ANNA_SUBJECT, 1, "2026-10-02T09:00", "2026-10-02T12:00", "Hannover");

    assertThat(availability.availabilityFor(period("12:00", "14:00"), null))
        .allMatch(CarAvailability::available);
  }

  @Test
  void tripBeingEditedIsIgnored() {
    UUID id =
        TripSeeder.insert(
            jdbc, ANNA_SUBJECT, 1, "2026-10-02T09:00", "2026-10-02T12:00", "Hannover");

    assertThat(availability.availabilityFor(period("10:00", "11:00"), id))
        .allMatch(CarAvailability::available);
    assertThat(availability.isAvailable((short) 1, period("10:00", "11:00"), id)).isTrue();
    assertThat(availability.isAvailable((short) 1, period("10:00", "11:00"), null)).isFalse();
  }

  @Test
  void noneAvailableWhenAllCarsAreBooked() {
    for (int car = 1; car <= 3; car++) {
      TripSeeder.insert(jdbc, BEN_SUBJECT, car, "2026-10-02T09:00", "2026-10-02T12:00", "Celle");
    }

    assertThat(
            AvailabilityService.noneAvailable(
                availability.availabilityFor(period("10:00", "11:00"), null)))
        .isTrue();
  }
}
