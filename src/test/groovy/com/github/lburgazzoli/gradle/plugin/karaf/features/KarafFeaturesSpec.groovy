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

import com.github.lburgazzoli.gradle.plugin.karaf.features.impl.BundleDefinitionCalculatorMvnImpl
import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ComponentSelectionReason
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.tasks.bundling.Jar
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * @author Luca Burgazzoli
 * @author Sergey Nekhviadovich
 */
@Slf4j
class KarafFeaturesSpec extends Specification {

    // *************************************************************************
    //
    // *************************************************************************

    def 'Apply plugin'() {
        given:
            def project = ProjectBuilder.builder().build()
        when:
            setupProject(project)
        then:
            getKarafFeaturesExtension(project) instanceof KarafFeaturesTaskExtension
            getKarafFeaturesTasks(project) instanceof KarafFeaturesTask
    }

    // *************************************************************************
    //
    // *************************************************************************

    def 'Single project'() {
        given:
            def project = setupProjectAndDependencies()
            project.version = '1.2.3'
            def features = getKarafFeaturesExtension(project).features
            def task = getKarafFeaturesTasks(project)
        when:
            def feature = features.create('myFeature')
            feature.name = 'karaf-features-simple-project'
            feature.description = 'feature-description'
            feature.bundlesFrom('myAdditionalDependencies')
            feature.bundle('com.squareup.retrofit:converter-jackson') {
                include = false
            }
            feature.bundle('org.apache.activemq:activemq-web') {
                type = 'war'
            }

            def featuresStr = task.generateFeatures()
            def featuresXml = new XmlSlurper().parseText(featuresStr)
        then:
            featuresStr != null
            featuresXml != null

            log.info("${featuresStr}")

            featuresXml.feature.@name == 'karaf-features-simple-project'
            featuresXml.feature.@description == 'feature-description'
            featuresXml.feature.@version == '1.2.3'

            featuresXml.feature.bundle.'**'.findAll {
                    it.text().contains('mvn:commons-lang/commons-lang/2.6')
                }.size() == 1
            featuresXml.feature.bundle.'**'.findAll {
                    it.text().contains('mvn:com.google.guava/guava/18.0')
                }.size() == 1
            featuresXml.feature.bundle.'**'.findAll {
                    it.text().contains('mvn:com.squareup.retrofit/retrofit/1.9.0')
                }.size() == 1
            featuresXml.feature.bundle.'**'.findAll {
                    it.text().contains('mvn:org.apache.activemq/activemq-web/5.12.1/war')
                }.size() == 1
            featuresXml.feature.bundle.'**'.findAll {
                    it.text().contains('mvn:org.apache.activemq/activemq-web-console/5.12.1/war')
                }.size() == 1
            featuresXml.feature.bundle.'**'.findAll {
                    it.text().contains('mvn:com.squareup.retrofit/converter-jackson/1.9.0')
                }.size() == 0
    }

    // *************************************************************************
    //
    // *************************************************************************

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

            BundleDefinitionCalculatorMvnImpl.hasOsgiManifestHeaders(_) >> true
            BundleDefinitionCalculatorMvnImpl.collectDependencies(_, _, _, _, _) >> {
                feature, orderedDependencyMap, configuration, extension, includeRoot ->
                    def mv = new DefaultModuleVersionIdentifier(subProject.group, subProject.name, subProject.version)
                    def result = Mock(ResolvedComponentResult)
                    result.getModuleVersion() >> mv
                    result.getSelectionReason() >> Mock(ComponentSelectionReason)
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

            log.info("${featuresStr}")

            feature.projectDescriptors.size() == 1
            def pd = feature.projectDescriptors[0];
            pd.project.name == 'sub1'
            pd.dependencies.transitive == false

            featuresXml.feature.@name == 'karaf-features-project-dependencies'
            featuresXml.feature.bundle.'**'.findAll { it.text().contains('mvn:test.pkg/sub1/1.2.3')}.size() == 1
    }
    
    def 'Feature with dependencies'() {
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
            feature.feature('dependencyFeatureName1')
            feature.feature('dependencyFeatureName2') {
                version = "5.6.7"
                dependency = true
            }

            def featuresStr = task.generateFeatures()
            def featuresXml = new XmlSlurper().parseText(featuresStr)
        then:
            featuresStr != null
            featuresXml != null

            log.info("${featuresStr}")

            featuresXml.feature.@name == 'karaf-features-simple-project'
            featuresXml.feature.@description == 'feature-description'
            featuresXml.feature.@version == '1.2.3'
            featuresXml.feature.feature[0].text() == 'dependencyFeatureName1'
            featuresXml.feature.feature[1].text() == 'dependencyFeatureName2'
            featuresXml.feature.feature[1].@version == '5.6.7'
            featuresXml.feature.feature[1].attributes().get('dependency') == null
    }
    
    def 'Feature with dependencies for xsd 1.3.0'() {
        given:
            def project = setupProjectAndDependencies()
            project.version = '1.2.3'
            def features = getKarafFeaturesExtension(project).features
            def task = getKarafFeaturesTasks(project)
        when:
            def feature = features.create('myFeature')
            feature.name = 'karaf-features-simple-project'
            feature.description = 'feature-description'
            getKarafFeaturesExtension(project).xsdVersion = "1.3.0"
            feature.bundle {
                match: [ group: 'com.squareup.retrofit', name: 'converter-jackson' ]
                include: false
            }
            feature.feature('dependencyFeatureName1')
            feature.feature('dependencyFeatureName2') {
                version = "5.6.7"
                dependency = true
            }

            def featuresStr = task.generateFeatures()
            def featuresXml = new XmlSlurper().parseText(featuresStr)
        then:
            featuresStr != null
            featuresXml != null

            log.info("${featuresStr}")

            featuresXml.feature.@name == 'karaf-features-simple-project'
            featuresXml.feature.@description == 'feature-description'
            featuresXml.feature.@version == '1.2.3'
            featuresXml.feature.feature[0].text() == 'dependencyFeatureName1'
            featuresXml.feature.feature[1].text() == 'dependencyFeatureName2'
            featuresXml.feature.feature[1].@version == '5.6.7'
            featuresXml.feature.feature[1].@dependency == 'true'

            featuresStr.contains("xmlns=\"http://karaf.apache.org/xmlns/features/v1.3.0\"") == true
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

    def setupProject(Project project) {
        new KarafFeaturesPlugin().apply(project)
        project.apply plugin: 'java'
        project.apply plugin: 'maven'

        project.repositories {
            mavenLocal()
            mavenCentral()
        }

        project.configurations {
            myAdditionalDependencies
        }

        return project
    }

    def setupProjectDependencies(Project project) {
        project.dependencies {
            compile "com.google.guava:guava:18.0"
            compile "com.squareup.retrofit:retrofit:1.9.0"
            compile "com.squareup.retrofit:converter-jackson:1.9.0"
            compile "org.apache.activemq:activemq-web:5.12.1"
            compile "org.apache.activemq:activemq-web-console:5.12.1@war"

            myAdditionalDependencies "commons-lang:commons-lang:2.6"
        }
    }

    def setupMultiProjectAndDependencies() {
        def project = ProjectBuilder.builder().withName('root').build()
        project.apply plugin: 'com.github.lburgazzoli.karaf.features'

        def sp1 = setupSubproject(project, "subproject1")
        def sp2 = setupSubproject(project, "subproject2")

        project.subprojects.each {
            it.apply plugin: 'java'
            it.apply plugin: 'maven'
            it.apply plugin: 'osgi'

            it.repositories {
                mavenLocal()
                mavenCentral()
            }
        }

        sp1.dependencies {
            compile 'commons-io:commons-io:2.4'
        }
        sp2.dependencies {
            compile 'commons-lang:commons-lang:2.6'
        }

        return project
    }

    def setupSubproject(Project root, String name) {
        return ProjectBuilder.builder().withName(name).withParent(root).build()
    }

    def getKarafFeaturesExtension(project) {
        project.extensions.getByName(KarafFeaturesPlugin.EXTENSION_NAME)
    }

    def getKarafFeaturesTasks(project) {
        project.tasks.getByName(KarafFeaturesPlugin.TASK_NAME)
    }
}
