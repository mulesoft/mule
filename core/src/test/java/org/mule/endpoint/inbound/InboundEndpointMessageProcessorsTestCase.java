/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint.inbound;

import static org.junit.Assert.assertEquals;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.endpoint.AbstractMessageProcessorTestCase;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Unit test for configuring message processors on an inbound endpoint.
 */
public class InboundEndpointMessageProcessorsTestCase extends AbstractMessageProcessorTestCase
{
    private static final String TEST_MESSAGE = "test";

    private InboundEndpoint endpoint;
    private MuleMessage inMessage;
    private MuleEvent requestEvent;
    private MuleEvent result;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        inMessage = createTestRequestMessage();
        endpoint = createTestInboundEndpoint(null, null, null, null, 
            MessageExchangePattern.REQUEST_RESPONSE, null);
        requestEvent = createTestRequestEvent(endpoint);
    }

    @Test
    public void testProcessors() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new TestMessageProcessor("1"), new TestMessageProcessor("2"), new TestMessageProcessor("3"));
        MessageProcessor mpChain = builder.build();
        
        result = mpChain.process(requestEvent);
        assertEquals(TEST_MESSAGE + ":1:2:3", result.getMessage().getPayload());
    }

    @Test
    public void testNoProcessors() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        MessageProcessor mpChain = builder.build();
        
        result = mpChain.process(requestEvent);
        assertEquals(TEST_MESSAGE, result.getMessage().getPayload());
    }

    protected MuleMessage createTestRequestMessage()
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("prop1", "value1");
        return new DefaultMuleMessage(TEST_MESSAGE, props, muleContext);
    }

    protected MuleEvent createTestRequestEvent(InboundEndpoint endpoint) throws Exception
    {
        return new DefaultMuleEvent(inMessage, endpoint, getTestFlow(), getTestSession(null, muleContext));
    }    
}
