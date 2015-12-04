/*
 * Copyright 2015, Luca Burgazzoli and contributors as indicated by the @author tags
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
package com.github.lburgazzoli.gradle.plugin.karaf.features.tasks

import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * @author lburgazzoli
 */
abstract class KarafTask extends DefaultTask {
    private KarafFeaturesTaskExtension extension_;
    private Configuration extraBundles_;

    public KarafTask() {
        super();
    }

    def KarafFeaturesTaskExtension getExtension() {
        // Don't keep looking it up...
        if (extension_ == null) {
            extension_ = lookupExtension(project)
        }

        return extension_;
    }


    def getExtraBundles() {
        // Don't keep looking it up...
        if (extraBundles_ == null) {
            extraBundles_ = lookupExtraBundlesConfiguration(project)
        }

        return extraBundles_;
    }


    public static KarafFeaturesTaskExtension lookupExtension(Project project) {
        return project.extensions.findByName(KarafFeaturesPlugin.EXTENSION_NAME) as KarafFeaturesTaskExtension
    }

    public static Configuration lookupExtraBundlesConfiguration(Project project) {
        return project.configurations.findByName(KarafFeaturesPlugin.CONFIGURATION_NAME)
    }
}
