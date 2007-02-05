/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.quartz;

import org.mule.providers.NullPayload;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;

public class TestComponent implements Callable
{
    protected static final CountDownLatch QuartzCounter = new CountDownLatch(4);

    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        if (eventContext.getMessageAsString().equals("quartz test") ||
            (eventContext.getMessage().getPayload() instanceof NullPayload )) 
        {
            if (QuartzCounter != null)
            {
                QuartzCounter.countDown();
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
