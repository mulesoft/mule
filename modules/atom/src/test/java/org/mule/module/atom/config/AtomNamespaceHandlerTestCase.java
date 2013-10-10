/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.config;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.module.atom.endpoint.AtomInboundEndpoint;
import org.mule.module.atom.routing.EntryLastUpdatedFilter;
import org.mule.module.atom.routing.FeedLastUpdatedFilter;
import org.mule.module.atom.routing.FeedSplitter;
import org.mule.module.atom.transformers.ObjectToFeed;
import org.mule.routing.MessageFilter;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.junit4.FunctionalTestCase;

import java.text.SimpleDateFormat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AtomNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "namespace-config.xml";
    }

    @Test
    public void testEndpointConfig() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("test");
        assertNotNull(service);
        assertTrue(((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoints().get(0) instanceof AtomInboundEndpoint);
        AtomInboundEndpoint ep = (AtomInboundEndpoint) ((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoints().get(0);
        assertEquals(FeedSplitter.class, ep.getMessageProcessors().get(0).getClass());
        assertNotNull(ep.getLastUpdate());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        assertEquals(sdf.parse("2009-10-01"), ep.getLastUpdate());
    }

    @Test
    public void testFlowConfig() throws Exception
    {
        SimpleFlowConstruct flowConstruct = muleContext.getRegistry().lookupObject("flowTest");
        assertNotNull(flowConstruct);
        assertTrue(flowConstruct.getMessageSource() instanceof InboundEndpoint);
        InboundEndpoint ep = ((InboundEndpoint)flowConstruct.getMessageSource());
        assertEquals(3, ep.getMessageProcessors().size());
        MessageProcessor mp = ep.getMessageProcessors().get(0);
        assertTrue(mp instanceof FeedSplitter);
        mp = ep.getMessageProcessors().get(1);
        assertTrue(mp instanceof MessageFilter);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        assertEquals(sdf.parse("2009-10-01"), ((EntryLastUpdatedFilter)((MessageFilter)mp).getFilter()).getLastUpdate());
    }

    @Test
    public void testGlobalFilterConfig() throws Exception 
    {
        FeedLastUpdatedFilter filter = muleContext.getRegistry().lookupObject("feedFilter");
        assertNotNull(filter);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        assertEquals(sdf.parse("2009-10-01 13:00:00"), filter.getLastUpdate());
        assertFalse(filter.isAcceptWithoutUpdateDate());
    }
    
    @Test
    public void testObjectToFeedTransformer() throws Exception
    {
        Transformer transformer = muleContext.getRegistry().lookupTransformer("ObjectToFeed");
        assertNotNull(transformer);
        assertTrue(transformer instanceof ObjectToFeed);
    }

}
