/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.rss;

import org.mule.api.annotations.param.Payload;

import com.sun.syndication.feed.synd.SyndEntry;

import java.util.concurrent.atomic.AtomicInteger;

public class EntryReceiver
{
    private AtomicInteger count = new AtomicInteger(0);

    public void readEntry(@Payload SyndEntry entry) throws Exception
    {
        count.getAndIncrement();
    }

    public int getCount()
    {
        return count.get();
    }
}
