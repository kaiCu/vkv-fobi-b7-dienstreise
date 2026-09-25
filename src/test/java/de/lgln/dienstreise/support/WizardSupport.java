package de.lgln.dienstreise.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Helpers for posting wizard steps and parsing the rendered HTML. */
public final class WizardSupport {

  private WizardSupport() {}

  /** Form values of a valid period on 2026-10-02 (Berlin) plus extra key/value pairs. */
  public static Map<String, String> period(String from, String to, String... extra) {
    Map<String, String> values = new LinkedHashMap<>();
    values.put("startDate", "2026-10-02");
    values.put("startTime", from);
    values.put("endDate", "2026-10-02");
    values.put("endTime", to);
    for (int i = 0; i < extra.length; i += 2) {
      values.put(extra[i], extra[i + 1]);
    }
    return values;
  }

  public static MockHttpServletRequestBuilder postForm(
      String url, OidcLoginRequestPostProcessor user, Map<String, String> values) {
    MockHttpServletRequestBuilder request = post(url).with(user).with(csrf());
    values.forEach(request::param);
    return request;
  }

  public static Document html(MvcResult result) throws Exception {
    return Jsoup.parse(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
  }
}
