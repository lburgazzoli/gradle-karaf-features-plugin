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
package com.github.lburgazzoli.gradle.plugin.karaf.featureGen.model

import org.gradle.util.ConfigureUtil

/**
 * DSL extension allowing instruction on how to produce a {@code <bundle/>} entry
 * in a Karaf features repository file
 *
 * @author Steve Ebersole
 */
class BundleInstructionDescriptor {
	/**
	 * Defines the parameters for matching the dependency that this descriptor describes.
	 */
	private Matcher matcher = new Matcher();

	/**
	 * Should the selected dependency be included as a bundle?  The default is {@code true}; if
	 * set to false, no bundle definition will be generated for the matched dependency nor any of
	 * its transitive dependencies.
	 */
	def boolean include = true;

	/**
	 * The start level to apply to the generated bundle element (i.e., {@code <bundle start-level="?"/>})
	 */
	def String startLevel;

	/**
	 *
	 * Whether the bundle should be generated as a dependency (i.e., {@code <bundle dependency="true"/>})
	 */
	def Boolean dependency

	def Matcher remap

	private BundleWrapInstructionsDescriptor bundleWrapInstructionsDescriptor;

	def match(Closure closure) {
		ConfigureUtil.configure( closure, matcher )
	}

	def match(Map properties) {
		ConfigureUtil.configureByMap( properties, matcher )
	}

	def Matcher getMatcher() {
		return matcher
	}

	def remap(Closure closure) {
		remap = new Matcher()
		ConfigureUtil.configure( closure, remap )
	}

	def remap(Map properties) {
		remap = new Matcher()
		ConfigureUtil.configureByMap( properties, remap )
	}

	Matcher getRemap() {
		return remap
	}

	def wrap(Closure closure) {
		bundleWrapInstructionsDescriptor = new BundleWrapInstructionsDescriptor()
		ConfigureUtil.configure( closure, bundleWrapInstructionsDescriptor )
	}

	def boolean hasExplicitWrapInstructions() {
		return bundleWrapInstructionsDescriptor != null;
	}

	BundleWrapInstructionsDescriptor _getBundleWrapInstructionsDescriptor() {
		return bundleWrapInstructionsDescriptor
	}

}
