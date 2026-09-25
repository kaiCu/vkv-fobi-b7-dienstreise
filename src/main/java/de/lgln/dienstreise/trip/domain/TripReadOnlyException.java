package de.lgln.dienstreise.trip.domain;

/** The trip has started and can no longer be changed or deleted (FR-023). */
public class TripReadOnlyException extends RuntimeException {

  public TripReadOnlyException() {
    super("trip has started and is read-only");
  }
}
