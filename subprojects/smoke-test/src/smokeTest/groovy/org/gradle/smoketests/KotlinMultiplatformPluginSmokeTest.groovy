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

import org.gradle.api.internal.DocumentationRegistry
import org.gradle.util.internal.VersionNumber
import spock.lang.Issue

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class KotlinMultiplatformPluginSmokeTest extends AbstractKotlinPluginSmokeTest {

    /**
     * This tests that the usage of deprecated methods in {@code org.gradle.api.tasks.testing.TestReport} task
     * is okay, and ensures the methods are not removed until the versions of the kotlin plugin that uses them
     * is no longer tested.
     *
     * See usage here: https://cs.android.com/android-studio/kotlin/+/master:libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/testing/internal/KotlinTestReport.kt;l=136?q=KotlinTestReport.kt:136&ss=android-studio
     */
    @Issue("https://github.com/gradle/gradle/issues/22246")
    def 'ensure kotlin multiplatform allTests aggregation task can be created (kotlin=#kotlinVersion)'() {
        given:
        buildFile << """
            plugins {
                id 'org.jetbrains.kotlin.multiplatform' version '$kotlinVersion'
            }

            ${mavenCentralRepository()}

            kotlin {
                jvm()
            }
        """

        when:
        def result = runner(false, VersionNumber.parse(kotlinVersion), ':tasks')
                .deprecations(KotlinDeprecations) {
                    expectOrgGradleUtilWrapUtilDeprecation(kotlinVersion)
                    expectTestReportReportOnDeprecation(kotlinVersion)
                    expectTestReportDestinationDirOnDeprecation(kotlinVersion)
                    expectProjectConventionDeprecation(kotlinVersion)
                    expectConventionTypeDeprecation(kotlinVersion)
                    expectJavaPluginConventionDeprecation(kotlinVersion)
                    expectBuildIdentifierNameDeprecation(kotlinVersion)
                }
                .build()

        then:
        result.task(':tasks').outcome == SUCCESS
        result.output.contains('allTests - Runs the tests for all targets and create aggregated report')

        where:
        kotlinVersion << TestedVersions.kotlin.versions
    }

    @Issue("https://github.com/gradle/gradle/issues/22952")
    def "kotlin project can consume kotlin multiplatform java project"() {
        given:
        buildFile << """
            plugins {
                id 'org.jetbrains.kotlin.jvm' version '$kotlinVersion'
            }

            ${mavenCentralRepository()}

            dependencies {
                implementation project(":other")
            }

            task resolve {
                def files = configurations.compileClasspath
                doLast {
                    println("Files: " + files.files)
                }
            }
        """

        settingsFile << "include 'other'"
        file("other/build.gradle") << """
            plugins {
                id 'org.jetbrains.kotlin.multiplatform'
            }

            ${mavenCentralRepository()}

            kotlin {
                jvm {
                    withJava()
                }
            }
        """

        when:
        def versionNumber = VersionNumber.parse(kotlinVersion)
        def testRunner = runner(false, versionNumber, ':resolve', '--stacktrace')

        if (versionNumber < VersionNumber.parse('1.7.22')) {
            testRunner.expectLegacyDeprecationWarning("The AbstractCompile.destinationDir property has been deprecated. This is scheduled to be removed in Gradle 9.0. " +
                    "Please use the destinationDirectory property instead. " +
                    "Consult the upgrading guide for further information: ${new DocumentationRegistry().getDocumentationFor("upgrading_version_7", "compile_task_wiring")}")
        }

        testRunner.deprecations(KotlinDeprecations) {
            expectOrgGradleUtilWrapUtilDeprecation(kotlinVersion)
            2.times {
                expectProjectConventionDeprecation(kotlinVersion)
                expectConventionTypeDeprecation(kotlinVersion)
                expectJavaPluginConventionDeprecation(kotlinVersion)
            }
            expectConfigureUtilDeprecation(kotlinVersion)
            expectBuildIdentifierNameDeprecation(kotlinVersion)
        }

        def result = testRunner.build()

        then:
        result.output.contains("other-jvm.jar")

        where:
        // withJava is incompatible pre 1.6.20 since it attempts to set the `archiveName` convention property on the Jar task.
        kotlinVersion << TestedVersions.kotlin.versions.findAll { VersionNumber.parse(it) > VersionNumber.parse("1.6.10") }
    }

    @Override
    Map<String, Versions> getPluginsToValidate() {
        [
                'org.jetbrains.kotlin.multiplatform': TestedVersions.kotlin
        ]
    }

    @Override
    void configureValidation(String testedPluginId, String version) {
        validatePlugins {
            alwaysPasses()
            if (testedPluginId == 'org.jetbrains.kotlin.multiplatform') {
                buildFile << """
                    kotlin {
                        jvm()
                        js(IR) { browser() }
                    }
                """
            }
            settingsFile << """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                        google()
                    }
                }
            """
        }
    }
}
