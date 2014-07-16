/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;
import org.mule.construct.Flow;
import org.mule.module.rss.routing.EntryLastUpdatedFilter;
import org.mule.module.rss.routing.FeedLastUpdatedFilter;
import org.mule.module.rss.routing.FeedSplitter;
import org.mule.module.rss.transformers.ObjectToRssFeed;
import org.mule.routing.MessageFilter;
import org.mule.tck.junit4.FunctionalTestCase;

import java.text.SimpleDateFormat;

import org.junit.Test;

public class RssNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
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
        assertTrue(transformer instanceof ObjectToRssFeed);
    }
}
