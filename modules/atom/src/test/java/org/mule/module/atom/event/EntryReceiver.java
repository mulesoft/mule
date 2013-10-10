/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
