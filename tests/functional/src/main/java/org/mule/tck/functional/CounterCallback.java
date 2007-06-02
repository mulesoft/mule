/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.functional;

import org.mule.umo.UMOEventContext;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

/**
 * A test callback that counts the number of messages received.
 */
public class CounterCallback implements EventCallback
{
    private AtomicInteger callbackCount = new AtomicInteger(0);

    public void eventReceived(UMOEventContext context, Object Component) throws Exception
    {
        incCallbackCount();
    }

    protected void incCallbackCount()
    {
        callbackCount.incrementAndGet();
    }

    public int getCallbackCount()
    {
        return callbackCount.intValue();
    }
}