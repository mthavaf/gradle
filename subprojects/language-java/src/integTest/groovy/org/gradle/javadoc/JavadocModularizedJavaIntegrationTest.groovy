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

package org.gradle.javadoc

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.test.fixtures.file.TestFile
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition
import spock.lang.Issue

@Requires(TestPrecondition.JDK9_OR_LATER)
class JavadocModularizedJavaIntegrationTest extends AbstractIntegrationSpec {

    TestFile testBuildFile

    def setup() {
        file('test/src/main/java/module-info.java') << """
            module test {
                exports test;
            }
        """
        file("test/src/main/java/test/Test.java") << """
            package test;

            import test.internal.TestInternal;

            public class Test {
                public void doSomething() {
                    TestInternal.doSomething();
                }
            }
        """
        file("test/src/main/java/test/internal/TestInternal.java") << """
            package test.internal;

            public class TestInternal {
                public static void doSomething() { }
            }
        """

        settingsFile << """
            include 'test'
"""
        testBuildFile = testDirectory.file('test/build.gradle')
    }

    @Issue("https://github.com/gradle/gradle/issues/19726")
    def "can build javadoc from modularized java"() {
        testBuildFile << """
            apply plugin: 'java-library'

            java {
                withJavadocJar()
            }
        """

        expect:
        succeeds("javadoc")
    }

    @Issue("https://github.com/gradle/gradle/issues/19726")
    def "can build javadoc from modularized java with exclusions"() {
        testBuildFile << """
            apply plugin: 'java-library'

            java {
                withJavadocJar()
            }

            tasks.withType(Javadoc) {
                exclude("test/internal")
            }
        """

        expect:
        succeeds("javadoc")
    }

    @Issue("https://github.com/gradle/gradle/issues/21399")
    def "can build javadoc from modularized java with -module-source-path specified"() {

        testBuildFile << """
            apply plugin: 'java-library'

            java {
                withJavadocJar()
            }

            // Module source path needs a folder structure where the module name appears in the hierarchy
            tasks.withType(Javadoc) {
                options.addStringOption('-module-source-path', "\$projectDir/../*/src/main/java")
            }
"""

        expect:
        succeeds('javadoc')
    }
}
