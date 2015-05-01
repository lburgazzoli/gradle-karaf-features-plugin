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
package com.github.lburgazzoli.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.util.ConfigureUtil

import org.apache.commons.collections.ClosureUtils

/**
 * Models the information used to generate a single {@code <feature/>} entry
 * in a Karaf features repository file
 *
 * @author Steve Ebersole
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
	 * Any projects to be included in this feature.  We will pick up
	 * all of their {@code configurations.runtime} dependencies and
	 * add them as bundles.
	 */
	def Project[] projects

	/**
	 * Any Configurations containing additional dependencies to apply as bundles
	 * to this feature.
	 */
	def Configuration[] extraBundleDependencies

	/**
	 * Any specific bundle instructions to apply within this feature.
	 */
	def BundleInstructionsDescriptor[] bundles

	FeatureDescriptor(String name, Project project) {
		this.name = name
		this.version = project.version
		this.projects = [project]
	}

	def project(Project project) {
		if ( this.projects == null ) {
			this.projects = [project]
		}
		else {
			this.projects += project
		}
	}

	def bundle(String selector, Closure closure) {
		BundleInstructionsDescriptor descriptor = new BundleInstructionsDescriptor();
		descriptor.selector = selector;
		ConfigureUtil.configure( closure, descriptor )

		if ( bundles == null ) {
			bundles = [descriptor]
		}
		else {
			bundles += descriptor
		}
	}

	def bundle(Closure closure) {
		BundleInstructionsDescriptor descriptor = new BundleInstructionsDescriptor();
		ConfigureUtil.configure( closure, descriptor )

		if ( bundles == null ) {
			bundles = [descriptor]
		}
		else {
			bundles += descriptor
		}
	}
}
