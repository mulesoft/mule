/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.transport.AbstractConnectorTestCase;
import org.mule.compatibility.transport.http.HttpsConnector;
import org.mule.compatibility.transport.tcp.TcpConnector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.construct.Flow;

import java.io.IOException;

import org.junit.Test;


public class HttpsConnectorTestCase extends AbstractConnectorTestCase {

  @Override
  public Connector createConnector() throws Exception {
    return createConnector(muleContext, false);
  }

  public static HttpsConnector createConnector(MuleContext context, boolean initialised)
      throws IOException, InitialisationException {
    HttpsConnector cnn = new HttpsConnector(muleContext);
    cnn.setName("HttpsConnector");
    cnn.setKeyStore("serverKeystore");
    cnn.setClientKeyStore("clientKeystore");
    cnn.setClientKeyStorePassword("mulepassword");
    cnn.setKeyPassword("mulepassword");
    cnn.setKeyStorePassword("mulepassword");
    cnn.setTrustStore("trustStore");
    cnn.setTrustStorePassword("mulepassword");
    cnn.getDispatcherThreadingProfile().setDoThreading(false);

    if (initialised) {
      cnn.initialise();
    }
    return cnn;
  }

  @Override
  public String getTestEndpointURI() {
    return "https://localhost:60127";
  }

  @Override
  public Object getValidMessage() throws Exception {
    return "Hello".getBytes();
  }

  @Test
  public void testValidListener() throws Exception {
    InboundEndpoint endpoint = getEndpointFactory().getInboundEndpoint(getTestEndpointURI());

    getConnector().registerListener(endpoint, getSensingNullMessageProcessor(), mock(Flow.class));
  }

  @Test
  public void testProperties() throws Exception {
    HttpsConnector c = (HttpsConnector) getConnector();

    c.setSendBufferSize(1024);
    assertEquals(1024, c.getSendBufferSize());
    c.setSendBufferSize(0);
    assertEquals(TcpConnector.DEFAULT_BUFFER_SIZE, c.getSendBufferSize());

    // all kinds of timeouts are tested in TcpConnectorTestCase now
  }
}
