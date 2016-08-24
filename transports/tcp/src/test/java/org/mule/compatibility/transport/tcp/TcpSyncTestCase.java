/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.transport.tcp.TcpConnector;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;

public class TcpSyncTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "tcp-sync-flow.xml";
  }

  protected MuleMessage send(Object payload) throws Exception {
    MuleClient client = muleContext.getClient();
    return client
        .send(((InboundEndpoint) (((Flow) muleContext.getRegistry().lookupObject("service"))).getMessageSource()).getAddress(),
              payload, null)
        .getRight();
  }

  @Test
  public void testSendString() throws Exception {
    MuleMessage message = send("data");
    assertNotNull(message);
    String response = getPayloadAsString(message);
    assertEquals("data", response);
  }

  @Test
  public void testSyncResponseOfBufferSize() throws Exception {
    int size = 1024 * 16;
    TcpConnector tcp = (TcpConnector) muleContext.getRegistry().lookupObject("tcpConnector");
    tcp.setSendBufferSize(size);
    tcp.setReceiveBufferSize(size);
    byte[] data = fillBuffer(new byte[size]);
    MuleMessage message = send(data);
    assertNotNull(message);
    byte[] response = getPayloadAsBytes(message);
    assertEquals(data.length, response.length);
    assertTrue(Arrays.equals(data, response));
  }

  @Test
  public void testManySyncResponseOfBufferSize() throws Exception {
    int size = 1024 * 16;
    TcpConnector tcp = (TcpConnector) muleContext.getRegistry().lookupObject("tcpConnector");
    tcp.setSendBufferSize(size);
    tcp.setReceiveBufferSize(size);
    byte[] data = fillBuffer(new byte[size]);
    for (int i = 0; i < 20; ++i) {
      MuleMessage message = send(data);
      assertNotNull(message);
      byte[] response = getPayloadAsBytes(message);
      assertEquals(data.length, response.length);
      assertTrue(Arrays.equals(data, response));
    }
  }

  @Test
  public void testSyncResponseVeryBig() throws Exception {
    byte[] data = fillBuffer(new byte[1024 * 1024]);
    MuleMessage message = send(data);
    assertNotNull(message);
    byte[] response = getPayloadAsBytes(message);
    assertEquals(data.length, response.length);
    assertTrue(Arrays.equals(data, response));
  }

  protected byte[] fillBuffer(byte[] buffer) {
    for (int i = 0; i < buffer.length; ++i) {
      buffer[i] = (byte) (i % 255);
    }
    return buffer;
  }
}
