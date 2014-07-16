/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom;

import static org.junit.Assert.assertEquals;

import org.mule.api.client.LocalMuleClient;
import org.mule.module.atom.event.FeedReceiver;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class JmsAtomFeedConsumeTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "jms-atom-consume.xml";
    }

    @Test
    public void testConsumeFeed() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        FeedReceiver component = (FeedReceiver)getComponent("feedConsumer");
        component.getReceivedEntries().set(0);
        String feed = loadResourceAsString("sample-feed.atom");
        client.dispatch("jms://feed.in", feed, null);
        Thread.sleep(2000);
        assertEquals(25, component.getCount());
    }

    @Test
    public void testConsumeSplitFeed() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        FeedReceiver component = (FeedReceiver)getComponent("feedConsumer");
        component.getReceivedEntries().set(0); //reset since the build reports that it's getting incremented someplace else
        String feed = loadResourceAsString("sample-feed.atom");
        client.dispatch("jms://feed.split.in", feed, null);
        Thread.sleep(5000);
        assertEquals(25, component.getCount());
    }
}
