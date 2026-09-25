package de.lgln.dienstreise.car;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One of the three pool cars (FR-002, FR-003). */
@Entity
@Table(name = "car")
public class Car {

  @Id private Short id;

  @Column(name = "display_name", nullable = false, length = 80)
  private String displayName;

  @Column(name = "license_plate", nullable = false, length = 15)
  private String licensePlate;

  protected Car() {}

  public Car(Short id, String displayName, String licensePlate) {
    this.id = id;
    this.displayName = displayName;
    this.licensePlate = licensePlate;
  }

  void rename(String displayName, String licensePlate) {
    this.displayName = displayName;
    this.licensePlate = licensePlate;
  }

  public Short getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getLicensePlate() {
    return licensePlate;
  }

  /** Name and plate, e.g. "VW ID.3 (blau) (H-LG 1001)". */
  public String label() {
    return displayName + " (" + licensePlate + ")";
  }
}
