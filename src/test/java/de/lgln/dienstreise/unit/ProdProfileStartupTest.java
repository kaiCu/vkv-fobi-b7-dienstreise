package de.lgln.dienstreise.unit;

import static org.assertj.core.api.Assertions.assertThat;

import de.lgln.dienstreise.config.DienstreiseProperties;
import de.lgln.dienstreise.config.ProdStartupGuard;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** FR-024: in production the application refuses to start without a retention period. */
class ProdProfileStartupTest {

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(DienstreiseProperties.class)
  @Import(ProdStartupGuard.class)
  static class Config {}

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withInitializer(ctx -> ctx.getEnvironment().setActiveProfiles("prod"))
          .withUserConfiguration(Config.class)
          .withPropertyValues(
              "dienstreise.cars[0].name=Wagen 1", "dienstreise.cars[0].license-plate=H-A 1",
              "dienstreise.cars[1].name=Wagen 2", "dienstreise.cars[1].license-plate=H-A 2",
              "dienstreise.cars[2].name=Wagen 3", "dienstreise.cars[2].license-plate=H-A 3");

  @Test
  void missingRetentionPeriodRefusesStartup() {
    runner.run(
        ctx -> {
          assertThat(ctx).hasFailed();
          assertThat(ctx.getStartupFailure())
              .rootCause()
              .hasMessageContaining("DIENSTREISE_RETENTION_PERIOD");
        });
  }

  @Test
  void configuredRetentionPeriodStarts() {
    runner
        .withPropertyValues("dienstreise.retention.period=P30D")
        .run(ctx -> assertThat(ctx).hasNotFailed().hasSingleBean(ProdStartupGuard.class));
  }
}
