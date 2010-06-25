/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.endpoint;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import java.util.List;

public class EndpointMessageProcessorsTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/endpoint/endpoint-message-processors.xml";
    }

    public void testGlobalEndpoint1() throws MuleException
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("ep1");
        
        List <MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestMessageProcessor);

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestMessageProcessor);
    }

    public void testGlobalEndpoint2() throws MuleException
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint("ep2");
        
        List <MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertEquals("1", ((TestMessageProcessor) processors.get(0)).getLabel());
        assertEquals("2", ((TestMessageProcessor) processors.get(1)).getLabel());

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertEquals("3", ((TestMessageProcessor) processors.get(0)).getLabel());
        assertEquals("4", ((TestMessageProcessor) processors.get(1)).getLabel());
}

    public void testLocalEndpoints() throws MuleException
    {
        ImmutableEndpoint endpoint = 
            muleContext.getRegistry().lookupService("localEndpoints").getInboundRouter().getEndpoint("ep3");

        List <MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertEquals("A", ((TestMessageProcessor) processors.get(0)).getLabel());
        assertEquals("B", ((TestMessageProcessor) processors.get(1)).getLabel());

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertEquals("C", ((TestMessageProcessor) processors.get(0)).getLabel());
        assertEquals("D", ((TestMessageProcessor) processors.get(1)).getLabel());

        endpoint = 
            ((OutboundPassThroughRouter) muleContext.getRegistry().lookupService("localEndpoints").
                getOutboundRouter().getRouters().get(0)).getEndpoint("ep4");

        processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertEquals("E", ((TestMessageProcessor) processors.get(0)).getLabel());
        assertEquals("F", ((TestMessageProcessor) processors.get(1)).getLabel());

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertEquals("G", ((TestMessageProcessor) processors.get(0)).getLabel());
        assertEquals("H", ((TestMessageProcessor) processors.get(1)).getLabel());
    }
}
