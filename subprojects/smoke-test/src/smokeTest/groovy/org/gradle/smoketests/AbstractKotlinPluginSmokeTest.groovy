/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.smoketests

import org.gradle.api.JavaVersion
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.internal.reflect.validation.ValidationMessageChecker
import org.gradle.util.GradleVersion
import org.gradle.util.internal.VersionNumber

import static org.gradle.api.internal.DocumentationRegistry.RECOMMENDATION

abstract class AbstractKotlinPluginSmokeTest extends AbstractPluginValidatingSmokeTest implements ValidationMessageChecker {
    protected static class KotlinDeprecations extends BaseDeprecations implements WithKotlinDeprecations {
        public static final DocumentationRegistry DOC_REGISTRY = new DocumentationRegistry()

        private static final String ARCHIVE_NAME_DEPRECATION = "The AbstractArchiveTask.archiveName property has been deprecated. " +
            "This is scheduled to be removed in Gradle 8.0. Please use the archiveFileName property instead. " +
            String.format(RECOMMENDATION, "information", DOC_REGISTRY.getDslRefForProperty("org.gradle.api.tasks.bundling.AbstractArchiveTask", "archiveName"))

        KotlinDeprecations(SmokeTestGradleRunner runner) {
            super(runner)
        }

        void expectKotlinArchiveNameDeprecation(String kotlinPluginVersion) {
            VersionNumber kotlinVersionNumber = VersionNumber.parse(kotlinPluginVersion)
            runner.expectLegacyDeprecationWarningIf(kotlinVersionNumber.minor == 3, ARCHIVE_NAME_DEPRECATION)
        }

        void expectKotlin2JsPluginDeprecation(String version) {
            VersionNumber versionNumber = VersionNumber.parse(version)
            runner.expectLegacyDeprecationWarningIf(versionNumber >= VersionNumber.parse('1.4.0'),
                "The `kotlin2js` Gradle plugin has been deprecated."
            )
        }

        void expectKotlinParallelTasksDeprecation(String version) {
            VersionNumber versionNumber = VersionNumber.parse(version)
            runner.expectLegacyDeprecationWarningIf(
                versionNumber >= VersionNumber.parse('1.5.20') && versionNumber <= VersionNumber.parse('1.6.10'),
                "Project property 'kotlin.parallel.tasks.in.project' is deprecated."
            )
        }

        void expectAbstractCompileDestinationDirDeprecation(String version) {
            VersionNumber versionNumber = VersionNumber.parse(version)
            runner.expectLegacyDeprecationWarningIf(
                versionNumber <= VersionNumber.parse("1.6.21"),
                "The AbstractCompile.destinationDir property has been deprecated. " +
                    "This is scheduled to be removed in Gradle 9.0. " +
                    "Please use the destinationDirectory property instead. " +
                    "Consult the upgrading guide for further information: ${DOC_REGISTRY.getDocumentationFor("upgrading_version_7", "compile_task_wiring")}"
            )
        }

        void expectOrgGradleUtilWrapUtilDeprecation(String version) {
            VersionNumber versionNumber = VersionNumber.parse(version)
            runner.expectLegacyDeprecationWarningIf(
                versionNumber < VersionNumber.parse("1.7.20"),
                "The org.gradle.util.WrapUtil type has been deprecated. " +
                    "This is scheduled to be removed in Gradle 9.0. " +
                    "Consult the upgrading guide for further information: " +
                    "${DOC_REGISTRY.getDocumentationFor("upgrading_version_7", "org_gradle_util_reports_deprecations")}"
            )
        }

        void expectTestReportReportOnDeprecation(String version) {
            VersionNumber versionNumber = VersionNumber.parse(version)
            runner.expectLegacyDeprecationWarningIf(
                versionNumber.baseVersion < VersionNumber.parse("1.8.20"),
                "The TestReport.reportOn(Object...) method has been deprecated. " +
                    "This is scheduled to be removed in Gradle 9.0. " +
                    "Please use the testResults method instead. " +
                    String.format(RECOMMENDATION,"information",  DOC_REGISTRY.getDslRefForProperty("org.gradle.api.tasks.testing.TestReport", "testResults"))
            )
        }

        void expectTestReportDestinationDirOnDeprecation(String version) {
            VersionNumber versionNumber = VersionNumber.parse(version)
            runner.expectLegacyDeprecationWarningIf(
                versionNumber.baseVersion < VersionNumber.parse("1.8.20"),
                "The TestReport.destinationDir property has been deprecated. " +
                    "This is scheduled to be removed in Gradle 9.0. " +
                    "Please use the destinationDirectory property instead. " +
                    String.format(RECOMMENDATION, "information", DOC_REGISTRY.getDslRefForProperty("org.gradle.api.tasks.testing.TestReport", "destinationDir"))
            )
        }

        void expectProjectConventionDeprecation(String kotlinVersion) {
            VersionNumber kotlinVersionNumber = VersionNumber.parse(kotlinVersion)
            runner.expectLegacyDeprecationWarningIf(
                kotlinVersionNumber < VersionNumber.parse("1.7.22"),
                PROJECT_CONVENTION_DEPRECATION
            )
        }

        void expectBasePluginConventionDeprecation(String kotlinVersion) {
            VersionNumber kotlinVersionNumber = VersionNumber.parse(kotlinVersion)
            runner.expectLegacyDeprecationWarningIf(
                kotlinVersionNumber < VersionNumber.parse("1.7.0"),
                BASE_PLUGIN_CONVENTION_DEPRECATION
            )
        }

        void expectBasePluginConventionDeprecation(String kotlinVersion, String agpVersion) {
            VersionNumber kotlinVersionNumber = VersionNumber.parse(kotlinVersion)
            VersionNumber agpVersionNumber = VersionNumber.parse(agpVersion)
            runner.expectLegacyDeprecationWarningIf(
                agpVersionNumber < VersionNumber.parse("7.4.0") || kotlinVersionNumber < VersionNumber.parse("1.7.0"),
                BASE_PLUGIN_CONVENTION_DEPRECATION
            )
        }

        void expectJavaPluginConventionDeprecation(String kotlinVersion) {
            VersionNumber kotlinVersionNumber = VersionNumber.parse(kotlinVersion)
            runner.expectLegacyDeprecationWarningIf(
                kotlinVersionNumber < VersionNumber.parse("1.7.22"),
                JAVA_PLUGIN_CONVENTION_DEPRECATION
            )
        }

        void expectProjectConventionDeprecation(String kotlinVersion, String agpVersion) {
            VersionNumber kotlinVersionNumber = VersionNumber.parse(kotlinVersion)
            VersionNumber agpVersionNumber = VersionNumber.parse(agpVersion)
            runner.expectLegacyDeprecationWarningIf(
                agpVersionNumber < VersionNumber.parse("7.4.0") || (agpVersionNumber >= VersionNumber.parse("7.4.0") && kotlinVersionNumber < VersionNumber.parse("1.7.0")),
                PROJECT_CONVENTION_DEPRECATION
            )
        }

        void expectConventionTypeDeprecation(String kotlinVersion) {
            VersionNumber kotlinVersionNumber = VersionNumber.parse(kotlinVersion)
            runner.expectLegacyDeprecationWarningIf(
                kotlinVersionNumber < VersionNumber.parse("1.7.22"),
                CONVENTION_TYPE_DEPRECATION
            )
        }

        void expectConventionTypeDeprecation(String kotlinVersion, String agpVersion) {
            VersionNumber kotlinVersionNumber = VersionNumber.parse(kotlinVersion)
            VersionNumber agpVersionNumber = VersionNumber.parse(agpVersion)
            runner.expectLegacyDeprecationWarningIf(
                agpVersionNumber < VersionNumber.parse("7.4.0") || (agpVersionNumber >= VersionNumber.parse("7.4.0") && kotlinVersionNumber < VersionNumber.parse("1.7.22")),
                CONVENTION_TYPE_DEPRECATION
            )
        }

        void expectConfigureUtilDeprecation(String version) {
            VersionNumber versionNumber = VersionNumber.parse(version)
            runner.expectLegacyDeprecationWarningIf(
                versionNumber < VersionNumber.parse("1.7.22"),
                "The org.gradle.util.ConfigureUtil type has been deprecated. " +
                    "This is scheduled to be removed in Gradle 9.0. " +
                    "Consult the upgrading guide for further information: " +
                    DOC_REGISTRY.getDocumentationFor("upgrading_version_8", "org_gradle_util_reports_deprecations")
            )
        }

        void expectBuildIdentifierNameDeprecation(String kotlinVersion) {
            VersionNumber versionNumber = VersionNumber.parse(kotlinVersion)
            runner.expectDeprecationWarningIf(versionNumber >= VersionNumber.parse("1.8.20"),
                "The BuildIdentifier.getName() method has been deprecated. " +
                    "This is scheduled to be removed in Gradle 9.0. " +
                    "Use getBuildPath() to get a unique identifier for the build. " +
                    "Consult the upgrading guide for further information: https://docs.gradle.org/${GradleVersion.current().version}/userguide/upgrading_version_8.html#build_identifier_name_and_current_deprecation",
                "https://youtrack.jetbrains.com/issue/KT-58157"
            )
        }
    }

    protected SmokeTestGradleRunner runner(boolean workers, VersionNumber kotlinVersion, String... tasks) {
        return runnerFor(this, workers, kotlinVersion, tasks)
    }

    protected SmokeTestGradleRunner runnerFor(AbstractSmokeTest smokeTest, boolean workers, String... tasks) {
        smokeTest.runner(tasks + ["--parallel", "-Pkotlin.parallel.tasks.in.project=$workers"] as String[])
            .forwardOutput()
    }

    protected SmokeTestGradleRunner runnerFor(AbstractSmokeTest smokeTest, boolean workers, VersionNumber kotlinVersion, String... tasks) {
        if (kotlinVersion.getMinor() < 5 && JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_16)) {
            String kotlinOpts = "-Dkotlin.daemon.jvm.options=--add-exports=java.base/sun.nio.ch=ALL-UNNAMED,--add-opens=java.base/java.util=ALL-UNNAMED"
            return runnerFor(smokeTest, workers, tasks + [kotlinOpts] as String[])
        }
        runnerFor(smokeTest, workers, tasks)
    }
}
