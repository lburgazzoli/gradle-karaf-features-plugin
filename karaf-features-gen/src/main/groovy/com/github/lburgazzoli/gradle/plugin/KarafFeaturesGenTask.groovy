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
package com.github.lburgazzoli.gradle.plugin

import java.util.jar.JarFile
import java.util.jar.Manifest

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar

import groovy.xml.MarkupBuilder

/**
 * The Gradle task to perform generation of a Karaf features repository file (XML or kar)
 *
 * @author Luca Burgazzoli
 * @author Steve Ebersole
 */
class KarafFeaturesGenTask extends DefaultTask {

    public KarafFeaturesGenTask() {
        super();
    }

    @TaskAction
    def generateFeaturesFile() {
        def writer = new StringWriter()
        def builder = new MarkupBuilder(writer)

        builder.features(xmlns:'http://karaf.apache.org/xmlns/features/v1.0.0') {
            extension.features.each { feature->
                // The LinkedHashMap here will hold the dependencies in order, transitivity depth first
                //      IMPL NOTE: Initially tried LinkedHashSet<ResolvedComponentResult>, but
                //          ResolvedComponentResult does not properly implement equals/hashCode in terms
                //          of GAV which is what we need this uniquely based on.  So for now we use
                //          a LinkedHashMap keyed by the GAV.
                LinkedHashMap<ModuleVersionIdentifier,ResolvedComponentResult> orderedDependencyMap = new LinkedHashMap<ModuleVersionIdentifier,ResolvedComponentResult>()
                // The Map here will hold all resolved artifacts (access to the actual files) for each dependency
                Map<ModuleVersionIdentifier,File> resolvedArtifactMap = new HashMap<ModuleVersionIdentifier,File>()

                collectDependencies( feature, orderedDependencyMap, resolvedArtifactMap, extraBundles )

                feature.extraBundleDependencies.each {
                    collectDependencies( feature, orderedDependencyMap, resolvedArtifactMap, it )
                }

                feature.projects.each { bundledProject ->
                    collectDependencies( feature, orderedDependencyMap, resolvedArtifactMap, bundledProject.configurations.runtime )
                    resolvedArtifactMap.put(
                            new DefaultModuleVersionIdentifier(
                                    "${bundledProject.group}",
                                    "${bundledProject.name}",
                                    "${bundledProject.version}"
                            ),
                            ( bundledProject.tasks.jar as Jar ).archivePath
                    )
                }

                builder.feature(name:"${feature.name}", version:"${feature.version}") {
                    generateBundles( builder, orderedDependencyMap, resolvedArtifactMap, feature )
                }
            }
        }

        // for now just write out a features repository xml.  Still some open questions wrt
        extension.outputDir.mkdirs()
        def out = new BufferedWriter(
                new FileWriter(
                        new File( extension.outputDir, extension.featuresXmlFileName )
                )
        )
        out.write( writer.toString() )
        out.close()
    }

    void collectDependencies(
            FeatureDescriptor feature,
            LinkedHashMap<ModuleVersionIdentifier,ResolvedComponentResult> orderedDependencyMap,
            Map<ModuleVersionIdentifier, File> resolvedArtifactMap,
            Configuration configuration) {
        collectOrderedDependencies( feature, orderedDependencyMap, configuration.incoming.resolutionResult.root )

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
    void collectOrderedDependencies(
            FeatureDescriptor feature,
            LinkedHashMap<ModuleVersionIdentifier,ResolvedComponentResult> orderedDependencyMap,
            ResolvedComponentResult resolvedComponentResult) {
        if ( shouldExclude( feature, resolvedComponentResult ) ) {
            return;
        }

        // add dependencies first
        resolvedComponentResult.dependencies.each {
            if ( it instanceof UnresolvedDependencyResult ) {
                // skip it
                logger.debug( "Skipping dependency [%s] as it is unresolved", it.requested.displayName )
                return;
            }

            collectOrderedDependencies( feature, orderedDependencyMap, ( (ResolvedDependencyResult) it ).selected )
        }

        // then add this one
        orderedDependencyMap.put( resolvedComponentResult.moduleVersion, resolvedComponentResult )
    }

    /**
     * Should the dependency indicated be excluded from adding as a bundle to the feature?
     *
     * @param dep The dependency resolution result for the dependency to check
     *
     * @return {@code true} indicates the dependency should be excluded; {@code false} indicates it should not.
     */
    static def shouldExclude(FeatureDescriptor feature, ResolvedComponentResult dep) {
        final BundleInstructionsDescriptor bundleInstructions = findBundleInstructions( dep, feature )
        return !bundleInstructions.include;
    }

    /**
     * Using the passed MarkupBuilder, generate {@code <bundle/>} element for each dependency.
     *
     * @param builder The MarkupBuilder to use.
     * @param orderedDependencies The ordered set of dependencies
     */
    static void generateBundles(
            MarkupBuilder builder,
            LinkedHashMap<ModuleVersionIdentifier,ResolvedComponentResult> orderedDependencyMap,
            Map<ModuleVersionIdentifier,File> resolvedArtifactMap,
            FeatureDescriptor feature) {
        orderedDependencyMap.values().each { dep ->
            final BundleInstructionsDescriptor bundleInstructions = findBundleInstructions( dep, feature )

            if ( !bundleInstructions.include ) {
                return
            }

            def bundleUrl = "mvn:${dep.moduleVersion.group}/${dep.moduleVersion.name}/${dep.moduleVersion.version}"
            bundleUrl = adjustUrl( bundleUrl, bundleInstructions, dep, resolvedArtifactMap )

            if ( bundleInstructions.startLevel == null ) {
                builder.bundle( bundleUrl )
            }
            else {
                builder.bundle( "start-level": bundleInstructions.startLevel, bundleUrl )
            }
        }
    }

    static BundleInstructionsDescriptor findBundleInstructions(ResolvedComponentResult dep, FeatureDescriptor feature) {
        feature.bundles.each {
            if ( matchesPattern( dep, it.selector ) ) {
                return it;
            }
        }

        return new BundleInstructionsDescriptor();
    }

    static matchesPattern = { ResolvedComponentResult dep, patterns ->
        for ( String pattern : patterns ) {
            if ( "${dep.moduleVersion.group}/${dep.moduleVersion.name}/${dep.moduleVersion.version}".matches( pattern ) ) {
                return true;
            }
        }

        return false
    }

    static def adjustUrl(
            String url,
            BundleInstructionsDescriptor bundleInstructions,
            ResolvedComponentResult dep,
            Map<ModuleVersionIdentifier,File> resolvedArtifactMap) {
        if ( bundleInstructions.wrap != null ) {
            url = "wrap:${url}"
            if ( bundleInstructions.wrap.instructions != null ) {
                def sep = '?'
                bundleInstructions.wrap.instructions.entrySet().each {
                    // do these need to be encoded?
                    url = "${url}${sep}${it.key}=${it.value}"
                    sep = '&'
                }
            }
            return url;
        }

        File resolvedArtifact = resolvedArtifactMap.get( dep.moduleVersion )
        if ( resolvedArtifact != null ) {
            if ( !hasOsgiManifestHeaders( resolvedArtifact ) ) {
                return  "wrap:${url}"
            }
        }

        return url;
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

    KarafFeaturesGenTaskExtension extension_;

    def getExtension() {
        // Don't keep looking it up...
        if ( extension_ == null ) {
            extension_ = project.extensions.findByName( KarafFeaturesGenPlugin.EXTENSION_NAME ) as KarafFeaturesGenTaskExtension
        }
        return extension_;
    }


    Configuration extraBundles_;

    def getExtraBundles() {
        // Don't keep looking it up...
        if ( extraBundles_ == null ) {
            extraBundles_ = project.configurations.findByName( KarafFeaturesGenPlugin.CONFIGURATION_NAME )
        }
        return extraBundles_;
    }
}
