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

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.gradle.api.tasks.bundling.Jar
import org.gradle.testfixtures.ProjectBuilder

import com.github.lburgazzoli.gradle.plugin.karaf.features.model.FeatureDescriptor;

import spock.lang.Specification

/**
 * @author Luca Burgazzoli
 * @author Sergey Nekhviadovich
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
            project.version = '1.2.3'
            def features = getKarafFeaturesExtension(project).features
            def task = getKarafFeaturesTasks(project)
        when:
            def feature = features.create('myFeature')
            feature.name = 'karaf-features-simple-project'
            feature.description = 'feature-description'
            feature.bundle {
                match: [ group: 'com.squareup.retrofit', name: 'converter-jackson' ]
                include: false
            }

            def featuresStr = task.generateFeatures()
            def featuresXml = new XmlSlurper().parseText(featuresStr)
        then:
            featuresStr != null
            featuresXml != null

            featuresXml.feature.@name == 'karaf-features-simple-project'
            featuresXml.feature.@description == 'feature-description'
            featuresXml.feature.@version == '1.2.3'
    }
    
    def 'Test project dependencies DSL'() {
        given:
            def project = setupProjectAndDependencies()
            project.version = '1.1.1'
            def subProject = ProjectBuilder.builder().withName('sub1').withParent(project).build()
            subProject.group = 'test.pkg'
            subProject.version = '1.2.3'
            subProject.configurations {
                runtime
            }
            subProject.configure([subProject]) {
                tasks.create(name: 'jar', type: Jar){}
            }
            GroovyMock(BundleDefinitionCalculatorMvnImpl, global: true)
            
            BundleDefinitionCalculatorMvnImpl.hasOsgiManifestHeaders() >> true
            BundleDefinitionCalculatorMvnImpl.collectDependencies(_, _, _, _, _, _) >> {feature, orderedDependencyMap, resolvedArtifactMap, configuration, extension, includeRoot ->
                def mv = new DefaultModuleVersionIdentifier(subProject.group, subProject.name, subProject.version)
                def result = Mock(ResolvedComponentResult);
                result.getModuleVersion() >> mv
                orderedDependencyMap.put(mv, result)
            }
            BundleDefinitionCalculatorMvnImpl.baseMvnUrl(_) >> 'mvn:test.pkg/sub1/1.2.3'
            
            def features = getKarafFeaturesExtension(project).features
            def task = getKarafFeaturesTasks(project)
        when:
            
            def feature = features.create('myFeature')
            feature.name = 'karaf-features-project-dependencies'
            feature.bundle {
                match: [ group: 'com.squareup.retrofit', name: 'converter-jackson' ]
                include: false
            }
            feature.project(':sub1') {
                dependencies {
                    transitive = false
                }
            }

            def featuresStr = task.generateFeatures()
            def featuresXml = new XmlSlurper().parseText(featuresStr)
        then:
            featuresStr != null
            featuresXml != null

            feature.getProjectDescriptors().size() == 1
            def pd = feature.getProjectDescriptors()[0];
            pd.project.name == 'sub1'
            pd.dependencies.transitive == false

            featuresXml.feature.@name == 'karaf-features-project-dependencies'
            featuresXml.feature.bundle.'**'.findAll { it.text().contains('mvn:test.pkg/sub1/1.2.3')}.size() == 1
    }

    // *************************************************************************
    //
    // *************************************************************************

    def setupProjectAndDependencies() {
        def project = ProjectBuilder.builder().build()
        setupProject(project)
        setupProjectDependencies(project)

        return project
    }

    def setupProject(project) {
        new KarafFeaturesPlugin().apply(project)
        project.apply plugin: 'java'
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
