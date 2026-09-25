package de.lgln.dienstreise.trip.domain;

/** The trip was changed after the edit form was loaded (optimistic locking, V11). */
public class TripStaleException extends RuntimeException {

  public TripStaleException() {
    super("trip was changed in the meantime");
  }
}
