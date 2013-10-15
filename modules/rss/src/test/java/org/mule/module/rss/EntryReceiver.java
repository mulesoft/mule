/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
