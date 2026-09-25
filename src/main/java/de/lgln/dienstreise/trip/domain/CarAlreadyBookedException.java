package de.lgln.dienstreise.trip.domain;

/** The car is booked in the period. Carries no data of the other trip (FR-019). */
public class CarAlreadyBookedException extends RuntimeException {

  private final Short carId;

  public CarAlreadyBookedException(Short carId) {
    super("car already booked: " + carId);
    this.carId = carId;
  }

  public Short carId() {
    return carId;
  }
}
