package de.lgln.dienstreise.trip.domain;

import java.util.List;

/** A rule failed when saving (e.g. V4: the start has passed in the meantime). */
public class TripValidationException extends RuntimeException {

  public record Violation(String field, String code) {}

  private final List<Violation> violations;

  public TripValidationException(List<Violation> violations) {
    super("trip validation failed: " + violations);
    this.violations = List.copyOf(violations);
  }

  public TripValidationException(String field, String code) {
    this(List.of(new Violation(field, code)));
  }

  public List<Violation> violations() {
    return violations;
  }

  public List<String> codes() {
    return violations.stream().map(Violation::code).toList();
  }
}
