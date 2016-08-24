/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class TcpJmsResponseBridgeTestCase extends FunctionalTestCase {

  @Rule
  public final DynamicPort tcpPort = new DynamicPort("tcpPort");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/usecases/sync/tcp-jms-response-bridge.xml";
  }

  @Test
  public void testSyncResponse() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage message = client.send("tcp://localhost:" + tcpPort.getNumber(), "request", null).getRight();
    assertNotNull(message);
    assertEquals("Received: request", getPayloadAsString(message));
  }
}
