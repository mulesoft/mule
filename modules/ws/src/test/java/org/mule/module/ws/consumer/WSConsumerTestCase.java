/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.api.config.MuleProperties.OBJECT_MULE_ENDPOINT_FACTORY;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.endpoint.DefaultEndpointFactory;
import org.mule.endpoint.DefaultOutboundEndpoint;
import org.mule.module.ws.security.WSSecurity;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.http.HttpConnector;

import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;

import org.junit.Test;
import org.mockito.Mockito;

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

    @Test
    public void outboundEndpointIsDisposed() throws Exception
    {
        TestEndpointFactory endpointFactory = new TestEndpointFactory();
        muleContext.getRegistry().registerObject(OBJECT_MULE_ENDPOINT_FACTORY, endpointFactory);
        WSConsumer wsConsumer = createConsumer();
        wsConsumer.getConfig().setConnector(new HttpConnector(muleContext));
        wsConsumer.initialise();
        wsConsumer.dispose();

        verify((Disposable) endpointFactory.getCreatedEndpoint()).dispose();
    }

    @Test
    public void onlyTheFirstWSConsumerCreatesANewRequestBodyForTheSameOperation() throws InitialisationException, WSDLException {
        String echoOperation = "echo";

        WSConsumerConfig spyConfig = spy(new WSConsumerConfig());

        WSConsumer wsConsumer1 = createConsumer(spyConfig, echoOperation);
        WSConsumer wsConsumer2 = createConsumer(spyConfig, echoOperation);
        WSConsumer wsConsumer3 = createConsumer(spyConfig, echoOperation);

        wsConsumer1.initialise();
        wsConsumer2.initialise();
        wsConsumer3.initialise();

        verify(spyConfig, times(1)).createRequestBody(Mockito.any(Definition.class), Mockito.any(BindingOperation.class));
        verify(spyConfig, times(2)).getRequestBodyFromCache(Mockito.any(String.class));
    }

    @Test
    public void onlyTheFirstWSConsumerCreatesANewRequestBodyPerOperation() throws InitialisationException, WSDLException {
        String echoOperation = "echo";
        String echoWithHeadersOperation = "echoWithHeaders";

        WSConsumerConfig spyConfig = spy(new WSConsumerConfig());

        /* create three consumers with an operation */
        WSConsumer echoWSConsumer1 = createConsumer(spyConfig, echoOperation);
        WSConsumer echoWSConsumer2 = createConsumer(spyConfig, echoOperation);
        WSConsumer echoWSConsumer3 = createConsumer(spyConfig, echoOperation);

        /* create more consumers with another operation */
        WSConsumer withHeadersConsumer1 = createConsumer(spyConfig, echoWithHeadersOperation);
        WSConsumer withHeadersConsumer2 = createConsumer(spyConfig, echoWithHeadersOperation);
        WSConsumer withHeadersConsumer3 = createConsumer(spyConfig, echoWithHeadersOperation);
        WSConsumer withHeadersConsumer4 = createConsumer(spyConfig, echoWithHeadersOperation);

        echoWSConsumer1.initialise();
        echoWSConsumer2.initialise();
        echoWSConsumer3.initialise();

        withHeadersConsumer1.initialise();
        withHeadersConsumer2.initialise();
        withHeadersConsumer3.initialise();
        withHeadersConsumer4.initialise();

        verify(spyConfig, times(2)).createRequestBody(Mockito.any(Definition.class), Mockito.any(BindingOperation.class));
        verify(spyConfig, times(5)).getRequestBodyFromCache(Mockito.any(String.class));
    }

    private WSConsumer createConsumer()
    {
        return createConsumer(new WSConsumerConfig(), "echo");
    }

    private WSConsumer createConsumer(WSConsumerConfig wsConsumerConfig, String operation)
    {
        wsConsumerConfig.setWsdlLocation("Test.wsdl");
        wsConsumerConfig.setServiceAddress("http://localhost/test");
        wsConsumerConfig.setService("TestService");
        wsConsumerConfig.setPort("TestPort");
        wsConsumerConfig.setMuleContext(muleContext);

        WSConsumer wsConsumer = new WSConsumer();
        wsConsumer.setOperation(operation);
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
