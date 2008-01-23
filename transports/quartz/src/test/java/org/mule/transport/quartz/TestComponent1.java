/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.quartz;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.transport.NullPayload;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;

public class TestComponent1 implements Callable
{
    protected static CountDownLatch QUARTZ_COUNTER = new CountDownLatch(4);

    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        if (eventContext.getMessageAsString().equals("quartz test") ||
            (eventContext.getMessage().getPayload() instanceof NullPayload )) 
        {
            if (QUARTZ_COUNTER != null)
            {
                QUARTZ_COUNTER.countDown();
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

    public static void resetCounter()
    {
        QUARTZ_COUNTER = new CountDownLatch(4);
    }
}
