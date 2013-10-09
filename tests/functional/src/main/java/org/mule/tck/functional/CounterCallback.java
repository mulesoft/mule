/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.functional;

import org.mule.api.MuleEventContext;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A test callback that counts the number of messages received.
 */
public class CounterCallback implements EventCallback
{
    private AtomicInteger callbackCount;

    public CounterCallback()
    {
        callbackCount = new AtomicInteger(0);
    }

    public CounterCallback(AtomicInteger callbackCount)
    {
        this.callbackCount = callbackCount;
    }

    public void eventReceived(MuleEventContext context, Object Component) throws Exception
    {
        incCallbackCount();
    }

    /**
     * Increment callback count.
     * @return current count after increment
     */
    protected int incCallbackCount()
    {
        return callbackCount.incrementAndGet();
    }

    public int getCallbackCount()
    {
        return callbackCount.intValue();
    }
}
