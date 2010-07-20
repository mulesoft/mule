/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.client;

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
import org.mule.api.transport.PropertyScope;
import org.mule.tck.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.Map;

import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

public class DefaultLocalMuleClientTestCase extends AbstractMuleTestCase
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

    public void testProcessOutboundEndpointObjectMapOfStringObject() throws MuleException
    {
        stub(outboundEndpoint.process(any(MuleEvent.class))).toReturn(responseEvent);
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        MuleMessage response = client.process(outboundEndpoint, messagePaylaod, messageProperties);

        verify(outboundEndpoint).process((MuleEvent) argument.capture());

        assertSame(message.getPayload(), argument.getValue().getMessage().getPayload());
        assertSame(message.getProperty(PROP_KEY_1, PropertyScope.OUTBOUND), argument.getValue()
            .getMessage()
            .getProperty(PROP_KEY_1, PropertyScope.OUTBOUND));
        assertSame(outboundEndpoint, argument.getValue().getEndpoint());

        assertSame(responseEvent.getMessage(), response);
    }

    public void testProcessOutboundEndpointMuleMessage() throws MuleException
    {
        stub(outboundEndpoint.process(any(MuleEvent.class))).toReturn(responseEvent);
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        MuleMessage response = client.process(outboundEndpoint, message);

        verify(outboundEndpoint).process((MuleEvent) argument.capture());

        assertSame(message, argument.getValue().getMessage());
        assertSame(outboundEndpoint, argument.getValue().getEndpoint());
        assertSame(responseEvent.getMessage(), response);
    }

    public void testRequestInboundEndpointLong() throws Exception
    {
        stub(inboundEndpoint.request(Matchers.anyLong())).toReturn(responseEvent.getMessage());
        MuleMessage result = client.request(inboundEndpoint, TEST_RESPONSE_TIMEOUT);
        assertSame(responseEvent.getMessage(), result);
    }

    public void testDispatchStringObjectMapOfStringObject() throws MuleException
    {
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        client.dispatch(TEST_URI, messagePaylaod, messageProperties);

        verify(outboundEndpoint).process((MuleEvent) argument.capture());

        assertSame(message.getPayload(), argument.getValue().getMessage().getPayload());
        assertSame(message.getProperty(PROP_KEY_1, PropertyScope.OUTBOUND), argument.getValue()
            .getMessage()
            .getProperty(PROP_KEY_1, PropertyScope.OUTBOUND));
        assertSame(outboundEndpoint, argument.getValue().getEndpoint());
    }

    public void testSendStringObjectMapOfStringObject() throws MuleException
    {
        stub(outboundEndpoint.process(any(MuleEvent.class))).toReturn(responseEvent);
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        MuleMessage response = client.send(TEST_URI, messagePaylaod, messageProperties);

        verify(outboundEndpoint).process((MuleEvent) argument.capture());

        assertSame(message.getPayload(), argument.getValue().getMessage().getPayload());
        assertSame(message.getProperty(PROP_KEY_1, PropertyScope.OUTBOUND), argument.getValue()
            .getMessage()
            .getProperty(PROP_KEY_1, PropertyScope.OUTBOUND));
        assertSame(outboundEndpoint, argument.getValue().getEndpoint());

        assertSame(responseEvent.getMessage(), response);
    }

    public void testSendStringMuleMessage() throws MuleException
    {
        stub(outboundEndpoint.process(any(MuleEvent.class))).toReturn(responseEvent);
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        MuleMessage response = client.send(TEST_URI, message);

        verify(outboundEndpoint).process((MuleEvent) argument.capture());

        assertSame(message, argument.getValue().getMessage());
        assertSame(outboundEndpoint, argument.getValue().getEndpoint());

        assertSame(responseEvent.getMessage(), response);
    }

    public void testSendStringObjectMapOfStringObjectInt() throws MuleException
    {
        stub(outboundEndpoint.process(any(MuleEvent.class))).toReturn(responseEvent);
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        MuleMessage response = client.send(TEST_URI, messagePaylaod, messageProperties, TEST_RESPONSE_TIMEOUT);

        verify(outboundEndpoint).process((MuleEvent) argument.capture());

        assertSame(message.getPayload(), argument.getValue().getMessage().getPayload());
        assertSame(message.getProperty(PROP_KEY_1, PropertyScope.OUTBOUND), argument.getValue()
            .getMessage()
            .getProperty(PROP_KEY_1, PropertyScope.OUTBOUND));
        assertSame(outboundEndpoint, argument.getValue().getEndpoint());

        assertSame(responseEvent.getMessage(), response);
    }

    public void testSendStringMuleMessageInt() throws MuleException
    {
        stub(outboundEndpoint.process(any(MuleEvent.class))).toReturn(responseEvent);
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        MuleMessage response = client.send(TEST_URI, message, TEST_RESPONSE_TIMEOUT);

        verify(outboundEndpoint).process((MuleEvent) argument.capture());

        assertSame(message, argument.getValue().getMessage());
        assertSame(outboundEndpoint, argument.getValue().getEndpoint());

        assertSame(responseEvent.getMessage(), response);
    }

    public void testDispatchStringMuleMessage() throws MuleException
    {
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        client.dispatch(TEST_URI, message);

        verify(outboundEndpoint).process((MuleEvent) argument.capture());

        assertSame(message, argument.getValue().getMessage());
        assertSame(outboundEndpoint, argument.getValue().getEndpoint());
    }

    public void testRequestStringLong() throws Exception
    {
        stub(inboundEndpoint.request(Matchers.anyLong())).toReturn(responseEvent.getMessage());
        MuleMessage result = client.request(TEST_URI, TEST_RESPONSE_TIMEOUT);
        assertSame(responseEvent.getMessage(), result);
    }

    public void testProcessStringMessageExchangePatternObjectMapOfStringObject() throws MuleException
    {
        stub(outboundEndpoint.process(any(MuleEvent.class))).toReturn(responseEvent);
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        MuleMessage response = client.send(TEST_URI, messagePaylaod, messageProperties, TEST_RESPONSE_TIMEOUT);

        verify(outboundEndpoint).process((MuleEvent) argument.capture());

        assertSame(message.getPayload(), argument.getValue().getMessage().getPayload());
        assertSame(message.getProperty(PROP_KEY_1, PropertyScope.OUTBOUND), argument.getValue()
            .getMessage()
            .getProperty(PROP_KEY_1, PropertyScope.OUTBOUND));
        assertSame(outboundEndpoint, argument.getValue().getEndpoint());

        assertSame(responseEvent.getMessage(), response);
    }

    public void testProcessStringMessageExchangePatternMuleMessage() throws MuleException
    {
        stub(outboundEndpoint.process(any(MuleEvent.class))).toReturn(responseEvent);
        ArgumentCaptor<MuleEvent> argument = ArgumentCaptor.forClass(MuleEvent.class);

        MuleMessage response = client.send(TEST_URI, message, TEST_RESPONSE_TIMEOUT);

        verify(outboundEndpoint).process((MuleEvent) argument.capture());

        assertSame(message, argument.getValue().getMessage());
        assertSame(outboundEndpoint, argument.getValue().getEndpoint());

        assertSame(responseEvent.getMessage(), response);
    }

    public void testInboundEndpointCreation() throws MuleException
    {
        InboundEndpoint endpoint = client.createInboundEndpoint(TEST_URI, TEST_MEP);
        assertEquals(TEST_URI, endpoint.getEndpointURI().getUri().toString());
        assertEquals(TEST_MEP, endpoint.getExchangePattern());
    }

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
            return super.getInboundEndpoint(uri, mep);
        }

        OutboundEndpoint createOutboundEndpoint(String uri, MessageExchangePattern mep, Long responseTimeout)
            throws MuleException
        {
            return super.getOutboundEndpoint(uri, mep, responseTimeout);
        }

        @Override
        protected InboundEndpoint getInboundEndpoint(String uri, MessageExchangePattern mep)
            throws MuleException
        {
            return inboundEndpoint;
        }

        @Override
        protected OutboundEndpoint getOutboundEndpoint(String uri,
                                                       MessageExchangePattern mep,
                                                       Long responseTimeout) throws MuleException
        {
            return outboundEndpoint;
        }
    }

}
