package de.lgln.dienstreise.web;

import de.lgln.dienstreise.trip.domain.TripPeriod;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** German date and time output in Europe/Berlin (FR-015). Used in templates as {@code @fmt}. */
@Component("fmt")
public class GermanDateTimeFormat {

  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm", Locale.GERMANY).withZone(TripPeriod.ZONE);
  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY).withZone(TripPeriod.ZONE);
  private static final DateTimeFormatter TIME =
      DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY).withZone(TripPeriod.ZONE);

  public String dateTime(Instant instant) {
    return DATE_TIME.format(instant);
  }

  public String date(Instant instant) {
    return DATE.format(instant);
  }

  public String time(Instant instant) {
    return TIME.format(instant);
  }
}
