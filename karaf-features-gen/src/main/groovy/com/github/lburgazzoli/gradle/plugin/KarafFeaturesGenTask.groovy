/**
 * Copyright 2013 lb
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
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.specs.Specs
import org.gradle.api.tasks.TaskAction

import groovy.xml.MarkupBuilder

/**
 * The Gradle task to perform generation of a Karaf features file from a project's
 * runtime dependencies.
 */
class KarafFeaturesGenTask extends DefaultTask {
    public static final String NAME = 'generateKarafFeatures'

    public KarafFeaturesGenTask() {
        getOutputs().upToDateWhen(Specs.satisfyNone());
    }

    @TaskAction
    def doExecuteTask() {
        def writer = new StringWriter()
        def builder = new MarkupBuilder(writer)

        // Don't keep looking it up...
        KarafFeaturesGenTaskExtension extension = project.extensions.findByName( KarafFeaturesGenTaskExtension.NAME ) as KarafFeaturesGenTaskExtension

        // The LinkedHashSet here will hold the dependencies in order, transitivity depth first
        LinkedHashSet<ResolvedComponentResult> orderedDependencies = new LinkedHashSet<ResolvedComponentResult>()
        collectOrderedDependencies( orderedDependencies, project.configurations.runtime.incoming.resolutionResult.root, extension )

        builder.features(xmlns:'http://karaf.apache.org/xmlns/features/v1.0.0') {
            extension.projects.each { project ->
                builder.feature(name:"${project.name}", version:"${project.version}") {
                    generateBundles( builder, orderedDependencies, extension )
                    extension.extraBundles.each { extraBundle ->
                        builder.bundle( extraBundle )
                    }
                }
            }
        }

        if ( extension.outputFile != null ) {
            def out = new BufferedWriter(new FileWriter(extension.outputFile))
            out.write(writer.toString())
            out.close()
        }
        else {
            println writer.toString()
        }
    }

    /**
     * Recursive method walking the dependency graph depth first in order to build a a set of
     * dependencies ordered by their transitivity depth.
     *
     * @param orderedDependencies The ordered set of dependencies being built
     * @param resolvedComponentResult The dependency to process
     * @param extension The karafFeatures extension
     */
    static void collectOrderedDependencies(
            LinkedHashSet<ResolvedComponentResult> orderedDependencies,
            ResolvedComponentResult resolvedComponentResult,
            KarafFeaturesGenTaskExtension extension) {
        if ( shouldExclude( resolvedComponentResult, extension ) ) {
            return;
        }

        // add dependencies first
        resolvedComponentResult.dependencies.each {
            if ( it instanceof UnresolvedDependencyResult ) {
                // skip it
                logger.debug( "Skipping dependency [%s] as it is unresolved", it.requested.displayName )
                return;
            }

            collectOrderedDependencies( orderedDependencies, ( (ResolvedDependencyResult) it ).selected, extension )
        }

        // then add this one
        orderedDependencies.add( resolvedComponentResult )
    }

    /**
     * Should the dependency indicated be excluded from adding as a bundle to the feature?
     *
     * @param dep The dependency resolution result for the dependency to check
     * @param extension The karafFeatures extension
     *
     * @return {@code true} indicates the dependency should be excluded; {@code false} indicates it should not.
     */
    static def shouldExclude(ResolvedComponentResult dep, KarafFeaturesGenTaskExtension extension) {
        return matchesPattern( dep, extension.excludes )
    }

    /**
     * Using the passed MarkupBuilder, generate {@code <bundle/>} element for each dependency.
     *
     * @param builder The MarkupBuilder to use.
     * @param orderedDependencies The ordered set of dependencies
     * @param extension The karafFeatures extension
     */
    void generateBundles(
            MarkupBuilder builder,
            LinkedHashSet<ResolvedComponentResult> orderedDependencies,
            KarafFeaturesGenTaskExtension extension) {

        // The determination of whether to wrap partially involves seeing if the
        // artifact (file) resolved from the dependency defined OSGi metadata.  So we need a Map
        // of the ResolvedArtifacts by their identifier (GAV)
        def Map<ModuleVersionIdentifier,ResolvedArtifact> resolvedArtifactMap = new HashMap<ModuleVersionIdentifier,ResolvedArtifact>()
        project.configurations.runtime.resolvedConfiguration.resolvedArtifacts.each {
            resolvedArtifactMap.put( it.moduleVersion.id, it );
        }

        orderedDependencies.each { dep ->
            def mavenUrl = "mvn:${dep.moduleVersion.group}/${dep.moduleVersion.name}/${dep.moduleVersion.version}"

            if ( shouldWrap( dep, extension, resolvedArtifactMap ) ) {
                mavenUrl = "wrap:${mavenUrl}"
            }

            def startLevel = getBundleStartLevel(dep, extension)
            if ( startLevel == null ) {
                builder.bundle(mavenUrl)
            }
            else {
                builder.bundle("start-level": startLevel, mavenUrl)
            }
        }
    }

    /**
     * Should the bundle generated from this dependency use the {@code wrap:} url scheme?
     *
     * @param dep The dependency to check
     * @param extension The karafFeatures extension
     * @param resolvedArtifactMap The map of GAV->ResolvedArtifact
     *
     * @return {@code true} to indicate that the dependency should be wrapped; {@code false} indicates it should not.
     */
    static boolean shouldWrap(
            ResolvedComponentResult dep,
            KarafFeaturesGenTaskExtension extension,
            Map<ModuleVersionIdentifier,ResolvedArtifact> resolvedArtifactMap) {
        return matchesPattern( dep, extension.wraps) || !isOsgi( dep, resolvedArtifactMap )
    }

    /**
     * Method to determine if a given jar file is am OSGi bundle.
     * This is useful for determining if we need to wrap it, determined by the existence
     * of a Bundle-SymbolicName manifest attribute..
     *
     * @param dep The dependency to check.
     * @param resolvedArtifactMap Map of dependency ids (GAV) to ResolvedArtifact
     *
     * @return True if this dependency resolved to a jar with an OSGi bundle
     */
    static boolean isOsgi(ResolvedComponentResult dep, Map<ModuleVersionIdentifier,ResolvedArtifact> resolvedArtifactMap) {
        ResolvedArtifact resolvedArtifact = resolvedArtifactMap.get( dep.moduleVersion )
        if ( resolvedArtifact == null ) {
            // then what?
            return false;
        }

        JarFile jarFile = new JarFile( resolvedArtifact.file );
        Manifest manifest = jarFile.getManifest();
        if ( manifest != null ) {
            Object value = manifest.getMainAttributes().getValue("Bundle-SymbolicName")
            if( value != null && ! value.toString().isEmpty() ) {
                return true;
            }
        }

        return false;
    }

    static def getBundleStartLevel(ResolvedComponentResult dep, KarafFeaturesGenTaskExtension extension) {
        String startLevel = null
        extension.startLevels.each { pattern, sl ->
            if("${dep.moduleVersion.group}/${dep.moduleVersion.name}/${dep.moduleVersion.version}".matches(pattern as String)) {
                startLevel = sl;
            }
        }

        return startLevel
    }

    static boolean matchesPattern(ResolvedComponentResult dep, patterns) {
        for(String pattern : patterns) {
            if("${dep.moduleVersion.group}/${dep.moduleVersion.name}/${dep.moduleVersion.version}".matches(pattern)) {
                return true;
            }
        }

        return false
    }
}
