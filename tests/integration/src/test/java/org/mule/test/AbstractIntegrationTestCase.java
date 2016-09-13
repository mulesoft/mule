/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.test.runner.ArtifactClassLoaderRunner;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

/**
 * Base {@link Class} for functional integration tests, it will run the functional test case with
 * {@link ArtifactClassLoaderRunner} in order to use the hierarchical {@link ClassLoader}'s as
 * standalone mode. Every test on integration module should extend from this base {@link Class}.
 *
 * @since 4.0
 */
@ArtifactClassLoaderRunnerConfig(
    plugins = {"org.mule.modules:mule-module-validation", "org.mule.modules:mule-module-file"})
public abstract class AbstractIntegrationTestCase extends MuleArtifactFunctionalTestCase {

}
