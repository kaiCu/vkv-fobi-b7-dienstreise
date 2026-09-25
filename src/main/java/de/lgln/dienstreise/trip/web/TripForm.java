package de.lgln.dienstreise.trip.web;

import de.lgln.dienstreise.trip.domain.TripPeriod;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

/** Values of all wizard steps; carried along in (hidden) form fields and never trusted. */
public class TripForm {

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @DateTimeFormat(pattern = "HH:mm")
  private LocalTime startTime;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate endDate;

  @DateTimeFormat(pattern = "HH:mm")
  private LocalTime endTime;

  private Short carId;
  private String destination;
  private Integer version;
  private String action;

  /** The period, or null if any part is missing. */
  public TripPeriod toPeriod() {
    if (startDate == null || startTime == null || endDate == null || endTime == null) {
      return null;
    }
    return TripPeriod.ofLocal(startDate, startTime, endDate, endTime);
  }

  public boolean isBack() {
    return "back".equals(action);
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalTime endTime) {
    this.endTime = endTime;
  }

  public Short getCarId() {
    return carId;
  }

  public void setCarId(Short carId) {
    this.carId = carId;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }
}
