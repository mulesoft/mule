/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
