/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.rss;

import org.mule.api.annotations.param.Payload;

import com.sun.syndication.feed.synd.SyndFeed;

import java.util.concurrent.atomic.AtomicInteger;

public class FeedReceiver
{
    private AtomicInteger count = new AtomicInteger(0);

    public void readFeed(@Payload SyndFeed feed) throws Exception
    {
        count.set(feed.getEntries().size());            
    }

    public int getCount()
    {
        return count.get();
    }
}
