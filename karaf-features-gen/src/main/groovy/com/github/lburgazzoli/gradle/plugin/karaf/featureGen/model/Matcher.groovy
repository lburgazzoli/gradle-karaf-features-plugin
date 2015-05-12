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
package com.github.lburgazzoli.gradle.plugin.karaf.featureGen.model;

import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;

/**
 * @author Steve Ebersole
 */
public class Matcher {
	def String group
	def String module
	def String version

	public boolean matches(ResolvedComponentResult check) {
		return matches( check.moduleVersion )
	}

	public boolean matches(ModuleVersionIdentifier check) {
		return check.group.equals( group ) && check.name.equals( module ) && ( version == null || check.version.equals( version ) )
	}

	public ModuleVersionIdentifier asModuleVersionIdentifier() {
		return new DefaultModuleVersionIdentifier( group, module, version )
	}
}
