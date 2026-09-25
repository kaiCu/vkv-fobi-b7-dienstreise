package de.lgln.dienstreise.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * Dummy OIDC client registration with explicit endpoints, so no discovery request is sent. Spring
 * Boot's client registration auto-configuration backs off when this bean exists.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestOidcConfig {

  public static final String ISSUER = "https://idp.test/realms/dienstreise";

  @Bean
  ClientRegistrationRepository clientRegistrationRepository() {
    return new InMemoryClientRegistrationRepository(
        ClientRegistration.withRegistrationId("dienstreise")
            .clientId("dienstreise")
            .clientSecret("test-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile")
            .issuerUri(ISSUER)
            .authorizationUri(ISSUER + "/protocol/openid-connect/auth")
            .tokenUri(ISSUER + "/protocol/openid-connect/token")
            .jwkSetUri(ISSUER + "/protocol/openid-connect/certs")
            .userInfoUri(ISSUER + "/protocol/openid-connect/userinfo")
            .userNameAttributeName("sub")
            .build());
  }
}
