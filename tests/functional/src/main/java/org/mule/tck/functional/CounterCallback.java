/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
