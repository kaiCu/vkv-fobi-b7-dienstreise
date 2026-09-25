package de.lgln.dienstreise.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A test clock whose instant can be set and advanced. Zone is always UTC. */
public class MutableClock extends Clock {

  private volatile Instant instant;

  public MutableClock(Instant instant) {
    this.instant = instant;
  }

  public void setInstant(Instant instant) {
    this.instant = instant;
  }

  public void advance(Duration duration) {
    this.instant = instant.plus(duration);
  }

  @Override
  public ZoneId getZone() {
    return ZoneOffset.UTC;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    throw new UnsupportedOperationException("MutableClock is always UTC");
  }

  @Override
  public Instant instant() {
    return instant;
  }
}
