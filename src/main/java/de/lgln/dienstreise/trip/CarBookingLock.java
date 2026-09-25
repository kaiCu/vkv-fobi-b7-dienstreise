package de.lgln.dienstreise.trip;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serializes bookings of one car across all pods with a transaction-scoped PostgreSQL advisory
 * lock. Without it, concurrent inserts checked against the exclusion constraint can deadlock
 * (SQLSTATE 40P01) instead of failing cleanly with 23P01. The constraint stays the final guard.
 */
@Component
class CarBookingLock {

  /** First key of the two-part advisory lock, so locks of other applications cannot collide. */
  static final int NAMESPACE = 0x44525345; // "DRSE"

  private final JdbcTemplate jdbc;

  CarBookingLock(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** Blocks until no other transaction is booking this car; released at commit or rollback. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void lock(Short carId) {
    jdbc.query("SELECT pg_advisory_xact_lock(?, ?)", rs -> null, NAMESPACE, (int) carId);
  }
}
