package de.lgln.dienstreise.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

  static final String CONTENT_SECURITY_POLICY =
      "default-src 'self'; img-src 'self' data:; frame-ancestors 'none'; form-action 'self';"
          + " base-uri 'self'";

  /**
   * Actuator endpoints live on the separate management port, which is not exposed via the ingress.
   * Only health and prometheus are exposed; everything else there is 404.
   */
  @Bean
  @Order(1)
  SecurityFilterChain managementFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/actuator/**")
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .requestCache(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, ClientRegistrationRepository clientRegistrations) throws Exception {
    OidcClientInitiatedLogoutSuccessHandler logoutSuccess =
        new OidcClientInitiatedLogoutSuccessHandler(clientRegistrations);
    logoutSuccess.setPostLogoutRedirectUri("{baseUrl}/");

    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/webjars/**", "/css/**", "/js/**", "/favicon.ico", "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2Login(Customizer.withDefaults())
        .csrf(Customizer.withDefaults())
        .sessionManagement(s -> s.sessionFixation(f -> f.changeSessionId()))
        .headers(
            h ->
                h.contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                    .httpStrictTransportSecurity(
                        hsts ->
                            hsts.includeSubDomains(true)
                                .maxAgeInSeconds(Duration.ofDays(365).toSeconds()))
                    .referrerPolicy(r -> r.policy(ReferrerPolicy.SAME_ORIGIN))
                    .frameOptions(f -> f.deny()))
        .logout(l -> l.logoutSuccessHandler(logoutSuccess));
    return http.build();
  }
}
