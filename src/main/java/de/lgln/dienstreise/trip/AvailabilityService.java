package de.lgln.dienstreise.trip;

import de.lgln.dienstreise.car.CarRepository;
import de.lgln.dienstreise.trip.domain.TripPeriod;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Availability of the three cars for a period (FR-004a), ignoring the trip being edited. */
@Service
@Transactional(readOnly = true)
public class AvailabilityService {

  private final CarRepository cars;
  private final TripRepository trips;

  public AvailabilityService(CarRepository cars, TripRepository trips) {
    this.cars = cars;
    this.trips = trips;
  }

  public List<CarAvailability> availabilityFor(TripPeriod period, UUID excludeTripId) {
    return cars.findAllByOrderByIdAsc().stream()
        .map(
            car ->
                new CarAvailability(
                    car.getId(), car.label(), !overlaps(car.getId(), period, excludeTripId)))
        .toList();
  }

  public boolean isAvailable(Short carId, TripPeriod period, UUID excludeTripId) {
    return !overlaps(carId, period, excludeTripId);
  }

  public Optional<String> carLabel(Short carId) {
    return carId == null ? Optional.empty() : cars.findById(carId).map(car -> car.label());
  }

  public static boolean noneAvailable(List<CarAvailability> availability) {
    return availability.stream().noneMatch(CarAvailability::available);
  }

  private boolean overlaps(Short carId, TripPeriod period, UUID excludeTripId) {
    return excludeTripId == null
        ? trips.existsOverlap(carId, period.start(), period.end())
        : trips.existsOverlapExcluding(carId, period.start(), period.end(), excludeTripId);
  }
}
