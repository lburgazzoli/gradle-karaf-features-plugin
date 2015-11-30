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
package com.github.lburgazzoli.gradle.plugin.karaf.features.model

import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
/**
 * @author lburgazzoli
 */
class BundleDescriptor {

    ModuleVersionIdentifier version
    String type
    File path

    public BundleDescriptor(String group, String artifact, String version) {
        this(new DefaultModuleVersionIdentifier(group, artifact, version), null, 'jar')
    }

    public BundleDescriptor(String group, String artifact, String version, File path) {
        this(new DefaultModuleVersionIdentifier(group, artifact, version), path, 'jar')
    }

    public BundleDescriptor(ResolvedComponentResult result) {
        this(result.moduleVersion, null, 'jar')
    }

    public BundleDescriptor(ModuleVersionIdentifier version, File path) {
        this(version, path, 'jar')
    }

    public BundleDescriptor(ModuleVersionIdentifier version, File path, String type) {
        this.version = version
        this.type = type
        this.path = path
    }

    boolean isJar() {
        return this.type.equals('jar')
    }

    boolean isWar() {
        return this.type.equals('war')
    }
}
