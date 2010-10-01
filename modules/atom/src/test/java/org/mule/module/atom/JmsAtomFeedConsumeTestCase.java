/*
 * $Id:  $
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom;

import org.mule.api.client.LocalMuleClient;
import org.mule.module.atom.event.EntryReceiver;
import org.mule.module.atom.event.FeedReceiver;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.CounterCallback;

public class JmsAtomFeedConsumeTestCase extends FunctionalTestCase
{
    private final CounterCallback counter = new CounterCallback();

    @Override
    protected String getConfigResources()
    {
        return "jms-atom-consume.xml";
    }

    @Override
    protected void doTearDown() throws Exception
    {
        FeedReceiver.receivedEntries.set(0);
    }

    public void testConsumeFeed() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        String feed = loadResourceAsString("sample-feed.atom");
        client.dispatch("jms://feed.in", feed, null);
        Thread.sleep(2000);
        assertEquals(25, FeedReceiver.receivedEntries.get());
    }

    public void testConsumeSplitFeed() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        String feed = loadResourceAsString("sample-feed.atom");
        client.dispatch("jms://feed.split.in", feed, null);
        Thread.sleep(2000);                
        assertEquals(25, EntryReceiver.receivedEntries.get());
    }
}
