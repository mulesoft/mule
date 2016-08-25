/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpPollingFunctionalTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "mule-http-polling-config-flow.xml";
  }

  @Test
  public void testPollingHttpConnector() throws Exception {
    FunctionalTestComponent ftc = getFunctionalTestComponent("polled");
    assertNotNull(ftc);
    ftc.setEventCallback((context, component, muleContext) -> assertEquals(
                                                                           "The Accept header should be set on the incoming message",
                                                                           "application/xml",
                                                                           context.getMessage()
                                                                               .<String>getInboundProperty("Accept")));

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.request("vm://toclient", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(result.getPayload());
    assertEquals("foo", getPayloadAsString(result));
  }
}
