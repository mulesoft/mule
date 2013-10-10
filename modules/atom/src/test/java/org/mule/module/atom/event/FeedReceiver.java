/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.atom.event;

import org.mule.api.annotations.param.Payload;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.abdera.model.Feed;

public class FeedReceiver
{

    private final AtomicInteger receivedEntries = new AtomicInteger(0);

    public void processFeed(@Payload Feed feed) throws Exception
    {
        receivedEntries.set(0);
        System.out.println("Received " + feed.getEntries().size() + " events");
        receivedEntries.set(feed.getEntries().size());
    }

    public int getCount()
    {
        return receivedEntries.get();
    }

    public AtomicInteger getReceivedEntries()
    {
        return receivedEntries;
    }
}
