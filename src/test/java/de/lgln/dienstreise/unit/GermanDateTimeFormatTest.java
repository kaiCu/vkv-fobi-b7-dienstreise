package de.lgln.dienstreise.unit;

import static org.assertj.core.api.Assertions.assertThat;

import de.lgln.dienstreise.web.GermanDateTimeFormat;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GermanDateTimeFormatTest {

  private final GermanDateTimeFormat fmt = new GermanDateTimeFormat();

  @Test
  void summerTime() {
    Instant instant = Instant.parse("2026-09-27T12:30:00Z");
    assertThat(fmt.dateTime(instant)).isEqualTo("27.09.2026, 14:30");
    assertThat(fmt.date(instant)).isEqualTo("27.09.2026");
    assertThat(fmt.time(instant)).isEqualTo("14:30");
  }

  @Test
  void winterTime() {
    assertThat(fmt.dateTime(Instant.parse("2026-12-01T08:05:00Z"))).isEqualTo("01.12.2026, 09:05");
  }
}
