/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.quartz;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;

import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;

public class TestComponent implements Initialisable, Disposable, Callable
{
    private static CountDownLatch QuartzCounter;

    public static synchronized CountDownLatch getQuartzCounter()
    {
        return QuartzCounter;
    }

    public void initialise()
    {
        TestComponent.QuartzCounter = new CountDownLatch(3);
    }

    public void dispose()
    {
        TestComponent.QuartzCounter = null;
    }

    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        if (eventContext.getMessageAsString().equals("quartz test"))
        {
            if (TestComponent.getQuartzCounter() != null)
            {
                TestComponent.getQuartzCounter().countDown();
            }
            else
            {
                throw new IllegalStateException("QuartzCounter is null!");
            }
        }
        else
        {
            throw new IllegalArgumentException("Unrecognised event payload");
        }
        return null;
    }

}
