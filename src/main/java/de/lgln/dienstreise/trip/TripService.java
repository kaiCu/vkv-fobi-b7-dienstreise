package de.lgln.dienstreise.trip;

import de.lgln.dienstreise.car.Car;
import de.lgln.dienstreise.car.CarRepository;
import de.lgln.dienstreise.trip.domain.BusinessTrip;
import de.lgln.dienstreise.trip.domain.CarAlreadyBookedException;
import de.lgln.dienstreise.trip.domain.TripNotFoundException;
import de.lgln.dienstreise.trip.domain.TripPeriod;
import de.lgln.dienstreise.trip.domain.TripReadOnlyException;
import de.lgln.dienstreise.trip.domain.TripStaleException;
import de.lgln.dienstreise.trip.domain.TripStatus;
import de.lgln.dienstreise.trip.domain.TripValidationException;
import de.lgln.dienstreise.trip.web.TripForm;
import de.lgln.dienstreise.trip.web.TripListItem;
import de.lgln.dienstreise.web.CurrentUser;
import de.lgln.dienstreise.web.GermanDateTimeFormat;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for business trips. Logs ids only, never destinations or owners. */
@Service
public class TripService {

  private static final Logger log = LoggerFactory.getLogger(TripService.class);

  private final TripRepository trips;
  private final CarRepository cars;
  private final AvailabilityService availability;
  private final Clock clock;
  private final MeterRegistry meters;
  private final CarBookingLock bookingLock;
  private final GermanDateTimeFormat fmt;

  public TripService(
      TripRepository trips,
      CarRepository cars,
      AvailabilityService availability,
      Clock clock,
      MeterRegistry meters,
      CarBookingLock bookingLock,
      GermanDateTimeFormat fmt) {
    this.trips = trips;
    this.cars = cars;
    this.availability = availability;
    this.clock = clock;
    this.meters = meters;
    this.bookingLock = bookingLock;
    this.fmt = fmt;
  }

  /** The user's own trips, sorted by start, then time of recording (FR-011, FR-013). */
  @Transactional(readOnly = true)
  public List<TripListItem> listFor(CurrentUser user) {
    Instant now = clock.instant();
    return trips.findByOwnerSubjectOrderByStartAtAscCreatedAtAscIdAsc(user.subject()).stream()
        .map(trip -> TripListItem.from(trip, fmt, now))
        .toList();
  }

  /** Re-validates V4 and V6 at save time; the database constraint is the final guard. */
  @Transactional
  public UUID create(CurrentUser user, TripCommand cmd) {
    Instant now = clock.instant();
    requireFutureStart(cmd, now);
    Car car = requireAvailableCar(cmd, null);
    BusinessTrip trip =
        new BusinessTrip(
            UUID.randomUUID(), user.subject(), car, cmd.period(), cmd.destination().strip(), now);
    saveAndFlush(trip);
    log.atInfo()
        .addKeyValue("event", "trip.created")
        .addKeyValue("tripId", trip.getId())
        .addKeyValue("carId", car.getId())
        .log("trip created");
    return trip.getId();
  }

  /** V9, V10 and conversion to Berlin local date and time for the edit wizard. */
  @Transactional(readOnly = true)
  public TripForm loadForEdit(CurrentUser user, UUID id) {
    BusinessTrip trip = editable(user, id);
    ZonedDateTime start = trip.getStartAt().atZone(TripPeriod.ZONE);
    ZonedDateTime end = trip.getEndAt().atZone(TripPeriod.ZONE);
    TripForm form = new TripForm();
    form.setStartDate(start.toLocalDate());
    form.setStartTime(start.toLocalTime());
    form.setEndDate(end.toLocalDate());
    form.setEndTime(end.toLocalTime());
    form.setCarId(trip.getCar().getId());
    form.setDestination(trip.getDestination());
    form.setVersion(trip.getVersion());
    return form;
  }

  /** V9, V10 for the intermediate wizard steps. */
  @Transactional(readOnly = true)
  public void checkEditable(CurrentUser user, UUID id) {
    editable(user, id);
  }

  /** V9, V10 for the delete confirmation page. */
  @Transactional(readOnly = true)
  public TripListItem loadForDelete(CurrentUser user, UUID id) {
    return TripListItem.from(editable(user, id), fmt, clock.instant());
  }

  /** V9, V10, V11, V4 and V6 (excluding the trip itself), then saves. */
  @Transactional
  public void update(CurrentUser user, UUID id, int version, TripCommand cmd) {
    BusinessTrip trip = editable(user, id);
    if (trip.getVersion() != version) {
      throw new TripStaleException();
    }
    Instant now = clock.instant();
    requireFutureStart(cmd, now);
    Car car = requireAvailableCar(cmd, id);
    trip.change(car, cmd.period(), cmd.destination().strip(), now);
    saveAndFlush(trip);
    log.atInfo()
        .addKeyValue("event", "trip.updated")
        .addKeyValue("tripId", id)
        .addKeyValue("carId", car.getId())
        .log("trip updated");
  }

  /** V9, V10, then deletes permanently; the slot becomes free (FR-021). */
  @Transactional
  public void delete(CurrentUser user, UUID id) {
    BusinessTrip trip = editable(user, id);
    trips.delete(trip);
    trips.flush();
    log.atInfo().addKeyValue("event", "trip.deleted").addKeyValue("tripId", id).log("trip deleted");
  }

  /** Other users' trips behave as not existing (FR-013, FR-022). */
  private BusinessTrip editable(CurrentUser user, UUID id) {
    BusinessTrip trip =
        trips.findByIdAndOwnerSubject(id, user.subject()).orElseThrow(TripNotFoundException::new);
    if (trip.status(clock.instant()) != TripStatus.UPCOMING) {
      throw new TripReadOnlyException();
    }
    return trip;
  }

  /** The exclusion constraint is the final authority under concurrent saves (FR-018). */
  private void saveAndFlush(BusinessTrip trip) {
    try {
      trips.saveAndFlush(trip);
    } catch (OptimisticLockingFailureException e) {
      throw new TripStaleException();
    } catch (DataIntegrityViolationException e) {
      RuntimeException translated = BookingConflictTranslator.translate(e, trip.getCar().getId());
      if (translated instanceof CarAlreadyBookedException) {
        conflict("constraint", trip.getCar().getId());
      }
      throw translated;
    }
  }

  private void conflict(String source, Short carId) {
    meters.counter("dienstreise.booking.conflicts", "source", source).increment();
    log.atInfo()
        .addKeyValue("event", "trip.conflict")
        .addKeyValue("carId", carId)
        .log("booking conflict");
  }

  private static void requireFutureStart(TripCommand cmd, Instant now) {
    if (!cmd.period().start().isAfter(now)) {
      throw new TripValidationException("startDate", "trip.start.notInFuture");
    }
  }

  private Car requireAvailableCar(TripCommand cmd, UUID excludeTripId) {
    Car car =
        cars.findById(cmd.carId())
            .orElseThrow(() -> new TripValidationException("carId", "trip.car.required"));
    bookingLock.lock(car.getId());
    if (!availability.isAvailable(car.getId(), cmd.period(), excludeTripId)) {
      conflict("precheck", car.getId());
      throw new CarAlreadyBookedException(car.getId());
    }
    return car;
  }
}
