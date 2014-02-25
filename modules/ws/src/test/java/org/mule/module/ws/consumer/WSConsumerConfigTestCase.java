/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import static org.junit.Assert.assertEquals;
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.http.HttpConnector;

import org.junit.Test;

@SmallTest
public class WSConsumerConfigTestCase extends AbstractMuleContextTestCase
{

    private static final String SERVICE_ADDRESS = "http://localhost";

    @Test
    public void createOutboundEndpointWithDefaultConnector() throws MuleException
    {
        WSConsumerConfig config = createConsumerConfig();
        OutboundEndpoint outboundEndpoint = config.createOutboundEndpoint();
        assertEquals(SERVICE_ADDRESS, outboundEndpoint.getAddress());
    }

    @Test
    public void createOutboundEndpointWithProvidedConnector() throws MuleException
    {
        WSConsumerConfig config = createConsumerConfig();
        HttpConnector httpConnector = new HttpConnector(muleContext);
        config.setConnector(httpConnector);
        OutboundEndpoint outboundEndpoint = config.createOutboundEndpoint();
        assertEquals(httpConnector, outboundEndpoint.getConnector());
    }

    @Test(expected = MuleException.class)
    public void failToCreateOutboundEndpointWithUnsupportedProtocol() throws MuleException
    {
        WSConsumerConfig config = createConsumerConfig();
        config.setServiceAddress("unsupported://test");
        config.createOutboundEndpoint();
    }

    @Test(expected = IllegalStateException.class)
    public void failToCreateOutboundEndpointWithWrongConnector() throws MuleException
    {
        WSConsumerConfig config = createConsumerConfig();
        config.setServiceAddress("jms://test");
        HttpConnector httpConnector = new HttpConnector(muleContext);
        config.setConnector(httpConnector);
        config.createOutboundEndpoint();
    }

    @Test(expected = IllegalStateException.class)
    public void failToCreateOutboundEndpointWithEmptyServiceAddress() throws MuleException
    {
        WSConsumerConfig config = createConsumerConfig();
        config.setServiceAddress(null);
        config.createOutboundEndpoint();
    }

    private WSConsumerConfig createConsumerConfig()
    {
        WSConsumerConfig config = new WSConsumerConfig();
        config.setMuleContext(muleContext);
        config.setWsdlLocation("TestWsdlLocation");
        config.setServiceAddress(SERVICE_ADDRESS);
        config.setService("TestService");
        config.setPort("TestPort");
        return config;
    }

}
