/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.junit.Assert.assertTrue;

import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.transport.http.HttpConnector;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.message.SessionHandler;

import org.junit.Test;

public class HttpServiceOverridesTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "http-service-overrides.xml";
  }

  @Test
  public void testSessionHandler() {
    Connector connector = muleContext.getRegistry().lookupObject("httpConnector");
    assertTrue(connector instanceof HttpConnector);

    HttpConnector httpConnector = (HttpConnector) connector;
    SessionHandler sessionHandler = httpConnector.getSessionHandler();
    assertTrue(sessionHandler instanceof TestSessionHandler);
  }
}
