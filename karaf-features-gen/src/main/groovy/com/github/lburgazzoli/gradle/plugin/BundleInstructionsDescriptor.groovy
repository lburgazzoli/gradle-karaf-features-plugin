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
package com.github.lburgazzoli.gradle.plugin

import org.gradle.util.ConfigureUtil

/**
 * Models the information used to in generating a single {@code <bundle/>} entry
 * in a Karaf features repository file
 *
 * @author Steve Ebersole
 */
class BundleInstructionsDescriptor {
	def String selector
	def boolean include = true;
	def String startLevel;

	private BundleWrapInstructionsDescriptor wrap;

	def wrap(Closure closure) {
		if ( wrap == null ) {
			wrap = new BundleWrapInstructionsDescriptor()
			wrap.requested = true
		}
		ConfigureUtil.configure( closure, wrap )
	}

	BundleWrapInstructionsDescriptor getWrap() {
		return wrap;
	}
}
