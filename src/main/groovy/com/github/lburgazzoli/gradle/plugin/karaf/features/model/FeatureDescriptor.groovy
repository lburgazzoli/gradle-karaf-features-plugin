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

import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesTaskExtension

/**
 * DSL extension allowing instruction on how to produce a {@code <feature/>} entry
 * in a Karaf features repository file
 *
 * @author Steve Ebersole
 * @author Luca Burgazzoli
 */
class FeatureDescriptor {
	/**
	 * The name to be used in the {@code <feature/>} entry
	 */
	def String name

	/**
	 * The version to be used in the {@code <feature/>} entry
	 */
	def String version

	/**
	 * Optional description for the feature
	 */
	def String description

	/**
	 * The project from which the plugin is instantiated
	 */
	def Project project

	/**
	 * Any projects to be included in this feature.  We will pick up
	 * all of their {@code configurations.runtime} dependencies and
	 * add them as bundles.  These project runtime configurations are
	 * considered additive to the {@link #bundleDependencies} configurations
	 */
	def ProjectDescriptor[] projectDescriptors

	/**
	 * Any Configurations containing dependencies to apply as bundles
	 * to this feature.  These configurations are considered additive to the
	 * project runtime configurations from {@link #projectDescriptors}
	 */
	def Configuration[] bundleDependencies

	/**
	 * Any specific bundle instructions to apply within this feature.
	 */
	def BundleInstructionDescriptor[] bundles

	def String[] dependencyFeatureNames = []

	private final KarafFeaturesTaskExtension extension

	FeatureDescriptor(String name, Project project, KarafFeaturesTaskExtension extension) {
		this.extension = extension
		this.name = name
		this.version = project.version
		this.project = project
	}

	def project(Project project) {
		this.project.logger.debug("Add project '${project.name}' to feature '${this.name}'");
		def projectDescriptor = new ProjectDescriptor(project)
		if ( this.projectDescriptors == null ) {
			this.projectDescriptors = [ projectDescriptor ]
		} else {
			this.projectDescriptors += projectDescriptor
		}
	}
	
	def project(Closure closure) {
		this.project.logger.debug("Add new project via closure to feature '${this.name}'");
		def projectDescriptor = new ProjectDescriptor(null)
		ConfigureUtil.configure( closure, projectDescriptor )
        this.project.logger.debug("ConfigureUtil finished")
		if ( this.projectDescriptors == null ) {
			this.projectDescriptors = [ projectDescriptor ]
		} else {
			this.projectDescriptors += projectDescriptor
		}
		this.project.logger.debug("Added new project descriptor '${projectDescriptor}' to feature '${this.name}'");
	}

	def bundle(String pattern, Closure closure) {
		def descriptor = new BundleInstructionDescriptor(BundleMatcher.from(pattern))
		ConfigureUtil.configure( closure, descriptor )

		if ( bundles == null ) {
			bundles = [ descriptor ]
		} else {
			bundles += descriptor
		}
	}

	def bundle(Closure closure) {
		def descriptor = new ExtendedBundleInstructionDescriptor()
		ConfigureUtil.configure( closure, descriptor )

		if ( bundles == null ) {
			bundles = [ descriptor ]
		} else {
			bundles += descriptor
		}
	}

	@Deprecated
	/**
	 * Please use getProjectDescriptors instead
	 */
	public Project[] getProjects() {
		if ( this.projectDescriptors == null) {
			return [ this.project ]
		}
		def List<Project> projects = new ArrayList(this.projectDescriptors.size())
		this.projectDescriptors.each { p -> projects.add(p.project) }
		return projects
	}
	
	public ProjectDescriptor[] getProjectDescriptors() {
		return this.projectDescriptors != null ? this.projectDescriptors : [ new ProjectDescriptor(this.project) ]
	}
}
