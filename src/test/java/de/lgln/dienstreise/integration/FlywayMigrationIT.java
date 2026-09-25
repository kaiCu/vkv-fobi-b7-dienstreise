package de.lgln.dienstreise.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.lgln.dienstreise.support.AbstractIntegrationTest;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

class FlywayMigrationIT extends AbstractIntegrationTest {

  private static final String DAY = "2026-10-02T";

  @Test
  void tablesExist() {
    for (String table : new String[] {"car", "business_trip", "spring_session"}) {
      Integer count =
          jdbc.queryForObject(
              "SELECT count(*) FROM information_schema.tables WHERE table_name = ?",
              Integer.class,
              table);
      assertThat(count).as(table).isEqualTo(1);
    }
  }

  @Test
  void endMustBeAfterStart() {
    assertSqlState(() -> insertTrip(1, "09:00", "09:00", "Hannover"), "23514");
  }

  @Test
  void destinationMustNotBeBlank() {
    assertSqlState(() -> insertTrip(1, "09:00", "10:00", "   "), "23514");
  }

  @Test
  void onlyThreeCarSlots() {
    assertSqlState(
        () ->
            jdbc.update(
                "INSERT INTO car (id, display_name, license_plate) VALUES (4, 'X', 'H-X 4')"),
        "23514");
  }

  @Test
  void overlappingTripsForSameCarAreRejected() {
    insertTrip(1, "09:00", "12:00", "Hannover");
    assertSqlState(() -> insertTrip(1, "11:00", "13:00", "Celle"), "23P01");
  }

  @Test
  void backToBackTripsAreAllowed() {
    insertTrip(1, "09:00", "12:00", "Hannover");
    insertTrip(1, "12:00", "14:00", "Celle");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM business_trip", Integer.class))
        .isEqualTo(2);
  }

  @Test
  void overlappingTripsOnDifferentCarsAreAllowed() {
    insertTrip(1, "09:00", "12:00", "Hannover");
    insertTrip(2, "10:00", "11:00", "Celle");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM business_trip", Integer.class))
        .isEqualTo(2);
  }

  private void insertTrip(int carId, String start, String end, String destination) {
    jdbc.update(
        "INSERT INTO business_trip (id, owner_subject, car_id, start_at, end_at, destination,"
            + " updated_at) VALUES (?, ?, ?, ?, ?, ?, now())",
        UUID.randomUUID(),
        ANNA_SUBJECT,
        carId,
        Timestamp.from(Instant.parse(DAY + start + ":00Z")),
        Timestamp.from(Instant.parse(DAY + end + ":00Z")),
        destination);
  }

  private static void assertSqlState(Runnable statement, String sqlState) {
    assertThatThrownBy(statement::run)
        .isInstanceOf(DataAccessException.class)
        .satisfies(
            e -> {
              Throwable cause = e;
              while (cause != null && !(cause instanceof SQLException)) {
                cause = cause.getCause();
              }
              assertThat(cause).isNotNull();
              assertThat(((SQLException) cause).getSQLState()).isEqualTo(sqlState);
            });
  }
}
