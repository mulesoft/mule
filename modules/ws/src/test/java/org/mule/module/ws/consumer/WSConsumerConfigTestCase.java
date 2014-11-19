/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.http.internal.config.HttpConfiguration;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.http.HttpConnector;

import org.junit.Test;

@SmallTest
public class WSConsumerConfigTestCase extends AbstractMuleContextTestCase
{

    private static final String SERVICE_ADDRESS = "http://localhost";

    @Test
    public void createOutboundEndpointWithDefaultConnectorFromHttpTransport() throws Exception
    {
        MuleTestUtils.testWithSystemProperty(HttpConfiguration.USE_HTTP_TRANSPORT_FOR_URIS, Boolean.TRUE.toString(),
                                             new MuleTestUtils.TestCallback()
                                             {
                                                 @Override
                                                 public void run() throws Exception
                                                 {
                                                     WSConsumerConfig config = createConsumerConfig();
                                                     MessageProcessor mp = config.createOutboundMessageProcessor();
                                                     assertThat(mp, instanceOf(OutboundEndpoint.class));
                                                 }
                                             });
    }

    @Test
    public void createOutboundEndpointWithProvidedConnector() throws MuleException
    {
        WSConsumerConfig config = createConsumerConfig();
        HttpConnector httpConnector = new HttpConnector(muleContext);
        config.setConnector(httpConnector);
        OutboundEndpoint outboundEndpoint = (OutboundEndpoint) config.createOutboundMessageProcessor();
        assertEquals(httpConnector, outboundEndpoint.getConnector());
    }

    @Test(expected = MuleException.class)
    public void failToCreateOutboundEndpointWithUnsupportedProtocol() throws MuleException
    {
        WSConsumerConfig config = createConsumerConfig();
        config.setServiceAddress("unsupported://test");
        config.createOutboundMessageProcessor();
    }

    @Test(expected = IllegalStateException.class)
    public void failToCreateOutboundEndpointWithWrongConnector() throws MuleException
    {
        WSConsumerConfig config = createConsumerConfig();
        config.setServiceAddress("jms://test");
        HttpConnector httpConnector = new HttpConnector(muleContext);
        config.setConnector(httpConnector);
        config.createOutboundMessageProcessor();
    }

    @Test(expected = IllegalStateException.class)
    public void failToCreateOutboundEndpointWithEmptyServiceAddress() throws MuleException
    {
        WSConsumerConfig config = createConsumerConfig();
        config.setServiceAddress(null);
        config.createOutboundMessageProcessor();
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
