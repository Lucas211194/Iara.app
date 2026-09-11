package com.iara.common.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Testes de Arquitetura (ArchUnit) - Regras Obrigatórias IARA
 */
class ArchitectureRulesTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .importPackages("com.iara");

    @Test
    void domainModulesShouldNotAccessOtherDomainRepositoriesDirectly() {
        // REGRA: Módulos de domínio isolados - nenhum acesso direto ao banco de outro módulo
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.iara.cycle..")
                .should().accessClassesThat().resideInAnyPackage(
                    "com.iara.symptom.repository..",
                    "com.iara.mood.repository..",
                    "com.iara.sleep.repository..",
                    "com.iara.weight.repository..",
                    "com.iara.activity.repository..",
                    "com.iara.reproductive.repository..",
                    "com.iara.partner.repository.."
                )
                .because("Módulos de domínio devem ser isolados. Comunicação via eventos ou API Gateway.");

        rule.check(CLASSES);
    }

    @Test
    void sensitiveHealthDataFieldsMustBeEncrypted() {
        // REGRA: Campos @SensitiveHealthData devem ter @Convert(ColumnEncryptor.EncryptedStringConverter.class)
        ArchRule rule = fields()
                .that().areAnnotatedWith("com.iara.common.annotation.SensitiveHealthData")
                .should().beAnnotatedWith("jak.persistence.Convert")
                .because("Dados de saúde sensíveis DEVEM ser criptografados em nível de coluna");

        rule.check(CLASSES);
    }

    @Test
    void noThirdPartyAnalyticsLibrariesInDomainModules() {
        // REGRA: Nenhuma dependência de Firebase Analytics, Amplitude, Facebook SDK, etc. nos módulos de domínio
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("com.iara.cycle..", "com.iara.symptom..", "com.iara.mood..",
                    "com.iara.sleep..", "com.iara.weight..", "com.iara.activity..",
                    "com.iara.reproductive..", "com.iara.partner..", "com.iara.ai..")
                .should().dependOnClassesThat().haveSimpleNameContainingAnyOf(
                    "FirebaseAnalytics", "Amplitude", "FacebookSdk", "AppsFlyer", "Adjust",
                    "Mixpanel", "Segment", "Sentry", "DataDog", "NewRelic"
                )
                .because("Módulos de domínio NUNCA podem depender de bibliotecas de analytics/monitoring de terceiros");

        rule.check(CLASSES);
    }

    @Test
    void aiLayerShouldNotDiagnose() {
        // REGRA: Camada de IA não pode ter métodos que sugerem diagnóstico
        ArchRule rule = noMethods()
                .that().areDeclaredInClassesThat().resideInAPackage("com.iara.ai..")
                .should().haveNameMatching("diagnose|predictCondition|detectDisease|identifyPathology")
                .because("IA IARA é EXPLICATIVA, nunca diagnóstica");

        rule.check(CLASSES);
    }

    @Test
    void partnerPermissionEnforcedInBackendNotJustUI() {
        // REGRA: Controle de permissão do parceiro deve estar no service/repository, não só controller
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat().resideInAPackage("com.iara.partner.service..")
                .or().areDeclaredInClassesThat().resideInAPackage("com.iara.partner.repository..")
                .should().haveNameMatching("canAccess|checkPermission|enforcePermission|revokeAccess")
                .because("Permissões do parceiro DEVEM ser validadas no backend (service/repository), não apenas na UI");

        rule.check(CLASSES);
    }
}