package de.lgln.dienstreise.web;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * The signed-in user.
 *
 * @param subject stable ownership key {@code iss|sub}, stored with each trip
 * @param displayName shown in the page header only, never stored
 */
public record CurrentUser(String subject, String displayName) {

  public static CurrentUser of(OidcUser user) {
    String name = user.getFullName();
    if (name == null || name.isBlank()) {
      name = user.getPreferredUsername();
    }
    return new CurrentUser(user.getIssuer() + "|" + user.getSubject(), name);
  }
}
