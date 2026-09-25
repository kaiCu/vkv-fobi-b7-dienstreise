package de.lgln.dienstreise.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@AnalyzeClasses(
    packages = "de.lgln.dienstreise",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  @ArchTest
  static final ArchRule noDirectSystemTime =
      noClasses()
          .that()
          .resideOutsideOfPackage("..config..")
          .should()
          .callMethod(Instant.class, "now")
          .orShould()
          .callMethod(LocalDateTime.class, "now")
          .orShould()
          .callMethod(LocalDate.class, "now")
          .orShould()
          .callMethod(ZonedDateTime.class, "now")
          .orShould()
          .callMethod(Clock.class, "systemUTC")
          .orShould()
          .callMethod(Clock.class, "systemDefaultZone")
          .orShould()
          .callMethod(Clock.class, "system", java.time.ZoneId.class)
          .because("time must come from the injected Clock (deterministic tests)");

  @ArchTest
  static final ArchRule webDoesNotUseRepositories =
      noClasses()
          .that()
          .resideInAPackage("..web..")
          .should()
          .dependOnClassesThat()
          .haveSimpleNameEndingWith("Repository");

  @ArchTest
  static final ArchRule noSystemOut =
      noClasses()
          .should()
          .accessField(System.class, "out")
          .orShould()
          .accessField(System.class, "err");
}
