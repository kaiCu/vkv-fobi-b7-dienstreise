package de.lgln.dienstreise.trip;

import de.lgln.dienstreise.trip.domain.CarAlreadyBookedException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;

/** Translates a violation of the overlap exclusion constraint into the domain error (FR-018). */
final class BookingConflictTranslator {

  static final String EXCLUSION_VIOLATION = "23P01";
  static final String OVERLAP_CONSTRAINT = "business_trip_no_overlap";

  private BookingConflictTranslator() {}

  static RuntimeException translate(DataIntegrityViolationException e, Short carId) {
    for (Throwable cause = e; cause != null; cause = cause.getCause()) {
      if (cause instanceof PSQLException psql && EXCLUSION_VIOLATION.equals(psql.getSQLState())) {
        ServerErrorMessage message = psql.getServerErrorMessage();
        if (message != null && OVERLAP_CONSTRAINT.equals(message.getConstraint())) {
          return new CarAlreadyBookedException(carId);
        }
      }
    }
    return e;
  }
}
