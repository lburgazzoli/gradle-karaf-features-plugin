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
package com.github.lburgazzoli.gradle.plugin.karaf.featureGen

import java.util.jar.JarFile
import java.util.jar.Manifest

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.tasks.bundling.Jar

import com.github.lburgazzoli.gradle.plugin.karaf.featureGen.model.BundleInstructionDescriptor
import com.github.lburgazzoli.gradle.plugin.karaf.featureGen.model.FeatureDescriptor

/**
 * @author Steve Ebersole
 */
public class BundleDefinitionCalculatorMvnImpl implements BundleDefinitionCalculator {
	/**
	 * Singleton access
	 */
	public static final BundleDefinitionCalculatorMvnImpl INSTANCE = new BundleDefinitionCalculatorMvnImpl();

	@Override
	public List<BundleDefinition> calculateBundleDefinitions(
			FeatureDescriptor feature,
			KarafFeaturesGenTaskExtension extension,
			Configuration extraBundles) {
		// The LinkedHashMap here will hold the dependencies in order, transitivity depth first
		//      IMPL NOTE: Initially tried LinkedHashSet<ResolvedComponentResult>, but
		//          ResolvedComponentResult does not properly implement equals/hashCode in terms
		//          of GAV which is what we need this uniquely based on.  So for now we use
		//          a LinkedHashMap keyed by the GAV.
		LinkedHashMap<ModuleVersionIdentifier,ResolvedComponentResult> orderedDependencyMap = new LinkedHashMap<ModuleVersionIdentifier,ResolvedComponentResult>()
		// The Map here will hold all resolved artifacts (access to the actual files) for each dependency
		Map<ModuleVersionIdentifier,File> resolvedArtifactMap = new HashMap<ModuleVersionIdentifier,File>()

		collectDependencies( feature, orderedDependencyMap, resolvedArtifactMap, extraBundles, extension, false )

		feature.bundleDependencies.each {
			collectDependencies( feature, orderedDependencyMap, resolvedArtifactMap, it, extension, false )
		}

		feature.projects.each { bundledProject ->
			collectDependencies( feature, orderedDependencyMap, resolvedArtifactMap, bundledProject.configurations.runtime, extension, true )
			resolvedArtifactMap.put(
					new DefaultModuleVersionIdentifier(
							"${bundledProject.group}",
							"${bundledProject.name}",
							"${bundledProject.version}"
					),
					( bundledProject.tasks.jar as Jar ).archivePath
			)
		}

		List<BundleDefinition> bundleDefinitions = []

		orderedDependencyMap.values().each { dep ->
			final BundleInstructionDescriptor bundleDescriptor = findBundleInstructions( dep, feature )
			final File resolvedBundleArtifact = resolvedArtifactMap.get( dep.moduleVersion )

			final String url;
			if ( bundleDescriptor != null && bundleDescriptor.remap != null ) {
				url = baseMvnUrl( bundleDescriptor.remap.asModuleVersionIdentifier() )
			}
			else {
				url = renderUrl( dep.moduleVersion, bundleDescriptor, resolvedBundleArtifact )
			}

			final BundleDefinition bundleDefinition = new BundleDefinition( url )

			if ( bundleDescriptor != null ) {
				bundleDefinition.dependency = bundleDescriptor.dependency
				bundleDefinition.startLevel = bundleDescriptor.startLevel
			}

			bundleDefinitions.add( bundleDefinition )
		}

		return bundleDefinitions
	}

	static void collectDependencies(
			FeatureDescriptor feature,
			LinkedHashMap<ModuleVersionIdentifier,ResolvedComponentResult> orderedDependencyMap,
			Map<ModuleVersionIdentifier, File> resolvedArtifactMap,
			Configuration configuration,
			KarafFeaturesGenTaskExtension extension,
			boolean includeRoot) {
		collectOrderedDependencies( feature, orderedDependencyMap, configuration.incoming.resolutionResult.root, extension, includeRoot )

		configuration.resolvedConfiguration.resolvedArtifacts.each {
			resolvedArtifactMap.put( it.moduleVersion.id, it.file );
		}
	}


	/**
	 * Recursive method walking the dependency graph depth first in order to build a a set of
	 * dependencies ordered by their transitivity depth.
	 *
	 * @param orderedDependencies The ordered set of dependencies being built
	 * @param resolvedComponentResult The dependency to process
	 */
	static void collectOrderedDependencies(
			FeatureDescriptor feature,
			LinkedHashMap<ModuleVersionIdentifier,ResolvedComponentResult> orderedDependencyMap,
			ResolvedComponentResult resolvedComponentResult,
			KarafFeaturesGenTaskExtension extension,
			boolean includeResolvedComponentResult) {
		final BundleInstructionDescriptor bundleInstructions = findBundleInstructions( resolvedComponentResult, feature )
		if ( bundleInstructions != null && !bundleInstructions.include ) {
			return;
		}

		// add dependencies first
		for ( DependencyResult dependencyResult : resolvedComponentResult.dependencies ) {
			if ( dependencyResult instanceof UnresolvedDependencyResult ) {
				continue;
			}

			collectOrderedDependencies( feature, orderedDependencyMap, ( (ResolvedDependencyResult) dependencyResult ).selected, extension, true )
		}

		// then add this one (if param says to)
		if ( includeResolvedComponentResult ) {
			orderedDependencyMap.put( resolvedComponentResult.moduleVersion, resolvedComponentResult )
		}
	}

	static BundleInstructionDescriptor findBundleInstructions(ResolvedComponentResult dep, FeatureDescriptor feature) {
		for ( BundleInstructionDescriptor it : feature.bundles ) {
			if ( it.matcher.matches( dep ) ) {
				return it;
			}
		}

		return null;
	}

	static String renderUrl(
			ModuleVersionIdentifier bundleCoordinates,
			BundleInstructionDescriptor bundleInstructionDescriptor,
			File resolvedBundleArtifact) {
		String bundleUrl = baseMvnUrl( bundleCoordinates )

		if ( bundleInstructionDescriptor != null && bundleInstructionDescriptor.hasExplicitWrapInstructions() ) {
			bundleUrl = "wrap:${bundleUrl}"

			if ( bundleInstructionDescriptor._getBundleWrapInstructionsDescriptor().instructions != null ) {
				def sep = '?'
				bundleInstructionDescriptor._getBundleWrapInstructionsDescriptor().instructions.entrySet().each {
					// do these need to be encoded?
					bundleUrl = "${bundleUrl}${sep}${it.key}=${it.value}"
					sep = '&'
				}
			}
		}
		else if ( resolvedBundleArtifact != null && !hasOsgiManifestHeaders( resolvedBundleArtifact ) ) {
			// if the resolved file does not have "proper" OSGi headers we
			// implicitly do the wrap as a courtesy...
			bundleUrl = "wrap:${bundleUrl}"
		}

		return bundleUrl
	}

	public static String baseMvnUrl(ModuleVersionIdentifier bundleCoordinates) {
		return "mvn:${bundleCoordinates.group}/${bundleCoordinates.name}/${bundleCoordinates.version}";
	}

	public static boolean hasOsgiManifestHeaders(File file) {
		JarFile jarFile = new JarFile( file );
		Manifest manifest = jarFile.getManifest();
		if ( manifest != null ) {
			if ( hasAttribute( manifest, "Bundle-SymbolicName" ) ) {
				return true;
			}
			if ( hasAttribute( manifest, "Bundle-Name" ) ) {
				return true;
			}
		}

		return false;
	}

	public static boolean hasAttribute(Manifest manifest, String attributeName) {
		String value = manifest.mainAttributes.getValue( attributeName )
		return value != null && !value.trim().isEmpty()
	}
}
