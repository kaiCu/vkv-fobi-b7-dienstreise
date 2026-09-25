package de.lgln.dienstreise.unit;

import static org.assertj.core.api.Assertions.assertThat;

import de.lgln.dienstreise.trip.web.TripForm;
import de.lgln.dienstreise.trip.web.TripFormValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

class TripFormValidatorTest {

  private final TripFormValidator validator =
      new TripFormValidator(Clock.fixed(Instant.parse("2026-10-01T06:00:00Z"), ZoneOffset.UTC));

  private TripForm form;
  private Errors errors;

  @BeforeEach
  void validForm() {
    form = new TripForm();
    form.setStartDate(LocalDate.parse("2026-10-02"));
    form.setStartTime(LocalTime.parse("09:00"));
    form.setEndDate(LocalDate.parse("2026-10-02"));
    form.setEndTime(LocalTime.parse("12:00"));
    form.setCarId((short) 1);
    form.setDestination("Hannover");
    errors = new BeanPropertyBindingResult(form, "tripForm");
  }

  private String code(String field) {
    return errors.getFieldError(field) == null ? null : errors.getFieldError(field).getCode();
  }

  @Test
  void validPeriodHasNoErrors() {
    validator.validatePeriod(form, errors);
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void missingFieldsAreReportedPerField() {
    form.setStartDate(null);
    form.setStartTime(null);
    form.setEndDate(null);
    form.setEndTime(null);
    validator.validatePeriod(form, errors);
    assertThat(code("startDate")).isEqualTo("trip.start.required");
    assertThat(code("startTime")).isEqualTo("trip.startTime.required");
    assertThat(code("endDate")).isEqualTo("trip.end.required");
    assertThat(code("endTime")).isEqualTo("trip.endTime.required");
  }

  @Test
  void endEqualToStartIsRejected() {
    form.setEndTime(LocalTime.parse("09:00"));
    validator.validatePeriod(form, errors);
    assertThat(code("endTime")).isEqualTo("trip.end.notAfterStart");
  }

  @Test
  void endBeforeStartIsRejected() {
    form.setEndDate(LocalDate.parse("2026-10-01"));
    validator.validatePeriod(form, errors);
    assertThat(code("endTime")).isEqualTo("trip.end.notAfterStart");
  }

  @Test
  void startEqualToNowIsRejected() {
    form.setStartDate(LocalDate.parse("2026-10-01"));
    form.setStartTime(LocalTime.parse("08:00"));
    validator.validatePeriod(form, errors);
    assertThat(code("startDate")).isEqualTo("trip.start.notInFuture");
  }

  @Test
  void startInThePastIsRejected() {
    form.setStartDate(LocalDate.parse("2026-09-30"));
    validator.validatePeriod(form, errors);
    assertThat(code("startDate")).isEqualTo("trip.start.notInFuture");
  }

  @Test
  void startOneMinuteAfterNowIsValid() {
    form.setStartDate(LocalDate.parse("2026-10-01"));
    form.setStartTime(LocalTime.parse("08:01"));
    validator.validatePeriod(form, errors);
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void missingCarIsRejected() {
    form.setCarId(null);
    validator.validateCar(form, errors);
    assertThat(code("carId")).isEqualTo("trip.car.required");
  }

  @Test
  void unknownCarIsRejected() {
    form.setCarId((short) 4);
    validator.validateCar(form, errors);
    assertThat(code("carId")).isEqualTo("trip.car.required");
  }

  @Test
  void blankDestinationIsRejected() {
    form.setDestination("   ");
    validator.validateDestination(form, errors);
    assertThat(code("destination")).isEqualTo("trip.destination.required");
  }

  @Test
  void tooLongDestinationIsRejected() {
    form.setDestination("x".repeat(201));
    validator.validateDestination(form, errors);
    assertThat(code("destination")).isEqualTo("trip.destination.tooLong");
  }

  @Test
  void destinationOfExactly200CharactersIsValid() {
    form.setDestination("x".repeat(200));
    validator.validateDestination(form, errors);
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void controlCharactersAreRejected() {
    form.setDestination("Hann\u0007over");
    validator.validateDestination(form, errors);
    assertThat(code("destination")).isEqualTo("trip.destination.invalidChars");
  }

  @Test
  void destinationIsTrimmed() {
    form.setDestination("  Hannover  ");
    validator.validateDestination(form, errors);
    assertThat(errors.hasErrors()).isFalse();
    assertThat(form.getDestination()).isEqualTo("Hannover");
  }
}
