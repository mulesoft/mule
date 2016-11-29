/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.extensions;

import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

/**
 * Defines the {@link ArtifactClassLoaderRunnerConfig} configuration for {@link CompatibilityFunctionalTestCase}.
 */
@ArtifactClassLoaderRunnerConfig(plugins = {"org.mule.modules:mule-module-sockets", "org.mule.modules:mule-module-http-ext"},
    providedInclusions = "org.mule.modules:mule-module-sockets")
public interface CompatibilityFunctionalTestCaseRunnerConfig {

}
