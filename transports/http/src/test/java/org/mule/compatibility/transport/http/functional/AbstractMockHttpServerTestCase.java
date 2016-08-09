/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.FunctionalTestCase;

public abstract class AbstractMockHttpServerTestCase extends FunctionalTestCase {

  private MockHttpServer mockHttpServer;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    mockHttpServer = getHttpServer();
    new Thread(mockHttpServer).start();

    assertTrue("MockHttpServer start failed", mockHttpServer.waitUntilServerStarted());
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();

    assertTrue("MockHttpServer failed to shut down", mockHttpServer.waitUntilServerStopped());
  }

  /**
   * Subclasses must implement this method to return their Subclass of {@link MockHttpServer}.
   */
  protected abstract MockHttpServer getHttpServer();
}
