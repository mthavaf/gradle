/*
 * Copyright 2016 the original author or authors.
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
import org.gradle.integtests.fixtures.executer.GradleContextualExecuter
import org.gradle.util.internal.VersionNumber

import static org.gradle.internal.reflect.validation.Severity.WARNING
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import static org.junit.Assume.assumeFalse
import static org.junit.Assume.assumeTrue

class KotlinPluginSmokeTest extends AbstractKotlinPluginSmokeTest {

    def 'kotlin jvm (kotlin=#version, workers=#parallelTasksInProject)'() {
        given:
        useSample("kotlin-example")
        replaceVariablesInBuildFile(kotlinVersion: version)
        def versionNumber = VersionNumber.parse(version)

        when:
        def result = runner(parallelTasksInProject, parallelTasksInProject, 'run')
            .deprecations(KotlinDeprecations) {
                expectKotlinParallelTasksDeprecation(versionNumber, parallelTasksInProject)
                expectKotlinArchiveNameDeprecation(versionNumber)
                expectAbstractCompileDestinationDirDeprecation(versionNumber)
                expectOrgGradleUtilWrapUtilDeprecation(versionNumber)
                expectProjectConventionDeprecation(versionNumber)
                expectConventionTypeDeprecation(versionNumber)
                expectJavaPluginConventionDeprecation(versionNumber)
                if (GradleContextualExecuter.isConfigCache()) {
                    expectBasePluginConventionDeprecation(versionNumber)
                }
            }.build()

        then:
        result.task(':compileKotlin').outcome == SUCCESS
        assert result.output.contains("Hello world!")

        when:
        result = runner(parallelTasksInProject, versionNumber, 'run')
            .deprecations(KotlinDeprecations) {
                if (GradleContextualExecuter.isNotConfigCache()) {
                    expectOrgGradleUtilWrapUtilDeprecation(versionNumber)
                    expectProjectConventionDeprecation(versionNumber)
                    expectConventionTypeDeprecation(versionNumber)
                    expectJavaPluginConventionDeprecation(versionNumber)
                }
            }.build()

        then:
        result.task(':compileKotlin').outcome == UP_TO_DATE
        assert result.output.contains("Hello world!")

        where:
        [version, parallelTasksInProject] << [
            TestedVersions.kotlin.versions,
            ParallelTasksInProject.values()
        ].combinations()
    }

    def 'kotlin javascript (kotlin=#version, workers=#parallelTasksInProject)'() {

        // kotlinjs has been removed in Kotlin 1.7 in favor of kotlin-mpp
        assumeTrue(VersionNumber.parse(version).baseVersion < VersionNumber.version(1, 7))

        given:
        useSample("kotlin-js-sample")
        withKotlinBuildFile()
        replaceVariablesInBuildFile(kotlinVersion: version)
        def versionNumber = VersionNumber.parse(version)

        when:
        def result = runner(parallelTasksInProject, versionNumber, 'compileKotlin2Js')
            .deprecations(KotlinDeprecations) {
                expectKotlinParallelTasksDeprecation(versionNumber, parallelTasksInProject)
                expectKotlin2JsPluginDeprecation(versionNumber)
                expectKotlinCompileDestinationDirPropertyDeprecation(versionNumber)
                expectKotlinArchiveNameDeprecation(versionNumber)
                expectOrgGradleUtilWrapUtilDeprecation(versionNumber)
                expectProjectConventionDeprecation(versionNumber)
                expectConventionTypeDeprecation(versionNumber)
                expectJavaPluginConventionDeprecation(versionNumber)
                expectBasePluginConventionDeprecation(versionNumber)
            }.build()

        then:
        result.task(':compileKotlin2Js').outcome == SUCCESS

        where:
        [version, parallelTasksInProject] << [
            TestedVersions.kotlin.versions,
            ParallelTasksInProject.values()
        ].combinations()
    }

    def 'kotlin jvm and groovy plugins combined (kotlin=#kotlinVersion)'() {

        def versionNumber = VersionNumber.parse(kotlinVersion)
        def kotlinCompileClasspathPropertyName = versionNumber >= VersionNumber.parse("1.7.0") ? 'libraries' : 'classpath'

        given:
        buildFile << """
            plugins {
                id 'groovy'
                id 'org.jetbrains.kotlin.jvm' version '$kotlinVersion'
            }

            repositories {
                mavenCentral()
            }

            tasks.named('compileGroovy') {
                classpath = sourceSets.main.compileClasspath
            }
            tasks.named('compileKotlin') {
                ${kotlinCompileClasspathPropertyName}.from(files(sourceSets.main.groovy.classesDirectory))
            }

            dependencies {
                implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
                implementation localGroovy()
            }
        """
        file("src/main/groovy/Groovy.groovy") << "class Groovy { }"
        file("src/main/kotlin/Kotlin.kt") << "class Kotlin { val groovy = Groovy() }"
        file("src/main/java/Java.java") << "class Java { private Kotlin kotlin = new Kotlin(); }" // dependency to compileJava->compileKotlin is added by Kotlin plugin
        def parallelTasksInProject = ParallelTasksInProject.FALSE

        when:
        def result = runner(parallelTasksInProject, versionNumber, 'compileJava')
            .deprecations(KotlinDeprecations) {
                expectKotlinParallelTasksDeprecation(versionNumber, parallelTasksInProject)
                expectKotlinArchiveNameDeprecation(versionNumber)
                expectAbstractCompileDestinationDirDeprecation(versionNumber)
                expectOrgGradleUtilWrapUtilDeprecation(versionNumber)
                expectProjectConventionDeprecation(versionNumber)
                expectConventionTypeDeprecation(versionNumber)
                expectJavaPluginConventionDeprecation(versionNumber)
                if (GradleContextualExecuter.isConfigCache()) {
                    expectBasePluginConventionDeprecation(versionNumber)
                }
            }.build()

        then:
        result.task(':compileJava').outcome == SUCCESS
        result.tasks.collect { it.path } == [':compileGroovy', ':compileKotlin', ':compileJava']

        where:
        kotlinVersion << TestedVersions.kotlin.versions
    }

    def 'kotlin jvm and java-gradle-plugin plugins combined (kotlin=#kotlinVersion)'() {

        assumeFalse(kotlinVersion.startsWith("1.3."))
        assumeFalse(kotlinVersion.startsWith("1.4."))
        assumeFalse(kotlinVersion.startsWith("1.5."))
        assumeFalse(kotlinVersion.startsWith("1.6."))

        given:
        buildFile << """
            plugins {
                id 'java-gradle-plugin'
                id 'org.jetbrains.kotlin.jvm' version '$kotlinVersion'
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
            }
        """
        file("src/main/kotlin/Kotlin.kt") << "class Kotlin { }"
        def versionNumber = VersionNumber.parse(kotlinVersion)

        when:
        def result = runner(ParallelTasksInProject.FALSE, versionNumber, 'build')
            .deprecations(KotlinDeprecations) {
                expectAbstractCompileDestinationDirDeprecation(versionNumber)
                expectOrgGradleUtilWrapUtilDeprecation(versionNumber)
                expectProjectConventionDeprecation(versionNumber)
                expectConventionTypeDeprecation(versionNumber)
                expectJavaPluginConventionDeprecation(versionNumber)
            }.build()

        then:
        result.task(':compileKotlin').outcome == SUCCESS

        when:
        result = runner(ParallelTasksInProject.FALSE, versionNumber, 'build')
            .deprecations(KotlinDeprecations) {
                if (GradleContextualExecuter.isNotConfigCache()) {
                    expectOrgGradleUtilWrapUtilDeprecation(versionNumber)
                    expectProjectConventionDeprecation(versionNumber)
                    expectConventionTypeDeprecation(versionNumber)
                    expectJavaPluginConventionDeprecation(versionNumber)
                }
            }.build()

        then:
        result.task(':compileKotlin').outcome == UP_TO_DATE

        where:
        kotlinVersion << TestedVersions.kotlin.versions
    }


    @Override
    Map<String, Versions> getPluginsToValidate() {
        [
            'org.jetbrains.kotlin.jvm': TestedVersions.kotlin,
            'org.jetbrains.kotlin.js': TestedVersions.kotlin,
            'org.jetbrains.kotlin.android': TestedVersions.kotlin,
            'org.jetbrains.kotlin.android.extensions': TestedVersions.kotlin,
            'org.jetbrains.kotlin.kapt': TestedVersions.kotlin,
            'org.jetbrains.kotlin.plugin.scripting': TestedVersions.kotlin,
            'org.jetbrains.kotlin.native.cocoapods': TestedVersions.kotlin,
        ]
    }

    @Override
    Map<String, String> getExtraPluginsRequiredForValidation(String testedPluginId, String version) {
        def androidVersion = AGP_VERSIONS.latestStable
        if (testedPluginId in ['org.jetbrains.kotlin.kapt', 'org.jetbrains.kotlin.plugin.scripting']) {
            return ['org.jetbrains.kotlin.jvm': version]
        }
        if (isAndroidKotlinPlugin(testedPluginId)) {
            AGP_VERSIONS.assumeAgpSupportsCurrentJavaVersionAndKotlinVersion(androidVersion, version)
            def extraPlugins = ['com.android.application': androidVersion]
            if (testedPluginId == 'org.jetbrains.kotlin.android.extensions') {
                extraPlugins.put('org.jetbrains.kotlin.android', version)
            }
            return extraPlugins
        }
        return [:]
    }

    @Override
    void configureValidation(String testedPluginId, String version) {
        validatePlugins {
            if (isAndroidKotlinPlugin(testedPluginId)) {
                buildFile << """
                    android {
                        namespace = "org.gradle.smoke.test"
                        compileSdkVersion 24
                        buildToolsVersion '${TestedVersions.androidTools}'
                    }
                """
            }
            if (testedPluginId == 'org.jetbrains.kotlin.js') {
                buildFile << """
                    kotlin { js(IR) { browser() } }
                """
            }
            if (testedPluginId == 'org.jetbrains.kotlin.multiplatform') {
                buildFile << """
                    kotlin {
                        jvm()
                        js(IR) { browser() }
                    }
                """
            }

            /*
             * Register validation failures due to unsupported nested types
             * The issue picked up by validation was fixed in Kotlin 1.7.2,
             * see https://youtrack.jetbrains.com/issue/KT-51532
             */
            if (version == '1.7.0') {
                // Register validation failure for plugin itself (or jvm plugin respectively)
                if (testedPluginId in ['org.jetbrains.kotlin.kapt', 'org.jetbrains.kotlin.plugin.scripting']) {
                    onPlugins(['org.jetbrains.kotlin.jvm']) { registerValidationFailure(delegate) }
                } else {
                    onPlugin(testedPluginId) { registerValidationFailure(delegate) }
                }
                // Register validation failures for plugins brought in by this plugin
                if (testedPluginId in ['org.jetbrains.kotlin.android', 'org.jetbrains.kotlin.android.extensions']) {
                    onPlugins(['com.android.application',
                               'com.android.build.gradle.api.AndroidBasePlugin',
                               'com.android.internal.application',
                               'com.android.internal.version-check']) { alwaysPasses() }
                }
                if (testedPluginId == 'org.jetbrains.kotlin.jvm'
                    || testedPluginId == 'org.jetbrains.kotlin.multiplatform'
                    || testedPluginId == 'org.jetbrains.kotlin.kapt'
                    || testedPluginId == 'org.jetbrains.kotlin.plugin.scripting') {
                    onPlugins(['org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin',
                               'org.jetbrains.kotlin.gradle.scripting.internal.ScriptingKotlinGradleSubplugin',
                    ]) { registerValidationFailure(delegate) }
                }
                if (testedPluginId == 'org.jetbrains.kotlin.js'
                    || testedPluginId == 'org.jetbrains.kotlin.multiplatform') {
                    onPlugins(['org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin',
                               'org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolverPlugin',
                               'org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin'
                    ]) { registerValidationFailure(delegate) }
                }
                if (testedPluginId == 'org.jetbrains.kotlin.kapt') {
                    onPlugin('kotlin-kapt') { registerValidationFailure(delegate) }
                }
            } else {
                alwaysPasses()
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

    def registerValidationFailure(PluginValidation pluginValidation) {
        pluginValidation.failsWith(nestedTypeUnsupported {
            type('org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest')
                .property('environment')
                .annotatedType('java.lang.String')
                .reason('Nested types are expected to either declare some annotated properties or some behaviour that requires capturing the type as input')
                .includeLink()
        }, WARNING)
    }

    static SmokeTestGradleRunner runnerFor(AbstractSmokeTest smokeTest, boolean workers, String... tasks) {
        smokeTest.runner(tasks + ["--parallel", "-Pkotlin.parallel.tasks.in.project=$workers"] as String[])
            .forwardOutput()
    }

    static SmokeTestGradleRunner runnerFor(AbstractSmokeTest smokeTest, boolean workers, VersionNumber kotlinVersion, String... tasks) {
        if (kotlinVersion.getMinor() < 5 && JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_16)) {
            String kotlinOpts = "-Dkotlin.daemon.jvm.options=--add-exports=java.base/sun.nio.ch=ALL-UNNAMED,--add-opens=java.base/java.util=ALL-UNNAMED"
            return runnerFor(smokeTest, workers, tasks + [kotlinOpts] as String[])
        }
        runnerFor(smokeTest, workers, tasks)
    }

    private static boolean isAndroidKotlinPlugin(String pluginId) {
        return pluginId.contains('android')
    }
}
