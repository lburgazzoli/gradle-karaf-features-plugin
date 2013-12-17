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
import org.gradle.api.tasks.TaskAction

/**
 *
 */
class KarafFeaturesGenTask extends DefaultTask {

    /**
     *
     */
    @TaskAction
    def doExecuteTask() {
        def writer = new StringWriter()
        def builder = new MarkupBuilder(writer);

        builder.features(xmlns:'http://karaf.apache.org/xmlns/features/v1.0.0') {
            if(project.subprojects.size() > 0) {
                project.subprojects.each { subproject ->
                    feature(name:"${subproject.name}", version:"${subproject.version}") {
                        processRuntimeDependencies(builder,subproject.configurations.runtime.allDependencies)
                    }
                }
            } else {
                feature(name:"${project.name}", version:"${project.version}") {
                    processRuntimeDependencies(builder,project.configurations.runtime.allDependencies)
                }
            }
        }

        println writer.toString()
    }

    /**
     *
     * @param builder
     * @param dependencies
     * @return
     */
    def processRuntimeDependencies(builder,dependencies) {
        dependencies.each { dep ->
            if(dep.group != null && dep.version != null && !isExcluded(dep)) {
                def startLevel = getBundleStartLevel(dep)
                def mavenUrl = "mvn:${dep.group}/${dep.name}/${dep.version}"

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
        return matchesPattern(dep,project.karafFeatures.wraps)
    }

    /**
     *
     * @param dep
     * @return
     */
    def getBundleStartLevel(dep) {
        String startLevel = null
        project.karafFeatures.startLevels.each { pattern, sl ->
            if("${dep.group}/${dep.name}/${dep.version}".matches(pattern as String)) {
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
    def matchesPattern(dep,patterns) {
        for(String pattern : patterns) {
            if("${dep.group}/${dep.name}/${dep.version}".matches(pattern)) {
                return true;
            }
        }

        return false
    }
}
