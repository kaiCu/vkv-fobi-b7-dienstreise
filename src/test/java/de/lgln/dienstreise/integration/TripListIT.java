package de.lgln.dienstreise.integration;

import static de.lgln.dienstreise.support.TripSeeder.berlin;
import static de.lgln.dienstreise.support.WizardSupport.html;
import static de.lgln.dienstreise.support.WizardSupport.postForm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.lgln.dienstreise.support.AbstractIntegrationTest;
import de.lgln.dienstreise.support.TestClockConfig;
import de.lgln.dienstreise.support.TripSeeder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;

class TripListIT extends AbstractIntegrationTest {

  private Document list(OidcLoginRequestPostProcessor user) throws Exception {
    return html(mockMvc.perform(get("/trips").with(user)).andExpect(status().isOk()).andReturn());
  }

  private static List<String> column(Document doc, int index) {
    return doc.select("tbody tr").stream().map(tr -> tr.child(index).text()).toList();
  }

  private void seedAnnaAndBen() {
    TripSeeder.insert(jdbc, ANNA_SUBJECT, 1, "2026-10-05T09:00", "2026-10-05T12:00", "Emden");
    TripSeeder.insert(jdbc, ANNA_SUBJECT, 2, "2026-10-03T09:00", "2026-10-03T12:00", "Celle");
    TripSeeder.insert(jdbc, ANNA_SUBJECT, 1, "2026-10-04T09:00", "2026-10-04T12:00", "Hannover");
    TripSeeder.insert(
        jdbc,
        ANNA_SUBJECT,
        3,
        "2026-10-06T09:00",
        "2026-10-06T10:00",
        "Später erfasst",
        TestClockConfig.DEFAULT_NOW.plusSeconds(60));
    TripSeeder.insert(
        jdbc,
        ANNA_SUBJECT,
        2,
        "2026-10-06T09:00",
        "2026-10-06T10:00",
        "Früher erfasst",
        TestClockConfig.DEFAULT_NOW);
    TripSeeder.insert(jdbc, BEN_SUBJECT, 3, "2026-10-03T09:00", "2026-10-03T12:00", "Bens Ziel");
  }

  @Test
  void showsOwnTripsSortedByStartWithAllColumns() throws Exception {
    seedAnnaAndBen();

    Document doc = list(anna());

    assertThat(doc.selectFirst("table caption").text()).isEqualTo("Meine Dienstreisen");
    assertThat(doc.select("thead th[scope=col]").eachText())
        .containsExactly("Fahrzeug", "Beginn", "Ziel", "Ende", "Status", "Aktionen");
    assertThat(column(doc, 2))
        .containsExactly("Celle", "Hannover", "Emden", "Früher erfasst", "Später erfasst");
    Element first = doc.selectFirst("tbody tr");
    assertThat(first.child(0).text()).isEqualTo("Dienstwagen 2 (H-LG 102)");
    assertThat(first.child(1).tagName()).isEqualTo("th");
    assertThat(first.child(1).attr("scope")).isEqualTo("row");
    assertThat(first.child(1).text()).isEqualTo("03.10.2026, 09:00");
    assertThat(first.child(3).text()).isEqualTo("03.10.2026, 12:00");
    assertThat(doc.text()).doesNotContain("Bens Ziel");
  }

  @Test
  void statusIsDerivedFromServerClock() throws Exception {
    seedAnnaAndBen();
    clock.setInstant(berlin("2026-10-04T09:30"));

    Document doc = list(anna());

    assertThat(column(doc, 4).subList(0, 3)).containsExactly("Abgeschlossen", "Läuft", "Geplant");
  }

  @Test
  void emptyStateForNewUser() throws Exception {
    Document doc = list(user("new-sub", "Neu"));

    assertThat(doc.selectFirst("table")).isNull();
    assertThat(doc.text()).contains("Sie haben noch keine Dienstreisen erfasst.");
    Elements link = doc.select("main a[href=/trips/new]");
    assertThat(link.eachText()).contains("Neue Dienstreise erfassen");
  }

  @Test
  void newTripAppearsAtItsPosition() throws Exception {
    seedAnnaAndBen();
    mockMvc
        .perform(
            postForm(
                "/trips",
                anna(),
                Map.of(
                    "startDate", "2026-10-04",
                    "startTime", "12:00",
                    "endDate", "2026-10-04",
                    "endTime", "13:00",
                    "carId", "1",
                    "destination", "Neu")))
        .andExpect(status().isFound());

    assertThat(column(list(anna()), 2).subList(0, 4))
        .containsExactly("Celle", "Hannover", "Neu", "Emden");
  }

  @Test
  void onlyUpcomingTripsHaveActionsWithAccessibleNames() throws Exception {
    UUID past =
        TripSeeder.insert(jdbc, ANNA_SUBJECT, 1, "2026-09-30T09:00", "2026-09-30T12:00", "Emden");
    UUID upcoming =
        TripSeeder.insert(
            jdbc, ANNA_SUBJECT, 1, "2026-10-02T09:00", "2026-10-02T12:00", "Hannover");

    Document doc = list(anna());

    Element edit = doc.selectFirst("a[href=/trips/" + upcoming + "/edit]");
    Element delete = doc.selectFirst("a[href=/trips/" + upcoming + "/delete]");
    assertThat(edit.text())
        .contains("Bearbeiten")
        .contains("Dienstreise nach Hannover am 02.10.2026");
    assertThat(edit.selectFirst(".visually-hidden")).isNotNull();
    assertThat(delete.text())
        .contains("Löschen")
        .contains("Dienstreise nach Hannover am 02.10.2026");
    assertThat(doc.select("a[href^=/trips/" + past + "]")).isEmpty();
  }
}
