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
package com.github.lburgazzoli.gradle.plugin.karaf.features.impl

import com.github.lburgazzoli.gradle.plugin.karaf.features.BundleDefinitionCalculator
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.BundleDescriptor
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.BundleInstructionDescriptor
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.FeatureDescriptor
import com.github.lburgazzoli.gradle.plugin.karaf.features.tasks.KarafFeaturesTaskExtension
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.plugins.WarPlugin
import org.gradle.jvm.tasks.Jar

import java.util.jar.JarFile
import java.util.jar.Manifest

import static com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesUtils.asModuleVersionIdentifier
import static com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesUtils.hasAttribute
/**
 * @author Luca Burgazzoli
 * @author Steve Ebersole
 * @author Sergey Nekhviadovich
 */
public class BundleDefinitionCalculatorMvnImpl implements BundleDefinitionCalculator {
	/**
	 * Singleton access
	 */
	public static final BundleDefinitionCalculatorMvnImpl INSTANCE = new BundleDefinitionCalculatorMvnImpl();


	@Override
	public List<BundleDescriptor> calculate(
            FeatureDescriptor feature,
            KarafFeaturesTaskExtension extension,
            Configuration extraBundles) {

		// The LinkedHashMap here will hold the dependencies in order, transitivity depth first
		//	  IMPL NOTE: Initially tried LinkedHashSet<ResolvedComponentResult>, but
		//		  ResolvedComponentResult does not properly implement equals/hashCode in terms
		//		  of GAV which is what we need this uniquely based on.  So for now we use
		//		  a LinkedHashMap keyed by the GAV.
		Map<ModuleVersionIdentifier,BundleDescriptor> dependencyMap = new LinkedHashMap<ModuleVersionIdentifier,BundleDescriptor>()

		collectDependencies( feature, dependencyMap, extraBundles, extension, false )

		feature.configurations.each {
			collectDependencies( feature, dependencyMap, it, extension, false )
		}

		// A bit tricky approach to handle excluding transitive dependencies:
		// We have finalOrderedDependencyMap for result dependencies.
        // We add there root projects and full projects where transitive dependencies included
		// To exclude transitive dependencies we have temporary orderedDependencyMap
        Map<ModuleVersionIdentifier,BundleDescriptor> finalDependencyMap = new LinkedHashMap<ModuleVersionIdentifier,BundleDescriptor>()
        finalDependencyMap.putAll(dependencyMap)

		def projectIdentifiersMap = [:]

		(feature.projectDescriptors ?: [ feature.project ]).each { projectDescriptor ->
			collectDependencies(
                feature,
                projectDescriptor.dependencies.transitive ? finalDependencyMap : dependencyMap,
                projectDescriptor.project.configurations.runtime,
                extension,
                true )

            def projectVersionId = asModuleVersionIdentifier(projectDescriptor.project);
			projectIdentifiersMap[ projectVersionId ] = new BundleDescriptor(
                "${projectDescriptor.project.group}",
                projectDescriptor.artifactId ?: projectDescriptor.project.name,
                "${projectDescriptor.project.version}",
                (projectDescriptor.project.tasks.jar as Jar ).archivePath,
                projectDescriptor.project.plugins.hasPlugin(WarPlugin) ? 'war' : 'jar',
                projectDescriptor.startLevel
      )
		}

        dependencyMap.each { k, v ->
			if ( k in projectIdentifiersMap ) {
                finalDependencyMap[k] = projectIdentifiersMap[k]
			}
		}
        finalDependencyMap.each { k, v ->
            if ( k in projectIdentifiersMap ) {
                v.path = projectIdentifiersMap[k].path
            }
        }

		return finalDependencyMap.values().collect { bundleDescriptor ->
            final BundleInstructionDescriptor bundleInstruction = findBundleInstructions( bundleDescriptor.version , feature )

            if (bundleInstruction) {
                if(bundleInstruction.remap) {
                    bundleDescriptor.version = bundleInstruction.remap.asModuleVersionIdentifier()
                }

                bundleDescriptor.dependency = bundleInstruction.dependency
                bundleDescriptor.startLevel = bundleInstruction.startLevel
            }

            return renderUrl(bundleDescriptor, bundleInstruction)
        }
	}

    /**
     *
     * @param feature
     * @param dependencyMap
     * @param configuration
     * @param extension
     * @param includeRoot
     */
	static void collectDependencies(
			FeatureDescriptor feature,
			Map<ModuleVersionIdentifier,BundleDescriptor> dependencyMap,
			Configuration configuration,
			KarafFeaturesTaskExtension extension,
			boolean includeRoot) {

		collectOrderedDependencies(
            feature,
            dependencyMap,
            configuration.incoming.resolutionResult.root,
            extension,
            includeRoot,
            new HashSet() )

		configuration.resolvedConfiguration.resolvedArtifacts.each {  artifact ->
			feature.project.logger.debug("Collect dependencies for feature '${feature.name}': add module id '${artifact.moduleVersion.id}'")
            dependencyMap[ artifact.moduleVersion.id ]?.with {
                path = artifact.file
                type = artifact.type
            }
		}
	}

    /**
     * Recursive method walking the dependency graph depth first in order to
     * build a a set of dependencies ordered by their transitivity depth.
     *
     * @param feature
     * @param dependencyMap
     * @param resolvedComponentResult
     * @param extension
     * @param includeResolvedComponentResult
     * @param processedComponents
     */
	static void collectOrderedDependencies(
			FeatureDescriptor feature,
			Map<ModuleVersionIdentifier,BundleDescriptor> dependencyMap,
			ResolvedComponentResult resolvedComponentResult,
			KarafFeaturesTaskExtension extension,
			boolean includeResolvedComponentResult,
			Set<ModuleVersionIdentifier> processedComponents) {

		feature.project.logger.debug("Processing dependency '${resolvedComponentResult}' for feature '${feature.name}'")
		final BundleInstructionDescriptor bundleInstructions = findBundleInstructions( resolvedComponentResult.moduleVersion, feature )
		if ( bundleInstructions != null && !bundleInstructions.include ) {
			return;
		}

		if (processedComponents.contains(resolvedComponentResult.moduleVersion)) {
			return
		}

        resolvedComponentResult.dependencies.findAll { it instanceof ResolvedDependencyResult }.each {
            processedComponents.add(resolvedComponentResult.moduleVersion)
            collectOrderedDependencies(
                feature,
                dependencyMap,
                ((ResolvedDependencyResult) it).selected,
                extension,
                true,
                processedComponents)
		}

		// then add this one (if param says to)
		if ( includeResolvedComponentResult && resolvedComponentResult.moduleVersion.group) {
            dependencyMap.put(resolvedComponentResult.moduleVersion, new BundleDescriptor(resolvedComponentResult))
		}
	}

    /**
     *
     * @param versionIdentifier
     * @param feature
     * @return
     */
	static BundleInstructionDescriptor findBundleInstructions(ModuleVersionIdentifier versionIdentifier, FeatureDescriptor feature) {
		for ( BundleInstructionDescriptor it : feature.bundles ) {
			if ( it.matcher.matches( versionIdentifier ) ) {
				return it
			}
		}

		return null
	}

    /**
     *
     * @param bundleDescriptor
     * @param bundleInstructionDescriptor
     * @param resolvedBundleArtifact
     * @return
     */
	static BundleDescriptor renderUrl(
			BundleDescriptor bundleDescriptor,
			BundleInstructionDescriptor bundleInstructionDescriptor) {

        bundleDescriptor.url = baseMvnUrl( bundleDescriptor )

		if (bundleInstructionDescriptor) {

            if(bundleDescriptor.isWar() || bundleInstructionDescriptor.hasExplicitWarType()) {
                bundleDescriptor.url = "${bundleDescriptor.url}/war"
            }

			if(bundleInstructionDescriptor.hasExplicitWrapInstructions() ) {
                bundleDescriptor.url = "wrap:${bundleDescriptor.url}"

                def sep = '?'
                bundleInstructionDescriptor.bundleWrapInstructionsDescriptor?.instructions.each { key , val ->
                    // do these need to be encoded?
                    bundleDescriptor.url = "${bundleDescriptor.url}${sep}${key}=${val}"
                    sep = '&'
                }
            }
		} else if (bundleDescriptor && bundleDescriptor.path && !hasOsgiManifestHeaders( bundleDescriptor.path ) ) {
			// if the resolved file does not have "proper" OSGi headers we
			// implicitly do the wrap as a courtesy...
            bundleDescriptor.url = "wrap:${bundleDescriptor.url}"
		}

		return bundleDescriptor
	}

    /**
     *
     * @param bundleCoordinates
     * @return
     */
	public static String baseMvnUrl(BundleDescriptor bundleCoordinates) {
		def gnv = "${bundleCoordinates.version.group}/${bundleCoordinates.version.name}/${bundleCoordinates.version.version}";
        return bundleCoordinates.isWar() ? "mvn:${gnv}/war" : "mvn:${gnv}"
    }

    /**
     *
     * @param file
     * @return
     */
    public static boolean hasOsgiManifestHeaders(File file) {
        JarFile jarFile = new JarFile( file )
        Manifest manifest = jarFile.getManifest()
        if ( manifest != null ) {
            if ( hasAttribute( manifest, "Bundle-SymbolicName" ) ) {
                return true
            }
            if ( hasAttribute( manifest, "Bundle-Name" ) ) {
                return true
            }
        }

        return false
    }
}