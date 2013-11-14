/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss;

import static org.junit.Assert.assertEquals;

import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class JmsRssFeedConsumeTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "jms-rss-consume.xml";
    }

    @Test
    public void testConsumeFeed() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        String feed = SampleFeed.feedAsString();
        client.dispatch("jms://feed.in", feed, null);
        Thread.sleep(3000);
        FeedReceiver component = (FeedReceiver) getComponent("feedConsumer");
        assertEquals(25, component.getCount());
    }

    @Test
    public void testConsumeSplitFeed() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        String feed = SampleFeed.feedAsString();
        client.dispatch("jms://feed.split.in", feed, null);
        Thread.sleep(3000);
        EntryReceiver component = (EntryReceiver) getComponent("feedSplitterConsumer");
        assertEquals(25, component.getCount());
    }
}
