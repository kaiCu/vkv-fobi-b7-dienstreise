package de.lgln.dienstreise.trip.domain;

/** The trip does not exist or belongs to another user; both look the same (FR-013, FR-022). */
public class TripNotFoundException extends RuntimeException {

  public TripNotFoundException() {
    super("trip not found");
  }
}
