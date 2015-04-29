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
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.specs.Specs
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar

import groovy.xml.MarkupBuilder

/**
 * The Gradle task to perform generation of a Karaf features file from a project's
 * runtime dependencies.
 */
class KarafFeaturesGenTask extends DefaultTask {
    public static final String NAME = 'generateKarafFeatures'

    public KarafFeaturesGenTask() {
        project.afterEvaluate {
            extension.projects.each { selectedProject ->
                // we need access the jar for any project we generate feature for
                dependsOn( selectedProject.tasks.jar )

                // we also want our inputs to be based on the runtime configuration
                inputs.files( selectedProject.configurations.runtime )
            }

            // if there is an output file, add that as an output
            if ( extension.outputFile != null ) {
                outputs.file( extension.outputFile )
            }
        }
    }

    @TaskAction
    def doExecuteTask() {
        def writer = new StringWriter()
        def builder = new MarkupBuilder(writer)

        builder.features(xmlns:'http://karaf.apache.org/xmlns/features/v1.0.0') {
            extension.projects.each { selectedProject ->
                // The LinkedHashSet here will hold the dependencies in order, transitivity depth first
                LinkedHashSet<ResolvedComponentResult> orderedDependencies = new LinkedHashSet<ResolvedComponentResult>()
                collectOrderedDependencies( orderedDependencies, selectedProject.configurations.runtime.incoming.resolutionResult.root )

                builder.feature(name:"${selectedProject.name}", version:"${selectedProject.version}") {
                    generateBundles( selectedProject, builder, orderedDependencies )

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
     */
    void collectOrderedDependencies(
            LinkedHashSet<ResolvedComponentResult> orderedDependencies,
            ResolvedComponentResult resolvedComponentResult) {
        if ( shouldExclude( resolvedComponentResult ) ) {
            return;
        }

        // add dependencies first
        resolvedComponentResult.dependencies.each {
            if ( it instanceof UnresolvedDependencyResult ) {
                // skip it
                logger.debug( "Skipping dependency [%s] as it is unresolved", it.requested.displayName )
                return;
            }

            collectOrderedDependencies( orderedDependencies, ( (ResolvedDependencyResult) it ).selected )
        }

        // then add this one
        orderedDependencies.add( resolvedComponentResult )
    }

    /**
     * Should the dependency indicated be excluded from adding as a bundle to the feature?
     *
     * @param dep The dependency resolution result for the dependency to check
     *
     * @return {@code true} indicates the dependency should be excluded; {@code false} indicates it should not.
     */
    def shouldExclude(ResolvedComponentResult dep) {
        return matchesPattern( dep, extension.excludes )
    }

    /**
     * Using the passed MarkupBuilder, generate {@code <bundle/>} element for each dependency.
     *
     * @param builder The MarkupBuilder to use.
     * @param orderedDependencies The ordered set of dependencies
     */
    void generateBundles(
            Project selectedProject,
            MarkupBuilder builder,
            LinkedHashSet<ResolvedComponentResult> orderedDependencies) {

        // The determination of whether to wrap partially involves seeing if the
        // artifact (file) resolved from the dependency defined OSGi metadata.  So we need a Map
        // of the ResolvedArtifacts by their identifier (GAV)
        def Map<ModuleVersionIdentifier,ResolvedArtifact> resolvedArtifactMap = new HashMap<ModuleVersionIdentifier,ResolvedArtifact>()
        selectedProject.configurations.runtime.resolvedConfiguration.resolvedArtifacts.each {
            resolvedArtifactMap.put( it.moduleVersion.id, it );
        }

        orderedDependencies.each { dep ->
            def mavenUrl = "mvn:${dep.moduleVersion.group}/${dep.moduleVersion.name}/${dep.moduleVersion.version}"

            if ( shouldWrap( dep, selectedProject, resolvedArtifactMap ) ) {
                mavenUrl = "wrap:${mavenUrl}"
            }

            def startLevel = getBundleStartLevel( dep )
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
     *
     * @return {@code true} to indicate that the dependency should be wrapped; {@code false} indicates it should not.
     */
    boolean shouldWrap(
            ResolvedComponentResult dep,
            Project selectedProject,
            Map<ModuleVersionIdentifier,ResolvedArtifact> resolvedArtifactMap) {
        if ( matchesPattern( dep, extension.wraps ) ) {
            return true;
        }

        if ( matchesPattern( dep, "${selectedProject.group}/${selectedProject.name}/${selectedProject.version}") ) {
            return !hasOsgiManifestHeaders( ( selectedProject.tasks.jar as Jar ).archivePath )
        }

        ResolvedArtifact resolvedArtifact = resolvedArtifactMap.get( dep.moduleVersion )
        if ( resolvedArtifact != null ) {
            return !hasOsgiManifestHeaders( resolvedArtifact.file )
        }

        return true
    }

    public boolean hasOsgiManifestHeaders(File file) {
        JarFile jarFile = new JarFile( file );
        Manifest manifest = jarFile.getManifest();
        if ( manifest != null ) {
            logger.lifecycle( "Found manifest [${file.absolutePath}], checking for OSGi metadata" )
            if ( hasAttribute( manifest, "Bundle-SymbolicName" ) ) {
                logger.lifecycle( "    >> Found Bundle-SymbolicName" )
                return true;
            }
            if ( hasAttribute( manifest, "Bundle-Name" ) ) {
                logger.lifecycle( "    >> Found Bundle-Name" )
                return true;
            }
        }

        return false;
    }

    public static boolean hasAttribute(Manifest manifest, String attributeName) {
        String value = manifest.mainAttributes.getValue( attributeName )
        return value != null && !value.trim().isEmpty()
    }

    def getBundleStartLevel(ResolvedComponentResult dep) {
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

    KarafFeaturesGenTaskExtension extension_;

    def getExtension() {
        // Don't keep looking it up...
        if ( extension_ == null ) {
            extension_ = project.extensions.findByName( KarafFeaturesGenTaskExtension.NAME ) as KarafFeaturesGenTaskExtension
        }
        return extension_;
    }
}
