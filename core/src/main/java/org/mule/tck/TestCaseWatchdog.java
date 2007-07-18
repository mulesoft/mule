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
    protected static final Log log = LogFactory.getLog(TestCaseWatchdog.class);
    protected static final AtomicInteger threadNumber = new AtomicInteger(0);

    private final long delay;
    private final TimeUnit unit;

    public TestCaseWatchdog(long delay, TimeUnit unit)
    {
        super("WatchdogThread-" + threadNumber.getAndIncrement());
        this.setDaemon(true);
        this.delay = delay;
        this.unit = unit;
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
            log.fatal("Timeout of " + millisToWait + "ms exceeded - exiting VM!");
            // we should really log a thread dump here to find deadlocks;
            // too bad the required JMX functionality is not in JDK 1.4 :(
            Runtime.getRuntime().halt(1);
        }
        catch (InterruptedException interrupted)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Watchdog stopped.");
            }
            Thread.currentThread().interrupt();
        }
    }

    public void cancel()
    {
        this.interrupt();
    }
}
