package org.ikigaidigital;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Enforces the hexagonal architecture of ADR 0003 (and ADR 0005 for Spring in application services) on the production
 * classes, so the rules in java/CLAUDE.md are checked by the build, not just written down.
 */
class ArchitectureTest {

    private static final String ROOT = "org.ikigaidigital";
    private static final String DOMAIN = ROOT + ".domain..";
    private static final String APPLICATION = ROOT + ".application..";
    private static final String ADAPTER = ROOT + ".adapter..";
    private static final String CONFIG = ROOT + ".config..";

    /** Frameworks the domain and the legacy classes must stay free of. */
    private static final String[] FRAMEWORKS = {
            "org.springframework..", "jakarta..", "org.hibernate..",
            "com.fasterxml.jackson..", "tools.jackson..", "io.swagger..", "org.springdoc.."};

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importProductionClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    @Test
    void dependenciesPointInward() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Domain").definedBy(DOMAIN)
                .layer("Application").definedBy(APPLICATION)
                .layer("Adapters").definedBy(ADAPTER)
                .layer("Config").definedBy(CONFIG)
                .whereLayer("Adapters").mayNotBeAccessedByAnyLayer()
                .whereLayer("Config").mayNotBeAccessedByAnyLayer()
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapters", "Config")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapters")
                .check(productionClasses);
    }

    @Test
    void domainIsFrameworkFree() {
        noClasses().that().resideInAPackage(DOMAIN)
                .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS)
                .because("the domain is plain Java (ADR 0003)")
                .check(productionClasses);
    }

    @Test
    void legacyClassesAreFrameworkFreeAndIndependentOfTheNewLayers() {
        noClasses().that().belongToAnyOf(TimeDeposit.class, TimeDepositCalculator.class)
                .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS)
                .orShould().dependOnClassesThat().resideInAnyPackage(APPLICATION, ADAPTER, CONFIG)
                .because("the shared legacy classes must stay unchanged and reusable (README, ADR 0003)")
                .check(productionClasses);
    }

    @Test
    void webAdapterUsesOnlyIncomingPorts() {
        noClasses().that().resideInAPackage(ROOT + ".adapter.in..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".application.service..", ROOT + ".application.port.out..", ROOT + ".adapter.out..")
                .because("controllers depend only on port/in interfaces (ADR 0003)")
                .check(productionClasses);
    }

    @Test
    void persistenceAdapterDoesNotDependOnTheWebAdapter() {
        noClasses().that().resideInAPackage(ROOT + ".adapter.out..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".adapter.in..")
                .check(productionClasses);
    }

    @Test
    void applicationDependsOnPortsNotOnAdapters() {
        noClasses().that().resideInAPackage(APPLICATION)
                .should().dependOnClassesThat().resideInAPackage(ADAPTER)
                .because("application services may use Spring (ADR 0005) but never adapter types")
                .check(productionClasses);
    }

    @Test
    void portsAreInterfaces() {
        classes().that().resideInAPackage(ROOT + ".application.port..")
                .and().haveSimpleNameEndingWith("Port")
                .or().haveSimpleNameEndingWith("UseCase")
                .should().beInterfaces()
                .check(productionClasses);
    }

    @Test
    void jpaEntitiesLiveOnlyInThePersistenceAdapterAndAreNotPublic() {
        classes().that().areAnnotatedWith(Entity.class)
                .should().resideInAPackage(ROOT + ".adapter.out.persistence..")
                .andShould().notBePublic()
                .because("JPA entities never leave the persistence adapter (ADR 0003)")
                .check(productionClasses);
    }

    @Test
    void controllersLiveOnlyInTheWebAdapter() {
        classes().that().areAnnotatedWith(RestController.class)
                .should().resideInAPackage(ROOT + ".adapter.in.web..")
                .check(productionClasses);
    }
}
