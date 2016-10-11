/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.extensions;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

/**
 * Provides a way for modules to be tested in a flow called through http, using the extension for that.
 * 
 * @since 4.0
 */
@ArtifactClassLoaderRunnerConfig(plugins = {"org.mule.modules:mule-module-sockets", "org.mule.modules:mule-module-http-ext"},
    providedInclusions = "org.mule.modules:mule-module-sockets")
public abstract class UsesHttpExtensionFunctionalTestCase extends MuleArtifactFunctionalTestCase {

}
