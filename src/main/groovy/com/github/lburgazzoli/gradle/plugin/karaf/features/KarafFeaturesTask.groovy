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

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskAction

import groovy.xml.MarkupBuilder

/**
 * The Gradle task to perform generation of a Karaf features repository file (XML or kar)
 *
 * @author Luca Burgazzoli
 * @author Steve Ebersole
 */
class KarafFeaturesTask extends DefaultTask {
    public KarafFeaturesTask() {
        super();
    }

    @TaskAction
    def generateFeaturesFile() {
        def writer = new StringWriter()
        def builder = new MarkupBuilder(writer)

        builder.features(xmlns:'http://karaf.apache.org/xmlns/features/v1.2.0', name: extension.featuresName) {
            extension.repositories.each {
                builder.repository( it )
            }

            extension.features.each { feature->
                Map featureAttributeMap = new HashMap()
                featureAttributeMap.put( "name", feature.name )
                featureAttributeMap.put( "version", feature.version )
                if ( feature.description ) {
                    featureAttributeMap.put( "description", feature.description )
                }

                builder.feature( featureAttributeMap ) {
                    // Render feature dependencies
                    if ( feature.dependencyFeatureNames != null ) {
                        feature.dependencyFeatureNames.each {
                            builder.feature( it )
                        }
                    }

                    // Render bundle dependencies
                    List<BundleDefinition> bundles = extension.bundleStrategy.bundleDefinitionCalculator.calculateBundleDefinitions(
                            feature,
                            extension,
                            extraBundles
                    )
                    Map attributeMap = new HashMap()
                    bundles.each { bundle->
                        if ( bundle.dependency != null ) {
                            attributeMap.put( "dependency", bundle.dependency )
                        }
                        if ( bundle.startLevel != null ) {
                            attributeMap.put( "start-level", bundle.startLevel )
                        }

                        builder.bundle( attributeMap, bundle.url )

                        // for next pass
                        attributeMap.clear()
                    }
                }
            }
        }

        // write out a features repository xml.
        extension.featuresXmlFile.parentFile.mkdirs()
        def out = new BufferedWriter( new FileWriter( extension.featuresXmlFile ) )
        out.write( writer.toString() )
        out.close()
    }

    KarafFeaturesTaskExtension extension_;

    def KarafFeaturesTaskExtension getExtension() {
        // Don't keep looking it up...
        if ( extension_ == null ) {
            extension_ = project.extensions.findByName( KarafFeaturesPlugin.EXTENSION_NAME ) as KarafFeaturesTaskExtension
        }
        return extension_;
    }


    Configuration extraBundles_;

    def getExtraBundles() {
        // Don't keep looking it up...
        if ( extraBundles_ == null ) {
            extraBundles_ = project.configurations.findByName( KarafFeaturesPlugin.CONFIGURATION_NAME )
        }
        return extraBundles_;
    }
}
