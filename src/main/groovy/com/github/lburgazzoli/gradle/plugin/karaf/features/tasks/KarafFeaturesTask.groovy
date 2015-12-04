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

import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesUtils
import groovy.xml.MarkupBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskAction
import org.gradle.util.VersionNumber

/**
 * The Gradle task to perform generation of a Karaf features file
 *
 * @author Luca Burgazzoli
 * @author Steve Ebersole
 * @author Sergey Nekhviadovich
 */
class KarafFeaturesTask extends DefaultTask {
    public static final String TASK_NAME = "generateKarafFeatures"
    public static final String FEATURES_XMLNS_PREFIX = 'http://karaf.apache.org/xmlns/features/v'
    public static final VersionNumber XMLNS_V13 = new  VersionNumber(1, 3, 0, null)

    private KarafFeaturesTaskExtension extension_;
    private Configuration extraBundles_;


    @TaskAction
    def run() {
        File outputFile = extension.getOutputFile()

        // write out a features repository xml.
        if(!outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }

        def out = new BufferedWriter(new FileWriter(outputFile))
        out.write(generateFeatures())
        out.close()
    }

    protected String generateFeatures() {
        def writer = new StringWriter()

        def builder = new MarkupBuilder(writer)
        builder.setOmitNullAttributes(true)
        builder.setDoubleQuotes(true)

        def xsdVer13 = VersionNumber.parse(extension.xsdVersion).compareTo(XMLNS_V13) >= 0
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
        if (extension_ == null) {
            extension_ = KarafFeaturesUtils.lookupExtension(project)
        }

        return extension_;
    }


    def getExtraBundles() {
        // Don't keep looking it up...
        if (extraBundles_ == null) {
            extraBundles_ = KarafFeaturesUtils.lookupExtraBundlesConfiguration(project)
        }

        return extraBundles_;
    }
}
