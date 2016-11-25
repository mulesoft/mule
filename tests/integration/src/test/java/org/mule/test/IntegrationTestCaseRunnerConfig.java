/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test;

import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

/**
 * Defines the {@link org.mule.test.runner.ArtifactClassLoaderRunnerConfig} for integration tests.
 */
@ArtifactClassLoaderRunnerConfig(
    plugins = {"org.mule.modules:mule-module-validation", "org.mule.modules:mule-module-file",
        "org.mule.modules:mule-module-http-ext", "org.mule.modules:mule-module-sockets"})
public interface IntegrationTestCaseRunnerConfig {

}
