package de.lgln.dienstreise.support;

import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class TestClockConfig {

  /** 08:00 Europe/Berlin. */
  public static final Instant DEFAULT_NOW = Instant.parse("2026-10-01T06:00:00Z");

  @Bean
  @Primary
  MutableClock testClock() {
    return new MutableClock(DEFAULT_NOW);
  }
}
