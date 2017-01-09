/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.performance.util;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Rule;

@ArtifactClassLoaderRunnerConfig(
    plugins = {"org.mule.modules:mule-module-ftp", "org.mule.modules:mule-module-file", "org.mule.modules:mule-module-http-ext",
        "org.mule.modules:mule-module-sockets"},
    providedInclusions = "org.mule.modules:mule-module-sockets")
public abstract class AbstractIsolatedFunctionalPerformanceTestCase extends MuleArtifactFunctionalTestCase {

  @Rule
  public ContiPerfRule rule = new ContiPerfRule();

}
