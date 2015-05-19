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

import com.github.lburgazzoli.gradle.plugin.karaf.features.model.BundleInstructionDescriptor
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.FeatureDescriptor

/**
 * @author Steve Ebersole
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
		collectDependencies( feature, dependencies, extraBundles )
		feature.bundleDependencies.each {
			collectDependencies( feature, dependencies, it )
		}
		feature.projects.each { bundledProject ->
			collectDependencies( feature, dependencies, bundledProject.configurations.runtime )
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
			Configuration configuration) {
		ResolvedComponentResult root = configuration.incoming.resolutionResult.root

		final BundleInstructionDescriptor bundleInstructions = findBundleInstructions( root.moduleVersion, feature )
		if ( bundleInstructions != null & !bundleInstructions.include ) {
			return;
		}

		// add dependencies first
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

		// then add the root one
		moduleVersionIdentifiers.add( root.moduleVersion )
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
