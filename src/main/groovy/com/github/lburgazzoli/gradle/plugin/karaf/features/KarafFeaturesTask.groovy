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

import groovy.xml.MarkupBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskAction

/**
 * The Gradle task to perform generation of a Karaf features repository file (XML or kar)
 *
 * @author Luca Burgazzoli
 * @author Steve Ebersole
 * @author Sergey Nekhviadovich
 */
class KarafFeaturesTask extends DefaultTask {
    public static final String FEATURES_XMLNS_PREFIX = 'http://karaf.apache.org/xmlns/features/v'

    private KarafFeaturesTaskExtension extension_;
    private Configuration extraBundles_;

    public KarafFeaturesTask() {
        super();
    }

    @TaskAction
    def generateFeaturesFile() {
        project.logger.debug("Karaf features task start");

        if(extension.outputFile != null) {
            // write out a features repository xml.
            extension.outputFile.parentFile.mkdirs()

            def out = new BufferedWriter(new FileWriter(extension.outputFile))
            out.write(generateFeatures())
            out.close()
        } else {
            println "\n${generateFeatures()}\n"
        }
    }

    protected def generateFeatures() {
        def writer = new StringWriter()

        def builder = new MarkupBuilder(writer)
        builder.setOmitNullAttributes(true)
        
        def (majXsdVer, minXsdVer) = extension.xsdVersion.split('\\.').collect { it.toInteger() }
        def xsdVer13 = majXsdVer > 1 || ( majXsdVer == 1 && minXsdVer >= 3 );
        def bundleDefinitionCalculator = extension.bundleStrategy.bundleDefinitionCalculator

        builder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8", standalone: "yes")
        builder.features(xmlns:FEATURES_XMLNS_PREFIX + extension.xsdVersion, name: extension.name) {
            extension.repositories.each {
                builder.repository( it )
            }

            extension.features.each { feature ->
                builder.feature(name: feature.name, version: feature.version, description: feature.description) {
                    feature.dependencyFeatures.each {
                        builder.feature(
                            [
                                version:  it.version,
                                dependency: (it.dependency && xsdVer13) ? true : null
                            ],
                            it.name
                        )
                    }

                    // Render bundle dependencies
                    extension.logger.warn("Calculate bundle definitions for feature '${feature.name}'")
                    bundleDefinitionCalculator.calculate(feature, extension, extraBundles).each {
                        builder.bundle(
                            [
                                'dependency' : it.dependency,
                                'start-level': it.startLevel
                            ],
                            it.url
                        )
                    }
                }
            }
        }

        return writer.toString()
    }

    def KarafFeaturesTaskExtension getExtension() {
        // Don't keep looking it up...
        if ( extension_ == null ) {
            extension_ = project.extensions.findByName( KarafFeaturesPlugin.EXTENSION_NAME ) as KarafFeaturesTaskExtension
        }

        return extension_;
    }


    def getExtraBundles() {
        // Don't keep looking it up...
        if ( extraBundles_ == null ) {
            extraBundles_ = project.configurations.findByName( KarafFeaturesPlugin.CONFIGURATION_NAME )
        }
        return extraBundles_;
    }
}
