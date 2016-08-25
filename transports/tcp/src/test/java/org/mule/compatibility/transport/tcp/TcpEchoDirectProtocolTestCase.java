/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class TcpEchoDirectProtocolTestCase extends FunctionalTestCase {

  protected static String TEST_MESSAGE = "Test TCP Request";

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "tcp-echo-test-flow.xml";
  }

  @Test
  public void testSend() throws Exception {
    MuleClient client = muleContext.getClient();

    MuleMessage response = client
        .send(((InboundEndpoint) ((Flow) muleContext.getRegistry().lookupObject("BounceTcpMMP")).getMessageSource()).getAddress(),
              TEST_MESSAGE, null)
        .getRight();

    assertNotNull(response);
    assertEquals(TEST_MESSAGE, response.getPayload());
  }
}
