package de.lgln.dienstreise.car;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarRepository extends JpaRepository<Car, Short> {

  List<Car> findAllByOrderByIdAsc();
}
