package de.lgln.dienstreise.integration;

import static org.assertj.core.api.Assertions.assertThat;

import de.lgln.dienstreise.car.CarCatalogInitializer;
import de.lgln.dienstreise.car.CarRepository;
import de.lgln.dienstreise.config.DienstreiseProperties;
import de.lgln.dienstreise.config.DienstreiseProperties.CarProperties;
import de.lgln.dienstreise.support.AbstractIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

class CarCatalogInitializerIT extends AbstractIntegrationTest {

  @Autowired DienstreiseProperties props;
  @Autowired CarRepository carRepository;
  @Autowired TransactionTemplate tx;

  @Test
  void carsAreReconciledFromConfiguration() {
    assertThat(cars())
        .containsExactly(
            List.of(1, "Dienstwagen 1", "H-LG 101"),
            List.of(2, "Dienstwagen 2", "H-LG 102"),
            List.of(3, "Dienstwagen 3", "H-LG 103"));
  }

  @Test
  void changedNameUpdatesRowWithoutChangingId() {
    List<CarProperties> changed = new ArrayList<>(props.cars());
    changed.set(0, new CarProperties("Dienstwagen 1 neu", "H-LG 101"));
    DienstreiseProperties changedProps = new DienstreiseProperties(changed, props.retention());
    CarCatalogInitializer initializer = new CarCatalogInitializer(changedProps, carRepository);
    try {
      tx.executeWithoutResult(s -> initializer.run(new DefaultApplicationArguments()));
      assertThat(cars().getFirst()).isEqualTo(List.of(1, "Dienstwagen 1 neu", "H-LG 101"));
    } finally {
      tx.executeWithoutResult(
          s ->
              new CarCatalogInitializer(props, carRepository)
                  .run(new DefaultApplicationArguments()));
    }
  }

  private List<List<Object>> cars() {
    return cars(jdbc);
  }

  private static List<List<Object>> cars(JdbcTemplate jdbc) {
    return jdbc.query(
        "SELECT id, display_name, license_plate FROM car ORDER BY id",
        (rs, i) -> List.of(rs.getInt(1), rs.getString(2), rs.getString(3)));
  }

  @Nested
  @ContextConfiguration(initializers = EnvironmentVariables.class)
  class RealEnvironmentVariables {

    /** Injected from this nested class's own context (it has its own database). */
    @Autowired JdbcTemplate nestedJdbc;

    @Test
    void environmentVariableForLicensePlateBinds() {
      assertThat(cars(nestedJdbc).get(2)).isEqualTo(List.of(3, "Dienstwagen 3", "H-LG 999"));
    }
  }

  static class EnvironmentVariables
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
      ctx.getEnvironment()
          .getPropertySources()
          .addFirst(
              new SystemEnvironmentPropertySource(
                  StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME + "-test",
                  Map.of("DIENSTREISE_CARS_2_LICENSE_PLATE", "H-LG 999")));
    }
  }
}
