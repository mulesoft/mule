/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

/**
 * This test was set for the new changes due to Mule1199
 */
public class CustomSerializationProtocolTestCase extends FunctionalTestCase {

  final private int messages = 1;

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "custom-serialisation-mule-config-flow.xml";
  }

  @Test
  public void testCustomObject() throws Exception {
    MuleClient client = muleContext.getClient();
    NonSerializableMessageObject message = new NonSerializableMessageObject(1, "Hello", true);

    for (int i = 0; i < messages; i++) {
      client.dispatch("vm://in", getTestMuleMessage(message));
    }

    for (int i = 0; i < messages; i++) {
      MuleMessage msg = client.request("vm://out", 30000).getRight().get();
      assertTrue(msg.getPayload() instanceof NonSerializableMessageObject);
      NonSerializableMessageObject received = (NonSerializableMessageObject) msg.getPayload();
      assertEquals("Hello", received.s);
      assertEquals(1, received.i);
      assertEquals(true, received.b);
    }
  }
}
