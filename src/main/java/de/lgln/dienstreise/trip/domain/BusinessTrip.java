package de.lgln.dienstreise.trip.domain;

import de.lgln.dienstreise.car.Car;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** A business trip recorded by one user for one car. */
@Entity
@Table(name = "business_trip")
public class BusinessTrip {

  @Id private UUID id;

  /** {@code iss|sub} of the OIDC identity; no other personal data (data minimization). */
  @Column(name = "owner_subject", nullable = false, updatable = false)
  private String ownerSubject;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "car_id", nullable = false)
  private Car car;

  @Column(name = "start_at", nullable = false)
  private Instant startAt;

  @Column(name = "end_at", nullable = false)
  private Instant endAt;

  @Column(nullable = false, length = 200)
  private String destination;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private int version;

  protected BusinessTrip() {}

  public BusinessTrip(
      UUID id, String ownerSubject, Car car, TripPeriod period, String destination, Instant now) {
    this.id = id;
    this.ownerSubject = ownerSubject;
    this.car = car;
    this.startAt = period.start();
    this.endAt = period.end();
    this.destination = destination;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void change(Car car, TripPeriod period, String destination, Instant now) {
    this.car = car;
    this.startAt = period.start();
    this.endAt = period.end();
    this.destination = destination;
    this.updatedAt = now;
  }

  public TripPeriod period() {
    return new TripPeriod(startAt, endAt);
  }

  public TripStatus status(Instant now) {
    return TripStatus.of(startAt, endAt, now);
  }

  public UUID getId() {
    return id;
  }

  public String getOwnerSubject() {
    return ownerSubject;
  }

  public Car getCar() {
    return car;
  }

  public Instant getStartAt() {
    return startAt;
  }

  public Instant getEndAt() {
    return endAt;
  }

  public String getDestination() {
    return destination;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public int getVersion() {
    return version;
  }
}
