/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.client.HttpClientConfiguration;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;

@ArtifactClassLoaderRunnerConfig(plugins = {"org.mule.modules:mule-module-sockets", "org.mule.modules:mule-module-http-ext",
    "com.mulesoft.weave:mule-plugin-weave"},
    providedInclusions = "org.mule.modules:mule-module-sockets")
public abstract class AbstractHttpTestCase extends MuleArtifactFunctionalTestCase {

  protected static final int DEFAULT_TIMEOUT = 1000;

  /**
   * This client is used to hit http listeners under test.
   */
  protected HttpClient httpClient;

  @Before
  public void createHttpClient() throws RegistrationException, IOException, InitialisationException {
    httpClient = muleContext.getRegistry().lookupObject(HttpService.class).getClientFactory()
        .create(new HttpClientConfiguration.Builder().build());
    httpClient.start();
  }

  @After
  public void disposeHttpClient() {
    httpClient.stop();
  }
}
