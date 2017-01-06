/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

/**
 * This test was set for the new changes due to Mule1199
 */
public class CustomByteProtocolTestCase extends CompatibilityFunctionalTestCase {

  final private int messages = 100;

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
      client.dispatch("vm://in", InternalMessage.builder().payload(message).build());
    }

    for (int i = 0; i < messages; i++) {
      InternalMessage msg = client.request("vm://out", 30000).getRight().get();
      assertTrue(msg.getPayload().getValue() instanceof NonSerializableMessageObject);
      NonSerializableMessageObject received = (NonSerializableMessageObject) msg.getPayload().getValue();
      assertEquals("Hello", received.s);
      assertEquals(1, received.i);
      assertEquals(true, received.b);
    }
  }

}
