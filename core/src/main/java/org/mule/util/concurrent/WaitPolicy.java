/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.concurrent;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A handler for unexecutable tasks that waits until the task can be submitted for
 * execution or times out. Generously snipped from the jsr166 repository at: <a
 * href="http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/concurrent/ThreadPoolExecutor.java"></a>.
 */
// @Immutable
public class WaitPolicy implements RejectedExecutionHandler
{
    private final long time;
    private final TimeUnit timeUnit;

    /**
     * Constructs a <tt>WaitPolicy</tt> which waits (almost) forever.
     */
    public WaitPolicy()
    {
        // effectively waits forever
        this(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    /**
     * Constructs a <tt>WaitPolicy</tt> with timeout. A negative <code>time</code>
     * value is interpreted as <code>Long.MAX_VALUE</code>.
     */
    public WaitPolicy(long time, TimeUnit timeUnit)
    {
        super();
        this.time = (time < 0 ? Long.MAX_VALUE : time);
        this.timeUnit = timeUnit;
    }

    @SuppressWarnings("boxing")
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e)
    {
        try
        {
            if (e.isShutdown())
            {
                throw new RejectedExecutionException("ThreadPoolExecutor is already shut down");
            }
            else if (!e.getQueue().offer(r, time, timeUnit))
            {
                String message = String.format("ThreadPoolExecutor did not accept within %1d %2s", 
                    time, timeUnit);
                throw new RejectedExecutionException(message);
            }
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException(ie);
        }
    }

}
