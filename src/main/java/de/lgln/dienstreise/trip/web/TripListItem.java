package de.lgln.dienstreise.trip.web;

import de.lgln.dienstreise.trip.domain.BusinessTrip;
import de.lgln.dienstreise.trip.domain.TripStatus;
import de.lgln.dienstreise.web.GermanDateTimeFormat;
import java.time.Instant;
import java.util.UUID;

/**
 * One row of the list, already formatted (FR-015).
 *
 * @param startDate date only, used in accessible names of the row actions
 * @param editable only upcoming trips can be changed or deleted (FR-023)
 */
public record TripListItem(
    UUID id,
    String carLabel,
    String start,
    String startDate,
    String destination,
    String end,
    TripStatus status,
    boolean editable) {

  public static TripListItem from(BusinessTrip trip, GermanDateTimeFormat fmt, Instant now) {
    TripStatus status = trip.status(now);
    return new TripListItem(
        trip.getId(),
        trip.getCar().label(),
        fmt.dateTime(trip.getStartAt()),
        fmt.date(trip.getStartAt()),
        trip.getDestination(),
        fmt.dateTime(trip.getEndAt()),
        status,
        status == TripStatus.UPCOMING);
  }
}
