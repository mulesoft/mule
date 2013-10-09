/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestCaseWatchdog extends Thread
{
    protected static final AtomicInteger threadNumber = new AtomicInteger(0);

    protected final Log log = LogFactory.getLog(getClass());

    protected final long delay;
    protected final TimeUnit unit;
    protected final TestCaseWatchdogTimeoutHandler handler;
    protected volatile boolean timedOut = false;

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
        if (this.delay < 0)
        {
            return;
        }
        
        long millisToWait = this.unit.toMillis(this.delay);
        if (log.isDebugEnabled())
        {
            log.debug("Starting with " + millisToWait + "ms timeout.");
        }

        try
        {
            Thread.sleep(millisToWait);
            timedOut = true;
            if (handler != null)
            {
                handler.handleTimeout(delay, unit);
            }
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

    public boolean isTimedOut()
    {
        return timedOut;
    }
}
