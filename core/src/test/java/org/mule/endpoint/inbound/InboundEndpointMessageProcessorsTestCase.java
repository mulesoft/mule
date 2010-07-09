/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint.inbound;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.processor.builder.InterceptingChainMessageProcessorBuilder;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit test for configuring message processors on an inbound endpoint.
 */
public class InboundEndpointMessageProcessorsTestCase extends AbstractInboundMessageProcessorTestCase
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
        endpoint = createTestInboundEndpoint(null, null, null, null, true, null);
        requestEvent = createTestRequestEvent(endpoint);
    }

    public void testProcessors() throws Exception
    {
        InterceptingChainMessageProcessorBuilder builder = new InterceptingChainMessageProcessorBuilder();
        builder.chain(new TestMessageProcessor("1"), new TestMessageProcessor("2"), new TestMessageProcessor("3"));
        MessageProcessor mpChain = builder.build();
        
        result = mpChain.process(requestEvent);
        assertEquals(TEST_MESSAGE + ":1:2:3", result.getMessage().getPayload());
    }

    public void testNoProcessors() throws Exception
    {
        InterceptingChainMessageProcessorBuilder builder = new InterceptingChainMessageProcessorBuilder();
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

    protected MuleEvent createTestRequestEvent(ImmutableEndpoint endpoint) throws Exception
    {
        return new DefaultMuleEvent(inMessage, endpoint, getTestSession(getTestService(), muleContext));
    }    
}
