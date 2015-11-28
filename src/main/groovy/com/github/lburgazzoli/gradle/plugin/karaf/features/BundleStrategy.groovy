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
package com.github.lburgazzoli.gradle.plugin.karaf.features

import com.github.lburgazzoli.gradle.plugin.karaf.features.impl.BundleDefinitionCalculatorMvnImpl
import com.github.lburgazzoli.gradle.plugin.karaf.features.impl.BundleDefinitionCalculatorObrImpl

/**
 * Enumeration of the supported strategies for handling bundle definitions in the produced
 * Karaf features.
 *
 * @author Steve Ebersole
 */
enum BundleStrategy {
	/**
	 * Support for using {@code mvn:} (and {@code wrap:mvn}) urls
	 */
	MVN( BundleDefinitionCalculatorMvnImpl.INSTANCE ),
	/**
	 * Support for using {@code obr:} urls.
	 */
	OBR( BundleDefinitionCalculatorObrImpl.INSTANCE );

	def final BundleDefinitionCalculator bundleDefinitionCalculator;

	BundleStrategy(BundleDefinitionCalculator bundleDefinitionCalculator) {
		this.bundleDefinitionCalculator = bundleDefinitionCalculator
	}
}