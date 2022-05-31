package io.lcalmsky.app;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packagesOf = App.class)
public class PackageDependencyTests {
    public static final String STUDY = "..modules.study..";
    public static final String EVENT = "..modules.event..";
    public static final String ACCOUNT = "..modules.account..";
    public static final String TAG = "..modules.tag..";
    public static final String ZONE = "..modules.zone..";
    public static final String MODULES = "io.lcalmsky.app.modules..";
    public static final JavaClasses CLASS = new ClassFileImporter().importPackagesOf(App.class);

    @ArchTest
    ArchRule studyPackageRule = classes().that()
            .resideInAPackage(STUDY)
            .should()
            .onlyBeAccessed()
            .byClassesThat()
            .resideInAnyPackage(STUDY, EVENT);

    @ArchTest
    ArchRule eventPackageRule = classes().that()
            .resideInAnyPackage(EVENT)
            .should()
            .accessClassesThat()
            .resideInAnyPackage(STUDY, ZONE, EVENT);

    @ArchTest
    ArchRule accountPackageRule = classes().that()
            .resideInAnyPackage(ACCOUNT)
            .should()
            .accessClassesThat()
            .resideInAnyPackage(TAG, ZONE, ACCOUNT);

//    @ArchTest
//    ArchRule cycleRule = slices().matching("io.lcalmsky.app.modules.(*)..")
//            .should()
//            .beFreeOfCycles();

    @ArchTest
    ArchRule modulesPackageRule = classes().that()
            .resideInAPackage(MODULES)
            .should()
            .onlyBeAccessed()
            .byClassesThat()
            .resideInAnyPackage(MODULES);

    @Test
    void studyPackageRuleTest() {
        studyPackageRule.check(CLASS);
    }

    @Test
    void eventPackageRuleTest() {
        eventPackageRule.check(CLASS);
    }

    @Test
    void accountPackageRuleTest() {
        accountPackageRule.check(CLASS);
    }

    @Test
    void modulesPackageRuleTest() {
        modulesPackageRule.check(CLASS);
    }

//    @Test
//    void cycleRuleTest() {
//        cycleRule.check(CLASS);
//    }
}
