/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class TcpFunctionalTestCase extends FunctionalTestCase {

  protected static String TEST_MESSAGE = "Test TCP Request";

  @Rule
  public DynamicPort dynamicPort1 = new DynamicPort("port1");

  @Rule
  public DynamicPort dynamicPort2 = new DynamicPort("port2");

  @Override
  protected String getConfigFile() {
    return "tcp-functional-test-flow.xml";
  }

  @Test
  public void testSend() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, null);
    assertEquals(TEST_MESSAGE + " Received", getPayloadAsString(result));
  }

  @Test
  public void testDispatchAndReply() throws Exception {
    MuleClient client = muleContext.getClient();
    client.dispatch("asyncClientEndpoint", TEST_MESSAGE, null);
    // MULE-2754
    Thread.sleep(100);
    MuleMessage result = client.request("asyncClientEndpoint", 10000);
    assertNotNull(result);
    assertEquals(TEST_MESSAGE + " Received Async", getPayloadAsString(result));
  }

  public void timeMultipleSend() throws Exception {
    MuleClient client = muleContext.getClient();
    long now = System.currentTimeMillis();
    int count = 1000;
    for (int i = 0; i < count; i++) {
      MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, null);
      assertEquals(TEST_MESSAGE + " Received", getPayloadAsString(result));
    }
    long later = System.currentTimeMillis();
    double speed = count * 1000.0 / (later - now);
    logger.error(speed + " messages per second");
  }
}
