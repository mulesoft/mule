/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.compatibility.transport.tcp.TcpConnector.DEFAULT_WAIT_TIMEOUT;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.compatibility.transport.tcp.protocols.ResponseOutputStream;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.junit.Rule;
import org.junit.Test;

public class TcpSocketsPoolTestCase extends CompatibilityFunctionalTestCase {

  protected static String TEST_MESSAGE = "Test TCP Request";

  @Rule
  public DynamicPort dynamicPort1 = new DynamicPort("port1");


  @Override
  protected String getConfigFile() {
    return "tcp-sockets-pool-test-flow.xml";
  }

  @Test
  public void testExceptionInSendReleasesSocket() throws Exception {
    TcpConnector tcpConnector = (TcpConnector) muleContext.getRegistry().lookupObject("connectorWithException");
    assertThat(tcpConnector, notNullValue());
    MuleClient client = muleContext.getClient();
    try {
      client.send("clientWithExceptionEndpoint", TEST_MESSAGE, null);
      fail("Dispatch exception was expected");
    } catch (MessagingException e) {
      assertThat(e.getCause(), instanceOf(DispatchException.class));
    }
    assertThat(tcpConnector.getSocketsPoolNumActive(), equalTo(0));
  }

  @Test
  public void testSocketsPoolSettings() throws Exception {
    TcpConnector tcpConnector = (TcpConnector) muleContext.getRegistry().lookupObject("connectorWithException");
    assertThat(tcpConnector.getSocketsPoolMaxActive(), equalTo(8));
    assertThat(tcpConnector.getSocketsPoolMaxIdle(), equalTo(1));
    assertThat(tcpConnector.getSocketsPoolMaxWait(), equalTo(3000L));
  }

  @Test
  public void testSocketsPoolDefaultSettings() throws Exception {
    TcpConnector tcpConnector = (TcpConnector) muleContext.getRegistry().lookupObject("tcpConnector");
    int maxActive = tcpConnector.getDispatcherThreadingProfile().getMaxThreadsActive();
    int maxIdle = tcpConnector.getDispatcherThreadingProfile().getMaxThreadsIdle();
    assertThat(tcpConnector.getSocketsPoolMaxActive(), equalTo(maxActive));
    assertThat(tcpConnector.getSocketsPoolMaxIdle(), equalTo(maxIdle));
    assertThat(tcpConnector.getSocketMaxWait(), equalTo(DEFAULT_WAIT_TIMEOUT));
  }

  public static class MockTcpProtocol implements TcpProtocol {

    @Override
    public ResponseOutputStream createResponse(Socket socket) throws IOException {
      throw new UnsupportedOperationException("createResponse");
    }

    @Override
    public Object read(InputStream is) throws IOException {
      throw new UnsupportedOperationException("read");
    }

    @Override
    public void write(OutputStream os, Object data) throws IOException {
      throw new UnsupportedOperationException("write");
    }
  }

}
