package de.lgln.dienstreise.car;

import de.lgln.dienstreise.config.DienstreiseProperties;
import de.lgln.dienstreise.config.DienstreiseProperties.CarProperties;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Reconciles the car table with the configuration: index i becomes id i+1. Never deletes. */
@Component
public class CarCatalogInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(CarCatalogInitializer.class);

  private final DienstreiseProperties properties;
  private final CarRepository cars;

  public CarCatalogInitializer(DienstreiseProperties properties, CarRepository cars) {
    this.properties = properties;
    this.cars = cars;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    List<Short> ids = new ArrayList<>();
    List<CarProperties> configured = properties.cars();
    // Plates are unique: temporarily free them so swapping plates between slots works.
    cars.findAll().forEach(car -> car.rename(car.getDisplayName(), "#" + car.getId()));
    cars.flush();
    for (int i = 0; i < configured.size(); i++) {
      CarProperties c = configured.get(i);
      short id = (short) (i + 1);
      String name = c.name().strip();
      String plate = c.licensePlate().strip();
      cars.findById(id)
          .ifPresentOrElse(
              car -> car.rename(name, plate), () -> cars.save(new Car(id, name, plate)));
      ids.add(id);
    }
    cars.flush();
    log.atInfo().addKeyValue("carIds", ids).log("car catalog reconciled");
  }
}
