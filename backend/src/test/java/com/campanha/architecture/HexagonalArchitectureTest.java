package com.campanha.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Trava a fronteira hexagonal desde o dia 1. Se o build falhar por aqui,
 * NÃO desabilite o teste — corrija o import ilegal.
 */
@AnalyzeClasses(
        packages = "com.campanha",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule dominioNaoDependeDeSpring = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "com.fasterxml.jackson..",
                    "org.hibernate..",
                    "software.amazon..",
                    "io.jsonwebtoken.."
            )
            .because("A camada domain é regra de negócio pura, sem dependência de infra.");

    @ArchTest
    static final ArchRule applicationNaoDependeDeInfrastructure = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("application depende só de domain e das próprias ports; adapters concretos ficam em infrastructure.");

    @ArchTest
    static final ArchRule camadasRespeitadas = Architectures.layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("domain").definedBy("..domain..")
            .layer("application").definedBy("..application..")
            .layer("infrastructure").definedBy("..infrastructure..")
            .whereLayer("infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("application").mayOnlyBeAccessedByLayers("infrastructure")
            .whereLayer("domain").mayOnlyBeAccessedByLayers("application", "infrastructure");
}
