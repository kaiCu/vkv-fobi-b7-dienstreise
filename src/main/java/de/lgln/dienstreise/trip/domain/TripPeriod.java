package de.lgln.dienstreise.trip.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** A half-open period {@code [start, end)} of a business trip. */
public record TripPeriod(Instant start, Instant end) {

  /** All user input and output is interpreted in this zone (FR-015). */
  public static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

  /**
   * Interprets local date and time in Europe/Berlin. Ambiguous times (autumn) resolve to the
   * earlier offset, non-existent times (spring) shift forward.
   */
  public static TripPeriod ofLocal(
      LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime) {
    return new TripPeriod(
        ZonedDateTime.of(startDate, startTime, ZONE).toInstant(),
        ZonedDateTime.of(endDate, endTime, ZONE).toInstant());
  }

  public boolean overlaps(TripPeriod other) {
    return start.isBefore(other.end) && other.start.isBefore(end);
  }

  public boolean isEndAfterStart() {
    return end.isAfter(start);
  }
}
