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
import groovy.transform.ToString

import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesTaskExtension

/**
 * DSL extension allowing instruction on how to connect project with properties to a {@code <feature/>} entry
 * in a Karaf features repository file
 *
 * @author Steve Ebersole
 * @author Luca Burgazzoli
 * @author Sergey Nekhviadovich
 */
 
@ToString(includeNames=true)
class ProjectDescriptor {
	/**
	 * Project to be included in this feature.  We will pick up
	 * all of its {@code configurations.runtime} dependencies and
	 * add it as bundle. This project runtime configurations are
	 * considered additive to the {@link #bundleDependencies} configurations
	 */
	def Project project

	/**
	 * dependencies descriptor object used to specify which dependencies should be included
	 */
	def ProjectDependenciesDescriptor dependenciesDescriptor

	/**
	 * Property to override project.name while feature geeration
	 */
	def String artifactId

	ProjectDescriptor(Project project) {
		this.project = project
		this.dependenciesDescriptor = new ProjectDependenciesDescriptor()
	}
    
	def dependencies(Closure cl) {
		ConfigureUtil.configure( cl, this.dependenciesDescriptor )
	}
    
	ProjectDependenciesDescriptor getDependencies() {
		return this.dependenciesDescriptor;
	}
}
