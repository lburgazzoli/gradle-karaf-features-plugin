/**
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
package com.github.lburgazzoli.gradle.plugin

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

/**
 * Extensions for KarafFeaturesGenTask
 *
 * @author Luca Burgazzoli
 * @author Steve Ebersole
 */
class KarafFeaturesGenTaskExtension {
    private final Project project

    def File outputDir
    def String featuresXmlFileName

    final NamedDomainObjectContainer<FeatureDescriptor> features;

    KarafFeaturesGenTaskExtension(Project project) {
        this.project = project

        this.outputDir = project.file( "${project.buildDir}/karafFeatures/" )
        this.featuresXmlFileName = "${project.name}-${project.version}-karaf.xml"

        // Create a dynamic container for FeatureDescriptor definitions by the user
        features = project.container( FeatureDescriptor, new FeatureDescriptorFactory( project ) )
    }

    def features(Closure closure) {
        features.configure( closure )
    }

    File getFeaturesXmlFile() {
        new File( outputDir, featuresXmlFileName )
    }
}
