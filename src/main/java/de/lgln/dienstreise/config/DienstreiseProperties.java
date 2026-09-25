package de.lgln.dienstreise.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Operator configuration (contracts/configuration.md). Startup fails when it is invalid.
 *
 * @param cars exactly the three pool cars; index i is car id i+1
 * @param retention automatic deletion of trips (FR-024)
 */
@ConfigurationProperties("dienstreise")
@Validated
public record DienstreiseProperties(
    @NotNull @Size(min = 3, max = 3) List<@Valid CarProperties> cars,
    @Valid @DefaultValue Retention retention) {

  public record CarProperties(
      @NotBlank @Size(max = 80) String name, @NotBlank @Size(max = 15) String licensePlate) {}

  /**
   * @param period ISO-8601 duration after the end of a trip; required in profile {@code prod}
   * @param cron Spring cron expression for the deletion job, {@code -} disables it
   */
  public record Retention(Duration period, @DefaultValue("0 0 * * * *") String cron) {

    @AssertTrue(message = "dienstreise.retention.period muss positiv sein")
    public boolean isPeriodPositive() {
      return period == null || (!period.isZero() && !period.isNegative());
    }
  }

  @AssertTrue(message = "Die Kennzeichen der Fahrzeuge müssen eindeutig sein")
  public boolean isLicensePlatesUnique() {
    if (cars == null) {
      return true;
    }
    return cars.stream()
            .map(CarProperties::licensePlate)
            .filter(p -> p != null)
            .map(String::strip)
            .distinct()
            .count()
        == cars.size();
  }
}
