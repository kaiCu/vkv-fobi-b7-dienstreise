package de.lgln.dienstreise.unit;

import static org.assertj.core.api.Assertions.assertThat;

import de.lgln.dienstreise.trip.domain.TripStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TripStatusTest {

  private static final Instant START = Instant.parse("2026-10-02T07:00:00Z");
  private static final Instant END = Instant.parse("2026-10-02T10:00:00Z");

  @Test
  void upcomingBeforeStart() {
    assertThat(TripStatus.of(START, END, START.minusSeconds(1))).isEqualTo(TripStatus.UPCOMING);
  }

  @Test
  void ongoingAtStart() {
    assertThat(TripStatus.of(START, END, START)).isEqualTo(TripStatus.ONGOING);
  }

  @Test
  void ongoingBeforeEnd() {
    assertThat(TripStatus.of(START, END, END.minusSeconds(1))).isEqualTo(TripStatus.ONGOING);
  }

  @Test
  void pastAtEnd() {
    assertThat(TripStatus.of(START, END, END)).isEqualTo(TripStatus.PAST);
  }

  @Test
  void messageKeys() {
    assertThat(TripStatus.UPCOMING.messageKey()).isEqualTo("trip.status.upcoming");
    assertThat(TripStatus.ONGOING.messageKey()).isEqualTo("trip.status.ongoing");
    assertThat(TripStatus.PAST.messageKey()).isEqualTo("trip.status.past");
  }
}
