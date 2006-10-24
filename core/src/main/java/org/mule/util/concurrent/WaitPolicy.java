/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.concurrent;

import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionHandler;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * A handler for unexecutable tasks that waits until the task can be submitted for
 * execution or times out. Generously snipped from the jsr166 repository at: <a
 * href="http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/concurrent/ThreadPoolExecutor.java"></a>.
 */
// @Immutable
public class WaitPolicy implements RejectedExecutionHandler
{
    private final long _time;
    private final TimeUnit _timeUnit;

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
        _time = (time < 0 ? Long.MAX_VALUE : time);
        _timeUnit = timeUnit;
    }

    public void rejectedExecution(Runnable r, ThreadPoolExecutor e)
    {
        try
        {
            if (e.isShutdown() || !e.getQueue().offer(r, _time, _timeUnit))
            {
                throw new RejectedExecutionException();
            }
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException(ie);
        }
    }

}
