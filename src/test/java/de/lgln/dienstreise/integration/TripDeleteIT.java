package de.lgln.dienstreise.integration;

import static de.lgln.dienstreise.support.TripSeeder.berlin;
import static de.lgln.dienstreise.support.WizardSupport.html;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.lgln.dienstreise.support.AbstractIntegrationTest;
import de.lgln.dienstreise.support.TripSeeder;
import de.lgln.dienstreise.trip.AvailabilityService;
import de.lgln.dienstreise.trip.domain.TripPeriod;
import java.util.UUID;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TripDeleteIT extends AbstractIntegrationTest {

  @Autowired AvailabilityService availability;

  UUID id;

  @BeforeEach
  void seed() {
    id =
        TripSeeder.insert(
            jdbc, ANNA_SUBJECT, 1, "2026-10-02T09:00", "2026-10-02T12:00", "Hannover");
  }

  @Test
  void confirmationPageShowsTripAndChoices() throws Exception {
    Document doc =
        html(
            mockMvc
                .perform(get("/trips/" + id + "/delete").with(anna()))
                .andExpect(status().isOk())
                .andReturn());

    assertThat(doc.selectFirst("h1").text()).isEqualTo("Dienstreise löschen?");
    assertThat(doc.text())
        .contains("Dienstwagen 1 (H-LG 101)", "02.10.2026, 09:00", "02.10.2026, 12:00", "Hannover");
    assertThat(doc.selectFirst("main form[method=post]").attr("action"))
        .isEqualTo("/trips/" + id + "/delete");
    assertThat(doc.selectFirst("main form button[type=submit]").text())
        .isEqualTo("Endgültig löschen");
    assertThat(doc.selectFirst("main a[href=/trips]").text()).isEqualTo("Abbrechen");
  }

  @Test
  void cancelKeepsTrip() throws Exception {
    mockMvc.perform(get("/trips/" + id + "/delete").with(anna())).andExpect(status().isOk());
    mockMvc.perform(get("/trips").with(anna())).andExpect(status().isOk());

    assertThat(TripSeeder.count(jdbc)).isEqualTo(1);
  }

  @Test
  void confirmDeletesAndFreesSlot() throws Exception {
    mockMvc
        .perform(post("/trips/" + id + "/delete").with(anna()).with(csrf()))
        .andExpect(redirectedUrl("/trips"))
        .andExpect(flash().attribute("flashSuccess", "flash.deleted"));

    assertThat(TripSeeder.count(jdbc)).isZero();
    assertThat(
            availability.isAvailable(
                (short) 1,
                new TripPeriod(berlin("2026-10-02T09:00"), berlin("2026-10-02T12:00")),
                null))
        .isTrue();
  }

  @Test
  void startedTripCannotBeDeleted() throws Exception {
    clock.setInstant(berlin("2026-10-02T09:00"));

    mockMvc
        .perform(get("/trips/" + id + "/delete").with(anna()))
        .andExpect(redirectedUrl("/trips"))
        .andExpect(flash().attribute("flashError", "trip.readOnly"));
    mockMvc
        .perform(post("/trips/" + id + "/delete").with(anna()).with(csrf()))
        .andExpect(redirectedUrl("/trips"))
        .andExpect(flash().attribute("flashError", "trip.readOnly"));
    assertThat(TripSeeder.count(jdbc)).isEqualTo(1);
  }

  @Test
  void otherUserGetsNotFound() throws Exception {
    mockMvc.perform(get("/trips/" + id + "/delete").with(ben())).andExpect(status().isNotFound());
    mockMvc
        .perform(post("/trips/" + id + "/delete").with(ben()).with(csrf()))
        .andExpect(status().isNotFound());
    assertThat(TripSeeder.count(jdbc)).isEqualTo(1);
  }

  @Test
  void deleteWithoutCsrfIsForbidden() throws Exception {
    mockMvc
        .perform(post("/trips/" + id + "/delete").with(anna()))
        .andExpect(status().isForbidden());
    assertThat(TripSeeder.count(jdbc)).isEqualTo(1);
  }
}
