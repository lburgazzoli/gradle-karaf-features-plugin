/*
 * Copyright 2013, contributors as indicated by the @author tags
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
    private final Project project

    /**
     * The strategy for handling bundle definitions.
     */
    def BundleStrategy bundleStrategy = BundleStrategy.MVN

    /**
     * Name used for the {@code <features name="?"/>} attribute.  Default is to use the
     * name of the project to which the plugin is attached.
     */
    def featuresName

    /**
     * The output file
     */
    def File featuresXmlFile

    /**
     * Define any {@code <repository/>} entries to be added to the features file.
     */
    def Set<String> repositories = []

    /**
     * User configuration of any {@code <feature/>} generations to occur.
     */
    final NamedDomainObjectContainer<FeatureDescriptor> features;

    Logger getLogger() {
        return project.logger
    }

    KarafFeaturesTaskExtension(Project project) {
        this.project = project

        this.featuresName = project.name
        this.featuresXmlFile = project.file( "${project.buildDir}/karafFeatures/${project.name}-${project.version}-karaf.xml" )

        // Create a dynamic container for FeatureDescriptor definitions by the user
        features = project.container( FeatureDescriptor, new FeatureDescriptorFactory( project, this ) )
        
    }

    boolean isPreferObrBundles() {
        return bundleStrategy == BundleStrategy.OBR
    }

    void setPreferObrBundles(boolean preferObrBundles) {
        if ( preferObrBundles ) {
            bundleStrategy = BundleStrategy.OBR
        }
        else {
            bundleStrategy = BundleStrategy.MVN
        }
    }

    def features(Closure closure) {
        features.configure( closure )
    }

}
