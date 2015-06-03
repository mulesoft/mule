/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.PropertyScope;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.session.NullSessionHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.mule.TestMessageReceiver;
import org.mule.transaction.MuleTransactionConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/**
 * <li>Correct return value for ONE_WAY/REQUEST_RESPONSE endpoints. <li>//TODO: Test default
 * transformers are applied <li>//TODO: Test root message id propagation <li>//TODO: Test filter exception <li>
 * //TODO: Test response security context
 */
@RunWith(value = MockitoJUnitRunner.class)
@SmallTest
public class MessageReceiverTestCase extends AbstractMuleTestCase
{

    @Mock
    private MuleContext muleContext;
    @Mock
    private MuleSession muleSession;

    @Before
    public void setup()
    {
        Mockito.when(muleSession.getId()).thenReturn("1");
        MuleConfiguration muleConfiguration = Mockito.mock(MuleConfiguration.class);
        Mockito.when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
    }

    @Test
    public void routeMessageOneWayReturnsNotNull() throws MuleException
    {
        MessageReceiver receiver = createMessageReciever(MessageExchangePattern.ONE_WAY);

        assertNotNull(receiver.routeMessage(createRequestMessage()));
    }

    @Test
    public void routeMessageRequestResponseReturnsEvent() throws MuleException
    {
        MessageReceiver receiver = createMessageReciever(MessageExchangePattern.REQUEST_RESPONSE);
        MuleMessage request = createRequestMessage();

        assertEquals(request, receiver.routeMessage(request).getMessage());
    }

    protected MuleMessage createRequestMessage()
    {
        MuleMessage request = Mockito.mock(MuleMessage.class);
        Mockito.when(request.getMuleContext()).thenReturn(muleContext);
        Mockito.when(
            request.getProperty(Mockito.anyString(), Mockito.any(PropertyScope.class), Mockito.eq(false)))
            .thenReturn(Boolean.FALSE);
        return request;
    }

    protected MessageReceiver createMessageReciever(MessageExchangePattern mep) throws MuleException
    {
        AbstractConnector connector = Mockito.mock(AbstractConnector.class);
        Mockito.when(connector.getSessionHandler()).thenReturn(new NullSessionHandler());

        FlowConstruct flowConstruct = Mockito.mock(FlowConstruct.class);

        InboundEndpoint endpoint = Mockito.mock(InboundEndpoint.class);
        Mockito.when(endpoint.getExchangePattern()).thenReturn(mep);
        Mockito.when(endpoint.getConnector()).thenReturn(connector);
        Mockito.when(endpoint.getEndpointURI()).thenReturn(new MuleEndpointURI("test://test", muleContext));
        Mockito.when(endpoint.getTransactionConfig()).thenReturn(new MuleTransactionConfig());
        Mockito.when(endpoint.getExchangePattern()).thenReturn(mep);
        Mockito.when(endpoint.getMuleContext()).thenReturn(muleContext);

        MuleEvent responseEvent = Mockito.mock(MuleEvent.class);
        Mockito.when(responseEvent.getSession()).thenReturn(muleSession);

        MessageProcessor listener = Mockito.mock(MessageProcessor.class);
        Mockito.when(listener.process(Mockito.any(MuleEvent.class))).thenAnswer(new Answer<MuleEvent>()
        {
            @Override
            public MuleEvent answer(InvocationOnMock invocation) throws Throwable
            {
                return (MuleEvent) invocation.getArguments()[0];
            }
        });

        MessageReceiver messageReceiver = new TestMessageReceiver(connector, flowConstruct, endpoint);
        messageReceiver.setListener(listener);
        return messageReceiver;
    }

}
