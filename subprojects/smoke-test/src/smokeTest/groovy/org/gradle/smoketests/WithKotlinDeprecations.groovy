/*
 * Copyright 2022 the original author or authors.
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

import groovy.transform.SelfType
import org.gradle.smoketests.AbstractKotlinPluginSmokeTest.ParallelTasksInProject
import org.gradle.util.GradleVersion
import org.gradle.util.internal.VersionNumber

import static org.gradle.api.internal.DocumentationRegistry.RECOMMENDATION

@SelfType(BaseDeprecations)
trait WithKotlinDeprecations extends WithReportDeprecations {
    private static final VersionNumber KOTLIN_VERSION_USING_NEW_WORKERS_API = VersionNumber.parse('1.5.0')
    private static final VersionNumber KOTLIN_VERSION_WITH_OLD_WORKERS_API_REMOVED = VersionNumber.parse('1.6.10')

    private static final String ABSTRACT_COMPILE_DESTINATION_DIR_DEPRECATION = "The AbstractCompile.destinationDir property has been deprecated. " +
        "This is scheduled to be removed in Gradle 9.0. " +
        "Please use the destinationDirectory property instead. " +
        "Consult the upgrading guide for further information: https://docs.gradle.org/${GradleVersion.current().version}/userguide/upgrading_version_7.html#compile_task_wiring"

    private static final String ARCHIVE_NAME_DEPRECATION = "The AbstractArchiveTask.archiveName property has been deprecated. " +
        "This is scheduled to be removed in Gradle 8.0. Please use the archiveFileName property instead. " +
        String.format(RECOMMENDATION, "information", DOCUMENTATION_REGISTRY.getDslRefForProperty("org.gradle.api.tasks.bundling.AbstractArchiveTask", "archiveName"))

    boolean kotlinPluginUsesOldWorkerApi(VersionNumber kotlinPluginVersion) {
        kotlinPluginVersion >= KOTLIN_VERSION_USING_NEW_WORKERS_API && kotlinPluginVersion <= KOTLIN_VERSION_WITH_OLD_WORKERS_API_REMOVED
    }

    void expectKotlinCompileDestinationDirPropertyDeprecation(String version) {
        VersionNumber versionNumber = VersionNumber.parse(version)
        runner.expectLegacyDeprecationWarningIf(
            versionNumber >= VersionNumber.parse('1.5.20') && versionNumber <= VersionNumber.parse('1.6.21'),
            ABSTRACT_COMPILE_DESTINATION_DIR_DEPRECATION
        )
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

    void expectKotlinParallelTasksDeprecation(VersionNumber version, ParallelTasksInProject parallelTasksInProject) {
        runner.expectLegacyDeprecationWarningIf(
            parallelTasksInProject != ParallelTasksInProject.OMIT && kotlinPluginUsesOldWorkerApi(version),
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
                "Consult the upgrading guide for further information: ${DOCUMENTATION_REGISTRY.getDocumentationFor("upgrading_version_7", "compile_task_wiring")}"
        )
    }

    void expectOrgGradleUtilWrapUtilDeprecation(String version) {
        VersionNumber versionNumber = VersionNumber.parse(version)
        runner.expectLegacyDeprecationWarningIf(
            versionNumber < VersionNumber.parse("1.7.20"),
            "The org.gradle.util.WrapUtil type has been deprecated. " +
                "This is scheduled to be removed in Gradle 9.0. " +
                "Consult the upgrading guide for further information: " +
                "${DOCUMENTATION_REGISTRY.getDocumentationFor("upgrading_version_7", "org_gradle_util_reports_deprecations")}"
        )
    }

    void expectTestReportReportOnDeprecation(String version) {
        VersionNumber versionNumber = VersionNumber.parse(version)
        runner.expectLegacyDeprecationWarningIf(
            versionNumber.baseVersion < VersionNumber.parse("1.8.20"),
            "The TestReport.reportOn(Object...) method has been deprecated. " +
                "This is scheduled to be removed in Gradle 9.0. " +
                "Please use the testResults method instead. " +
                String.format(RECOMMENDATION,"information",  DOCUMENTATION_REGISTRY.getDslRefForProperty("org.gradle.api.tasks.testing.TestReport", "testResults"))
        )
    }

    void expectTestReportDestinationDirOnDeprecation(String version) {
        VersionNumber versionNumber = VersionNumber.parse(version)
        runner.expectLegacyDeprecationWarningIf(
            versionNumber.baseVersion < VersionNumber.parse("1.8.20"),
            "The TestReport.destinationDir property has been deprecated. " +
                "This is scheduled to be removed in Gradle 9.0. " +
                "Please use the destinationDirectory property instead. " +
                String.format(RECOMMENDATION, "information", DOCUMENTATION_REGISTRY.getDslRefForProperty("org.gradle.api.tasks.testing.TestReport", "destinationDir"))
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
                DOCUMENTATION_REGISTRY.getDocumentationFor("upgrading_version_8", "org_gradle_util_reports_deprecations")
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

    void expectVersionSpecificDeprecations(VersionNumber kotlinVersion) {
        expectVersionSpecificDeprecations(kotlinVersion, ParallelTasksInProject.OMIT)
    }

    void expectVersionSpecificDeprecations(VersionNumber kotlinVersion, ParallelTasksInProject parallelTasksInProject) {
        expectKotlinParallelTasksDeprecation(kotlinVersion, parallelTasksInProject)
        if (kotlinVersion <= VersionNumber.parse("1.7.0")) {
            expectOrgGradleUtilWrapUtilDeprecation(kotlinVersion.toString())
            expectConventionTypeDeprecation(kotlinVersion.toString())
            expectJavaPluginConventionDeprecation(kotlinVersion.toString())
            expectAbstractCompileDestinationDirDeprecation(kotlinVersion.toString())
        }
        if (kotlinVersion <= VersionNumber.parse("1.7.22")) {
            expectProjectConventionDeprecation(kotlinVersion.toString())
        }
        if (kotlinVersion <= VersionNumber.parse("1.8.0")) {
            expectTestReportReportOnDeprecation(kotlinVersion.toString())
            expectTestReportDestinationDirOnDeprecation(kotlinVersion.toString())
        }
        if (kotlinVersion > VersionNumber.parse("1.8.0")) {
            expectBuildIdentifierNameDeprecation(kotlinVersion.toString())
        }
    }
}
