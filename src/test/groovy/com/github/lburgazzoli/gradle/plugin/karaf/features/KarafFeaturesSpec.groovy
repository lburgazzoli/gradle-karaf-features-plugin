/*
 * Copyright (c) 2015, contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.lburgazzoli.gradle.plugin.karaf.features

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class KarafFeaturesSpec extends Specification {

    def 'Single project'() {
        given:
            def project = singleProject()
        when:
            def value = true
        then:
            value
    }

    def singleProject() {
        def project = new ProjectBuilder().withName('single').build()
        /*
        project.dependencies {
            compile("org.slf4j:slf4j-api:1.7.12") { transitive = false }
            compile("org.slf4j:slf4j-ext:1.7.12") { transitive = false }

            compile "org.apache.logging.log4j:log4j-api:2.3"
            compile "org.apache.logging.log4j:log4j-core:2.3"
            compile "org.apache.logging.log4j:log4j-jcl:2.3"
            compile "org.apache.logging.log4j:log4j-jul:2.3"
            compile "org.apache.logging.log4j:log4j-slf4j-impl:2.3"

            compile "com.google.guava:guava:18.0"

            compile "com.squareup.retrofit:retrofit:1.9.0"
            compile "com.squareup.retrofit:converter-jackson:1.9.0"
        }
        */
    }
}
