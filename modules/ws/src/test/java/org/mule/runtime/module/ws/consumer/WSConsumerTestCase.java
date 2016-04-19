/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.ws.consumer;


import static org.mockito.Mockito.mock;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.endpoint.EndpointBuilder;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.endpoint.DefaultEndpointFactory;
import org.mule.runtime.core.endpoint.DefaultOutboundEndpoint;
import org.mule.runtime.module.ws.security.WSSecurity;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class WSConsumerTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void initialisesCorrectlyWithValidArguments() throws MuleException
    {
        WSConsumer wsConsumer = createConsumer();
        wsConsumer.initialise();
    }

    @Test
    public void initialisesCorrectlyWithNullSecurityStrategyList() throws MuleException
    {
        WSConsumer wsConsumer = createConsumer();
        wsConsumer.getConfig().setSecurity(new WSSecurity());
        wsConsumer.initialise();
    }

    @Test(expected = InitialisationException.class)
    public void failsToInitializeWithInvalidWsdlLocation() throws MuleException
    {
        WSConsumer wsConsumer = createConsumer();
        wsConsumer.getConfig().setWsdlLocation("invalid");
        wsConsumer.initialise();
    }

    @Test(expected = InitialisationException.class)
    public void failsToInitializeWithInvalidService() throws MuleException
    {
        WSConsumer wsConsumer = createConsumer();
        wsConsumer.getConfig().setService("invalid");
        wsConsumer.initialise();
    }

    @Test(expected = InitialisationException.class)
    public void failsToInitializeWithInvalidPort() throws MuleException
    {
        WSConsumer wsConsumer = createConsumer();
        wsConsumer.getConfig().setPort("invalid");
        wsConsumer.initialise();
    }

    @Test(expected = InitialisationException.class)
    public void failsToInitializeWithInvalidOperation() throws MuleException
    {
        WSConsumer wsConsumer = createConsumer();
        wsConsumer.setOperation("invalid");
        wsConsumer.initialise();
    }

    private WSConsumer createConsumer()
    {
        WSConsumerConfig wsConsumerConfig = new WSConsumerConfig();

        wsConsumerConfig.setWsdlLocation("Test.wsdl");
        wsConsumerConfig.setServiceAddress("http://localhost/test");
        wsConsumerConfig.setService("TestService");
        wsConsumerConfig.setPort("TestPort");
        wsConsumerConfig.setMuleContext(muleContext);

        WSConsumer wsConsumer = new WSConsumer();
        wsConsumer.setOperation("echo");
        wsConsumer.setConfig(wsConsumerConfig);
        wsConsumer.setMuleContext(muleContext);

        return wsConsumer;
    }

    private class TestEndpointFactory extends DefaultEndpointFactory
    {

        private DefaultOutboundEndpoint createdEndpoint = mock(DefaultOutboundEndpoint.class);

        @Override
        public OutboundEndpoint getOutboundEndpoint(EndpointBuilder builder) throws MuleException
        {
            return createdEndpoint;
        }

        public DefaultOutboundEndpoint getCreatedEndpoint()
        {
            return createdEndpoint;
        }

    }
}
