package de.lgln.dienstreise.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Turns the pending retention decision of the DPO into an explicit release gate (FR-024). */
@Component
@Profile("prod")
public class ProdStartupGuard implements InitializingBean {

  static final String MISSING_RETENTION =
      "DIENSTREISE_RETENTION_PERIOD ist nicht gesetzt – Aufbewahrungsfrist mit dem"
          + " Datenschutzbeauftragten festlegen (FR-024).";

  private final DienstreiseProperties properties;

  public ProdStartupGuard(DienstreiseProperties properties) {
    this.properties = properties;
  }

  @Override
  public void afterPropertiesSet() {
    if (properties.retention().period() == null) {
      throw new IllegalStateException(MISSING_RETENTION);
    }
  }
}
