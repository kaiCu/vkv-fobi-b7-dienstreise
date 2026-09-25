package de.lgln.dienstreise.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ClockConfig {

  /** The only source of "now" in the application (FR-006a, FR-023: server time). */
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
