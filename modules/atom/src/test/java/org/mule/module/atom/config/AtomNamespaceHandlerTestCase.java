/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.atom.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;
import org.mule.construct.Flow;
import org.mule.module.atom.routing.EntryLastUpdatedFilter;
import org.mule.module.atom.routing.FeedLastUpdatedFilter;
import org.mule.module.atom.routing.FeedSplitter;
import org.mule.module.atom.transformers.ObjectToFeed;
import org.mule.routing.MessageFilter;
import org.mule.tck.junit4.FunctionalTestCase;

import java.text.SimpleDateFormat;

import org.junit.Test;

public class AtomNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "namespace-config.xml";
    }

    @Test
    public void testFlowConfig() throws Exception
    {
        Flow flowConstruct = muleContext.getRegistry().lookupObject("flowTest");
        assertNotNull(flowConstruct);
        assertTrue(flowConstruct.getMessageSource() instanceof InboundEndpoint);
        InboundEndpoint ep = ((InboundEndpoint)flowConstruct.getMessageSource());
        assertEquals(2, ep.getMessageProcessors().size());
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
