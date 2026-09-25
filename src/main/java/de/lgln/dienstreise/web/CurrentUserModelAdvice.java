package de.lgln.dienstreise.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Provides the display name for the page header. */
@ControllerAdvice
public class CurrentUserModelAdvice {

  @ModelAttribute("currentUserName")
  public String currentUserName() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof OidcUser user) {
      return CurrentUser.of(user).displayName();
    }
    return null;
  }
}
