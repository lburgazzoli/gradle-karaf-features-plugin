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

/**
 * @author Luca Burgazzoli
 */
class KarafFeaturesSpec extends Specification {

    def 'Apply plugin'() {
        given:
            def project = ProjectBuilder.builder().build()
        when:
            setupProject(project)
        then:
            getKarafFeaturesExtension(project) instanceof KarafFeaturesTaskExtension
            getKarafFeaturesTasks(project) instanceof KarafFeaturesTask
    }

    def 'Simple project'() {
        given:
            def project = setupProjectAndDependencies()
            def features = getKarafFeaturesExtension(project).features
            def task = getKarafFeaturesTasks(project)
        when:
            def feature = features.create('myFeature')
            //feature.name = 'karaf-features-simple-project'
            //feature.bundle {
            //}

            /*
            project.extensions.karafFeatures.features {

                myFeature {
                    name = 'karaf-features-simple-project'
                    bundle {
                        matcher group: 'com.squareup.retrofit', name: 'converter-jackson'
                        include: false
                    }
                }
            }
            */

            def featuresStr = task.generateFeatures()
            def featuresXml = new XmlSlurper().parseText(featuresStr)
        then:
            featuresStr != null
            featuresXml != null
    }

    // *************************************************************************
    //
    // *************************************************************************

    def setupProjectAndDependencies() {
        def project = ProjectBuilder.builder().build()
        setupProject(project)
        //setupProjectDependencies(project)

        return project
    }

    def setupProject(project) {
        project.apply plugin: KarafFeaturesPlugin.PLUGIN_ID
        //project.apply plugin: 'java'
        project.apply plugin: 'maven'

        project.repositories {
            mavenLocal()
            mavenCentral()
        }

        return project
    }

    def setupProjectDependencies(project) {
        project.dependencies {
            compile 'com.google.guava:guava:18.0'
            compile "com.squareup.retrofit:retrofit:1.9.0"
            compile "com.squareup.retrofit:converter-jackson:1.9.0"
        }
    }

    def getKarafFeaturesExtension(project) {
        project.extensions.getByName(KarafFeaturesPlugin.EXTENSION_NAME)
    }

    def getKarafFeaturesTasks(project) {
        project.tasks.getByName(KarafFeaturesPlugin.TASK_NAME)
    }
}
