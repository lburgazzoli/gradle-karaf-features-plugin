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

import groovy.xml.MarkupBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.specs.Specs
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.util.jar.JarFile
import java.util.jar.Manifest

/**
 *
 */
class KarafFeaturesGenTask extends DefaultTask {
		
    public KarafFeaturesGenTask() {
        getOutputs().upToDateWhen(Specs.satisfyNone());
    }

    /**
     *
     */
    @TaskAction
    def doExecuteTask() {
        def writer = new StringWriter()
        def builder = new MarkupBuilder(writer)

        builder.features(xmlns:'http://karaf.apache.org/xmlns/features/v1.0.0') {
            if(project.subprojects.size() > 0) {
                project.subprojects.each { subproject ->
                    feature(name:"${subproject.name}", version:"${subproject.version}") {
                        processRuntimeDependencies(builder,
                                subproject.configurations.runtime.resolvedConfiguration.resolvedArtifacts)
                        project.karafFeatures.extraBundles.each { dep ->
                            builder.bundle(dep)
                        }
                    }
                }
            } else {
                feature(name:"${project.name}", version:"${project.version}") {
                    processRuntimeDependencies(builder,
                            project.configurations.runtime.resolvedConfiguration.resolvedArtifacts)
                    project.karafFeatures.extraBundles.each { bundl ->
                        builder.bundle(bundl)
                    }
                }
            }
        }

        if(project.karafFeatures.outputFile != null) {
            def out = new BufferedWriter(new FileWriter(project.karafFeatures.outputFile))
            out.write(writer.toString())
            out.close()
        } else {
            println writer.toString()
        }
    }

    /**
     *
     * @param builder
     * @param dependencies
     * @return
     */
    def processRuntimeDependencies(builder, dependencyArtifacts) {
        dependencyArtifacts.each { dep ->
            if( dep.moduleVersion.id.group != null && dep.moduleVersion.id.version != null && !isExcluded(dep) ) {
                def startLevel = getBundleStartLevel(dep)
                def mavenUrl = "mvn:${dep.moduleVersion.id.group}/${dep.moduleVersion.id.name}/${dep.moduleVersion.id.version}"

                if(isWrapped(dep)) {
                    mavenUrl = "wrap:${mavenUrl}"
                }

                if(startLevel == null) {
                    builder.bundle(mavenUrl)
                } else {
                    builder.bundle("start-level": startLevel, mavenUrl)
                }
            }
        }
    }

    /**
     *
     * @param dep
     * @return
     */
    def isExcluded(dep) {
        return matchesPattern(dep,project.karafFeatures.excludes)
    }

    /**
     *
     * @param dep
     * @return
     */
    def isWrapped(dep) {
        return matchesPattern(dep,project.karafFeatures.wraps) || !isOsgi(dep.file)
    }

    /**
     * Method to determine if a given jar file is am OSGi bundle.
     * This is useful for determining if we need to wrap it, determined by the existence
     * of a Bundle-SymbolicName manifest attribute..
     * @param jar The file to check.
     * @return True if this jar is an OSGi bundle
     */
    static boolean isOsgi(File jar) {
        JarFile jarFile = new JarFile(jar);
        Manifest manifest = jarFile.getManifest();
        if( manifest != null ) {
            Object value = manifest.getMainAttributes().getValue("Bundle-SymbolicName")
            if( value != null && ! value.toString().isEmpty() ) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param dep
     * @return
     */
    def getBundleStartLevel(dep) {
        String startLevel = null
        project.karafFeatures.startLevels.each { pattern, sl ->
            if("${dep.moduleVersion.id.group}/${dep.moduleVersion.id.name}/${dep.moduleVersion.id.version}".matches(pattern as String)) {
                startLevel = sl;
            }
        }

        return startLevel
    }

    /**
     *
     * @param dep
     * @param patterns
     * @return
     */
    static def matchesPattern(ResolvedArtifact dep,patterns) {
        for(String pattern : patterns) {
            if("${dep.moduleVersion.id.group}/${dep.moduleVersion.id.name}/${dep.moduleVersion.id.version}".matches(pattern)) {
                return true;
            }
        }

        return false
    }
}
