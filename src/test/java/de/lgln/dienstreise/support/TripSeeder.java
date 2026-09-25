package de.lgln.dienstreise.support;

import de.lgln.dienstreise.trip.domain.TripPeriod;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Inserts trips directly, bypassing all application rules. Times are local Europe/Berlin. */
public final class TripSeeder {

  private TripSeeder() {}

  public static Instant berlin(String localDateTime) {
    return LocalDateTime.parse(localDateTime).atZone(TripPeriod.ZONE).toInstant();
  }

  public static UUID insert(
      JdbcTemplate jdbc, String owner, int carId, String start, String end, String destination) {
    return insert(jdbc, owner, carId, start, end, destination, TestClockConfig.DEFAULT_NOW);
  }

  public static UUID insert(
      JdbcTemplate jdbc,
      String owner,
      int carId,
      String start,
      String end,
      String destination,
      Instant createdAt) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO business_trip (id, owner_subject, car_id, start_at, end_at, destination,"
            + " created_at, updated_at, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)",
        id,
        owner,
        carId,
        Timestamp.from(berlin(start)),
        Timestamp.from(berlin(end)),
        destination,
        Timestamp.from(createdAt),
        Timestamp.from(createdAt));
    return id;
  }

  public static int count(JdbcTemplate jdbc) {
    return jdbc.queryForObject("SELECT count(*) FROM business_trip", Integer.class);
  }
}
