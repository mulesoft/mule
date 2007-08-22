/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestCaseWatchdog extends Thread
{
    protected static final AtomicInteger threadNumber = new AtomicInteger(0);

    protected final Log log = LogFactory.getLog(getClass());

    protected final long delay;
    protected final TimeUnit unit;
    protected final TestCaseWatchdogTimeoutHandler handler;

    public TestCaseWatchdog(long delay, TimeUnit unit, TestCaseWatchdogTimeoutHandler timeoutHandler)
    {
        super("WatchdogThread-" + threadNumber.getAndIncrement());
        this.setDaemon(true);
        this.delay = delay;
        this.unit = unit;
        this.handler = timeoutHandler;
    }

    public void run()
    {
        long millisToWait = this.unit.toMillis(this.delay);
        if (log.isDebugEnabled())
        {
            log.debug("Starting with " + millisToWait + "ms timeout.");
        }

        try
        {
            Thread.sleep(millisToWait);
            handler.handleTimeout(delay, unit);
        }
        catch (InterruptedException interrupted)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Watchdog stopped.");
            }
        }
    }

    public void cancel()
    {
        this.interrupt();
    }

}
