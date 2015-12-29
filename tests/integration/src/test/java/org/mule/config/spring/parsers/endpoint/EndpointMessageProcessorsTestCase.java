/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.construct.Flow;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import java.util.List;

import org.junit.Test;

public class EndpointMessageProcessorsTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/endpoint/endpoint-message-processors-flow.xml";
    }

    @Test
    public void testGlobalEndpoint1() throws MuleException
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep1");

        List<MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestMessageProcessor);

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof MessageProcessorChain);
    }

    @Test
    public void testGlobalEndpoint2() throws MuleException
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep2");

        List<MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertEquals("1", ((TestMessageProcessor) processors.get(0)).getLabel());
        assertEquals("2", ((TestMessageProcessor) processors.get(1)).getLabel());

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);

        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof MessageProcessorChain);
        MessageProcessorChain chain = (MessageProcessorChain) processors.get(0);
        assertEquals("3", ((TestMessageProcessor) chain.getMessageProcessors().get(0)).getLabel());
        assertEquals("4", ((TestMessageProcessor) chain.getMessageProcessors().get(1)).getLabel());
    }

    @Test
    public void testLocalEndpoints() throws MuleException
    {
        Flow flow = muleContext.getRegistry().lookupObject("localEndpoints");
        ImmutableEndpoint endpoint = (ImmutableEndpoint) flow.getMessageSource();

        List<MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertEquals("A", ((TestMessageProcessor) processors.get(0)).getLabel());
        assertEquals("B", ((TestMessageProcessor) processors.get(1)).getLabel());

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof MessageProcessorChain);
        MessageProcessorChain chain = (MessageProcessorChain) processors.get(0);
        assertEquals(2, chain.getMessageProcessors().size());
        assertEquals("C", ((TestMessageProcessor) chain.getMessageProcessors().get(0)).getLabel());
        assertEquals("D", ((TestMessageProcessor) chain.getMessageProcessors().get(1)).getLabel());

        MessageProcessor mp = ((Flow) muleContext.getRegistry().lookupObject("localEndpoints")).getMessageProcessors().get(0);

        endpoint = (ImmutableEndpoint) mp;
        processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertEquals("E", ((TestMessageProcessor) processors.get(0)).getLabel());
        assertEquals("F", ((TestMessageProcessor) processors.get(1)).getLabel());

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof MessageProcessorChain);
        chain = (MessageProcessorChain) processors.get(0);
        assertEquals(2, chain.getMessageProcessors().size());
        assertEquals("G", ((TestMessageProcessor) chain.getMessageProcessors().get(0)).getLabel());
        assertEquals("H", ((TestMessageProcessor) chain.getMessageProcessors().get(1)).getLabel());
    }
}
