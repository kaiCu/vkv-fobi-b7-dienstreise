package de.lgln.dienstreise.trip.domain;

import java.time.Instant;

/** Derived from the server clock; never stored. Only upcoming trips can be changed (FR-023). */
public enum TripStatus {
  UPCOMING("trip.status.upcoming"),
  ONGOING("trip.status.ongoing"),
  PAST("trip.status.past");

  private final String messageKey;

  TripStatus(String messageKey) {
    this.messageKey = messageKey;
  }

  public String messageKey() {
    return messageKey;
  }

  public static TripStatus of(Instant start, Instant end, Instant now) {
    if (now.isBefore(start)) {
      return UPCOMING;
    }
    return now.isBefore(end) ? ONGOING : PAST;
  }
}
