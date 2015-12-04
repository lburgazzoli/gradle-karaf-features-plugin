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
package com.github.lburgazzoli.gradle.plugin.karaf.features

import com.github.lburgazzoli.gradle.plugin.karaf.features.tasks.KarafFeaturesTask
import com.github.lburgazzoli.gradle.plugin.karaf.features.tasks.KarafFeaturesTaskExtension
import com.github.lburgazzoli.gradle.plugin.karaf.features.tasks.KarafKarTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Plugin for integrating Karaf features generation into a build.  Execution is configured
 * through the KarafFeaturesGenTaskExtension DSL extension registered under {@code karafFeatures}
 *
 * @author Luca Burgazzoli
 * @author Steve Ebersole
 * @author Sergey Nekhviadovich
 */
class KarafFeaturesPlugin implements Plugin<Project> {
    public static final String CONFIGURATION_NAME = 'karafFeaturesBundles'
    public static final String EXTENSION_NAME = 'karafFeatures'

    @Override
    void apply(Project project) {
        def configuration = project.configurations.maybeCreate( CONFIGURATION_NAME )
        def extension = project.extensions.create( EXTENSION_NAME, KarafFeaturesTaskExtension, project )

        def featuresTask = project.task( KarafFeaturesTask.TASK_NAME, type: KarafFeaturesTask )
        def karTask = project.task( KarafKarTask.TASK_NAME, type: KarafKarTask )

        afterEvaluate(project, featuresTask, extension)
        afterEvaluate(project, karTask, extension)
    }

    private afterEvaluate(Project project, Task task, KarafFeaturesTaskExtension extension) {
        extension.features.each { feature ->
            feature.configurations.each {
                task.inputs.files(it)
                task.dependsOn(it)
            }

            feature.projectDescriptors.each { descriptor ->
                // we need access the jar for any project we generate feature for
                task.dependsOn descriptor.project.tasks.jar
                // we also want our descriptor.project to be based on the runtime configuration
                task.inputs.files(descriptor.project.configurations.runtime)
            }
        }

        // if there is an output file, add that as an output
        if (extension.outputFile != null) {
            task.outputs.file(extension.outputFile)
        }
    }
}
