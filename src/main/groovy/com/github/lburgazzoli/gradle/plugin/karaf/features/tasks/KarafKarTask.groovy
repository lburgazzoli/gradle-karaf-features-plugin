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
import org.gradle.jvm.tasks.Jar
/**
 * The Gradle task to perform generation of a Karaf KAR archive
 *
 * @author Luca Burgazzoli
 */
class KarafKarTask extends Jar {
    public static final String TASK_NAME = "generateKarafKar"

    @Override
    protected void copy() {
        File root = project.file("${project.buildDir}/karafKar/repository")
        if(!root.exists()) {
            root.mkdirs()
        }

        def featuresExtension = KarafFeaturesUtils.lookupExtension(project)
        def featuresExtras = KarafFeaturesUtils.lookupExtraBundlesConfiguration(project)
        def bundleDefinitionCalculator = featuresExtension.bundleStrategy.bundleDefinitionCalculator
        
        featuresExtension.features.each { feature ->
            bundleDefinitionCalculator.calculate(feature, featuresExtension, featuresExtras).each {
                def path = "${it.version.group}/${it.version.name}/${it.version.version}"
                def name = "${it.version.name}-${it.version.version}.${it.type}"

                copy(it.path, new File(root, "${path}/${name}"))
            }
        }

        def path = "${featuresExtension.project.group}/${featuresExtension.name}/${featuresExtension.project.version}"
        def name = "${featuresExtension.name}-${featuresExtension.project.version}.xml"
        copy(featuresExtension.getOutputFile(), new File(root, "${path}/${name}"))

        baseName = featuresExtension.name
        version = featuresExtension.project.version
        extension = "kar"
        destinationDir = new File("${project.buildDir}/libs")
        from(project.file("${project.buildDir}/karafKar"))

        super.copy()
    }

    def copy(File source, File destination) {
        if(source) {
            if (!destination.parentFile.exists()) {
                destination.parentFile.mkdirs()
            }

            destination << source
        }
    }
}
