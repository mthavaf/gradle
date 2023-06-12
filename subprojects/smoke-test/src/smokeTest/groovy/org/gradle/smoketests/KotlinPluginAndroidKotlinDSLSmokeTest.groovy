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
import org.gradle.smoketests.AbstractKotlinPluginSmokeTest.ParallelTasksInProject
import org.gradle.util.internal.VersionNumber

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@LocalOnly(because = "Needs Android environment")
class KotlinPluginAndroidKotlinDSLSmokeTest extends AbstractSmokeTest {

    def "kotlin android on android-kotlin-example-kotlin-dsl (kotlin=#kotlinPluginVersion, agp=#androidPluginVersion, workers=#parallelTasksInProject)"(String kotlinPluginVersion, String androidPluginVersion, ParallelTasksInProject parallelTasksInProject) {
        given:
        AndroidHome.assertIsSet()
        AGP_VERSIONS.assumeAgpSupportsCurrentJavaVersionAndKotlinVersion(androidPluginVersion, kotlinPluginVersion)
        useSample("android-kotlin-example-kotlin-dsl")

        def buildFileName = "build.gradle.kts"
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
        def runner = createRunner(parallelTasksInProject, kotlinPluginVersionNumber, androidPluginVersionNumber, 'clean', ':app:testDebugUnitTestCoverage')
        def result = useAgpVersion(androidPluginVersion, runner)
            .deprecations(KotlinAndroidDeprecations) {
                expectKotlinConfigurationAsDependencyDeprecation(kotlinPluginVersion)
                expectAndroidOrKotlinWorkerSubmitDeprecation(androidPluginVersionNumber, parallelTasksInProject, kotlinPluginVersionNumber)
                expectReportDestinationPropertyDeprecation(androidPluginVersion)
                expectKotlinCompileDestinationDirPropertyDeprecation(kotlinPluginVersionNumber)
                if (GradleContextualExecuter.isConfigCache() || kotlinPluginVersionNumber >= VersionNumber.parse("1.8.0")) {
                    expectBuildIdentifierIsCurrentBuildDeprecation(androidPluginVersion)
                }
            }.build()

        then:
        result.task(':app:testDebugUnitTestCoverage').outcome == SUCCESS

        where:
// To run a specific combination, set the values here, uncomment the following four lines
//  and comment out the lines coming after
//        kotlinPluginVersion = TestedVersions.kotlin.latestStable()
//        androidPluginVersion = TestedVersions.androidGradle.latestStable()
//        parallelTasksInProject = ParallelTasksInProject.FALSE

        [kotlinPluginVersion, androidPluginVersion, parallelTasksInProject] << [
            TestedVersions.kotlin.versions,
            TestedVersions.androidGradle.versions,
            ParallelTasksInProject.values(),
        ].combinations()
    }

    private SmokeTestGradleRunner createRunner(ParallelTasksInProject parallelTasksInProject, VersionNumber kotlinVersionNumber, VersionNumber agpVersionNumber, String... tasks) {
        return KotlinPluginSmokeTest.runnerFor(this, parallelTasksInProject, kotlinVersionNumber, tasks)
            .deprecations(KotlinPluginSmokeTest.KotlinDeprecations) {
                expectOrgGradleUtilWrapUtilDeprecation(kotlinVersionNumber)
                expectBasePluginConventionDeprecation(kotlinVersionNumber, agpVersionNumber)
                expectProjectConventionDeprecation(kotlinVersionNumber, agpVersionNumber)
                expectConventionTypeDeprecation(kotlinVersionNumber, agpVersionNumber)
            }
    }
}
