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
package com.github.lburgazzoli.gradle.plugin.karaf.features

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier

import com.github.lburgazzoli.gradle.plugin.karaf.features.model.BundleInstructionDescriptor
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.FeatureDescriptor
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.ProjectDescriptor

/**
 * @author Steve Ebersole
 * @author Sergey Nekhviadovich
 */
class BundleDefinitionCalculatorObrImpl implements BundleDefinitionCalculator {
	/**
	 * Singleton access
	 */
	public static final BundleDefinitionCalculatorObrImpl INSTANCE = new BundleDefinitionCalculatorObrImpl();

	@Override
	List<BundleDefinition> calculateBundleDefinitions(
			FeatureDescriptor feature,
			KarafFeaturesTaskExtension extension,
			Configuration extraBundles) {
		LinkedHashSet<ModuleVersionIdentifier> dependencies = []
		collectDependencies( feature, dependencies, extraBundles, null )
		feature.configurations.each {
			collectDependencies( feature, dependencies, it, null )
		}
		feature.projectsDescriptors.each { bundledProjectDescriptor ->
			collectDependencies( feature, dependencies, bundledProjectDescriptor.project.configurations.runtime, bundledProjectDescriptor )
		}

		List<BundleDefinition> bundleDefinitions = []
		dependencies.each { bundleCoordinates ->
			final BundleInstructionDescriptor bundleDescriptor = findBundleInstructions( bundleCoordinates, feature )

			BundleDefinition bundleDefinition = new BundleDefinition(
					"obr:${bundleCoordinates.group}/${bundleCoordinates.name}/${bundleCoordinates.version}"
			)

			if ( bundleDescriptor != null ) {
				bundleDefinition.dependency = bundleDescriptor.dependency
				bundleDefinition.startLevel = bundleDescriptor.startLevel
			}

			bundleDefinitions.add( bundleDefinition )
		}

		return bundleDefinitions
	}

	def static collectDependencies(
			FeatureDescriptor feature,
			LinkedHashSet<ModuleVersionIdentifier> moduleVersionIdentifiers,
			Configuration configuration, 
            ProjectDescriptor projectDescriptor) {
		ResolvedComponentResult root = configuration.incoming.resolutionResult.root

		final BundleInstructionDescriptor bundleInstructions = findBundleInstructions( root.moduleVersion, feature )
		if ( bundleInstructions != null & !bundleInstructions.include ) {
			return;
		}

		// add dependencies first
		if ( projectDescriptor.dependencies.transitive ) {
			for ( DependencyResult dependency : root.dependencies ) {
				if ( dependency instanceof UnresolvedDependencyResult ) {
					continue;
				}

				ResolvedDependencyResult resolvedDependencyResult = (ResolvedDependencyResult) dependency;
				final BundleInstructionDescriptor bundleInstructions2 = findBundleInstructions( resolvedDependencyResult.selected.moduleVersion, feature )
				if ( bundleInstructions2 == null || bundleInstructions2.include ) {
					moduleVersionIdentifiers.add( ( (ResolvedDependencyResult) dependency ).selected.moduleVersion )
				}
			}
		}
		

		// then add the root one
		ModuleVersionIdentifier rootModuleVersion = root.moduleVersion
		String artifactId = projectDescriptor.artifactId ?: rootModuleVersion.name
		ModuleVersionIdentifier projectModuleVersion = new DefaultModuleVersionIdentifier(
			"${rootModuleVersion.group}",
			"${artifactId}",
			"${rootModuleVersion.version}"
		)
		moduleVersionIdentifiers.add( projectModuleVersion )
	}

	static BundleInstructionDescriptor findBundleInstructions(ModuleVersionIdentifier dep, FeatureDescriptor feature) {
		for ( BundleInstructionDescriptor it : feature.bundles ) {
			if ( it.matcher.matches( dep ) ) {
				return it;
			}
		}

		return null;
	}
}
