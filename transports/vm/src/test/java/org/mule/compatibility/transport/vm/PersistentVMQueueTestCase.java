/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;

import org.junit.Test;

public class PersistentVMQueueTestCase extends CompatibilityFunctionalTestCase {

  private static final int RECEIVE_TIMEOUT = 5000;

  @Override
  protected String getConfigFile() {
    return "vm/persistent-vmqueue-test-flow.xml";
  }

  @Test
  public void testAsynchronousDispatching() throws Exception {
    String input = "Test message";
    String[] output = {"Test", "message"};

    MuleClient client = muleContext.getClient();
    client.dispatch("vm://receiver", input, null);
    InternalMessage result = client.request("vm://out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(result);
    assertNotNull(result.getPayload().getValue());
    String[] payload = (String[]) result.getPayload().getValue();
    assertEquals(output.length, payload.length);
    for (int i = 0; i < output.length; i++) {
      assertEquals(output[i], payload[i]);
    }
  }

  @Test
  public void testAsynchronousDispatchingInFlow() throws Exception {
    String input = "Test message";
    String[] output = {"Test", "message"};

    MuleClient client = muleContext.getClient();
    client.dispatch("vm://flowReceiver", input, null);
    InternalMessage result = client.request("vm://flowOut", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(result);
    assertNotNull(result.getPayload().getValue());
    String[] payload = (String[]) result.getPayload().getValue();
    assertEquals(output.length, payload.length);
    for (int i = 0; i < output.length; i++) {
      assertEquals(output[i], payload[i]);
    }
  }
}
