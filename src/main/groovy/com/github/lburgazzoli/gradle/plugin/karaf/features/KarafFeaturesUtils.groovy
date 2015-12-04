/*
 * Copyright 2015, contributors as indicated by the @author tags
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

import com.github.lburgazzoli.gradle.plugin.karaf.features.tasks.KarafFeaturesTaskExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier

import java.util.jar.Manifest

/**
 * @author lburgazzoli
 */
class KarafFeaturesUtils {

    public static KarafFeaturesTaskExtension lookupExtension(Project project) {
        return project.extensions.findByName(KarafFeaturesPlugin.EXTENSION_NAME) as KarafFeaturesTaskExtension
    }

    public static Configuration lookupExtraBundlesConfiguration(Project project) {
        return project.configurations.findByName(KarafFeaturesPlugin.CONFIGURATION_NAME)
    }

    public static ModuleVersionIdentifier asModuleVersionIdentifier(Project project) {
        new DefaultModuleVersionIdentifier(
            "${project.group}",
            "${project.name}",
            "${project.version}"
        )
    }

    public static boolean hasAttribute(Manifest manifest, String attributeName) {
        String value = manifest.mainAttributes.getValue( attributeName )
        return value != null && !value.trim().isEmpty()
    }
}
