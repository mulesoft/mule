/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;

import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

public class DefaultLocalMuleClientTestCase extends AbstractMuleContextTestCase
{
    protected final String PROP_KEY_1 = "key1";
    protected final String TEST_URI = "test://test";
    protected final long TEST_RESPONSE_TIMEOUT = 567;
    protected final long TEST_FREQ = 123;
    protected final MessageExchangePattern TEST_MEP = MessageExchangePattern.ONE_WAY;
    protected TestableLocalMuleClient client;
    protected InboundEndpoint inboundEndpoint;
    protected OutboundEndpoint outboundEndpoint;
    protected Object messagePaylaod = TEST_MESSAGE;
    protected Map<String, Object> messageProperties = new HashMap<String, Object>();
    protected MuleMessage message;
    protected MuleEvent responseEvent;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        client = new TestableLocalMuleClient(muleContext);
        messageProperties.put(PROP_KEY_1, "val1");
        message = new DefaultMuleMessage(messagePaylaod, messageProperties, muleContext);
        outboundEndpoint = mock(OutboundEndpoint.class);
        inboundEndpoint = mock(InboundEndpoint.class);
        responseEvent = getTestEvent("RESPONSE");
    }

    @Test
    public void testProcessOutboundEndpointObjectMapOfStringObject() throws MuleException
    {
        stub(outboundEndpoint.process(any(MuleEvent.class))).toReturn(responseEvent);
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        MuleMessage response = client.process(outboundEndpoint, messagePaylaod, messageProperties);

        verify(outboundEndpoint).process(argument.capture());

        assertSame(message.getPayload(), argument.getValue().getMessage().getPayload());
        assertSame(message.getOutboundProperty(PROP_KEY_1), argument.getValue()
                .getMessage().getOutboundProperty(PROP_KEY_1));
        assertSame(outboundEndpoint.getExchangePattern(), argument.getValue().getExchangePattern());
        assertSame(outboundEndpoint.getEndpointURI().getUri(), argument.getValue().getMessageSourceURI());

        assertSame(responseEvent.getMessage(), response);
    }

    @Test
    public void testProcessOutboundEndpointMuleMessage() throws MuleException
    {
        stub(outboundEndpoint.process(any(MuleEvent.class))).toReturn(responseEvent);
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        MuleMessage response = client.process(outboundEndpoint, message);

        verify(outboundEndpoint).process(argument.capture());

        assertSame(message, argument.getValue().getMessage());
        assertSame(outboundEndpoint.getExchangePattern(), argument.getValue().getExchangePattern());
        assertSame(outboundEndpoint.getEndpointURI().getUri(), argument.getValue().getMessageSourceURI());
        assertSame(responseEvent.getMessage(), response);
    }

    @Test
    public void testRequestInboundEndpointLong() throws Exception
    {
        stub(inboundEndpoint.request(Matchers.anyLong())).toReturn(responseEvent.getMessage());
        MuleMessage result = client.request(inboundEndpoint, TEST_RESPONSE_TIMEOUT);
        assertSame(responseEvent.getMessage(), result);
    }

    @Test
    public void testDispatchStringObjectMapOfStringObject() throws MuleException
    {
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        client.dispatch(TEST_URI, messagePaylaod, messageProperties);

        verify(outboundEndpoint).process(argument.capture());

        assertSame(message.getPayload(), argument.getValue().getMessage().getPayload());
        assertSame(message.getOutboundProperty(PROP_KEY_1), argument.getValue()
                .getMessage().getOutboundProperty(PROP_KEY_1));
        assertSame(outboundEndpoint.getExchangePattern(), argument.getValue().getExchangePattern());
        assertSame(outboundEndpoint.getEndpointURI().getUri(), argument.getValue().getMessageSourceURI());
    }

    @Test
    public void testSendStringObjectMapOfStringObject() throws MuleException
    {
        stub(outboundEndpoint.process(any(MuleEvent.class))).toReturn(responseEvent);
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        MuleMessage response = client.send(TEST_URI, messagePaylaod, messageProperties);

        verify(outboundEndpoint).process(argument.capture());

        assertSame(message.getPayload(), argument.getValue().getMessage().getPayload());
        assertSame(message.getOutboundProperty(PROP_KEY_1), argument.getValue()
                .getMessage().getOutboundProperty(PROP_KEY_1));
        assertSame(outboundEndpoint.getExchangePattern(), argument.getValue().getExchangePattern());
        assertSame(outboundEndpoint.getEndpointURI().getUri(), argument.getValue().getMessageSourceURI());

        assertSame(responseEvent.getMessage(), response);
    }

    @Test
    public void testSendStringMuleMessage() throws MuleException
    {
        stub(outboundEndpoint.process(any(MuleEvent.class))).toReturn(responseEvent);
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        MuleMessage response = client.send(TEST_URI, message);

        verify(outboundEndpoint).process(argument.capture());

        assertSame(message, argument.getValue().getMessage());
        assertSame(outboundEndpoint.getExchangePattern(), argument.getValue().getExchangePattern());
        assertSame(outboundEndpoint.getEndpointURI().getUri(), argument.getValue().getMessageSourceURI());

        assertSame(responseEvent.getMessage(), response);
    }

    @Test
    public void testSendStringObjectMapOfStringObjectInt() throws MuleException
    {
        stub(outboundEndpoint.process(any(MuleEvent.class))).toReturn(responseEvent);
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        MuleMessage response = client.send(TEST_URI, messagePaylaod, messageProperties, TEST_RESPONSE_TIMEOUT);

        verify(outboundEndpoint).process(argument.capture());

        assertSame(message.getPayload(), argument.getValue().getMessage().getPayload());
        assertSame(message.getOutboundProperty(PROP_KEY_1), argument.getValue()
                .getMessage().getOutboundProperty(PROP_KEY_1));
        assertSame(outboundEndpoint.getExchangePattern(), argument.getValue().getExchangePattern());
        assertSame(outboundEndpoint.getEndpointURI().getUri(), argument.getValue().getMessageSourceURI());

        assertSame(responseEvent.getMessage(), response);
    }

    @Test
    public void testSendStringMuleMessageInt() throws MuleException
    {
        stub(outboundEndpoint.process(any(MuleEvent.class))).toReturn(responseEvent);
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        MuleMessage response = client.send(TEST_URI, message, TEST_RESPONSE_TIMEOUT);

        verify(outboundEndpoint).process(argument.capture());

        assertSame(message, argument.getValue().getMessage());
        assertSame(outboundEndpoint.getExchangePattern(), argument.getValue().getExchangePattern());
        assertSame(outboundEndpoint.getEndpointURI().getUri(), argument.getValue().getMessageSourceURI());

        assertSame(responseEvent.getMessage(), response);
    }

    @Test
    public void testDispatchStringMuleMessage() throws MuleException
    {
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        client.dispatch(TEST_URI, message);

        verify(outboundEndpoint).process(argument.capture());

        assertSame(message, argument.getValue().getMessage());
        assertSame(outboundEndpoint.getExchangePattern(), argument.getValue().getExchangePattern());
        assertSame(outboundEndpoint.getEndpointURI().getUri(), argument.getValue().getMessageSourceURI());
    }

    @Test
    public void testRequestStringLong() throws Exception
    {
        stub(inboundEndpoint.request(Matchers.anyLong())).toReturn(responseEvent.getMessage());
        MuleMessage result = client.request(TEST_URI, TEST_RESPONSE_TIMEOUT);
        assertSame(responseEvent.getMessage(), result);
    }

    @Test
    public void testProcessStringMessageExchangePatternObjectMapOfStringObject() throws MuleException
    {
        stub(outboundEndpoint.process(any(MuleEvent.class))).toReturn(responseEvent);
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        MuleMessage response = client.send(TEST_URI, messagePaylaod, messageProperties, TEST_RESPONSE_TIMEOUT);

        verify(outboundEndpoint).process(argument.capture());

        assertSame(message.getPayload(), argument.getValue().getMessage().getPayload());
        assertSame(message.getOutboundProperty(PROP_KEY_1), argument.getValue()
                .getMessage().getOutboundProperty(PROP_KEY_1));
        assertSame(outboundEndpoint.getExchangePattern(), argument.getValue().getExchangePattern());
        assertSame(outboundEndpoint.getEndpointURI().getUri(), argument.getValue().getMessageSourceURI());

        assertSame(responseEvent.getMessage(), response);
    }

    @Test
    public void testProcessStringMessageExchangePatternMuleMessage() throws MuleException
    {
        stub(outboundEndpoint.process(any(MuleEvent.class))).toReturn(responseEvent);
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        MuleMessage response = client.send(TEST_URI, message, TEST_RESPONSE_TIMEOUT);

        verify(outboundEndpoint).process(argument.capture());

        assertSame(message, argument.getValue().getMessage());
        assertSame(outboundEndpoint.getExchangePattern(), argument.getValue().getExchangePattern());
        assertSame(outboundEndpoint.getEndpointURI().getUri(), argument.getValue().getMessageSourceURI());

        assertSame(responseEvent.getMessage(), response);
    }

    @Test
    public void testInboundEndpointCreation() throws MuleException
    {
        InboundEndpoint endpoint = client.createInboundEndpoint(TEST_URI, TEST_MEP);
        assertEquals(TEST_URI, endpoint.getEndpointURI().getUri().toString());
        assertEquals(TEST_MEP, endpoint.getExchangePattern());
    }

    @Test
    public void testOutboundEndpointCreation() throws MuleException
    {
        OutboundEndpoint endpoint = client.createOutboundEndpoint(TEST_URI, TEST_MEP, new Long(
            TEST_RESPONSE_TIMEOUT));
        assertEquals(TEST_URI, endpoint.getEndpointURI().getUri().toString());
        assertEquals(TEST_MEP, endpoint.getExchangePattern());
        assertEquals(TEST_RESPONSE_TIMEOUT, endpoint.getResponseTimeout());
    }

    class TestableLocalMuleClient extends DefaultLocalMuleClient
    {

        public TestableLocalMuleClient(MuleContext muleContext)
        {
            super(muleContext);
        }

        InboundEndpoint createInboundEndpoint(String uri, MessageExchangePattern mep) throws MuleException
        {
            return getInboundEndpoint(uri, mep);
        }

        OutboundEndpoint createOutboundEndpoint(String uri, MessageExchangePattern mep, Long responseTimeout)
            throws MuleException
        {
            return getOutboundEndpoint(uri, mep, responseTimeout);
        }

        protected InboundEndpoint getInboundEndpoint(String uri, MessageExchangePattern mep)
            throws MuleException
        {
            return inboundEndpoint;
        }

        protected OutboundEndpoint getOutboundEndpoint(String uri,
                                                       MessageExchangePattern mep,
                                                       Long responseTimeout) throws MuleException
        {
            return outboundEndpoint;
        }
    }
}
