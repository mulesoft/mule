/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss;

import com.sun.syndication.feed.synd.SyndEntry;

import java.util.concurrent.atomic.AtomicInteger;

public class RssEntryCounterComponent
{
    private AtomicInteger count = new AtomicInteger(0);

    public void readFeed(SyndEntry entry) throws Exception
    {
        System.out.println(entry.getTitle());
        count.getAndIncrement();
    }

    public int getCount()
    {
        return count.get();
    }
}
