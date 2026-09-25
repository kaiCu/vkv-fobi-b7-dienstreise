package de.lgln.dienstreise.unit;

import static org.assertj.core.api.Assertions.assertThat;

import de.lgln.dienstreise.config.DienstreiseProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class DienstreisePropertiesTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(Config.class);

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(DienstreiseProperties.class)
  static class Config {}

  private static String[] validCars() {
    return new String[] {
      "dienstreise.cars[0].name=Wagen 1", "dienstreise.cars[0].license-plate=H-A 1",
      "dienstreise.cars[1].name=Wagen 2", "dienstreise.cars[1].license-plate=H-A 2",
      "dienstreise.cars[2].name=Wagen 3", "dienstreise.cars[2].license-plate=H-A 3",
      "dienstreise.retention.period=P90D"
    };
  }

  @Test
  void validConfigurationStarts() {
    runner
        .withPropertyValues(validCars())
        .run(
            ctx -> {
              assertThat(ctx).hasNotFailed();
              DienstreiseProperties props = ctx.getBean(DienstreiseProperties.class);
              assertThat(props.cars()).hasSize(3);
              assertThat(props.cars().get(1).licensePlate()).isEqualTo("H-A 2");
              assertThat(props.retention().period()).isEqualTo(Duration.ofDays(90));
              assertThat(props.retention().cron()).isEqualTo("0 0 * * * *");
            });
  }

  @Test
  void fewerThanThreeCarsFail() {
    runner
        .withPropertyValues(
            "dienstreise.cars[0].name=Wagen 1",
            "dienstreise.cars[0].license-plate=H-A 1",
            "dienstreise.cars[1].name=Wagen 2",
            "dienstreise.cars[1].license-plate=H-A 2")
        .run(ctx -> assertThat(ctx).hasFailed());
  }

  @Test
  void moreThanThreeCarsFail() {
    runner
        .withPropertyValues(validCars())
        .withPropertyValues(
            "dienstreise.cars[3].name=Wagen 4", "dienstreise.cars[3].license-plate=H-A 4")
        .run(ctx -> assertThat(ctx).hasFailed());
  }

  @Test
  void blankNameFails() {
    runner
        .withPropertyValues(validCars())
        .withPropertyValues("dienstreise.cars[0].name= ")
        .run(ctx -> assertThat(ctx).hasFailed());
  }

  @Test
  void tooLongNameFails() {
    runner
        .withPropertyValues(validCars())
        .withPropertyValues("dienstreise.cars[0].name=" + "x".repeat(81))
        .run(ctx -> assertThat(ctx).hasFailed());
  }

  @Test
  void blankPlateFails() {
    runner
        .withPropertyValues(validCars())
        .withPropertyValues("dienstreise.cars[1].license-plate= ")
        .run(ctx -> assertThat(ctx).hasFailed());
  }

  @Test
  void tooLongPlateFails() {
    runner
        .withPropertyValues(validCars())
        .withPropertyValues("dienstreise.cars[1].license-plate=" + "X".repeat(16))
        .run(ctx -> assertThat(ctx).hasFailed());
  }

  @Test
  void duplicatePlateFails() {
    runner
        .withPropertyValues(validCars())
        .withPropertyValues("dienstreise.cars[2].license-plate=H-A 1")
        .run(ctx -> assertThat(ctx).hasFailed());
  }

  @Test
  void zeroRetentionFails() {
    runner
        .withPropertyValues(validCars())
        .withPropertyValues("dienstreise.retention.period=PT0S")
        .run(ctx -> assertThat(ctx).hasFailed());
  }

  @Test
  void negativeRetentionFails() {
    runner
        .withPropertyValues(validCars())
        .withPropertyValues("dienstreise.retention.period=-P1D")
        .run(ctx -> assertThat(ctx).hasFailed());
  }
}
