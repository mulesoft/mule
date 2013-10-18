/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.event;

import org.mule.api.annotations.expressions.Expr;
import org.mule.api.annotations.param.Payload;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;

public class EntryReceiver
{

    private AtomicInteger receivedEntries = new AtomicInteger(0);

    public void processEntry(@Payload Entry entry, @Expr("#[header:invocation:feed.object]") Feed feed) throws Exception
    {
        System.out.println("Received " + receivedEntries.incrementAndGet() + " of " + feed.getEntries().size() + " entries");
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
