/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.component.simple;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Passes on a message only after a specified delay time.  
 * It can be useful for simulating a long-running operation when testing.
 */
public class DelayComponent implements Callable
{
    /** Time to delay (in ms) */
    private int delay = 1000;
    
    private static Log logger = LogFactory.getLog(DelayComponent.class);

    public Object onCall(MuleEventContext context) throws Exception
    {
        logger.debug("Delaying for " + delay + " ms");
        Thread.sleep(delay);
        return context.transformMessage();
    }

    public int getDelay()
    {
        return delay;
    }

    public void setDelay(int delay)
    {
        this.delay = delay;
    }
}
