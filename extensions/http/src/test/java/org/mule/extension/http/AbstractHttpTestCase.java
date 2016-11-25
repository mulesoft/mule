/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.http;

import org.mule.extension.http.internal.request.HttpRequesterConfig;
import org.mule.extension.http.internal.request.HttpRequesterProvider;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

@ArtifactClassLoaderRunnerConfig(plugins = {"org.mule.modules:mule-module-sockets"},
    providedInclusions = "org.mule.modules:mule-module-sockets", exportPluginClasses = {HttpRequesterProvider.class,
        HttpRequesterConfig.class})
public abstract class AbstractHttpTestCase extends MuleArtifactFunctionalTestCase {

}
