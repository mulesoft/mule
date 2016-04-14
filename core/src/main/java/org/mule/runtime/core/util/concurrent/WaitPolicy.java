/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
