package de.lgln.dienstreise.integration;

import static de.lgln.dienstreise.support.TripSeeder.berlin;
import static de.lgln.dienstreise.support.WizardSupport.html;
import static de.lgln.dienstreise.support.WizardSupport.period;
import static de.lgln.dienstreise.support.WizardSupport.postForm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.lgln.dienstreise.support.AbstractIntegrationTest;
import de.lgln.dienstreise.support.TripSeeder;
import de.lgln.dienstreise.trip.TripService;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class TripEditWizardIT extends AbstractIntegrationTest {

  @MockitoSpyBean TripService tripService;

  UUID id;

  @BeforeEach
  void seed() {
    id =
        TripSeeder.insert(
            jdbc, ANNA_SUBJECT, 1, "2026-10-02T09:00", "2026-10-02T12:00", "Hannover");
  }

  private Map<String, String> changed(String version) {
    return Map.of(
        "startDate", "2026-10-06",
        "startTime", "14:00",
        "endDate", "2026-10-06",
        "endTime", "16:00",
        "carId", "3",
        "destination", "Braunschweig",
        "version", version);
  }

  private Timestamp storedStart() {
    return jdbc.queryForObject(
        "SELECT start_at FROM business_trip WHERE id = ?", Timestamp.class, id);
  }

  @Test
  void as1EditStartsPrefilled() throws Exception {
    Document doc =
        html(
            mockMvc
                .perform(get("/trips/" + id + "/edit").with(anna()))
                .andExpect(status().isOk())
                .andReturn());

    assertThat(doc.getElementById("startDate").val()).isEqualTo("2026-10-02");
    assertThat(doc.getElementById("startTime").val()).isEqualTo("09:00");
    assertThat(doc.getElementById("endTime").val()).isEqualTo("12:00");
    assertThat(doc.selectFirst("main form").attr("action"))
        .isEqualTo("/trips/" + id + "/edit/period");
    assertThat(doc.selectFirst("input[type=hidden][name=carId]").val()).isEqualTo("1");
    assertThat(doc.selectFirst("input[type=hidden][name=destination]").val()).isEqualTo("Hannover");
    assertThat(doc.selectFirst("input[type=hidden][name=version]").val()).isEqualTo("0");
    assertThat(doc.selectFirst("h1").text()).startsWith("Dienstreise bearbeiten");
  }

  @Test
  void as1OwnCarIsAvailableForOwnPeriod() throws Exception {
    Document doc =
        html(
            mockMvc
                .perform(
                    postForm(
                        "/trips/" + id + "/edit/period",
                        anna(),
                        period(
                            "09:00",
                            "12:00",
                            "carId",
                            "1",
                            "destination",
                            "Hannover",
                            "version",
                            "0")))
                .andExpect(status().isOk())
                .andReturn());

    assertThat(doc.selectFirst("input[name=carId][value=1]").hasAttr("disabled")).isFalse();
    assertThat(doc.selectFirst("input[name=carId][value=1]").hasAttr("checked")).isTrue();
    assertThat(doc.selectFirst("main form").attr("action")).isEqualTo("/trips/" + id + "/edit/car");
  }

  @Test
  void as2SaveChangesAndResort() throws Exception {
    TripSeeder.insert(jdbc, ANNA_SUBJECT, 2, "2026-10-04T09:00", "2026-10-04T12:00", "Celle");

    mockMvc
        .perform(postForm("/trips/" + id, anna(), changed("0")))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/trips"))
        .andExpect(flash().attribute("flashSuccess", "flash.updated"));

    assertThat(storedStart().toInstant()).isEqualTo(berlin("2026-10-06T14:00"));
    Document list = html(mockMvc.perform(get("/trips").with(anna())).andReturn());
    assertThat(list.select("tbody tr td:nth-child(3)").eachText())
        .containsExactly("Celle", "Braunschweig");
  }

  @Test
  void as3InvalidValuesShowSameMessages() throws Exception {
    Document doc =
        html(
            mockMvc
                .perform(
                    postForm(
                        "/trips/" + id + "/edit/period",
                        anna(),
                        period("12:00", "09:00", "version", "0")))
                .andExpect(status().isOk())
                .andReturn());

    assertThat(doc.getElementById("endTime-error").text())
        .isEqualTo("Das Ende muss nach dem Beginn liegen.");
  }

  @Test
  void as6StartedTripIsReadOnly() throws Exception {
    clock.setInstant(berlin("2026-10-02T10:00"));

    mockMvc
        .perform(get("/trips/" + id + "/edit").with(anna()))
        .andExpect(redirectedUrl("/trips"))
        .andExpect(flash().attribute("flashError", "trip.readOnly"));
    mockMvc
        .perform(postForm("/trips/" + id, anna(), changed("0")))
        .andExpect(redirectedUrl("/trips"))
        .andExpect(flash().attribute("flashError", "trip.readOnly"));
    mockMvc
        .perform(postForm("/trips/" + id + "/edit/period", anna(), changed("0")))
        .andExpect(redirectedUrl("/trips"));
    assertThat(storedStart().toInstant()).isEqualTo(berlin("2026-10-02T09:00"));
  }

  @Test
  void as7OtherUserGetsNotFound() throws Exception {
    Document doc =
        html(
            mockMvc
                .perform(get("/trips/" + id + "/edit").with(ben()))
                .andExpect(status().isNotFound())
                .andReturn());
    assertThat(doc.selectFirst("h1").text()).isEqualTo("Dienstreise nicht gefunden.");

    for (String url : new String[] {"/edit/period", "/edit/car", ""}) {
      mockMvc
          .perform(postForm("/trips/" + id + url, ben(), changed("0")))
          .andExpect(status().isNotFound());
    }
    assertThat(storedStart().toInstant()).isEqualTo(berlin("2026-10-02T09:00"));
  }

  @Test
  void staleVersionAsksToReload() throws Exception {
    jdbc.update("UPDATE business_trip SET version = 1 WHERE id = ?", id);

    Document doc =
        html(
            mockMvc
                .perform(postForm("/trips/" + id, anna(), changed("0")))
                .andExpect(status().isOk())
                .andReturn());

    assertThat(doc.getElementById("destination")).isNotNull();
    assertThat(doc.getElementById("error-summary").text())
        .contains("Die Dienstreise wurde zwischenzeitlich geändert. Bitte laden Sie sie neu.");
    assertThat(storedStart().toInstant()).isEqualTo(berlin("2026-10-02T09:00"));
  }

  @Test
  void technicalFailureKeepsData() throws Exception {
    doThrow(new DataAccessResourceFailureException("db down"))
        .when(tripService)
        .update(any(), any(), anyInt(), any());

    Document doc =
        html(
            mockMvc
                .perform(postForm("/trips/" + id, anna(), changed("0")))
                .andExpect(status().isOk())
                .andReturn());

    assertThat(doc.text())
        .contains(
            "Die Dienstreise konnte nicht gespeichert werden. Bitte versuchen Sie es erneut.");
    assertThat(doc.getElementById("destination").val()).isEqualTo("Braunschweig");
    assertThat(storedStart().toInstant()).isEqualTo(berlin("2026-10-02T09:00"));
  }
}
