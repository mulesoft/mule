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
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.construct.Flow;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class EndpointMessageProcessorsTestCase extends AbstractServiceAndFlowTestCase
{
    public EndpointMessageProcessorsTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/config/spring/parsers/endpoint/endpoint-message-processors-service.xml"},
            {ConfigVariant.FLOW, "org/mule/config/spring/parsers/endpoint/endpoint-message-processors-flow.xml"}
        });
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
        ImmutableEndpoint endpoint;

        if (variant.equals(ConfigVariant.FLOW))
        {
            Flow service = muleContext.getRegistry().lookupObject("localEndpoints");
            endpoint = (ImmutableEndpoint) service.getMessageSource();
        }
        else
        {
            endpoint = ((ServiceCompositeMessageSource) muleContext.getRegistry()
                .lookupService("localEndpoints")
                .getMessageSource()).getEndpoint("ep3");
        }

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

        MessageProcessor mp;

        if (variant.equals(ConfigVariant.FLOW))
        {            
            mp = ((Flow) muleContext.getRegistry().lookupObject("localEndpoints")).getMessageProcessors()
                .get(0);
        }
        else
        {
            mp = ((OutboundPassThroughRouter) ((OutboundRouterCollection) muleContext.getRegistry()
                .lookupService("localEndpoints")
                .getOutboundMessageProcessor()).getRoutes().get(0)).getRoute("ep4");
        }
        
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
