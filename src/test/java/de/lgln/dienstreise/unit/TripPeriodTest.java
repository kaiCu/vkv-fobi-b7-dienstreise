package de.lgln.dienstreise.unit;

import static org.assertj.core.api.Assertions.assertThat;

import de.lgln.dienstreise.trip.domain.TripPeriod;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class TripPeriodTest {

  private static TripPeriod local(
      String startDate, String startTime, String endDate, String endTime) {
    return TripPeriod.ofLocal(
        LocalDate.parse(startDate),
        LocalTime.parse(startTime),
        LocalDate.parse(endDate),
        LocalTime.parse(endTime));
  }

  private static TripPeriod hours(int from, int to) {
    return local("2026-10-02", "%02d:00".formatted(from), "2026-10-02", "%02d:00".formatted(to));
  }

  @Test
  void summerTimeIsConvertedToUtc() {
    TripPeriod p = local("2026-07-01", "09:00", "2026-07-01", "12:00");
    assertThat(p.start()).isEqualTo(Instant.parse("2026-07-01T07:00:00Z"));
    assertThat(p.end()).isEqualTo(Instant.parse("2026-07-01T10:00:00Z"));
  }

  @Test
  void ambiguousAutumnTimeResolvesToEarlierOffset() {
    TripPeriod p = local("2026-10-25", "02:30", "2026-10-25", "05:00");
    assertThat(p.start()).isEqualTo(Instant.parse("2026-10-25T00:30:00Z"));
  }

  @Test
  void nonExistentSpringTimeShiftsForward() {
    TripPeriod p = local("2027-03-28", "02:30", "2027-03-28", "06:00");
    assertThat(ZonedDateTime.ofInstant(p.start(), TripPeriod.ZONE).toLocalTime())
        .isEqualTo(LocalTime.of(3, 30));
  }

  @Test
  void overlapsUsesHalfOpenIntervals() {
    assertThat(hours(9, 12).overlaps(hours(11, 13))).isTrue();
    assertThat(hours(9, 12).overlaps(hours(12, 14))).isFalse();
    assertThat(hours(12, 14).overlaps(hours(9, 12))).isFalse();
    assertThat(hours(9, 12).overlaps(hours(9, 12))).isTrue();
    assertThat(hours(9, 12).overlaps(hours(10, 11))).isTrue();
  }

  @Test
  void endAfterStart() {
    assertThat(hours(9, 12).isEndAfterStart()).isTrue();
    assertThat(hours(9, 9).isEndAfterStart()).isFalse();
    assertThat(hours(12, 9).isEndAfterStart()).isFalse();
  }
}
