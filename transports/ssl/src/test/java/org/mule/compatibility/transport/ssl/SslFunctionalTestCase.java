/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.functional.CounterCallback;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class SslFunctionalTestCase extends FunctionalTestCase {

  private static int NUM_MESSAGES = 100;

  @Rule
  public DynamicPort dynamicPort2 = new DynamicPort("port2");

  @Rule
  public DynamicPort dynamicPort3 = new DynamicPort("port3");

  @Override
  protected String getConfigFile() {
    return "ssl-functional-test.xml";
  }

  @Test
  public void testSend() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("sendEndpoint", TEST_MESSAGE, null).getRight();
    assertEquals(TEST_MESSAGE + " Received", getPayloadAsString(result));
  }

  @Test
  @Ignore
  public void testSendMany() throws Exception {
    MuleClient client = muleContext.getClient();
    for (int i = 0; i < NUM_MESSAGES; ++i) {
      MuleMessage result = client.send("sendManyEndpoint", TEST_MESSAGE, null).getRight();
      assertEquals(TEST_MESSAGE + " Received", getPayloadAsString(result));
    }

    Flow c = (Flow) muleContext.getRegistry().lookupFlowConstruct("testComponent2");
    // assertTrue("Service should be a TestSedaService", c instanceof TestSedaService);
    Object ftc = getComponent(c);
    assertNotNull("Functional Test Service not found in the model.", ftc);
    assertTrue("Service should be a FunctionalTestComponent", ftc instanceof FunctionalTestComponent);

    EventCallback cc = ((FunctionalTestComponent) ftc).getEventCallback();
    assertNotNull("EventCallback is null", cc);
    assertTrue("EventCallback should be a CounterCallback", cc instanceof CounterCallback);
    assertEquals(NUM_MESSAGES, ((CounterCallback) cc).getCallbackCount());
  }
}
