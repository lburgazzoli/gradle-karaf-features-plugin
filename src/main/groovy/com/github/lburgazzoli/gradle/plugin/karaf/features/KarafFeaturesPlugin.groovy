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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger

/**
 * Plugin for integrating Karaf features generation into a build.  Execution is configured
 * through the KarafFeaturesGenTaskExtension DSL extension registered under {@code karafFeatures}
 *
 * @author Luca Burgazzoli
 * @author Steve Ebersole
 * @author Sergey Nekhviadovich
 */
class KarafFeaturesPlugin implements Plugin<Project> {
    public static final String PLUGIN_ID = 'com.github.lburgazzoli.karaf.features'
    public static final String CONFIGURATION_NAME = 'karafFeaturesBundles'
    public static final String EXTENSION_NAME = 'karafFeatures'
    public static final String TASK_NAME = 'generateKarafFeatures'

    @Override
    void apply(Project project) {
        project.logger.debug("Karaf features plugin apply");
        Configuration configuration = project.configurations.maybeCreate( CONFIGURATION_NAME )
        KarafFeaturesTaskExtension extension = project.extensions.create( EXTENSION_NAME, KarafFeaturesTaskExtension, project )
        Task task = project.task( TASK_NAME, type: KarafFeaturesTask )

        project.afterEvaluate {
            if ( extension.features.empty ) {
                // no features were added, so do the "default" bit...
                project.logger.debug("Karaf features plugin: features empty for project '${project.name}', adding default");
                extension.features {
                    feature {
                        name = project.name
                        projects = [project]
                        project.subprojects.each {
                            project.logger.debug("Karaf features plugin: adding default feature ${project.name}, it = ${it}");
                            project( it )
                        }
                    }
                }
            }

            task.inputs.files( configuration )

            extension.features.each { feature ->
                project.logger.debug("Karaf feature '${feature.name}' processing, projects '${feature.projectDescriptors}'");
                feature.bundleDependencies.each {
                    task.inputs.files( it )
                    task.dependsOn( it )
                }

                feature.projectDescriptors.each { bundleProjectDescriptor ->
                    // we need access the jar for any project we generate feature for
                    def bundleProject = bundleProjectDescriptor.project
                    project.logger.debug("Karaf feature '${feature.name}' processing project '${bundleProject.name}'");
                    task.dependsOn bundleProject.tasks.jar

                    // we also want our inputs to be based on the runtime configuration
                    task.inputs.files( bundleProject.configurations.runtime )
                }
            }

            // if there is an output file, add that as an output
            if ( extension.featuresXmlFile != null ) {
                task.outputs.file( extension.featuresXmlFile )
            }

        }
        project.logger.debug("Karaf features finish");
    }
}
