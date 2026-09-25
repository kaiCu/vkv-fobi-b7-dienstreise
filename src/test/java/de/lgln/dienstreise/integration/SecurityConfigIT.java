package de.lgln.dienstreise.integration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.lgln.dienstreise.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class SecurityConfigIT extends AbstractIntegrationTest {

  @Test
  void unauthenticatedRequestRedirectsToIdentityProvider() throws Exception {
    assertSecurityHeaders(
        mockMvc
            .perform(get("/trips"))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/oauth2/authorization/dienstreise")));
  }

  @Test
  void staticAssetsArePublic() throws Exception {
    assertSecurityHeaders(
        mockMvc
            .perform(get("/webjars/bootstrap/dist/css/bootstrap.min.css"))
            .andExpect(status().isOk()));
  }

  @Test
  void logoutWithoutCsrfIsForbidden() throws Exception {
    mockMvc.perform(post("/logout").with(anna())).andExpect(status().isForbidden());
  }

  @Test
  void authenticatedPageHasSecurityHeaders() throws Exception {
    assertSecurityHeaders(mockMvc.perform(get("/trips").with(anna())));
  }

  private static void assertSecurityHeaders(ResultActions result) throws Exception {
    result
        .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
        .andExpect(
            header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
        .andExpect(header().string("Content-Security-Policy", containsString("form-action 'self'")))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("Referrer-Policy", "same-origin"));
  }
}
