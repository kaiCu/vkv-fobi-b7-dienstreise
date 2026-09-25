package de.lgln.dienstreise.integration;

import static de.lgln.dienstreise.support.WizardSupport.html;
import static de.lgln.dienstreise.support.WizardSupport.period;
import static de.lgln.dienstreise.support.WizardSupport.postForm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.lgln.dienstreise.support.AbstractIntegrationTest;
import de.lgln.dienstreise.support.TripSeeder;
import de.lgln.dienstreise.trip.TripService;
import java.time.Duration;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;

class TripWizardCreateIT extends AbstractIntegrationTest {

  static final String BOOKED =
      "Dieses Fahrzeug ist im gewählten Zeitraum bereits gebucht. Bitte wählen Sie ein anderes"
          + " Fahrzeug oder einen anderen Zeitraum.";

  @MockitoSpyBean TripService tripService;

  private Document step(String url, Map<String, String> values) throws Exception {
    return html(
        mockMvc.perform(postForm(url, anna(), values)).andExpect(status().isOk()).andReturn());
  }

  private static Element radio(Document doc, int carId) {
    return doc.selectFirst("input[type=radio][name=carId][value=" + carId + "]");
  }

  @Test
  void as1PeriodStepHasLabelledDateAndTimeInputs() throws Exception {
    Document doc =
        html(
            mockMvc.perform(get("/trips/new").with(anna())).andExpect(status().isOk()).andReturn());

    assertThat(doc.selectFirst("html").attr("lang")).isEqualTo("de");
    for (String[] field :
        new String[][] {
          {"startDate", "date"}, {"startTime", "time"}, {"endDate", "date"}, {"endTime", "time"}
        }) {
      Element input = doc.getElementById(field[0]);
      assertThat(input).as(field[0]).isNotNull();
      assertThat(input.attr("type")).isEqualTo(field[1]);
      assertThat(input.hasAttr("required")).isTrue();
      assertThat(doc.selectFirst("label[for=" + field[0] + "]")).isNotNull();
    }
    assertThat(doc.selectFirst("h1").text()).contains("Schritt 1 von 3");
  }

  @Test
  void as2ValidPeriodShowsCarStep() throws Exception {
    Document doc = step("/trips/new/period", period("09:00", "12:00"));

    assertThat(doc.selectFirst("fieldset legend").text()).isEqualTo("Fahrzeug wählen");
    assertThat(doc.select("input[type=radio][name=carId]")).hasSize(3);
    assertThat(doc.selectFirst("label[for=car-1]").text()).contains("Dienstwagen 1", "H-LG 101");
    assertThat(doc.selectFirst("input[type=hidden][name=startDate]").val()).isEqualTo("2026-10-02");
    assertThat(doc.selectFirst("input[type=hidden][name=startTime]").val()).isEqualTo("09:00");
    assertThat(doc.selectFirst("input[type=hidden][name=endTime]").val()).isEqualTo("12:00");
  }

  @Test
  void as3BookedCarIsDisabledAndMarked() throws Exception {
    TripSeeder.insert(jdbc, BEN_SUBJECT, 1, "2026-10-02T10:00", "2026-10-02T11:00", "Celle");

    Document doc = step("/trips/new/period", period("09:00", "12:00"));

    assertThat(radio(doc, 1).hasAttr("disabled")).isTrue();
    assertThat(doc.selectFirst("label[for=car-1]").text()).contains("nicht verfügbar");
    assertThat(radio(doc, 2).hasAttr("disabled")).isFalse();
    assertThat(doc.selectFirst("label[for=car-2]").text()).doesNotContain("nicht verfügbar");
  }

  @Test
  void as4NoCarAvailable() throws Exception {
    for (int car = 1; car <= 3; car++) {
      TripSeeder.insert(jdbc, BEN_SUBJECT, car, "2026-10-02T10:00", "2026-10-02T11:00", "Celle");
    }

    Document doc = step("/trips/new/period", period("09:00", "12:00"));

    assertThat(doc.text())
        .contains("Für diesen Zeitraum ist kein Fahrzeug frei. Bitte ändern Sie Beginn oder Ende.");
    assertThat(doc.select("input[type=radio][name=carId]:not([disabled])")).isEmpty();
    assertThat(doc.selectFirst("button[name=action][value=back]")).isNotNull();
    assertThat(doc.select("main button[type=submit]:not([value=back])")).isEmpty();
  }

  @Test
  void as5CarStepLeadsToDestinationStepWithReview() throws Exception {
    Document doc = step("/trips/new/car", period("09:00", "12:00", "carId", "2"));

    assertThat(doc.getElementById("destination")).isNotNull();
    assertThat(doc.getElementById("destination").attr("maxlength")).isEqualTo("200");
    assertThat(doc.text()).contains("02.10.2026, 09:00", "02.10.2026, 12:00");
    assertThat(doc.text()).contains("Dienstwagen 2 (H-LG 102)");
    assertThat(doc.selectFirst("input[type=hidden][name=carId]").val()).isEqualTo("2");
  }

  @Test
  void as6SaveRedirectsWithConfirmation() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                postForm(
                    "/trips",
                    anna(),
                    period("09:00", "12:00", "carId", "2", "destination", "Hannover")))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/trips"))
            .andExpect(flash().attribute("flashSuccess", "flash.created"))
            .andReturn();

    assertThat(TripSeeder.count(jdbc)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT owner_subject FROM business_trip", String.class))
        .isEqualTo(ANNA_SUBJECT);

    // Follow the redirect with the same (database-backed) session, which holds the flash map
    Document list =
        html(
            mockMvc
                .perform(
                    get("/trips").with(anna()).cookie(result.getResponse().getCookie("SESSION")))
                .andReturn());
    Element status = list.selectFirst("[role=status]");
    assertThat(status).isNotNull();
    assertThat(status.text()).isEqualTo("Die Dienstreise wurde gespeichert.");
  }

  @Test
  void as7MissingFieldsShowErrorSummaryAndKeepValues() throws Exception {
    Document doc =
        step(
            "/trips/new/period",
            Map.of("startDate", "2026-10-02", "startTime", "09:00", "endDate", "", "endTime", ""));

    assertThat(doc.title()).startsWith("Fehler: ");
    Element summary = doc.getElementById("error-summary");
    assertThat(summary).isNotNull();
    assertThat(summary.attr("role")).isEqualTo("alert");
    assertThat(summary.select("a[href=#endDate]").text())
        .isEqualTo("Bitte geben Sie das Enddatum an.");
    assertThat(summary.select("a[href=#endTime]").text())
        .isEqualTo("Bitte geben Sie die Endzeit an.");
    assertThat(doc.getElementById("endDate").attr("aria-invalid")).isEqualTo("true");
    assertThat(doc.getElementById("endDate").attr("aria-describedby")).contains("endDate-error");
    assertThat(doc.getElementById("endDate-error").text())
        .isEqualTo("Bitte geben Sie das Enddatum an.");
    assertThat(doc.getElementById("startDate").val()).isEqualTo("2026-10-02");
    assertThat(doc.getElementById("startTime").val()).isEqualTo("09:00");
    assertThat(doc.getElementById("startDate").hasAttr("aria-invalid")).isFalse();
    assertThat(TripSeeder.count(jdbc)).isZero();
  }

  @Test
  void invalidDateValueIsReported() throws Exception {
    Document doc = step("/trips/new/period", period("09:00", "12:00", "startDate", "2026-13-45"));

    assertThat(doc.getElementById("error-summary").text())
        .contains("Bitte geben Sie ein gültiges Datum an.");
  }

  @Test
  void as8EndBeforeStart() throws Exception {
    Document doc = step("/trips/new/period", period("12:00", "09:00"));

    assertThat(doc.getElementById("endTime-error").text())
        .isEqualTo("Das Ende muss nach dem Beginn liegen.");
  }

  @Test
  void as9StartInThePastAtPeriodStep() throws Exception {
    Document doc =
        step(
            "/trips/new/period",
            Map.of(
                "startDate", "2026-09-30",
                "startTime", "09:00",
                "endDate", "2026-09-30",
                "endTime", "12:00"));

    assertThat(doc.getElementById("startDate-error").text())
        .startsWith("Dienstreisen können nur mit einem Beginn in der Zukunft gebucht werden.");
    assertThat(doc.getElementById("startDate").val()).isEqualTo("2026-09-30");
  }

  @Test
  void as9StartPassedBeforeSave() throws Exception {
    clock.advance(Duration.ofDays(2));

    Document doc =
        step("/trips", period("09:00", "12:00", "carId", "2", "destination", "Hannover"));

    assertThat(doc.getElementById("startDate")).isNotNull();
    assertThat(doc.getElementById("startDate-error").text())
        .startsWith("Dienstreisen können nur mit einem Beginn in der Zukunft gebucht werden.");
    assertThat(doc.selectFirst("input[type=hidden][name=destination]").val()).isEqualTo("Hannover");
    assertThat(doc.selectFirst("input[type=hidden][name=carId]").val()).isEqualTo("2");
    assertThat(TripSeeder.count(jdbc)).isZero();
  }

  @Test
  void manipulatedUnavailableCarIsRejected() throws Exception {
    TripSeeder.insert(jdbc, BEN_SUBJECT, 1, "2026-10-02T10:00", "2026-10-02T11:00", "Celle");

    Document doc = step("/trips/new/car", period("09:00", "12:00", "carId", "1"));

    assertThat(doc.select("input[type=radio][name=carId]")).hasSize(3);
    assertThat(doc.getElementById("error-summary").text()).contains(BOOKED);
    assertThat(doc.select("input[type=radio][name=carId][checked]")).isEmpty();
  }

  @Test
  void backFromCarStepKeepsPeriod() throws Exception {
    Document doc = step("/trips/new/car", period("09:00", "12:00", "carId", "2", "action", "back"));

    assertThat(doc.getElementById("startTime").val()).isEqualTo("09:00");
    assertThat(doc.getElementById("endTime").val()).isEqualTo("12:00");
    assertThat(doc.selectFirst("input[type=hidden][name=carId]").val()).isEqualTo("2");
    assertThat(doc.getElementById("error-summary")).isNull();
  }

  @Test
  void changedPeriodClearsNoLongerAvailableCar() throws Exception {
    TripSeeder.insert(jdbc, BEN_SUBJECT, 1, "2026-10-02T14:00", "2026-10-02T15:00", "Celle");

    Document doc = step("/trips/new/period", period("13:00", "16:00", "carId", "1"));

    assertThat(doc.select("input[type=radio][name=carId][checked]")).isEmpty();
    assertThat(doc.text())
        .contains("Das zuvor gewählte Fahrzeug ist im neuen Zeitraum nicht verfügbar.");
  }

  @Test
  void changedPeriodKeepsStillAvailableCar() throws Exception {
    Document doc = step("/trips/new/period", period("13:00", "16:00", "carId", "2"));

    assertThat(radio(doc, 2).hasAttr("checked")).isTrue();
    assertThat(doc.getElementById("error-summary")).isNull();
  }

  @Test
  void technicalFailureKeepsData() throws Exception {
    doThrow(new DataAccessResourceFailureException("db down"))
        .when(tripService)
        .create(any(), any());

    Document doc =
        step("/trips", period("09:00", "12:00", "carId", "2", "destination", "Hannover"));

    assertThat(doc.text())
        .contains(
            "Die Dienstreise konnte nicht gespeichert werden. Bitte versuchen Sie es erneut.");
    assertThat(doc.getElementById("destination").val()).isEqualTo("Hannover");
  }

  @Test
  void noDataLossWhenGoingBackAndForth() throws Exception {
    Map<String, String> all = period("09:00", "12:00", "carId", "2", "destination", "Hannover");

    Map<String, String> fromStep3Back = new java.util.HashMap<>(all);
    fromStep3Back.put("action", "back");
    Document step2 = step("/trips", fromStep3Back);
    assertThat(radio(step2, 2).hasAttr("checked")).isTrue();
    assertThat(step2.selectFirst("input[type=hidden][name=destination]").val())
        .isEqualTo("Hannover");

    Document step1 = step("/trips/new/car", fromStep3Back);
    assertThat(step1.selectFirst("input[type=hidden][name=carId]").val()).isEqualTo("2");
    assertThat(step1.selectFirst("input[type=hidden][name=destination]").val())
        .isEqualTo("Hannover");

    Document step2Again = step("/trips/new/period", all);
    assertThat(radio(step2Again, 2).hasAttr("checked")).isTrue();

    Document step3Again = step("/trips/new/car", all);
    assertThat(step3Again.getElementById("destination").val()).isEqualTo("Hannover");
  }
}
