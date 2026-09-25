package de.lgln.dienstreise.integration;

import static de.lgln.dienstreise.support.WizardSupport.html;
import static de.lgln.dienstreise.support.WizardSupport.period;
import static de.lgln.dienstreise.support.WizardSupport.postForm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import de.lgln.dienstreise.support.AbstractIntegrationTest;
import de.lgln.dienstreise.support.TripSeeder;
import java.util.Map;
import java.util.UUID;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

/** US4 AS2: another user saved first; ben is sent back to the car step. */
class RaceConditionWizardIT extends AbstractIntegrationTest {

  @Test
  void conflictAtSaveReturnsToCarStepAndRetrySucceeds() throws Exception {
    Map<String, String> bensTrip = period("10:00", "11:00", "carId", "2", "destination", "Celle");
    // ben reached the destination step while car 2 was still free
    mockMvc.perform(postForm("/trips/new/car", ben(), bensTrip)).andExpect(status().isOk());
    UUID annas =
        TripSeeder.insert(
            jdbc, ANNA_SUBJECT, 2, "2026-10-02T09:30", "2026-10-02T10:30", "Hannover");

    Document doc =
        html(
            mockMvc
                .perform(postForm("/trips", ben(), bensTrip))
                .andExpect(status().isOk())
                .andExpect(view().name("trips/car"))
                .andReturn());

    assertThat(doc.selectFirst("input[name=carId][value=2]").hasAttr("disabled")).isTrue();
    assertThat(doc.selectFirst("label[for=car-2]").text()).contains("nicht verfügbar");
    assertThat(doc.select("input[name=carId][checked]")).isEmpty();
    assertThat(doc.text()).contains(TripWizardCreateIT.BOOKED);
    assertThat(doc.selectFirst("input[type=hidden][name=startTime]").val()).isEqualTo("10:00");
    assertThat(doc.selectFirst("input[type=hidden][name=endTime]").val()).isEqualTo("11:00");
    assertThat(doc.selectFirst("input[type=hidden][name=destination]").val()).isEqualTo("Celle");
    assertThat(TripSeeder.count(jdbc)).isEqualTo(1);

    jdbc.update("DELETE FROM business_trip WHERE id = ?", annas);
    mockMvc.perform(postForm("/trips", ben(), bensTrip)).andExpect(status().isFound());
    assertThat(TripSeeder.count(jdbc)).isEqualTo(1);
  }
}
