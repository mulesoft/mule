/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.rss;

import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JmsRssFeedConsumeTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
