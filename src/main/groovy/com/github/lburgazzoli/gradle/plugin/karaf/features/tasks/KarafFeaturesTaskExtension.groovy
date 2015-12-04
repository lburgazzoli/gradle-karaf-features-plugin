/*
 * Copyright 2015, Luca Burgazzoli and contributors as indicated by the @author tags
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


package com.github.lburgazzoli.gradle.plugin.karaf.features.tasks

import com.github.lburgazzoli.gradle.plugin.karaf.features.BundleStrategy
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.logging.Logger

import com.github.lburgazzoli.gradle.plugin.karaf.features.model.FeatureDescriptor
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.FeatureDescriptorFactory

/**
 * DSL Extensions for KarafFeaturesGenTask
 *
 * @author Luca Burgazzoli
 * @author Steve Ebersole
 * @author Sergey Nekhviadovich
 */
class KarafFeaturesTaskExtension {
    final Project project

    /**
     * The strategy for handling bundle definitions.
     */
    BundleStrategy bundleStrategy

    /**
     * Name used for the {@code <features name="?"/>} attribute.  Default is to use the
     * name of the project to which the plugin is attached.
     */
    String name
    
    /**
     * Version of the xsd for target feature.xml file. Default is "1.2.0"
     * From version 1.3.0 dependency feature attribute is supported
     * @see https://karaf.apache.org/xmlns/features/v1.2.0
     */
    String xsdVersion

    /**
     * The output file
     */
    File outputFile

    /**
     * Define any {@code <repository/>} entries to be added to the features file.
     */
    protected final def Set<String> repositories

    /**
     * User configuration of any {@code <feature/>} generations to occur.
     */
    final NamedDomainObjectContainer<FeatureDescriptor> features;


    KarafFeaturesTaskExtension(Project project) {
        this.project = project
        this.name = project.name
        this.bundleStrategy = BundleStrategy.MVN
        this.xsdVersion = "1.2.0"
        this.repositories = []
        this.outputFile = null

        // Create a dynamic container for FeatureDescriptor definitions by the user
        this.features = project.container( FeatureDescriptor, new FeatureDescriptorFactory( project, this ) )
    }

    boolean isPreferObrBundles() {
        return bundleStrategy == BundleStrategy.OBR
    }

    void setPreferObrBundles(boolean preferObrBundles) {
        bundleStrategy = preferObrBundles ? BundleStrategy.OBR : BundleStrategy.MVN
    }

    boolean isPreferMvnBundles() {
        return bundleStrategy == BundleStrategy.NVN
    }

    void setPreferMvnBundles(boolean preferMvnBundles) {
        bundleStrategy = preferMvnBundles ? BundleStrategy.MVN : BundleStrategy.OBR
    }

    def repository(String repository) {
        this.repositories.add(repository)
    }

    def repositories(Collection<String> repositories) {
        this.repositories.clear()
        this.repositories.addAll(repositories)
    }

    def features(Closure closure) {
        features.configure( closure )
    }

    File getOutputFile() {
        if(outputFile == null) {
            def path = "${project.buildDir}/karafFeautures"
            def name = "${name}-${project.version}.xml"

            return new File(path, name)
        }

        return outputFile
    }

    Logger getLogger() {
        return project.logger
    }
}
