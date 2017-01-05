/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.transport.AbstractConnectorTestCase;
import org.mule.compatibility.transport.ssl.SslConnector;
import org.mule.runtime.core.construct.Flow;

import org.junit.Test;

public class SslConnectorTestCase extends AbstractConnectorTestCase {

  @Override
  public Connector createConnector() throws Exception {
    SslConnector cnn = new SslConnector(muleContext);
    cnn.setName("SslConnector");
    cnn.setKeyStore("serverKeystore");
    cnn.setClientKeyStore("clientKeystore");
    cnn.setClientKeyStorePassword("mulepassword");
    cnn.setKeyPassword("mulepassword");
    cnn.setKeyStorePassword("mulepassword");
    cnn.setTrustStore("trustStore");
    cnn.setTrustStorePassword("mulepassword");
    cnn.getDispatcherThreadingProfile().setDoThreading(false);
    return cnn;
  }

  @Test
  public void testClientConnector() throws Exception {
    SslConnector cnn = new SslConnector(muleContext);
    cnn.setClientKeyStore("clientKeystore");
    cnn.setClientKeyStorePassword("mulepassword");
    cnn.getDispatcherThreadingProfile().setDoThreading(false);
  }

  @Override
  public String getTestEndpointURI() {
    return "ssl://localhost:56801";
  }

  @Override
  public Object getValidMessage() throws Exception {
    return "Hello".getBytes();
  }

  @Test
  public void testValidListener() throws Exception {
    Connector connector = getConnector();

    InboundEndpoint endpoint2 = getEndpointFactory().getInboundEndpoint("ssl://localhost:30303");

    connector.registerListener(endpoint2, getSensingNullMessageProcessor(), mock(Flow.class));
    try {
      connector.registerListener(endpoint2, getSensingNullMessageProcessor(), mock(Flow.class));
      fail("cannot register on the same endpointUri");
    } catch (Exception e) {
      // expected
    }
  }

  @Test
  public void testProperties() throws Exception {
    SslConnector c = (SslConnector) getConnector();

    c.setSendBufferSize(1024);
    assertEquals(1024, c.getSendBufferSize());
    c.setSendBufferSize(0);
    assertEquals(SslConnector.DEFAULT_BUFFER_SIZE, c.getSendBufferSize());
  }

}
