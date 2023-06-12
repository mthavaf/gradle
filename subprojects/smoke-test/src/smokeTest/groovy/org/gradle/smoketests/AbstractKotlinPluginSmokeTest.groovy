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
import org.gradle.internal.reflect.validation.ValidationMessageChecker
import org.gradle.util.internal.VersionNumber

abstract class AbstractKotlinPluginSmokeTest extends AbstractPluginValidatingSmokeTest implements ValidationMessageChecker {
    private static final String PARALLEL_TASKS_IN_PROJECT_PROPERTY = 'kotlin.parallel.tasks.in.project'

    protected SmokeTestGradleRunner runner(ParallelTasksInProject parallelTasksInProject, VersionNumber kotlinVersion, String... tasks) {
        return runnerFor(this, parallelTasksInProject, kotlinVersion, tasks)
    }

    protected static SmokeTestGradleRunner runnerFor(AbstractSmokeTest smokeTest, ParallelTasksInProject parallelTasksInProject, String... tasks) {
        def args = ['--parallel']
        switch (parallelTasksInProject) {
            case ParallelTasksInProject.TRUE: {
                args += ["-P$PARALLEL_TASKS_IN_PROJECT_PROPERTY=true"]
                break
            }
            case ParallelTasksInProject.FALSE: {
                args += ["-P$PARALLEL_TASKS_IN_PROJECT_PROPERTY=false"]
                break
            }
        }

        smokeTest.runner(tasks + args as String[])
            .forwardOutput()
    }

    protected static SmokeTestGradleRunner runnerFor(AbstractSmokeTest smokeTest, ParallelTasksInProject parallelTasksInProject, VersionNumber kotlinVersion, String... tasks) {
        if (kotlinVersion.getMinor() < 5 && JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_16)) {
            String kotlinOpts = "-Dkotlin.daemon.jvm.options=--add-exports=java.base/sun.nio.ch=ALL-UNNAMED,--add-opens=java.base/java.util=ALL-UNNAMED"
            return runnerFor(smokeTest, parallelTasksInProject, tasks + [kotlinOpts] as String[])
        }
        runnerFor(smokeTest, parallelTasksInProject, tasks)
    }

    /**
     * Controls if and how to set the {@code #PARALLEL_TASKS_IN_PROJECT_PROPERTY} property.
     */
    protected static enum ParallelTasksInProject {
        TRUE,
        FALSE,
        OMIT;

        boolean isPropertyPresent() {
            return this != OMIT
        }
    }

    protected static class KotlinDeprecations extends BaseDeprecations implements WithKotlinDeprecations {
        KotlinDeprecations(SmokeTestGradleRunner runner) {
            super(runner)
        }
    }
}
