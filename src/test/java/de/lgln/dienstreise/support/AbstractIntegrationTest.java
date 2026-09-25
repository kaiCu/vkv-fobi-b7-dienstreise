package de.lgln.dienstreise.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, TestClockConfig.class, TestOidcConfig.class})
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

  public static final String ANNA_SUBJECT = TestOidcConfig.ISSUER + "|anna-sub";
  public static final String BEN_SUBJECT = TestOidcConfig.ISSUER + "|ben-sub";

  @Autowired protected MockMvc mockMvc;
  @Autowired protected JdbcTemplate jdbc;
  @Autowired protected MutableClock clock;

  @BeforeEach
  void resetState() {
    jdbc.update("DELETE FROM business_trip");
    clock.setInstant(TestClockConfig.DEFAULT_NOW);
  }

  protected static OidcLoginRequestPostProcessor anna() {
    return user("anna-sub", "Anna Beispiel");
  }

  protected static OidcLoginRequestPostProcessor ben() {
    return user("ben-sub", "Ben Muster");
  }

  protected static OidcLoginRequestPostProcessor user(String sub, String name) {
    return oidcLogin()
        .idToken(t -> t.issuer(TestOidcConfig.ISSUER).subject(sub).claim("name", name));
  }
}
