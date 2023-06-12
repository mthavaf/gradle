/*
 * Copyright 2020 the original author or authors.
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

import com.gradle.enterprise.testing.annotations.LocalOnly
import org.gradle.integtests.fixtures.android.AndroidHome
import org.gradle.integtests.fixtures.executer.GradleContextualExecuter
import org.gradle.util.internal.VersionNumber
import org.gradle.smoketests.AbstractKotlinPluginSmokeTest.ParallelTasksInProject

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@LocalOnly
class KotlinPluginAndroidGroovyDSLSmokeTest extends AbstractSmokeTest {

    def "kotlin android on android-kotlin-example (kotlin=#kotlinPluginVersion, agp=#androidPluginVersion, workers=#parallelTasksInProject)"(String kotlinPluginVersion, String androidPluginVersion, boolean workers) {
        given:
        AndroidHome.assertIsSet()
        AGP_VERSIONS.assumeAgpSupportsCurrentJavaVersionAndKotlinVersion(androidPluginVersion, kotlinPluginVersion)
        useSample("android-kotlin-example")

        def buildFileName = "build.gradle"
        [buildFileName, "app/$buildFileName"].each { sampleBuildFileName ->
            replaceVariablesInFile(
                file(sampleBuildFileName),
                kotlinVersion: kotlinPluginVersion,
                androidPluginVersion: androidPluginVersion,
                androidBuildToolsVersion: TestedVersions.androidTools)
        }
        def kotlinPluginVersionNumber = VersionNumber.parse(kotlinPluginVersion)
        def androidPluginVersionNumber = VersionNumber.parse(androidPluginVersion)

        when:
        def runner = createRunner(parallelTasksInProject, kotlinPluginVersionNumber, androidPluginVersionNumber, 'clean', ":app:testDebugUnitTestCoverage")
        def result = useAgpVersion(androidPluginVersion, runner)
            .deprecations(KotlinAndroidDeprecations) {
                expectKotlinConfigurationAsDependencyDeprecation(kotlinPluginVersionNumber)
                expectAndroidOrKotlinWorkerSubmitDeprecation(androidPluginVersionNumber, parallelTasksInProject, kotlinPluginVersionNumber)
                expectReportDestinationPropertyDeprecation(androidPluginVersion)
                expectKotlinCompileDestinationDirPropertyDeprecation(kotlinPluginVersionNumber)
                if (GradleContextualExecuter.configCache || VersionNumber.parse(kotlinPluginVersionNumber) >= VersionNumber.parse("1.8.0")) {
                    expectBuildIdentifierIsCurrentBuildDeprecation(androidPluginVersion)
                }
            }.build()

        then:
        result.task(':app:testDebugUnitTestCoverage').outcome == SUCCESS

        where:
// To run a specific combination, set the values here, uncomment the following four lines
//  and comment out the lines coming after
//        kotlinPluginVersion = TestedVersions.kotlin.versions.last()
//        androidPluginVersion = TestedVersions.androidGradle.versions.last()
//        workers = false

        [kotlinPluginVersion, androidPluginVersion, parallelTasksInProject] << [
            TestedVersions.kotlin.versions,
            TestedVersions.androidGradle.versions,
            ParallelTasksInProject.values(),
        ].combinations()
    }

    private SmokeTestGradleRunner createRunner(ParallelTasksInProject parallelTasksInProject, VersionNumber kotlinPluginVersionNumber, String androidPluginVersion, String... tasks) {
        return KotlinPluginSmokeTest.runnerFor(this, parallelTasksInProject, kotlinPluginVersionNumber)
            .deprecations(KotlinPluginSmokeTest.KotlinDeprecations) {
                expectOrgGradleUtilWrapUtilDeprecation(kotlinPluginVersionNumber)
                expectBasePluginConventionDeprecation(kotlinPluginVersionNumber, androidPluginVersion)
                expectProjectConventionDeprecation(kotlinPluginVersionNumber, VersionNumber.parse(androidPluginVersion))
                expectConventionTypeDeprecation(kotlinPluginVersionNumber, androidPluginVersion)
            }
    }
}
