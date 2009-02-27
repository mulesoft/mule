/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api;

import org.mule.DefaultMuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.util.concurrent.DaemonThreadFactory;

import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.Executor;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.FutureTask;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.TimeoutException;

/**
 * <code>FutureMessageResult</code> is an MuleMessage result of a remote invocation
 * on a Mule Server. This object makes the result available to the client code once
 * the request has been processed. This execution happens asynchronously.
 */
// @ThreadSafe
public class FutureMessageResult extends FutureTask
{
    /**
     * This is a simple default Executor for FutureMessageResults. Instead of
     * spawning a Thread for each invocation it uses a single daemon Thread with an
     * unbounded queue, so "truly" concurrent operation of multiple Futures or
     * otherwise customized execution behaviour requires calling the
     * {@link #setExecutor(Executor)} method and passing in a custom {@link Executor}.
     * This is strongly recommended in order to provide better control over
     * concurrency, resource consumption and possible overruns.
     * <p>
     * Reasons for these defaults:
     * <ul>
     * <li> a daemon thread does not block the VM on shutdown; lifecycle control
     * should be done elsewhere (e.g. the provider of the custom ExecutorService),
     * otherwise this class would become too overloaded
     * <li> a single thread provides for conservative & predictable yet async
     * behaviour from a client's point of view
     * <li> the unbounded queue is not optimal but probably harmless since e.g. a
     * MuleClient would have to create a LOT of Futures for an OOM. Cancelled/timed
     * out invocations are GC'ed so the problem is rather unlikely to occur.
     * </ul>
     */
    private static final Executor DefaultExecutor = Executors.newSingleThreadExecutor(
        new DaemonThreadFactory("MuleDefaultFutureMessageExecutor"));

    // @GuardedBy(this)
    private Executor executor;

    // @GuardedBy(this)
    private List transformers;

    public FutureMessageResult(Callable callable)
    {
        super(callable);
        this.executor = DefaultExecutor;
    }

    /**
     * Set an ExecutorService to run this invocation.
     * 
     * @param e the executor to be used.
     * @throws IllegalArgumentException when the executor is null or shutdown.
     */
    public void setExecutor(Executor e)
    {
        if (e == null)
        {
            throw new IllegalArgumentException("Executor must not be null.");
        }

        synchronized (this)
        {
            this.executor = e;
        }
    }

    /**
     * Set a post-invocation transformer.
     * 
     * @param t Transformers to be applied to the result of this invocation. May be
     *            null.
     */
    public void setTransformers(List t)
    {
        synchronized (this)
        {
            this.transformers = t;
        }
    }

    public MuleMessage getMessage() throws InterruptedException, ExecutionException, TransformerException
    {
        return this.getMessage(this.get());
    }

    public MuleMessage getMessage(long timeout)
        throws InterruptedException, ExecutionException, TimeoutException, TransformerException
    {
        return this.getMessage(this.get(timeout, TimeUnit.MILLISECONDS));
    }

    private MuleMessage getMessage(Object obj) throws TransformerException
    {
        MuleMessage result = null;
        if (obj != null)
        {
            if (obj instanceof MuleMessage)
            {
                result = (MuleMessage)obj;
            }
            else
            {
                result = new DefaultMuleMessage(obj);
            }

            synchronized (this)
            {
                if (transformers != null)
                {
                    result.applyTransformers(transformers);
                }
            }

        }
        return result;
    }

    /**
     * Start asynchronous execution of this task
     */
    public void execute()
    {
        synchronized (this)
        {
            executor.execute(this);
        }
    }

}
