package de.lgln.dienstreise.trip.web;

import de.lgln.dienstreise.trip.domain.TripPeriod;
import java.time.Clock;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/** Server-side rules V1-V8 per wizard step (FR-008). Error codes are message keys. */
@Component
public class TripFormValidator implements Validator {

  static final int DESTINATION_MAX = 200;
  private static final Pattern CONTROL_CHARS = Pattern.compile("\\p{Cc}");

  private final Clock clock;

  public TripFormValidator(Clock clock) {
    this.clock = clock;
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return TripForm.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    TripForm form = (TripForm) target;
    validatePeriod(form, errors);
    validateCar(form, errors);
    validateDestination(form, errors);
  }

  /** V1-V4. Binding errors (unparsable values, V2) are kept and not reported twice. */
  public void validatePeriod(TripForm form, Errors errors) {
    required(errors, "startDate", form.getStartDate(), "trip.start.required");
    required(errors, "startTime", form.getStartTime(), "trip.startTime.required");
    required(errors, "endDate", form.getEndDate(), "trip.end.required");
    required(errors, "endTime", form.getEndTime(), "trip.endTime.required");
    TripPeriod period = form.toPeriod();
    if (period == null) {
      return;
    }
    if (!period.isEndAfterStart()) {
      errors.rejectValue("endTime", "trip.end.notAfterStart");
    }
    if (!period.start().isAfter(clock.instant())) {
      errors.rejectValue("startDate", "trip.start.notInFuture");
    }
  }

  /** V5: a car id of 1-3. Availability (V6) is checked by the caller. */
  public void validateCar(TripForm form, Errors errors) {
    if (errors.hasFieldErrors("carId")) {
      return;
    }
    Short carId = form.getCarId();
    if (carId == null || carId < 1 || carId > 3) {
      errors.rejectValue("carId", "trip.car.required");
    }
  }

  /** V7, V8. Normalizes the destination by stripping surrounding whitespace. */
  public void validateDestination(TripForm form, Errors errors) {
    String destination = form.getDestination() == null ? "" : form.getDestination().strip();
    form.setDestination(destination);
    if (destination.isEmpty()) {
      errors.rejectValue("destination", "trip.destination.required");
    } else if (destination.length() > DESTINATION_MAX) {
      errors.rejectValue("destination", "trip.destination.tooLong");
    } else if (CONTROL_CHARS.matcher(destination).find()) {
      errors.rejectValue("destination", "trip.destination.invalidChars");
    }
  }

  private static void required(Errors errors, String field, Object value, String code) {
    if (value == null && !errors.hasFieldErrors(field)) {
      errors.rejectValue(field, code);
    }
  }
}
