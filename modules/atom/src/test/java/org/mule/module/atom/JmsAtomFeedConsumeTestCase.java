/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.atom;

import org.mule.api.client.LocalMuleClient;
import org.mule.module.atom.event.FeedReceiver;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JmsAtomFeedConsumeTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
