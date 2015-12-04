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
package com.github.lburgazzoli.gradle.plugin.karaf.features.model

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.util.ConfigureUtil

import com.github.lburgazzoli.gradle.plugin.karaf.features.tasks.KarafFeaturesTaskExtension

/**
 * DSL extension allowing instruction on how to produce a {@code <feature/>} entry
 * in a Karaf features repository file
 *
 * @author Steve Ebersole
 * @author Luca Burgazzoli
 * @author Sergey Nekhviadovich
 */
class FeatureDescriptor {
	/**
	 * The name to be used in the {@code <feature/>} entry
	 */
	String name

	/**
	 * The version to be used in the {@code <feature/>} entry
	 */
	String version

	/**
	 * Optional description for the feature
	 */
	String description

	/**
	 * The project from which the plugin is instantiated
	 */
	final ProjectDescriptor project

	/**
	 * Any projects to be included in this feature.  We will pick up
	 * all of their {@code configurations.runtime} dependencies and
	 * add them as bundles.  These project runtime configurations are
	 * considered additive to the {@link #configurations} configurations
	 */
    private List<ProjectDescriptor> projectDescriptors

	/**
	 * Any Configurations containing dependencies to apply as bundles
	 * to this feature.  These configurations are considered additive to the
	 * project runtime configurations from {@link #projectDescriptors}
	 */
	protected List<Configuration> configurations

	/**
	 * Any specific bundle instructions to apply within this feature.
	 */
    private List<BundleInstructionDescriptor> bundles
	
	/**
	 * List of feature dependencies. Support version and dependency properties.
	 */
	protected List<FeatureDependencyDescriptor> dependencyFeatures;

	private final KarafFeaturesTaskExtension extension

	FeatureDescriptor(String name, Project project, KarafFeaturesTaskExtension extension) {
		this.extension = extension
		this.name = name
		this.version = project.version
		this.project = new ProjectDescriptor(project)
        this.dependencyFeatures = []
        this.configurations = []
		this.bundles = []
        this.projectDescriptors = []
	}

    def rootProject(Closure closure) {
        ConfigureUtil.configure( closure, this.project )
    }

	def project(String projectName, Closure closure) {
		this.project.project.allprojects.find {it.name == projectName || ":${it.name}" == projectName }.each {
            Project project -> this.addProject(project, closure)
		}
	}

	def project(String projectName) {
        addProject(projectName, null)
	}

	def project(Project project) {
        addProject(Project, null)
	}

    def project(Closure closure) {
        this.addProject(null, closure)
    }

    private def addProject(Project project, Closure closure) {
        this.project.logger.debug("Add project '${project?.name}' to feature '${this.name}'");
        def projectDescriptor = new ProjectDescriptor(project)
        if(closure) {
            ConfigureUtil.configure( closure, projectDescriptor )
        }

        this.projectDescriptors += projectDescriptor

        this.project.logger.debug("Added new project descriptor '${projectDescriptor}' to feature '${this.name}'");
    }

    public List<ProjectDescriptor> getProjectDescriptors() {
        return this.projectDescriptors
    }

	def bundle(String pattern, Closure closure) {
        this.project.logger.debug("Add bundle '${pattern}' to feature '${this.name}'");

		def descriptor = new BundleInstructionDescriptor(BundleMatcher.from(pattern))
        if(closure) {
            ConfigureUtil.configure(closure, descriptor)
        }

        bundles += descriptor
	}

	def bundle(Closure closure) {
        bundles += ConfigureUtil.configure(closure, new ExtendedBundleInstructionDescriptor())
	}

    public List<BundleInstructionDescriptor> getBundles() {
        return this.bundles
    }

    def features(Collection<String> featureNames) {
        featureNames.each {
            this.feature(it, null)
        }
    }

    def feature(FeatureDescriptor feature) {
        this.feature(feature.name, null)
    }

    def feature(FeatureDescriptor feature, Closure closure) {
        this.feature(feature.name, closure)
    }
	
	def feature(String featureName) {
		this.feature(featureName, null)
	}
	
	def feature(String featureName, Closure closure) {
		def featureDependencyDescriptor = new FeatureDependencyDescriptor(featureName)
		if ( closure ) {
			ConfigureUtil.configure( closure, featureDependencyDescriptor )
		}

		this.project.logger.debug("Add feature dependency '${featureName}' to feature '${this.name}'");
		this.dependencyFeatures += featureDependencyDescriptor
	}

    def bundlesFrom(Configuration configuration) {
        if(configuration) {
            this.configurations.add(configuration)
        }
    }

    def bundlesFrom(String configurationName) {
        Configuration configuration = this.project.getConfigurationByName(configurationName);
        if(configuration) {
            this.configurations.add(configuration)
        }
    }
}
