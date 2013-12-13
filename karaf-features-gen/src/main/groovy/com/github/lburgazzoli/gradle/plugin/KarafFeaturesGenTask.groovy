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

        new MarkupBuilder(writer).features(xmlns:'http://karaf.apache.org/xmlns/features/v1.0.0') {
            if(project.subprojects.size() > 0) {
                project.subprojects.each { subproject ->
                    feature(name:"${subproject.name}", version:"${subproject.version}") {
                        subproject.configurations.runtime.allDependencies.each { dep ->
                            if(dep.group != null && dep.version != null && !isExcluded(dep)) {
                                def sl = getStartLevel(dep)
                                def bundleStr = null

                                if(isWrapped(dep)) {
                                    bundleStr = "wrap:mvn:${dep.group}/${dep.name}/${dep.version}"
                                } else {
                                    bundleStr = "mvn:${dep.group}/${dep.name}/${dep.version}"
                                }

                                if(sl == null) {
                                    bundle(bundleStr)
                                } else {
                                    bundle("start-level": sl,bundleStr)
                                }
                            }
                        }
                    }
                }
            } else {
                feature(name:"${project.name}", version:"${project.version}") {
                    project.configurations.runtime.allDependencies.each { dep ->
                        if(dep.group != null && dep.version != null && !isExcluded(dep)) {
                            def sl = getStartLevel(dep)
                            def bundleStr = null

                            if(isWrapped(dep)) {
                                bundleStr = "wrap:mvn:${dep.group}/${dep.name}/${dep.version}"
                            } else {
                                bundleStr = "mvn:${dep.group}/${dep.name}/${dep.version}"
                            }

                            if(sl == null) {
                                bundle(bundleStr)
                            } else {
                                bundle("start-level": sl,bundleStr)
                            }
                        }
                    }
                }
            }
        }

        println writer.toString()
    }

    /**
     *
     * @param dep
     * @return
     */
    def isExcluded(dep) {
        return matchPattern(dep,project.karafFeatures.excludes)
    }

    /**
     *
     * @param dep
     * @return
     */
    def isWrapped(dep) {
        return matchPattern(dep,project.karafFeatures.wraps)
    }

    /**
     *
     * @param dep
     * @return
     */
    def getStartLevel(dep) {
        String startLevel = null
        if(project.karafFeatures.startLevels) {
            project.karafFeatures.startLevels.each { pattern, sl ->
                if("${dep.group}/${dep.name}/${dep.version}".matches(pattern)) {
                    startLevel = sl
                }
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
    def matchPattern(dep,patterns) {
        for(String pattern : patterns) {
            if("${dep.group}/${dep.name}/${dep.version}".matches(pattern)) {
                return true;
            }
        }

        return false
    }
}
